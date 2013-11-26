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

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.vim25.mo.VirtualMachineSnapshot;



public class VirtualMachineStub implements VirtualMachine {
   @Override
   public void powerOn() {
      /* Empty */
   }

   @Override
   public void waitForTools() {
      /* Empty */
   }

   @Override
   public void waitForGuestIp() {
      /* Empty */
   }

   @Override
   public PowerState getPowerState() {
      return null;
   }

   @Override
   public void setScreenResolution(int width, int height) {
      // Empty
   }

   @Override
   public void powerOff() {
      /* Empty */
   }

   @Override
   public VCConfig getVcConfig() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public InstanceInfo getVmInfo() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public VirtualMachineSnapshot findSnapshot(String snapshotName) {
      // TODO Auto-generated method stub
      return null;
   }
}
