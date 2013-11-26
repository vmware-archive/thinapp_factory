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

import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.hibernate.util.SerializationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.google.common.eventbus.EventBus;
import com.vmware.thinapp.workpool.dao.InstanceableRepository;
import com.vmware.thinapp.workpool.dao.VmImageRepository;
import com.vmware.thinapp.workpool.exceptions.InvalidStateException;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;

import akka.actor.TypedActor;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.japi.Procedure;

public class VmImageInstanceImpl extends TypedActor implements VmImageInstance {
   private static final Logger log = LoggerFactory.getLogger(VmImageInstanceImpl.class);

   @Autowired
   private VCManager vcManager;
   @Autowired
   private InstanceableRepository instanceableDao;
   @Autowired
   private VmImageRepository vmImageDao;
   @Autowired
   private Util util;
   @Resource(name = "workpoolTransactionTemplate")
   private TransactionTemplate txn;

   private VmImageInstance self;
   private long vmImageId;
   private final EventBus eventBus = new EventBus();

   public VmImageInstanceImpl(VmImageModel vm) {
      this.vmImageId = vm.getId();
   }

   @Override
   public long getId() {
      return vmImageId;
   }

   @Override
   public void subscribe(Object observer) {
      eventBus.register(observer);
      VmImageModel vmImage = getVmImage();
      // Sends current state directly to listener when subscribing to sync up.
      VmImageStateChange change = new VmImageStateChange(vmImage.getState(), vmImage.getState(), self);
      eventBus.post(change);
   }

   @Override
   public void unsubscribe(Object observer) {
      eventBus.unregister(observer);
   }

   @Override
   public void preStart() {
      self = getContext().getSelfAs();

      // If instance is in a running state move it to its corresponding failed
      // state.  Otherwise it will stay in that running state forever.
      VmImageModel vmImage = getVmImage();
      VmImageModel.State state = vmImage.getState().getFailedStateOrSelf();
      setState(state);

      process();
   }

   private void setState(final VmImageModel.State state) {
      setState(state, "");
   }

   private void setState(final VmImageModel.State state, final String lastError) {
      VmImageModel.State originalState = txn.execute(new TransactionCallback<VmImageModel.State>() {
         @Override
         public VmImageModel.State doInTransaction(TransactionStatus status) {
            VmImageModel vmImage = getLocalImage();
            VmImageModel.State originalState = vmImage.getState();
            vmImage.setState(state);
            vmImage.setLastError(lastError);
            vmImageDao.update(vmImage);
            return originalState;
         }
      });

      log.info("Transitioned from state {} to {}.", originalState, state);
      VmImageStateChange change = new VmImageStateChange(originalState, state, self);
      eventBus.post(change);
   }

