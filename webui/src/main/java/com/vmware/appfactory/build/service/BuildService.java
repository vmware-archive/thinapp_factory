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

package com.vmware.appfactory.build.service;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.appfactory.build.dto.BuildComponents;
import com.vmware.appfactory.build.dto.IniDataRequest;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.dto.SelectOptions;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This is a service layer that provides re-usability of common build
 * functionality. This layer keeps the controller business logic
 * optimized.
 */
public interface BuildService
{
   /**
    * This method is used to load the build components for capturing a
    * new ThinApp or for rebuilding a project. Depending on the flag,
    * the components are selectively loaded.
    *
    * @throws DsException
    * @throws WpException
    */
   public BuildComponents loadBuildComponents(boolean forRebuild)
   throws DsException, WpException;

   /**
    * @return a list of datastores, but only the ones that are cached.
    */
   public Collection<DsDatastore> getCachedDatastores();

   /**
    * This method is used to load the datastores list. Additionally it can
    * filter the list of datastores by a collection of ids passed.
    *
    * @see loadDatastoresMap(Collection<Long> filterByIds)
    * @param filterByIds
    * @return
    * @throws DsException
    */
   public Collection<DsDatastore> loadDatastores(@Nullable Collection<Long> filterByIds)
   throws DsException;

   /**
    * Method to get a Map of datastoreId as key, and DsDatastore as value.
    *
    * @see loadDatastores(Collection<Long> filterByIds)
    * @return
    * @throws DsException
    */
   @Nonnull
   public Map<Long, DsDatastore> loadDatastoresMap(Collection<Long> filterByIds)
   throws DsException;

   /**
    * Creates a SelectOptions object for datastores.
    *
    * @param setInitialValue - Indicate if the initial value should be set.
    */
   public SelectOptions prepareDatastoreSelect(boolean setInitialValue) throws DsException;

   /**
    * Creates a SelectOptions object for ThinApp runtimes.
    *
    * @param setInitialValue - Indicate if the initial value should be set.
    */
   public SelectOptions prepareRuntimeSelect(boolean setInitialValue);

   /**
    * Submit all the capture requests via the executor.
    * For a single request, submit the request synchronously to the queue. Otherwise,
    * submit all in a FutureTask.
    *
    * @param captureRequests a capture request
    * @throws DsException
    * @throws AfNotFoundException
    * @throws WpException
    */
   public void submitTasks(@Nonnull final CaptureRequestImpl[] captureRequests)
      throws DsException, WpException, AfNotFoundException;

   /**
    * Service method to flag build as edited and update runtime and hzSupport flag
    *
    * @param iniRequest
    * @param build
    * @param updateRuntime
    */
   public void updateBuildRuntimeHzSupport(IniDataRequest iniRequest, Build build, boolean updateRuntime);
}
