/* ***********************************************************************
 * VMware ThinApp Factory
 * Copyright (c) 2009-2013 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ***********************************************************************/

package com.vmware.thinapp.workpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

import javax.annotation.Resource;

import org.hibernate.Hibernate;
import org.hibernate.util.SerializationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.google.common.eventbus.EventBus;
import com.vmware.thinapp.workpool.dao.InstanceRepository;
import com.vmware.thinapp.workpool.dao.LeaseRepository;
import com.vmware.thinapp.workpool.dao.VCConfigRepository;
import com.vmware.thinapp.workpool.dao.WorkpoolRepository;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.LeaseModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;
import com.vmware.vim25.mo.ServiceInstance;

import akka.actor.TypedActor;
import akka.dispatch.CompletableFuture;
import akka.dispatch.DefaultCompletableFuture;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.japi.Procedure;

public class WorkpoolInstanceImpl extends TypedActor implements WorkpoolInstance {
   private static final Logger log = LoggerFactory.getLogger(WorkpoolInstanceImpl.class);

   @Autowired private VCManager vcManager;
   @Autowired private InstanceRepository instanceDao;
   @Autowired private WorkpoolRepository workpoolDao;
   @Autowired private VCConfigRepository vcConfigDao;
   @Autowired private LeaseRepository leaseDao;
   @Resource(name = "workpoolTransactionTemplate")
   private TransactionTemplate txn;

   private final long workpoolId;
   private final Instancer instancer;
   // We have to wait for the instancer to become available before we can request
   // instances to be added.

   // Clone can be in progress, idle, or in use.
   private final Queue<CompletableFuture<LeaseModel>> waiters = new LinkedBlockingDeque<CompletableFuture<LeaseModel>>();
   private final EventBus eventBus = new EventBus();
   private WorkpoolInstance self;

   public WorkpoolInstanceImpl(WorkpoolModel newWorkpool, Instancer instancer) {
      // Need to prepare by creating a snapshot in vmImage for our own use during initial phase.
      this.workpoolId = newWorkpool.getId();
      this.instancer = instancer;
   }

   @Override
   public void subscribe(Object observer) {
      eventBus.register(observer);
      WorkpoolModel workpoolModel = getWorkpoolModel();
      // Sends current state directly to listener when subscribing to sync up.
      eventBus.post(new WorkpoolStateChange(workpoolModel.getState(), workpoolModel.getState(), self));
   }

   @Override
   public void unsubscribe(Object observer) {
      eventBus.unregister(observer);
   }

   @Override
   public Long getId() {
      return workpoolId;
   }

   @Override
   public Future<Void> delete() {
      WorkpoolModel workpool = getWorkpoolModel();
      if (!workpool.getState().isDeletable()) {
         throw new WorkpoolException(
                 "Cannot delete in the current state: " + workpool.getState().name() + ".");
      }

      setState(WorkpoolModel.State.deleting);
      process();
      return future(null);
   }

