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

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ToStringBuilder;

@Embeddable
public class OsInfoModel implements Serializable {
   private static final long serialVersionUID = -3039118831426874345L;

   // These match the OS types that easyinstall uses.
   public static enum OsType {
      winXPPro,
      winvista,
      windows7
   }

   @NotNull
   private OsType osType;
   @NotNull
   private String variant = ""; // Required for Vista+

   public OsType getOsType() {
      return osType;
   }

   public void setOsType(OsType osType) {
      this.osType = osType;
   }

   public String getVariant() {
      return variant;
   }

   public void setVariant(String variant) {
      this.variant = variant;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
