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

package com.vmware.appfactory.application.dao;

import java.util.List;

import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.common.dao.AfDao;

/**
 * This interface defines AppBuildRequest-related custom CRUD methods.
 */
public interface AppBuildRequestDao extends AfDao<AppBuildRequest> {

   /**
    * Get the list of BuildReuqests for the appId passed.
    *
    * @param appId
    * @return
    */
   public List<AppBuildRequest> findBuildRequestForApp(Long appId);

   /**
    * Update the RequestStage for the given record Id.
    *
    * @param appBuildRequestId
    * @param stage
    */
   public void updateBuildRequestStage(Long appBuildRequestId, RequestStage stage);

   /**
    * Update the buildId for the record for the param appBuildRequestId
    *
    * @param appBuildRequestId
    * @param buildId
    */
   public void updateBuildRequestBuildId(Long appBuildRequestId, Long buildId);
}
