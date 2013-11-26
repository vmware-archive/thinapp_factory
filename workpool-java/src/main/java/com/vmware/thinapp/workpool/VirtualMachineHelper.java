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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.ManagedObjectNotFound;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineSnapshot;

/**
 * Utility functions for VirtualMachine managed objects.
 */
public class VirtualMachineHelper {
   private static final Logger log = LoggerFactory.getLogger(VirtualMachineHelper.class);
   private final ServiceInstance serviceInstance;
   private final com.vmware.vim25.mo.VirtualMachine vm;

   /**
    * Constructor
    *
    * @param serviceInstance a VC connection
    * @param vm              an existing VM
    */
   public VirtualMachineHelper(ServiceInstance serviceInstance, VirtualMachine vm) {
      this.serviceInstance = serviceInstance;
      this.vm = vm;
   }

   /**
    * Constructor
    *
    * @param serviceInstance a VC connection
    * @param moid existing VM moid
    */
   public VirtualMachineHelper(ServiceInstance serviceInstance, String moid) {
      this.serviceInstance = serviceInstance;
      this.vm = createFromMoid(serviceInstance, moid);
   }

   /**
    * Create a VirtualMachine managed object from a moid.
    *
    * @param serviceInstance
    * @param moid
    * @return a VM that may not necessarily exist on the server
    */
   public static VirtualMachine createFromMoid(ServiceInstance serviceInstance, String moid) {
      ManagedObjectReference vmRef = new ManagedObjectReference();
      vmRef.setType("VirtualMachine");
      vmRef.setVal(moid);
      return new VirtualMachine(serviceInstance.getServerConnection(), vmRef);
   }

   private VirtualMachineSnapshot findSnapshot(VirtualMachineSnapshotTree[] nodes, String name) {
      if (nodes == null) {
         return null;
      }

      for (VirtualMachineSnapshotTree snapshot : nodes) {
         log.debug("Checking {}.", nodes);
         String snapshotName = snapshot.getName();

         if (name.equals(snapshotName)) {
            return new VirtualMachineSnapshot(serviceInstance.getServerConnection(), snapshot.getSnapshot());
         } else {
            return findSnapshot(snapshot.getChildSnapshotList(), name);
         }
      }

      return null;
   }

   /**
    * Finds the first snapshot of the given name if present
    *
    * @param snapshotName
    * @return a snapshot instance if found, otherwise null
    */
   public VirtualMachineSnapshot findSnapshot(String snapshotName) {
      VirtualMachineSnapshotInfo snapInfo = vm.getSnapshot();

      // VM may not have any snapshots at all
      if (snapInfo == null) {
         return null;
      }

      VirtualMachineSnapshotTree[] rootSnapshot = snapInfo.getRootSnapshotList();

      VirtualMachineSnapshot snapshot = findSnapshot(rootSnapshot, snapshotName);
      log.info("Found snapshot {}: {}.", snapshotName, snapshot);
      return snapshot;
   }

   /**
    * Power off the VM if it is running.
    */
   public void powerOff() {
      try {
         // Return immediately if not powered on.
         if (vm.getRuntime().powerState != VirtualMachinePowerState.poweredOn) {
            return;
         }
         Task task = vm.powerOffVM_Task();
         task.waitForTask();
      } catch (RuntimeException e) {
         // This is from vm.getRuntime().
         // the vm has been delted manually.
         // skip this error to let wp remove
         // the VM record.
         Throwable c = e.getCause();
         if (c instanceof ManagedObjectNotFound) {
            ManagedObjectNotFound n = (ManagedObjectNotFound)c;
            if (vm.getMOR().equals(n.getObj())) {
               log.info("VM not found, skip powerOff().");
               return;
            }
         }
         throw e;
      } catch (ManagedObjectNotFound e) {
         // This is from vm.powerOffVM_Task().
         // the vm has been deleted manually.
         // skp this error to let wp remove
         // the VM record.
         if (vm.getMOR().equals(e.getObj())) {
            log.info("VM not found, skip powerOffVM().");
            return;
         }
         throw new WorkpoolException(e);
      } catch (RemoteException e) {
         throw new WorkpoolException(e);
      } catch (InterruptedException e) {
         throw new WorkpoolException(e);
      }

      // Ensure it's powered off.
      if (vm.getRuntime().powerState != VirtualMachinePowerState.poweredOff) {
         throw new WorkpoolException("Virtual machine did not transition into powered off state.");
      }
   }

   /**
    * Deletes a VM including its associated virtual disks.
    */
   public void delete() {
      try {
         Task task = vm.destroy_Task();
         task.waitForTask();
      } catch (ManagedObjectNotFound e) {
         // the vm has been deleted manually.
         // skip this error to let wp remove
         // the VM record.
         if (vm.getMOR().equals(e.getObj())) {
            log.info("VM not found, skip destroyVM().");
            return;
         }
         throw new WorkpoolException(e);
      } catch (RemoteException e) {
         throw new WorkpoolException(e);
      } catch (InterruptedException e) {
         throw new WorkpoolException(e);
      }
   }

   /**
    * Ping VC to check the existance of the VM
    */
   public boolean isExistent() {
      try {
         vm.getName();
         return true;
      } catch (RuntimeException e) {
         Throwable c = e.getCause();
         if (c instanceof ManagedObjectNotFound) {
            ManagedObjectNotFound n = (ManagedObjectNotFound)c;
            if (vm.getMOR().equals(n.getObj())) {
               return false;
            }
         }
      } catch (Exception e) {
         // we don't trigger a failure here and leave it to be
         // handled by logic that is going to use this VM.
      }

      return true;
   }
}
