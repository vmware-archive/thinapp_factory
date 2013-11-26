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

package com.vmware.appfactory.build.dao;

import java.util.List;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.dao.AfDao;


/**
 * Interface for dealing with AfBuild objects.
 *
 * Custom methods that do not apply to AfRecord instances in general are
 * declared here.
 */
public interface BuildDao
   extends AfDao<Build>
{
   /**
    * Find all builds created for an application.
    * Only exact application matches are returned.
    *
    * @param app Application to match.
    *
    * @return A list of matching builds.
    */
   public List<Build> findForApp(AbstractApp app);


   /**
    * Find all builds that have the given application name.
    *
    * @param appName Application name.
    * @return A list of matching builds.
    */
   public List<Build> findForAppName(String appName);


   /**
    * Find all builds that have the given status.
    *
    * @param status Build status.
    * @return  A list of matching buillds.
    */
   public List<Build> findForStatus(Build.Status status);

   /**
    * Find a build by the corresponding projectId.
    *
    * @param projectId
    * @return
    */
   public Build findByProjectId(Long projectId);

   /**
    * Delete all projects that are stored on the given datastore.
    * @param datastoreId a datastore id
    */
   public void deleteAll(Long datastoreId);

   /**
    *
    * @param datastoreId
    * @return
    */
   public List<Build> findByDatastore(Long datastoreId);
}
