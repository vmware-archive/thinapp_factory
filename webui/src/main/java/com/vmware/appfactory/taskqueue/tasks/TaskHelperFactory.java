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

package com.vmware.appfactory.taskqueue.tasks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Supplier;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Accessor for other utility objects within the app.
 */
public interface TaskHelperFactory {

   /**
    * @return a handle to the DAO factory.
    */
   @Nonnull
   AfDaoFactory getDaoFactory();

   /**
    * @return a generator for new task IDs.
    */
   @Nonnull
   Supplier<Long> getTaskIdSupplier();

   /**
    * @return a handle to the Conversions service.
    */
   @Nonnull
   CwsClientService getCws();

   /**
    * @return a factory which can create new tasks.
    */
   @Nonnull
   TaskFactory getTaskFactory();

   /**
    * Notifies application listeners about changes
    * to a task's state.
    *
    * @param taskEvent event describing the change
    *
    * @see TaskEvent
    */
   void fireTaskEvent(@Nonnull TaskEvent taskEvent);

   long getDefaultWorkpoolId();

   long getDefaultDatastoreId();

   @Nullable
   Workpool getCachedWorkpool(Long workpoolId);

   @Nullable
   DsDatastore getCachedDatastore(Long datastoreId);

   @Nullable
   ThinAppRuntime getCachedRuntime(Long runtimeId);
}
