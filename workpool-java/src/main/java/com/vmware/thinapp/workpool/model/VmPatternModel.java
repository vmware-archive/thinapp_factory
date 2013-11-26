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

package com.vmware.thinapp.workpool.model;

import java.io.Serializable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Describes how to automatically create a VM with a Windows guest.
 */
@Entity
@Table(name = "vmpattern")
public class VmPatternModel extends InstanceableModel implements Serializable {
   private static final long serialVersionUID = -5569417406986119016L;

   @NotNull
   @Embedded
   private OsInfoModel osInfo;
   // XXX: Use moid for datastore instead of the name? [datastore] isos/winxpsp3.iso
   @NotNull
   private String sourceIso = "";
   @NotNull
   private String networkName = "";
   @Embedded
   @NotNull
   private OsRegistrationModel osRegistration;

   public VmPatternModel() {
      /* Empty */
   }

   public VmPatternModel(OsInfoModel osInfo, OsRegistrationModel osRegistration, String sourceIso, String networkName) {
      this.osInfo = osInfo;
      this.osRegistration = osRegistration;
      this.sourceIso = sourceIso;
      this.networkName = networkName;
   }

   public OsRegistrationModel getOsRegistration() {
      return osRegistration;
   }

   public void setOsRegistration(OsRegistrationModel osRegistration) {
      this.osRegistration = osRegistration;
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

   public OsInfoModel getOsInfo() {
      return osInfo;
   }

   public void setOsInfo(OsInfoModel osInfo) {
      this.osInfo = osInfo;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
   }
}
