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
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({@JsonSubTypes.Type(VmPattern.class), @JsonSubTypes.Type(value = ExistingVm.class)})
public class VmSource {
   protected OsType osType;
   protected OsRegistration osRegistration;

   public VmSource() {
      /* Empty */
   }

   public VmSource(OsType osType, OsRegistration osRegistration) {
      this.osType = osType;
      this.osRegistration = osRegistration;
   }

   public OsType getOsType() {
      return osType;
   }

   public void setOsType(OsType osType) {
      this.osType = osType;
   }

   public OsRegistration getOsRegistration() {
      return osRegistration;
   }

   public void setOsRegistration(OsRegistration osRegistration) {
      this.osRegistration = osRegistration;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
