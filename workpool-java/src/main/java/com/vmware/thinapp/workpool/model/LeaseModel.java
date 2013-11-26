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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
@Table(name = "lease")
public class LeaseModel implements Serializable {
   private static final long serialVersionUID = -8886311083832214350L;

   private long id;
   @NotNull
   private InstanceModel instance;
   private VCConfigModel vcConfig;

   @Id
   @GeneratedValue
   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   @OneToOne
   public InstanceModel getInstance() {
      return instance;
   }

   public void setInstance(InstanceModel instance) {
      this.instance = instance;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      LeaseModel lease = (LeaseModel) o;

      if (getId() != lease.getId()) {
         return false;
      }

      return true;
   }

   @Transient
   public VCConfigModel getVcConfig() {
      return vcConfig;
   }

   public void setVcConfig(VCConfigModel vcConfig) {
      this.vcConfig = vcConfig;
   }

   @Override
   public int hashCode() {
      return (int) (getId() ^ (getId() >>> 32));
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