   @Override
   public Future<Void> forget() {
      WorkpoolModel workpool = getWorkpoolModel();
      if (!workpool.getState().isDeletable()) {
         throw new WorkpoolException(
                 "Cannot delete in the current state: " + workpool.getState().name() + ".");
      }

      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            WorkpoolModel workpool = getLocalModel();
            workpool.getInstances().clear();
            workpoolDao.update(workpool);
         }
      });
      setState(WorkpoolModel.State.deleting);
      process();
      return future(null);
   }

   @Override
   public Future<Void> reset() {
      // Only allow an available workpool to be reset
      //XXX: This approach sucks, we're doing one transaction/copy just to check the state,
      //     then next we run another transaction just to set the state, THEN we do another
      //     transaction/copy in process().  There is probably a better way.
      WorkpoolModel workpool = getWorkpoolModel();
      if (workpool.getState() != WorkpoolModel.State.available) {
         throw new WorkpoolException("Cannot reset a workpool that is not available");
      }

      setState(WorkpoolModel.State.resetting);
      process();
      return future(null);
   }

   @Override
   public InstanceModel getInstance(final long instanceId) {
      return txn.execute(new TransactionCallback<InstanceModel>() {
         @Override
         public InstanceModel doInTransaction(TransactionStatus status) {
            InstanceModel model = instanceDao.get(instanceId);
            return model;
         }
      });
   }

   @Override
   public Future<InstanceModel> addInstance(final InstanceModel instance) {
      InstanceModel model = txn.execute(new TransactionCallback<InstanceModel>() {
         @Override
         public InstanceModel doInTransaction(TransactionStatus status) {
            instance.setWorkpool(getLocalModel());
            instance.setState(InstanceModel.State.available);
            instanceDao.save(instance);
            return instance;
         }
      });

      return future(model);
   }

   @Override
   public Future<Void> removeInstance(final long instanceId) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            InstanceModel model = instanceDao.get(instanceId);
            instanceDao.delete(model);
         }
      });

      return future(null);
   }

   @Override
   public void deleteInstance(final InstanceModel instance) {
      log.info("Deleting instance: {}.", instance);

      // XXX: Blocking.
      final ServiceInstance si = vcManager.getConnection().get().get();
      instance.setState(InstanceModel.State.deleting);
      instanceDao.update(instance);
      Futures.future(new Callable<Void>() {
         @Override
         public Void call() {
            if (instance.getMoid() != null) {
               VirtualMachineHelper vm = new VirtualMachineHelper(si, instance.getMoid());
               log.info("Powering off instance ({}, {})...", instance.getId(), instance.getMoid());
               vm.powerOff();
               log.info("Deleting instance...");
               vm.delete();
               log.info("Delete finished.");
            }
            return null;
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<Void>>() {
         @Override
         public void apply(Future<Void> param) {
            self._onInstanceRemoved(param, instance);
         }
      });
   }

   /**
    * Callback once backing VM is deleted so model can be removed.
    *
    * @param param
    * @param instance
    */
   @Override
   public void _onInstanceRemoved(final Future<Void> param, final InstanceModel instance) {
      if (param.result().isDefined()) {
         removeLocalModel(instance);
         process();
      } else {
         errorLocalModel(instance, param.exception().get());
      }
   }

   private void removeLocalModel(final InstanceModel instance) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            log.info("Removing instance model: {}.", instance);
            WorkpoolModel workpool = getLocalModel();
            workpool.getInstances().remove(instance);
            workpoolDao.update(workpool);
         }
      });

      process();
   }

   private void errorLocalModel(final InstanceModel instance, final Throwable error) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            log.error("Remove instance failed.", error);
            instance.setState(InstanceModel.State.deleteFailed);
            instanceDao.update(instance);
         }
      });

      process();
   }

   /**
    * Callback once all VMs have been powered down during a reset.
    *
    * @param param
    */
   @Override
   public void _onResetComplete(Future<Void> param) {
      // Set the state back to available
      setState(WorkpoolModel.State.available);
      process();
   }

   /**
    * Acquire a VM lease
    *
    * @return Future that is resolved when a lease is available (note that it is returned as an Object due to akka
    *         Future handling in typed actors
    */
   @Override
   public FutureWrapper<LeaseModel> acquire() {
      CompletableFuture<LeaseModel> f = new DefaultCompletableFuture<LeaseModel>(Long.MAX_VALUE);
      log.debug("Adding new waiter: {}.", f);
      waiters.add(f);
      log.debug("Current waiters: {}.", waiters);
      process();
      return new FutureWrapper<LeaseModel>(f);
   }

   /**
    * Add an additional instance to the workpool.
    *
    * @param workpool workpool model to add the instance to
    * @return runnable to run after the transaction commits
    */
   private Runnable addInstance(final WorkpoolModel workpool) {
      // We go ahead and add it to the list here so that we keep track of both completed
      // clones and those in flight.
      final InstanceModel instance = new InstanceModel();
      instance.setWorkpool(workpool);
      // XXX: The username and password can't be null so give them dummy values for now.
      instance.setGuestUsername("CHANGEME");
      instance.setGuestPassword("CHANGEME");
      instance.setState(InstanceModel.State.instancing);
      workpool.getInstances().add(instance);

      // I thought calling update() on the workpool would cascade to a save of
      // the instance but it doesn't appear to always work so that's why there is
      // an explicit save here.
      instanceDao.save(instance);
      workpoolDao.update(workpool);

      return new Runnable() {
         @Override
         public void run() {
            InstanceModel instanceRef = new InstanceModel();
            instanceRef.setId(instance.getId());

            Future<CloneRunner.Result> future = instancer.addInstance(instanceRef).get();
            future.onComplete(new Procedure<Future<CloneRunner.Result>>() {
               @Override
               public void apply(Future<CloneRunner.Result> param) {
                  // Random note: if an exception is thrown here it's lost and not even logged???
                  self._onInstancingComplete(param, instance);
               }
            });
         }
      };
   }

   /**
    * Create a lease for the given instance.
    *
    * @param instance
    * @return runnable to run after the transaction commits
    */
   private Runnable completeLease(final InstanceModel instance, final CompletableFuture<LeaseModel> item) {
      // A instance is available for leasing.
      final WorkpoolModel workpool = workpoolDao.get(workpoolId);

      final LeaseModel lease = new LeaseModel();
      lease.setInstance(instance);
      lease.setVcConfig(vcConfigDao.getConfig());
      workpool.getLeases().add(lease);

      instanceDao.save(instance);
      workpoolDao.update(workpool);

      return new Runnable() {
         @Override
         public void run() {
            // Completes an acquire request with an existing lease.
            item.completeWithResult(lease);
            log.debug("Completed waiter {} with result {}.", item, lease);
         }
      };
   }

   @Override
   public WorkpoolModel getWorkpoolModel() {
      return txn.execute(new TransactionCallback<WorkpoolModel>() {
         @Override
         public WorkpoolModel doInTransaction(TransactionStatus transactionStatus) {
            WorkpoolModel model = getLocalModel();
            // Eagerly load instances since they will be accessed out of session.
            Hibernate.initialize(model.getInstances());
            return (WorkpoolModel) SerializationHelper.clone(model);
         }
      });
   }

   /**
    * Callback for when a instance is finished being created.
    *
    * @param result
    * @param instance
    */
   @Override
   public void _onInstancingComplete(final Future<CloneRunner.Result> result, final InstanceModel instance) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            log.info("Instancing completed.");
            instanceDao.refresh(instance);
            WorkpoolModel workpool = workpoolDao.get(workpoolId);

            try {
               CloneRunner.Result res = result.get();

               if (res.isSuccess()) {
                  instance.setMoid(res.getMoid());
                  instance.setState(InstanceModel.State.available);
                  instanceDao.update(instance);
                  setState(getLocalModel().getState());
               } else {
                  throw new RuntimeException(res.getError());
               }
            } catch (Exception e) {
               String message = String.format("Instancing failed - %s.",
                                              e.getMessage());
               log.error(message, e);
               // the instance itself is going to be removed next and can't
               // be used to convey the last error message. Has to set the
               // error on the workpool itself.
               //
               // Q: Should we define a workpoolmodel state to indicating
               // an instancing failure or should we hold the instance
               // until user manually remove it after retrieve the error?
               setState(getLocalModel().getState(), message);
               if (!workpool.getInstances().remove(instance)) {
                  throw new RuntimeException("Unable to remove instance: " + instance);
               }
            }
         }
      });

      process();
   }

   @Override
   public void postStop() {
      instancer.unsubscribe(self);
      TypedActor.stop(instancer);
   }

   @Override
   public void preStart() {
      self = getContext().getSelfAs();
      instancer.subscribe(self);

      // Move all instances from any running state to a failed state.
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            WorkpoolModel workpool = getLocalModel();
            workpool.getLeases().clear();
            WorkpoolModel.State state = workpool.getState().getFailedStateOrSelf();
            setState(state);

            Iterator<InstanceModel> instanceIter = workpool.getInstances().iterator();

            while (instanceIter.hasNext()) {
               InstanceModel instance = instanceIter.next();
               instance.setState(instance.getState().getFailedStateOrSelf());
               switch (instance.getState()) {
                  // XXX: In the instancing case we don't yet have a moid to delete
                  // the underlying VM so the best we can do is delete the instance.
                  case instancingFailed:
                  // XXX: We don't handle failed well.  Might should retry?
                  case deleteFailed:
                     log.debug("Removing instance: {}", instance);
                     instanceIter.remove();
                     break;
                  case instancing:
                  case deleting:
                  case created:
                  case available:
               }
            }

            workpoolDao.update(workpool);
         }
      });

      // Process in case anything has changed while we weren't running.
      process();
   }

   /** Processes waiters to fulfill workpool requests. */
   private void process() {
      log.debug("Processing workpool.");

      // The idea here is that the transaction usually needs to commit before
      // commencing with an action so we can return an arbitrary runnable that
      // is called after the transaction commits.
      Runnable runnable = txn.execute(new TransactionCallback<Runnable>() {
         // Note that if any one of our runnables throws an exception then
         // the rest of the runnables won't be run.  This would be a
         // disaster because records have already been committed to the
         // database.  Therefore runnables should be as simple as possible.
         RunnableList runnables = new RunnableList();

         @Override
         public Runnable doInTransaction(TransactionStatus transactionStatus) {
            WorkpoolModel workpool = getLocalModel();

            switch (workpool.getState()) {
               case created:
                  break;
               case available:
                  reclaimOverallocatedInstances(workpool);
                  processWaiters(workpool);
                  break;
               case unavailable:
                  break;
               case deleted:
                  break;
               case deleting:
                  failWaiters("Workpool is being deleted.");
                  processDeletion(workpool);
                  break;
               case resetting:
                  failWaiters("Workpool is resetting.");
                  processReset(workpool);
                  break;
               case waitingForInstancer:
                  break;
               case deleteFailed:
                  break;
            }

            return runnables;
         }

         /**
          * Immediately fail all current waiters by completing them all with an
          * exception.
          *
          * @param msg message to include in the exception that the waiter is failed with
          */
         private void failWaiters(String msg) {
            Iterator<CompletableFuture<LeaseModel>> waiterIter = waiters.iterator();

            while (waiterIter.hasNext()) {
               CompletableFuture<LeaseModel> waiter = waiterIter.next();
               waiter.completeWithException(new WorkpoolException(msg));
               waiterIter.remove();
            }
         }

         /**
          * Reclaims any surplus instances that aren't in use that are over
          * the maximum number of configured instances.
          *
          * @param workpool workpool model
          */
         private void reclaimOverallocatedInstances(WorkpoolModel workpool) {
            assert workpool.getState() == WorkpoolModel.State.available;

            if (!workpool.isGrowable()) {
               // Don't attempt to reap instances for a custom workpool.
               return;
            }

            // Find any extra instances.  Note that this just finds instances that
            // are in the available state as any instances we previously
            // reaped may be in the deleting state.  Conceivably we could also reap
            // instances that are in the process of instancing but that's a little
            // more complicated as onInstancingComplete logic would have to
            // compensate for that case.
            List<InstanceModel> freeInstances = workpoolDao.getFreeInstances(workpool);
            int numberOverallocated = freeInstances.size() - workpool.getMaximumInstances();
            List<InstanceModel> toReap = new ArrayList<InstanceModel>();

            if (numberOverallocated > 0) {
               log.debug("{} instances are overallocated.", numberOverallocated);
               // Can only reap up to either the number of overallocated
               // or the number of free instances.
               int reapable = Math.min(numberOverallocated, freeInstances.size());
               log.debug("Number of reapable instances: {}.", reapable);
               toReap.addAll(freeInstances.subList(0, reapable));
            }

            log.debug("Reaping instances: {}", toReap);

            for (InstanceModel i : toReap) {
               deleteInstance(i);
            }
         }

         /**
          * Attempt to satisfy waiters by either leasing out existing instances
          * or spinning up new ones as needed.
          *
          * @param workpool workpool model
          */
         private void processWaiters(WorkpoolModel workpool) {
            assert workpool.getState() == WorkpoolModel.State.available;

            int countOfInstancing = 0;
            for (InstanceModel instance : workpool.getInstances()) {
               if (instance.getState() == InstanceModel.State.instancing) {
                  countOfInstancing ++;
               }
            }

            Iterator<CompletableFuture<LeaseModel>> waiterIter = waiters.iterator();

            // Use iterator interface so we can remove elements while iterating.
            while (waiterIter.hasNext()) {
               CompletableFuture<LeaseModel> waiter = waiterIter.next();

               // Try to satisfy waiters with existing available instances.
               if (tryExistingInstances(waiter, workpool)) {
                  log.info("Completed waiter with a free lease.");
                  waiterIter.remove();
                  // Satisfy any additional waiters by spinning up new instances.
               } else if (countOfInstancing > 0) {
                  /*
                   * There seems to have a race condition here. Namely, when
                   * the code reach here, the actual VM in instancing state
                   * might have already changed due to some instancing tasks
                   * have just completed and we might miss this "available"
                   * VM and wait it forever (until use start another capture).
                   *
                   * This race condition should need to be worried. In case
                   * of a instancing VM becomes available, the
                   * _onInstancingCompleted() method will always call the
                   * process() (which will trigger another waiters scan) after
                   * it change the instance's state to available. Therefore,
                   * even if we missed this available in this round, it will
                   * be picked up immediately be the next process() call.
                   */
                  countOfInstancing --;
               } else if (tryForNewInstances(waiter, workpool)) {
                  log.info("Initiated spinup of new instance to satisfy waiter.");
               } else {
                  // Waiter's gonna wait.  We've completed all leases and spun up as many VMs as possible so stop
                  // processing;
                  break;
               }
            }
         }

         /**
          * Try to complete the waiter (in the future) by spinning up a new instance.
          *
          * @param waiter
          * @param workpool
          * @return true if able to spin up a new instance, otherwise false
          */
         private boolean tryForNewInstances(CompletableFuture<LeaseModel> waiter, WorkpoolModel workpool) {
            if (!getWorkpoolModel().isGrowable()) {
               return false;
            }

            if (workpool.getInstances().size() >= workpool.getMaximumInstances()) {
               log.info("Workpool is already at maximum capacity.");
               return false;
            } else {
               runnables.add(addInstance(workpool));
               return true;
            }
         }

         /**
          * Try to complete the waiter by supplying it an existing unused instance.
          *
          * @param waiter
          * @param workpool
          * @return true if waiter was completed, otherwise false
          */
         private boolean tryExistingInstances(CompletableFuture<LeaseModel> waiter, WorkpoolModel workpool) {
            Collection<InstanceModel> freeInstances = workpoolDao.getFreeInstances(workpool);

            for (InstanceModel instance : freeInstances) {
               if (isVmExistent(instance)) {
                  runnables.add(completeLease(instance, waiter));
                  return true;
               } else {
                  log.debug("VM {} not found in VC. Clean it up.", instance.getMoid());
                  deleteInstance(instance);
                  // TODO: use removeLocalModel(instance);
               }
            }

            return false;
         }

         private boolean isVmExistent(InstanceModel instance) {
             final ServiceInstance si = vcManager.getConnection().get().get();
             VirtualMachineHelper vm = new VirtualMachineHelper(si,
                                                  instance.getMoid());
             return vm.isExistent();
         }
      });

      runnable.run();
   }

   /**
    * Process workpool deleting and deleted states.
    *
    * @param workpool
    */
   private void processDeletion(WorkpoolModel workpool) {
      assert workpool.getState() == WorkpoolModel.State.deleting;

      log.debug("Processing deletion.");

      if (CollectionUtils.isEmpty(workpool.getInstances())) {
         log.debug("Number of instances has reached 0.  Deleting workpool.");
         setState(WorkpoolModel.State.deleted);
         txn.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
               WorkpoolModel model = getLocalModel();
               workpoolDao.delete(model);
            }
         });
      } else {
         // Clear out all leases.
         log.debug("All leases [{}] cleared.", workpool.getLeases());
         workpool.getLeases().clear();

         for (final InstanceModel i : workpool.getInstances()) {
            switch (i.getState()) {
               case created:
               case available:
               case instancingFailed:
               case deleteFailed:
                  deleteInstance(i);
                  break;
               case deleting:
               case instancing:
            }
         }
      }
   }

   /**
    * Process workpool reset.
    *
    * @param workpool
    */
   private void processReset(WorkpoolModel workpool) {
      assert workpool.getState() == WorkpoolModel.State.resetting;

      log.debug("Processing reset.");

      // Clear out all leases.
      log.debug("All leases [{}] cleared.", workpool.getLeases());
      workpool.getLeases().clear();

      // Power off all instances
      powerOffInstances();
   }

   private void powerOffInstances() {
      log.info("Powering off all instances.");

      final Set<InstanceModel> instances = getWorkpoolModel().getInstances();

      // XXX: Blocking.
      final ServiceInstance si = vcManager.getConnection().get().get();
      Futures.future(new Callable<Void>() {
         @Override
         public Void call() {
            for (InstanceModel instance : instances) {
               if (instance.getMoid() != null) {
                  VirtualMachineHelper vm = new VirtualMachineHelper(si, instance.getMoid());
                  log.info("Powering off instance ({}, {})...", instance.getId(), instance.getMoid());
                  vm.powerOff();
                  log.info("Instance powered off.");
               }
            }
            return null;
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<Void>>() {
         @Override
         public void apply(Future<Void> param) {
            self._onResetComplete(param);
         }
      });
   }

   @Override
   public Future<Void> release(final LeaseModel lease) {
      log.debug("Releasing lease: {}.", lease);
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            // Load a real copy of the lease (probably just has id).
            LeaseModel localLease = leaseDao.get(lease.getId());
            WorkpoolModel workpool = workpoolDao.get(workpoolId);

            if (!workpool.getLeases().contains(lease)) {
               // Note: We might have cleared out leases on deletion and then
               // the client calls release and we throw because we don't know
               // anything about it anymore.  Not sure if it matters or not.
               throw new RuntimeException("Lease " + lease + " is not present.");
            }

            log.info("Releasing lease: {}.", localLease);

            if (!workpool.getLeases().remove(localLease)) {
               throw new RuntimeException("Unable to remove instance " + localLease + ".");
            }

            workpoolDao.update(workpool);
         }
      });

      process();
      return future(null);
   }

   @Override
   public void update(InstancerStateChange stateChange) {
      log.debug("Received instancer status change: {}.", stateChange);
      Instancer.State instancerState = stateChange.getNewState();

      // Set workpool state to available if its instancer becomes available.
      switch (instancerState) {
         case available:
            setState(WorkpoolModel.State.available);
            break;
         case unavailable:
            setState(WorkpoolModel.State.waitingForInstancer);
            break;
         case failed:
            setState(WorkpoolModel.State.unavailable);
      }

      process();
   }

   @Override
   public Future<Void> updateModel(final WorkpoolModel model) {
      log.debug("Updating workpool: {}.", model);

      if (model.getMaximumInstances() < 0) {
         throw new IllegalArgumentException("Workpool size cannot be less than zero.");
      }

      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            WorkpoolModel existing = getLocalModel();
            existing.setName(model.getName());
            existing.setMaximumInstances(model.getMaximumInstances());
            workpoolDao.update(existing);
         }
      });
      // Process in case maximum instances changed so instances can
      // be adjusted.
      process();
      return future(null);
   }

   /**
    * Set instance to a new state.
    *
    * @param state new state
    */
   private void setState(final WorkpoolModel.State state) {
      setState(state, "");
   }

   private void setState(final WorkpoolModel.State state, final String lastError) {
      WorkpoolModel.State originalState = txn.execute(new TransactionCallback<WorkpoolModel.State>() {
         @Override
         public WorkpoolModel.State doInTransaction(TransactionStatus status) {
            WorkpoolModel model = getLocalModel();
            WorkpoolModel.State originalState = model.getState();
            model.setState(state);
            model.setLastError(lastError);
            return originalState;
         }
      });

      log.info("Transitioned from state {} to {}.", originalState, state);
      WorkpoolStateChange change = new WorkpoolStateChange(originalState, state, self);
      eventBus.post(change);
   }

   private WorkpoolModel getLocalModel() {
      return workpoolDao.get(workpoolId);
   }
}
