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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Represents an acquired lease from the workpool for a VM.
 */
public class Lease {
   private Long id;
   private VCConfig vc;
   private InstanceInfo vm;
   private Workpool workpool;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public VCConfig getVc() {
      return vc;
   }

   public void setVc(VCConfig vc) {
      this.vc = vc;
   }

   public InstanceInfo getVm() {
      return vm;
   }

   public void setVm(InstanceInfo vm) {
      this.vm = vm;
   }

   public Workpool getWorkpool() {
      return workpool;
   }

   public void setWorkpool(Workpool workpool) {
      this.workpool = workpool;
   }

   @Override
   public String toString() {
      return ReflectionToStringBuilder.toString(this);
   }
}
