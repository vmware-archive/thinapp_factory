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

package com.vmware.thinapp.common.workpool.dto;

/**
 * Workpool whose instances are linked clones based off a VmImage.
 *
 * @see VmImage
 */
public class LinkedWorkpool extends Workpool {
   private VmImage vmImage;

   public VmImage getVmImage() {
      return vmImage;
   }

   public void setVmImage(VmImage vmImage) {
      this.vmImage = vmImage;
   }

   @Override
   public OsType getOsType() {
      return (vmImage == null || vmImage.getVmSource() == null)? null : vmImage.getVmSource().getOsType();
   }
}