   /**
    * Callback for installation completion.
    *
    * @param param result of the installation
    * @param si service instance
    */
   @Override
   public void _onInstallationFinished(final Future<InstallRunner.Result> param, ServiceInstance si) {
      try {
         final InstallRunner.Result result = param.get();
         if (!result.isSuccess()) {
            throw new RuntimeException(result.getError());
         }

         txn.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
               VmImageModel vmImage = getLocalImage();
               vmImage.setMoid(result.getMoid());
               instanceableDao.saveOrUpdate(vmImage);
            }
         });

         log.info("Installation was successful.");
         setState(VmImageModel.State.installFinished);
      } catch (Exception e) {
         String message = String.format("Installation was unsuccessful - %s.",
                                        e.getMessage());
         log.error(message, e);
         setState(VmImageModel.State.installFailed, message);
      }

      process();
   }

   @Override
   public VmImageModel getVmImage() {
      return txn.execute(new TransactionCallback<VmImageModel>() {
         @Override
         public VmImageModel doInTransaction(TransactionStatus status) {
            // Return a cloned instance so that the receiver has a snapshot
            // of the image at this point in time.  Otherwise it may change
            // behind their back.
            VmImageModel vmImage = getLocalImage();
            return (VmImageModel) SerializationHelper.clone(vmImage);
         }
      });
   }

   protected InstallRunner getInstallerRunner(InstallRequest request) {
      return (InstallRunner) util.appCtxt.getAutowireCapableBeanFactory().getBean("installRunner", request);
   }

   /**
    * Initiate automatic OS installation to the VM image.
    *
    * @param si
    */
   public void install(final ServiceInstance si) {
      log.info("Requesting an installation.");

      VmImageModel vmImage = getVmImage();
      VCConfigModel config = vcManager.getConfig();
      final InstallRequest request = new InstallRequest(vmImage.getName(), config, vmImage.getVmPattern(),
              HardwareConfiguration.DEFAULT, config.getVmLocation(), Constants.GUEST_PASSWORD /* this password will
              just be reset when cloning */);
      final InstallRunner runner = getInstallerRunner(request);

      // Note that we currently have no way to receive notification that a future reached
      // its timeout so we just use max for now.  See:
      // https://www.assembla.com/spaces/akka/tickets/904-add--ontimeout--callback-to-futures
      Futures.future(new Callable<InstallRunner.Result>() {
         @Override
         public InstallRunner.Result call() throws Exception {
            return runner.run();
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<InstallRunner.Result>>() {
         @Override
         public void apply(Future<InstallRunner.Result> param) {
            self._onInstallationFinished(param, si);
         }
      });
   }

   /**
    * Callback for base snapshot creation completion.
    *
    * @param param used only to communicate exceptions
    */
   @Override
   public void _onCreateCloningSnapshotFinished(Future<Void> param) {
      try {
         param.get();
         log.info("Successfully created base cloning snapshot or it already existed.");
         setState(VmImageModel.State.snapshotted);
      } catch (Exception e) {
         log.error(String.format(
             "Failed to create base cloning snapshot: %s.", e.getMessage()), e);
         setState(VmImageModel.State.snapshotFailed, e.getMessage());
      }

      process();
   }

   /**
    * Common function to be called before any kind of deletion.
    * <p/>
    * We have to check that no workpools currently use this instance. We set the state to deleting, which isn't really
    * necessary for the case where we're just doing a forget since the state will be set to deleted before the forget
    * function returns anyway.
    *
    * @param state state to set the image to after doing all state checks
    */
   private void trySetDeleting(final VmImageModel.State state) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            VmImageModel vmImage = getLocalImage();
            if (!vmImage.getState().isDeletable()) {
               throw new InvalidStateException(
                       "Cannot delete in the current state: " + vmImage.getState().name() + ".");
            }

            setState(state);
         }
      });
   }

   @Override
   public Future<Void> delete() {
      log.debug("Backing VM is being deleted.");
      trySetDeleting(VmImageModel.State.deleteRequested);
      process();
      return future(null);
   }

   @Override
   public Future<Void> forget() {
      trySetDeleting(VmImageModel.State.deleted);
      process();
      return future(null);
   }

   /**
    * Callback once the backing VM is finished deleting.
    */
   @Override
   public void _onBackingVmDeletionFinished() {
      setState(VmImageModel.State.deleted);
      process();
   }

   /**
    * Called once all necessary cleanup is done (such as deleting the VM if requested) to delete the image model entry
    * and actor.
    */
   private void deleteInstance() {
      log.info("Deleting image instance.");
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            VmImageModel vmImage = getLocalImage();
            vmImageDao.delete(vmImage);
         }
      });
   }

   /**
    * Delete the real VM from vSphere.
    *
    * @param si
    * @param vmImage
    */
   private void deleteBackingVm(final ServiceInstance si, final VmImageModel vmImage) {
      Futures.future(new Callable<Void>() {
         @Override
         public Void call() {
            VirtualMachineHelper vm = new VirtualMachineHelper(si, vmImage.getMoid());
            // VM must be powered off before deleting.
            vm.powerOff();
            vm.delete();
            return null;
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<Void>>() {
         @Override
         public void apply(Future<Void> future) {
            self._onBackingVmDeletionFinished();
         }
      });
   }

   /**
    * Prepare either a newly installed VM or imported VM for use.
    * <p/>
    * In order to be able to clone a VM there must exist a "ThinApp Cloning Snapshot" base snapshot to clone off of.
    *
    * @param si
    * @param vmMo
    */
   private void createBaseSnapshot(final ServiceInstance si, final VirtualMachine vmMo) {
      if (!Util.vmExists(vmMo)) {
         log.error("The VM with moid {} was not found.", vmMo.getMOR().getVal());
         setState(VmImageModel.State.vmDoesNotExist);
         return;
      }

      Futures.future(new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            log.info("Creating base snapshot for VM: {}.", vmMo.getName());

            VirtualMachineHelper vmHelper = new VirtualMachineHelper(si, vmMo);
            VirtualMachineSnapshot snapshot = vmHelper.findSnapshot(Constants.THINAPP_CLONING_SNAPSHOT);

            if (snapshot == null) {
               log.info("Cloning snapshot does not exist, creating...");
               Task newSnapshot = vmMo.createSnapshot_Task(Constants.THINAPP_CLONING_SNAPSHOT,
                       "Base snapshot used to create additional clones.", false, false);
               newSnapshot.waitForTask();
            } else {
               log.info("Cloning snapshot is already present.");
            }

            return null;
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<Void>>() {
         @Override
         public void apply(Future<Void> param) {
            self._onCreateCloningSnapshotFinished(param);
         }
      });
   }

   /**
    * Returns the VmImage when called within an existing transaction.
    *
    * @return
    */
   private VmImageModel getLocalImage() {
      return vmImageDao.get(vmImageId);
   }

   /**
    * Analyzes state of the VM image and attempts to resolve it by installing, preparing it for use, or deleting.
    */
   private void process() {
      log.info("Processing image instance.");

      // Note that this actor can't service any requests while we're waiting for the
      // ServiceInstance to be returned.  Not sure if that matters or not.  It also eats
      // up a thread.
      ServiceInstance si = vcManager.getConnection().get().get();
      VmImageModel vmImage = getVmImage();
      VmImageModel.State state = vmImage.getState();
      state = getNextStateDuringCreation(vmImage, state);

      // XXX: What should we do if we just started up and state is in
      // state deleting?  Operation may be in progress on VC.

      switch (state) {
         case waitingImport:
         case installFinished:
            // Instance was either created with an existing VM or an installation
            // just finished.  Either way we now need to create a snapshot.
            String moid = vmImage.getMoid();
            VirtualMachine vm = VirtualMachineHelper.createFromMoid(si, moid);
            setState(VmImageModel.State.creatingBaseSnapshot);
            createBaseSnapshot(si, vm);
            break;
         case waitingInstall:
            // Instance was created and needs to perform the installation.
            setState(VmImageModel.State.installing);
            install(si);
            break;
         case snapshotted:
            // Instance was either imported or installed and snapshotted and is now
            // ready for service.
            setState(VmImageModel.State.available);
            break;
         case deleteRequested:
            // A deletion of the backing VM was requested.
            setState(VmImageModel.State.deleting);
            deleteBackingVm(si, vmImage);
            break;
         case deleted:
            // The instance is to be deleted.
            deleteInstance();
            break;
         case created:
         case available:
         case vmDoesNotExist:
         case installing:
         case installFailed:
         case creatingBaseSnapshot:
         case snapshotFailed:
         case deleting:
         case unknown:
         case deleteFailed:
      }
   }

   /**
    * If in creation state determine the next state to transition to, based on whether a moid is already available.
    *
    * @param vmImage
    * @param currentState current state
    * @return either the given state or an import/install state based on whether in creation state or not
    */
   private VmImageModel.State getNextStateDuringCreation(VmImageModel vmImage, VmImageModel.State currentState) {
      final VmImageModel.State newState;

      if (currentState == VmImageModel.State.created) {
         if (StringUtils.hasLength(vmImage.getMoid())) {
            newState = VmImageModel.State.waitingImport;
         } else {
            newState = VmImageModel.State.waitingInstall;
         }

         setState(newState);
      } else {
         newState = currentState;
      }

      return newState;
   }
}
