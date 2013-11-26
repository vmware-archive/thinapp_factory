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

package com.vmware.appfactory.build.dto;

import com.vmware.appfactory.cws.CwsSettingsIni;

public class IniDataRequest {

   private CwsSettingsIni packageIni;

   private boolean hzSupported;

   private Long runtimeId;


   public IniDataRequest() {
      // Dummy constructor.
   }

   public IniDataRequest(CwsSettingsIni packageIni, boolean hzSupported, Long runtimeId) {
      this.packageIni = packageIni;
      this.hzSupported = hzSupported;
      this.runtimeId = runtimeId;
   }

   /**
    * @return the packageIni
    */
   public CwsSettingsIni getPackageIni() {
      return packageIni;
   }

   /**
    * @param packageIni the packageIni to set
    */
   public void setPackageIni(CwsSettingsIni packageIni) {
      this.packageIni = packageIni;
   }

   /**
    * @return the hzSupported
    */
   public boolean isHzSupported() {
      return hzSupported;
   }

   /**
    * @param hzSupported the hzSupported to set
    */
   public void setHzSupported(boolean hzSupported) {
      this.hzSupported = hzSupported;
   }

   /**
    * @return the runtimeId
    */
   public Long getRuntimeId() {
      return runtimeId;
   }

   /**
    * @param runtimeId the runtimeId to set
    */
   public void setRuntimeId(Long runtimeId) {
      this.runtimeId = runtimeId;
   }


}
