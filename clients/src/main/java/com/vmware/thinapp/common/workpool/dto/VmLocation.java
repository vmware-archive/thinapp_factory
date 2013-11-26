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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Describes where the VM should live in vSphere.
 */
public class VmLocation {
   private String datastoreName;
   private String computeResource;
   private String resourcePool; // Optional

   public String getDatastoreName() {
      return datastoreName;
   }

   public void setDatastoreName(String datastoreName) {
      this.datastoreName = datastoreName;
   }

   public String getResourcePool() {
      return resourcePool;
   }

   public void setResourcePool(String resourcePool) {
      this.resourcePool = resourcePool;
   }

   public String getComputeResource() {
      return computeResource;
   }

   public void setComputeResource(String computeResource) {
      this.computeResource = computeResource;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}