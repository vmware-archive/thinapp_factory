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

import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * A DTO to capture project import requests.
 */
public class ProjectImportRequest {

   private Datastore _datastore;

   private Long runtimeId;

   private boolean _addHorizonIntegration;

   /**
    * @return the datastore
    */
   public Datastore getDatastore() {
      return _datastore;
   }

   /**
    * @param datastore the datastore to set
    */
   public void setDatastore(Datastore datastore) {
      _datastore = datastore;
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

   /**
    * @return the addHorizonIntegration
    */
   public boolean isAddHorizonIntegration() {
      return _addHorizonIntegration;
   }

   /**
    * @param addHorizonIntegration the addHorizonIntegration to set
    */
   public void setAddHorizonIntegration(boolean addHorizonIntegration) {
      _addHorizonIntegration = addHorizonIntegration;
   }
}