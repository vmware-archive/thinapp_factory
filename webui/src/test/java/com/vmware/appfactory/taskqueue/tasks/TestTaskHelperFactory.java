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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.horizon.HorizonHelper;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Helper function to inject fake objects into the tasks.
 *
 * todo: fake out the service APIs and write tests for each task
 */
@SuppressWarnings({"ReturnOfNull", "ConstantConditions"})
public class TestTaskHelperFactory implements TaskHelperFactory {

   public long nextId = 1;
   public int numEventsFired = 0;

   @Nonnull
   @Override
   public AfDaoFactory getDaoFactory() {
      return null;
   }

   @Nonnull
   @Override
   public Supplier<Long> getTaskIdSupplier() {
      return Suppliers.synchronizedSupplier(
            new Supplier<Long>() {
               @Override
               public Long get() {
                  return ++nextId;
               }
            }
      );
   }

   @Nonnull
   @Override
   public CwsClientService getCws() {
      return null;
   }

   @Nonnull
   @Override
   public TaskFactory getTaskFactory() {
      // todo: create fake tasks
      return null;
   }

   @Override
   public void fireTaskEvent(@Nonnull TaskEvent taskEvent) {
      ++numEventsFired;
   }

   @Override
   public long getDefaultWorkpoolId() {
      return -1;
   }

   @Override
   public long getDefaultDatastoreId() {
      return -1;
   }

   @Override
   public Workpool getCachedWorkpool(Long workpoolId) {
      return null;
   }

   @Override
   public DsDatastore getCachedDatastore(Long datastoreId) {
      return null;
   }

   @Override
   public ThinAppRuntime getCachedRuntime(Long runtimeId) {
      return null;
   }

   @Override
   public HorizonHelper getHorizonHelper() {
      return null;
   }

   /**
    * @return the number of events fired since the last reset.
    */
   @SuppressWarnings("PublicMethodNotExposedInInterface")
   @VisibleForTesting
   public int resetNumEventsFired() {
      int result = numEventsFired;
      numEventsFired = 0;
      return result;
   }
}
