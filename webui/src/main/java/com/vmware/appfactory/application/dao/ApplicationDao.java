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

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.dao.AfDao;

/**
 * Interface for dealing with Application objects.
 *
 * Custom methods that do not apply to AfRecord instances in general are
 * declared here.
 */
public interface ApplicationDao
   extends AfDao<Application>
{
   /**
    * Find all applications from all feeds, that are not to be skipped.
    *
    * @return
    */
   public List<Application> findAllIncluded();

   /**
    * @return The number of applications returned by findAllIncluded().
    */
   public Long countAllIncluded();

   /**
    * Get all other applications that match the passed app name and are
    * marked for inclusion.
    *
    * @param app
    * @return
    */
   public List<Application> findOtherVersionsIncluded(Application app);

   /**
    * Find uploaded applications that are stored onto the given datastore.
    *
    * @param downloadId a valid datastore id.
    * @return a list of uploaded applications.
    */
   public List<Application> findUploadedApps(Long datastoreId);

   /**
    * Delete uploaded applications.
    *
    * @param datastoreId a valid datastore id.
    */
   public void deleteUploadedApps(Long datastoreId);
}
