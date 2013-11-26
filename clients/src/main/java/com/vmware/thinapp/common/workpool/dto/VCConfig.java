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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Describes the information needed to connect to the VC.
 */
public class VCConfig {
   private String host = "";
   private String username = "";
   private String password = "";
   private String datacenter = "";
   private String datacenterMoid = "";

   public static enum CloneSupport {
      // Cloning is supported.
      available,
      // Cloning isn't supported.
      unavailable,
      // Not connected so we can't find out.
      indeterminable
   }

   private CloneSupport cloneSupport;

   public static enum ApiType {
      // VC  -- full features and sophisticated MOB structure
      virtualCenter,
      // Host or WS -- less features and simple MOB structure
      hostAgent,
      // Not connected so we can't find out
      indeterminable
   }

   private ApiType apiType;

   private VmLocation vmLocation;

   public VmLocation getVmLocation() {
      return vmLocation;
   }

   public void setVmLocation(VmLocation vmLocation) {
      this.vmLocation = vmLocation;
   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getDatacenter() {
      return datacenter;
   }

   public void setDatacenter(String datacenter) {
      this.datacenter = datacenter;
   }

   public void setCloneSupport(CloneSupport cloneSupport) {
      this.cloneSupport = cloneSupport;
   }

   public CloneSupport getCloneSupport() {
      return cloneSupport;
   }

   public ApiType getApiType() {
      return apiType;
   }

   public void setApiType(ApiType apiType) {
      this.apiType = apiType;
   }

   public String getDatacenterMoid() {
      return datacenterMoid;
   }

   public void setDatacenterMoid(String dcMoid) {
      this.datacenterMoid = dcMoid;
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this)
              .setExcludeFieldNames(new String[]{"password"}).toString();
   }

   @Override
   public boolean equals(Object rhs) {
      return EqualsBuilder.reflectionEquals(this, rhs);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }
}