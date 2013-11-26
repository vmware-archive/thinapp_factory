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

package com.vmware.appfactory.taskqueue.tasks.state.tasks;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.manualmode.IManualModeService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.FeedScanState;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;
import com.vmware.appfactory.taskqueue.tasks.state.ManualModeState;
import com.vmware.appfactory.taskqueue.tasks.state.RebuildState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.AppConvertStateBuilder;
import com.vmware.appfactory.taskqueue.tasks.state.builder.ImportProjectStateBuilder;

/**
 * Public class to create all types of tasks, without exposing the class
 * implementation itself outside this package.
 */
@Service
public class TaskFactory {

   @Nonnull
   @Resource
   ConfigRegistry config;

   @Nonnull
   @Resource
   private TaskHelperFactory taskHelperFactory;


   public static String makeRecordId(@Nonnull String taskStateType, long recordId) {
      return taskStateType + '-' + recordId;
   }

   @Nonnull
   public AppFactoryTask<AppConvertState,AppConvertStateBuilder,AppConvertState.AppConvertStatus>
         newAppConvertTask(@Nonnull CaptureRequestImpl captureRequest, TaskQueue _conversionsQueue) {
      if (null == captureRequest.getTaskHelperFactory()) {
         captureRequest.setTaskHelperFactory(taskHelperFactory);
      }

      long stallCpu = config.getLong(ConfigRegistryConstants.CWS_STALL_CPU) * 100;
      long stallNet = config.getLong(ConfigRegistryConstants.CWS_STALL_NET) * 100;
      long stallDisk = config.getLong(ConfigRegistryConstants.CWS_STALL_DISK) * 100;
      long stallTimeout = config.getLong(ConfigRegistryConstants.CWS_STALL_TIMEOUT);
      boolean enableQR = config.getBool(ConfigRegistryConstants.CWS_ENABLE_QUALITY_REPORTING);
      String tagQR = config.getString(ConfigRegistryConstants.CWS_QUALITY_REPORTING_TAG);
      String horizonUrl = config.getHorizonUrlIfEnabled();

      return new AppConvertTask(captureRequest,taskHelperFactory,
                                _conversionsQueue,
                                horizonUrl,
                                stallCpu,
                                stallNet,
                                stallDisk,
                                stallTimeout,
                                enableQR,
                                tagQR);
   }

   @Nonnull
   public AppFactoryTask<RebuildState,RebuildState.Builder,RebuildState.RebuildStatus>
         newRebuildTask(@Nonnull Build build) {
      return new RebuildTask(build, taskHelperFactory);
   }

   @Nonnull
   public AppFactoryTask<ImportProjectState,ImportProjectStateBuilder,ImportProjectState.ImportProjectStatus>
         newImportProjectTask(@Nonnull DsDatastore ds,
                              @Nonnull Long runtimeId,
                              boolean addHorizonSupport) {
      return new ImportProjectTask(ds, taskHelperFactory, runtimeId, addHorizonSupport,
              config.getHorizonUrlIfEnabled(),
              config.getBool(ConfigRegistryConstants.CWS_ENABLE_QUALITY_REPORTING),
              config.getString(ConfigRegistryConstants.CWS_QUALITY_REPORTING_TAG));
   }

   @Nonnull
   public AppFactoryTask<ManualModeState,ManualModeState.Builder,ManualModeState.ManualModeStatus>
         newManualModeTask(@Nonnull CaptureRequestImpl captureRequest,
                           @Nonnull Long ticketId,
                           @Nonnull IManualModeService manualModeService,
                           @Nonnull TaskQueue conversionsQueue) {
      if (null == captureRequest.getTaskHelperFactory()) {
         captureRequest.setTaskHelperFactory(taskHelperFactory);
      }
      String horizonUrl = config.publishToHorizonEnabled() ?
              config.getString(ConfigRegistryConstants.HORIZON_URL) : null;
      return new ManualModeTask(captureRequest, ticketId, manualModeService, taskHelperFactory,
              horizonUrl, conversionsQueue);
   }

   public AppFactoryTask<FeedScanState,FeedScanState.Builder,FeedScanState.FeedScanStatus>
   newFeedScanTask(@Nonnull Feed feed,
                   @Nonnull TaskQueue conversionsQueue) {

      int feedsMaxConvertAttempts = config.getInteger(ConfigRegistryConstants.FEEDS_MAX_CONVERT_ATTEMPTS);

      return newFeedScanTask(
               feed,
               conversionsQueue,
               feedsMaxConvertAttempts,
               taskHelperFactory.getDefaultDatastoreId(),
               config.getDefaultRuntime()
         );
   }

   public AppFactoryTask<FeedScanState,FeedScanState.Builder,FeedScanState.FeedScanStatus>
         newFeedScanTask(@Nonnull Feed feed,
                         @Nonnull TaskQueue conversionsQueue,
                         int feedsMaxConvertAttempts,
                         long defaultDatastore,
                         long defaultRuntime) {
      return new FeedScanTask(feed,
                              taskHelperFactory,
                              conversionsQueue,
                              feedsMaxConvertAttempts,
                              taskHelperFactory.getDefaultWorkpoolId(),
                              defaultDatastore,
                              defaultRuntime);
   }

   @Nonnull
   public TaskHelperFactory getTaskHelperFactory() {
      return taskHelperFactory;
   }
}
