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
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Entity
@Table(name = "vcconfig")
public class VCConfigModel implements Serializable {
   private static final long serialVersionUID = 2472287577862087357L;

   @NotEmpty
   @Id
   private String name = "";
   private String host = "";
   private String username = "";
   private String password = "";
   private String datacenter = "";

   @Embedded
   private VmLocationModel vmLocation;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
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

   public VmLocationModel getVmLocation() {
      return vmLocation;
   }

   public void setVmLocation(VmLocationModel vmLocation) {
      this.vmLocation = vmLocation;
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
