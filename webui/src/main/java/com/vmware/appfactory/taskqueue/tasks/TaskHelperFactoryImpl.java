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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.model.ConfigChangeEvent;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

@Service("taskHelperFactory")
class TaskHelperFactoryImpl implements
      TaskHelperFactory,
      ApplicationListener,
      ApplicationEventPublisherAware {

   private final Logger log = LoggerFactory.getLogger(getClass());

   @Resource
   private ConfigRegistry _config;

   @Resource
   private WorkpoolClientService _wpClient;

   @Resource
   private DatastoreClientService _dsClient;

   @Resource
   private TaskIdSupplier _taskIdSupplier;

   @Resource
   private AfDaoFactory _daoFactory;

   @Resource
   private CwsClientService _cws;

   @Resource
   private TaskFactory _taskFactory;

   @Nullable
   private ApplicationEventPublisher _applicationEventPublisher = null;

   LoadingCache<Long, Workpool> _workpools =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(4)
                     .weakKeys()
                     .expireAfterWrite(10, TimeUnit.MINUTES)
                     .build(
                           new CacheLoader<Long, Workpool>() {
                              public Workpool load(Long id) throws AfNotFoundException, WpException {
                                 return _wpClient.getWorkpoolById(id);
                              }
                           });

   LoadingCache<Long, ThinAppRuntime> _runtimes =
         CacheBuilder.newBuilder()
                     .concurrencyLevel(4)
                     .weakKeys()
                     .expireAfterWrite(10, TimeUnit.MINUTES)
                     .build(
                           new CacheLoader<Long, ThinAppRuntime>() {
                              public ThinAppRuntime load(Long id) throws AfNotFoundException, WpException {
                                 return _config.newThinappRuntimeClient().findById(id);
                              }
                           });

   @Nonnull
   @Override
   public AfDaoFactory getDaoFactory() {
      return Preconditions.checkNotNull(_daoFactory);
   }

   @Nonnull
   @Override
   public Supplier<Long> getTaskIdSupplier() {
      return Preconditions.checkNotNull(_taskIdSupplier);
   }

   @Nonnull
   @Override
   public CwsClientService getCws() {
      return Preconditions.checkNotNull(_cws);
   }

   @Nonnull
   @Override
   public TaskFactory getTaskFactory() {
      return Preconditions.checkNotNull(_taskFactory);
   }

   @Override
   public void fireTaskEvent(@Nonnull TaskEvent taskEvent) {
      if (null != _applicationEventPublisher) {
         _applicationEventPublisher.publishEvent(taskEvent);
      }
   }

   @Override
   public long getDefaultWorkpoolId() {
      try {
         return _wpClient.getDefault().getId();
      } catch (AfNotFoundException e) {
         log.warn("No default workpool defined", e);
      } catch (WpException e) {
         log.warn("Could not get default workpool, assuming none", e);
      }
      return -1;
   }

   @Override
   public long getDefaultDatastoreId() {
      try {
         DsDatastore ds = _dsClient.getDefaultOutputDatastore();
         if (null != ds) {
            return ds.getId();
         }
         log.warn("No default datastore defined");
      } catch (DsException e) {
         log.warn("Could not get default datastore, assuming no datastores exist", e);
      }
      return -1;
   }


   @Override
   @Nullable
   public Workpool getCachedWorkpool(Long workpoolId) {
      try {
         return _workpools.get(workpoolId);
      } catch (ExecutionException e) {
         return null;
      }
   }

   @Override
   @Nullable
   public DsDatastore getCachedDatastore(Long datastoreId) {
      try {
         return _dsClient.findDatastore(datastoreId, true);
      } catch (DsException e) {
         return null;
      }
   }

   @Override
   @Nullable
   public ThinAppRuntime getCachedRuntime(Long runtimeId) {
      try {
         return _runtimes.get(runtimeId);
      } catch (ExecutionException e) {
         return null;
      }
   }

   @Override
   public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher applicationEventPublisher) {
      _applicationEventPublisher = applicationEventPublisher;
   }

   @Override
   public void onApplicationEvent(ApplicationEvent applicationEvent) {
      if (applicationEvent instanceof ConfigChangeEvent) {
         _runtimes.invalidateAll();
         _workpools.invalidateAll();
      }
   }
}
