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

import java.rmi.RemoteException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.SettableFuture;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.LeaseModel;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachineSnapshot;

import akka.japi.Procedure;
import scala.Option;

/**
 * Wrapper around the new-style workpool for existing code.
 */
@Component
public class WorkpoolImpl implements Workpool {
   private Logger log = LoggerFactory.getLogger(WorkpoolImpl.class);

   @Autowired
   private VCManager vcManager;

   @Autowired
   private WorkpoolManager workpool;

   /**
    * Return the managed object ID (moid) from the given datacenter name.
    *
    * @param name Human-readable datacenter name
    * @return moid of the given datacenter
    */
   private String datacenterNameToMoid(String name) {
      // XXX: Fix null issues with this.
      ServiceInstance si = vcManager.getConnection().get().get();
      return Util.findDatacenter(name, si).getMOR().getVal();
   }

   private void createTemporarySnapshot(VCConfig vcInfo, InstanceInfo vmInfo) {
      // Snapshot, power on, wait for tools, upload ThinApp, install ThinApp,
      // upload license, run Setup Capture.
      // XXX: Configure MDC in logging to attach current context state for us.
      log.debug("Beginning VM preparation for {}/{}.", vcInfo, vmInfo);

      ServiceInstance si = vcManager.getConnection().get().get();
      com.vmware.vim25.mo.VirtualMachine vm = Util.findVm(vcInfo, vmInfo, si);
      try {
         log.debug("Creating a temporary snapshot.");

         // XXX: Should we check for an existing snapshot?
         Task task =
                 vm.createSnapshot_Task("Temporary Snapshot",
                         "Manual mode snapshot", false, false);
         task.waitForTask();
         TaskInfo taskInfo = task.getTaskInfo();
         TaskInfoState state = taskInfo.getState();

         if (state.equals(TaskInfoState.success)) {
            log.debug("VM snapshot creation succeeded.");
         } else {
            log.error("VM snapshot creation failed.");
            throw new TemporarySnapshotException(String.format(
                    "Failed to create a temporary snapshot: %s.", state));
         }
      } catch (RuntimeException e) {
         throw e;
      } catch (InterruptedException ignored) {
         /* Ignore */
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }
   }

   private void removeCurrentSnapshot(VCConfig vcConfig, InstanceInfo vmInfo) {
      log.debug("Removing snapshot for {}/{}.", vcConfig, vmInfo);

      ServiceInstance si = vcManager.getConnection().get().get();
      com.vmware.vim25.mo.VirtualMachine vm = Util.findVm(vcConfig, vmInfo, si);

      try {
         Task revertTask = vm.revertToCurrentSnapshot_Task(null);
         revertTask.waitForTask();

         VirtualMachineSnapshot currentSnapshot = vm.getCurrentSnapShot();
         Task task =
                 currentSnapshot.removeSnapshot_Task(false /* removeChildren */);
         task.waitForTask();
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Future<Lease> acquire(final com.vmware.thinapp.common.workpool.dto.Workpool workpoolInstance) {
      Option<WorkpoolInstance> maybeInstance = workpool.get(workpoolInstance.getId());

      if (maybeInstance.isEmpty()) {
         throw new RuntimeException("Unable to locate workpool: " + workpoolInstance);
      }

      final WorkpoolInstance instance = maybeInstance.get();
      log.debug("Using workpool: {}.", instance);
      akka.dispatch.Future<LeaseModel> future = instance.acquire().get();
      // Regular Java future to return to caller.
      final SettableFuture<Lease> returnFuture = SettableFuture.create();

      future.onComplete(new Procedure<akka.dispatch.Future<LeaseModel>>() {
         @Override
         public void apply(akka.dispatch.Future<LeaseModel> param) {
            try {
               LeaseModel mlease = param.get();
               InstanceModel clone = mlease.getInstance();
               VCConfigModel vcConfigModel = mlease.getVcConfig();
               VCConfig vcConfig = new VCConfig();
               vcConfig.setDatacenter(vcConfigModel.getDatacenter());
               vcConfig.setHost(vcConfigModel.getHost());
               vcConfig.setUsername(vcConfigModel.getUsername());
               vcConfig.setPassword(vcConfigModel.getPassword());
               vcConfig.setDatacenterMoid(datacenterNameToMoid(vcConfig.getDatacenter()));
               Lease lease = new Lease();
               InstanceInfo inst = new InstanceInfo();
               lease.setId(mlease.getId());
               lease.setVc(vcConfig);
               lease.setVm(inst);
               lease.setWorkpool(workpoolInstance);
               inst.setGuestUsername(clone.getGuestUsername());
               inst.setGuestPassword(clone.getGuestPassword());
               inst.setAutologon(clone.getAutologon());
               inst.setMoid(clone.getMoid());
               ServiceInstance si = vcManager.getConnection().get().get();
               com.vmware.vim25.mo.VirtualMachine vm = Util.findVm(vcConfig, inst, si);
               inst.setVmxPath(vm.getConfig().files.getVmPathName());
               log.debug("Received lease: {}.", lease);
               returnFuture.set(lease);
            } catch (Throwable t) {
               returnFuture.setException(t);
            }
         }
      });

      return returnFuture;
   }

   @Override
   public void release(Lease lease) {
      log.debug("Releasing lease: {}.", lease);
      WorkpoolInstance instance = workpool.get(lease.getWorkpool().getId()).get();
      LeaseModel mlease = new LeaseModel();
      mlease.setId(lease.getId());
      instance.release(mlease).get();
   }

   @Override
   public void withTemporarySnapshot(CallWithTemporarySnapshot runWithSnapshot,
                                     Lease lease) throws Exception {
      ServiceInstance si = vcManager.getConnection().get().get();
      VirtualMachine vm =
              new VirtualMachineImpl(si, lease.getVc(), lease.getVm());
      try {
         createTemporarySnapshot(lease.getVc(), lease.getVm());
         runWithSnapshot.call(vm, lease);
      } finally {
         removeCurrentSnapshot(lease.getVc(), lease.getVm());
      }
   }

   /**
    * Run a job in a temporary snapshot with a given state
    *
    * @param runWithSnapshot
    * @param lease
    * @throws Exception
    */
   @Override
   public void withSnapshot(CallWithTemporarySnapshot runWithSnapshot, Lease lease) throws Exception {
      // It's kind of weird that we have two kinds of VM objects.  Ours is
      // basically just a utility wrapper so it should be refactored to act
      // more as a utility than as a complete wrapper.
      ServiceInstance si = vcManager.getConnection().get().get();
      com.vmware.vim25.mo.VirtualMachine vmMo =
              Util.findVm(lease.getVc(), lease.getVm(), si);
      VirtualMachine vm =
              new VirtualMachineImpl(si, lease.getVc(), lease.getVm());

      VirtualMachineSnapshot originalSnapshot = vmMo.getCurrentSnapShot();

      try {
         runWithSnapshot.call(vm, lease);
      } finally {
         log.info("Reverting to snapshot {}.", originalSnapshot);
         Task task = originalSnapshot.revertToSnapshot_Task(null);
         log.debug("Waiting for task {} to complete.", task);
         task.waitForTask();
      }
   }
}
