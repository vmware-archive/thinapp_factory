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

/**
 * Describes the hardware configuration for a VM.
 */
public class HardwareConfiguration {
   private int memoryMB;
   private int diskMB;

   // Default sizing for a capture VM.
   // TODO: Allow user customization.
   public static final HardwareConfiguration DEFAULT = new HardwareConfiguration(1024, 20480);

   public HardwareConfiguration(int memoryMB, int diskMB) {
      this.memoryMB = memoryMB;
      this.diskMB = diskMB;
   }

   public int getMemoryMB() {
      return memoryMB;
   }

   public int getDiskMB() {
      return diskMB;
   }
}
