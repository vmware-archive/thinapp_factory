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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineToolsRunningStatus;
import com.vmware.vim25.VirtualMachineToolsVersionStatus;
import com.vmware.vim25.mo.PropertyCollector;
import com.vmware.vim25.mo.PropertyFilter;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachineSnapshot;
import com.vmware.vim25.mo.util.PropertyCollectorUtil;

public class VirtualMachineImpl implements VirtualMachine {
   static final Logger log = LoggerFactory.getLogger(VirtualMachineImpl.class);

   private final ServiceInstance si;
   private final com.vmware.vim25.mo.VirtualMachine vm;

   private final VCConfig vcConfig;
   private final InstanceInfo vmInfo;
   private final VirtualMachineHelper vmHelper;

   public VirtualMachineImpl(ServiceInstance si, VCConfig vcConfig,
         InstanceInfo vmInfo) {
      this.si = si;
      this.vcConfig = vcConfig;
      this.vmInfo = vmInfo;
      vm = Util.findVm(vcConfig, vmInfo, si);
      vmHelper = new VirtualMachineHelper(si, vm);
   }

   @Override
   public PowerState getPowerState() {
      VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
      PowerState state = PowerState.valueOf(powerState.name());
      return state;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.thinapp.workpool.VirtualMachine#powerOn()
    */
   @Override
   public void powerOn() {
      try {
         Task task = vm.powerOnVM_Task(null);
         task.waitForTask();
      } catch (RuntimeException e) {
         throw e;
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void powerOff() {
      try {
         Task task = vm.powerOffVM_Task();
         task.waitForTask();
      } catch (RuntimeException e) {
         throw e;
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Determines whether the given Tools status is an acceptable state
    *
    * @param versionStatus
    * @param runningStatus
    * @return
    */
   private static boolean validateToolsRunningStatus(
         VirtualMachineToolsVersionStatus versionStatus,
         VirtualMachineToolsRunningStatus runningStatus) {
      // runningStatus will go into state guestToolsRunning right after installation
      // but before the reboot.  Checking versionStatus ensures that the reboot
      // has occurred.
      return versionStatus != VirtualMachineToolsVersionStatus.guestToolsNotInstalled
            && runningStatus == VirtualMachineToolsRunningStatus.guestToolsRunning;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.vmware.thinapp.workpool.VirtualMachine#waitForTools()
    */
   @Override
   public void waitForTools() {
      PropertyCollector propertyCollector = si.getPropertyCollector();

      PropertySpec[] propertySpec =
            PropertyCollectorUtil.buildPropertySpecArray(new String[][] { {
                  "VirtualMachine", "guest" } });
      ObjectSpec objectSpec = new ObjectSpec();
      objectSpec.setObj(vm.getMOR());
      objectSpec.setSkip(false);

      PropertyFilterSpec filterSpec = new PropertyFilterSpec();
      filterSpec.setObjectSet(new ObjectSpec[] { objectSpec });
      filterSpec.setPropSet(propertySpec);

      PropertyFilter propertyFilter;

      try {
         propertyFilter = propertyCollector.createFilter(filterSpec, false); /*
                                                                              * partialUpdates = false, not sure what it
                                                                              * does
                                                                              */
      } catch (RuntimeException e) {
         throw e;
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }

      String version = "";

      while (true) {
         UpdateSet update;

         try {
            // XXX: waitForUpdatesEx doesn't work against Workstation hostd for some
            // reason but does work on vSphere.  Receives vim25.InvalidRequest.
            update = propertyCollector.checkForUpdates(version);
         } catch (RuntimeException e) {
            throw e;
         } catch (RemoteException e) {
            throw new RuntimeException(e);
         }

         if (update == null || update.getFilterSet() == null) {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException ignored) {
               /* Ignore */
            }
            continue;
         }

         if (isToolsRunning(update)) {
            break;
         }

         version = update.getVersion();
      }

      // FIXME: This should probably be in a finally block that runs if
      // anything else above throws exceptions.
      try {
         propertyFilter.destroyPropertyFilter();
      } catch (RuntimeException e) {
         throw e;
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }
    }

    /**
     * Best effort call to set screen resolution through tools.
     * Should only be used when tools is in running state.
     */
    @Override
    public void setScreenResolution(int width, int height) {
       try {
          vm.setScreenResolution(width, height);
          log.debug("Changed screen resolution to {}x{}", width, height);
       } catch (Exception e) {
          log.error("Failed to change screen resolution", e);
       }
    }

   /**
    * Wait 120 seconds for guest to complete Ip initialization.
    */
    @Override
    public void waitForGuestIp() {
      // wait for 120 seconds
      log.info("Waiting for guest Ip.", vm.getName());
      for (int i = 0; i < 120; i++) {
         String ip = vm.getGuest().getIpAddress();
         if (validateIpAddress(ip)) {
            log.info("Validated guest Ip (" + ip + ").", vm.getName());
            return;
         }

         try {
            Thread.sleep(1000);
         } catch (InterruptedException ignored) {
            throw new RuntimeException(ignored);
         }
      }

      throw new RuntimeException("Guest Ip address not initialized within 120 seconds.");
   }

   private boolean validateIpAddress(String ip) {
      if (ip == null || ip.isEmpty()) {
         return false;
      }

      try {
         InetAddress ipAddr = InetAddress.getByName(ip);
         return !(ipAddr.isAnyLocalAddress()
                ||ipAddr.isLinkLocalAddress()
                ||ipAddr.isLoopbackAddress());
      } catch (UnknownHostException ignored) {
         return false;
      }
   }

   /**
    * Process the update to determine whether it indicates the guest tools are
    * running or not.
    *
    * @param update
    * @return whether the guest tools are running or not
    */
   private boolean isToolsRunning(UpdateSet update) {
      PropertyFilterUpdate[] filterSet = update.getFilterSet();

      for (PropertyFilterUpdate u : filterSet) {
         for (ObjectUpdate o : u.getObjectSet()) {
            if (o.getObj().getType().equals("VirtualMachine")) {
               PropertyChange[] changeSet = o.getChangeSet();

               if (changeSet.length != 1) {
                  log.error(
                        "Expected changeset of length one but received: {}.",
                        changeSet);
                  throw new RuntimeException(
                        "Expected a changeset of length one.");
               }

               GuestInfo guestInfo = (GuestInfo) changeSet[0].getVal();

               return validateToolsRunningStatus(
                     VirtualMachineToolsVersionStatus.valueOf(
                           VirtualMachineToolsVersionStatus.class,
                           guestInfo.toolsVersionStatus),
                     VirtualMachineToolsRunningStatus.valueOf(
                           VirtualMachineToolsRunningStatus.class,
                           guestInfo.toolsRunningStatus));
            } else {
               log.error("Uknown object update type: {}.", o.getObj().getType());
            }
         }
      }

      throw new VirtualMachineException(
            "No VirtualMachine update was received.");
   }

   public com.vmware.vim25.mo.VirtualMachine getVm() {
      return vm;
   }

   @Override
   public VCConfig getVcConfig() {
      return vcConfig;
   }

   @Override
   public InstanceInfo getVmInfo() {
      return vmInfo;
   }

   @Override
   public VirtualMachineSnapshot findSnapshot(String snapshotName) {
      return vmHelper.findSnapshot(snapshotName);
   }
}
