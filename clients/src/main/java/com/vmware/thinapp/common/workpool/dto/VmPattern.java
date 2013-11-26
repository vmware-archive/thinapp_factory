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

public class VmPattern extends VmSource {
   private Long id;
   // XXX: Use moid for datastore instead of the name? [datastore] isos/winxpsp3.iso
   private String sourceIso;
   private String networkName = "";

   public VmPattern() {
      /* Empty */
   }

   public VmPattern(Long id, OsType osType, OsRegistration osRegistration, String sourceIso, String networkName) {
      super(osType, osRegistration);
      this.id = id;
      this.sourceIso = sourceIso;
      this.networkName = networkName;
   }

   public VmPattern(OsType osType, OsRegistration osRegistration, String sourceIso, String networkName) {
      super(osType, osRegistration);
      this.sourceIso = sourceIso;
      this.networkName = networkName;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getId() {
      return id;
   }

   public String getNetworkName() {
      return networkName;
   }

   public void setNetworkName(String networkName) {
      this.networkName = networkName;
   }

   public String getSourceIso() {
      return sourceIso;
   }

   public void setSourceIso(String sourceIso) {
      this.sourceIso = sourceIso;
   }
}
