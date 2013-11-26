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


public interface VirtualMachine {
   public static enum PowerState {
      poweredOff, poweredOn, suspended
   }

   public void powerOn();
   public void waitForTools();
   public void waitForGuestIp();
   public void setScreenResolution(int width, int height);
   public PowerState getPowerState();
   public void powerOff();
   VCConfig getVcConfig();
   InstanceInfo getVmInfo();
   public VirtualMachineSnapshot findSnapshot(String snapshotName);
}
