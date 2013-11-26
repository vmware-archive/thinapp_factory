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

package com.vmware.appfactory.config.dto;

public class HorizonConfig {
   private String _horizonUrl;
   private String _horizonActivationToken;
   private Boolean _horizonTrustSslCert;

   public HorizonConfig() {
      // Empty default constructor.
   }

   public HorizonConfig(String horizonUrl, String activationToken, Boolean horizonTrustSslCert) {
      this._horizonUrl = horizonUrl;
      this._horizonActivationToken = activationToken;
      this._horizonTrustSslCert = horizonTrustSslCert;
   }

   /**
    * @return the horizonUrl
    */
   public String getHorizonUrl()
   {
      return _horizonUrl;
   }

   /**
    * @param horizonUrl the horizonUrl to set
    */
   public void setHorizonUrl(String horizonUrl)
   {
      _horizonUrl = horizonUrl;
   }

   /**
    * @return the horizonActivationToken
    */
   public String getHorizonActivationToken()
   {
      return _horizonActivationToken;
   }

   /**
    * @param horizonActivationToken the horizonActivationToken to set
    */
   public void setHorizonActivationToken(String horizonActivationToken)
   {
      _horizonActivationToken = horizonActivationToken;
   }

   /**
    * @return the horizonTrustSslCert
    */
   public Boolean getHorizonTrustSslCert()
   {
      return _horizonTrustSslCert;
   }

   /**
    * @param horizonTrustSslCert the horizonTrustSslCert to set
    */
   public void setHorizonTrustSslCert(Boolean horizonTrustSslCert)
   {
      _horizonTrustSslCert = horizonTrustSslCert;
   }
}
