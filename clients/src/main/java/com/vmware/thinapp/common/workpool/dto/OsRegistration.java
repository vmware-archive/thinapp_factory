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

public class OsRegistration {
   private String licenseKey;
   private String userName;
   private String organization;
   private String kmsServer; // Optional

   public OsRegistration() {
      /* Empty */
   }

   public OsRegistration(String licenseKey, String userName, String organization) {
      this.licenseKey = licenseKey;
      this.userName = userName;
      this.organization = organization;
   }

   public OsRegistration(String licenseKey, String userName, String organization, String kmsServer) {
      this.licenseKey = licenseKey;
      this.userName = userName;
      this.organization = organization;
      this.kmsServer = kmsServer;
   }

   public String getLicenseKey() {
      return licenseKey;
   }

   public void setLicenseKey(String licenseKey) {
      this.licenseKey = licenseKey;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getOrganization() {
      return organization;
   }

   public void setOrganization(String organization) {
      this.organization = organization;
   }

   public String getKmsServer() {
      return kmsServer;
   }

   public void setKmsServer(String kmsServer) {
      this.kmsServer = kmsServer;
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this).setExcludeFieldNames(new String[]{"licenseKey"}).toString();

   }
}
