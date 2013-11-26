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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dto.BuildComponents;
import com.vmware.appfactory.build.dto.IniDataRequest;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.common.dto.EachOption;
import com.vmware.appfactory.common.dto.GroupOption;
import com.vmware.appfactory.common.dto.SelectOptions;
import com.vmware.appfactory.common.dto.SingleOption;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.util.StorageUnit;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfConstant;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

@Service("buildService")
public class BuildServiceImpl
   implements BuildService
{
   @Resource
   private WorkpoolClientService _wpClient;

   @Resource
   protected DatastoreClientService _dsClient;

   @Resource
   protected ConfigRegistry _config;

   @Resource
   protected AfDaoFactory _daoFactory;

   @Resource(name = "conversionsQueue")
   protected TaskQueue _conversionsQueue;

   @Resource
   protected TaskFactory _taskFactory;

   @Resource(name = "executor")
   protected TaskExecutor _executor;

   private static final Logger _log = LoggerFactory
         .getLogger(BuildServiceImpl.class);

   @Override
   public BuildComponents loadBuildComponents(boolean forRebuild)
      throws DsException, WpException
   {
      BuildComponents components = new BuildComponents();
      components.setDatastoreOptions(prepareDatastoreSelect(true));
      components.setRuntimeOptions(prepareRuntimeSelect(true));

      // Prepare workpool data for capture, but not rebuild.
      if(!forRebuild) {
         components.setWorkpoolOptions(prepareWorkpoolSelect(true));
      }
      return components;
   }

   @Override
   @Nonnull
   public Collection<DsDatastore> getCachedDatastores() {
      return _dsClient.getCachedDatastores();
   }

   @Override
   public Collection<DsDatastore> loadDatastores(@Nullable Collection<Long> filterByIds)
   throws DsException
   {
      // Load datastores from dsClient.
      DsDatastore[] datastores = _dsClient.getAllDatastores();
      if (filterByIds == null) {
         // No filtering needed return everything.
         return Arrays.asList(datastores);
      }

      // Create a unique set of datastoreIds.
      Set<Long> idSet = new HashSet<Long>(filterByIds);
      Set<DsDatastore> dsSet = new HashSet<DsDatastore>();
      for (int i = 0; i < datastores.length; i++) {
         if (idSet.contains(datastores[i].getId())) {
            dsSet.add(datastores[i]);
         }
      }
      return dsSet;
   }

   /**
    * Helper method to get a Map of datastoreId as key, and DsDatastore as value.
    *
    * @return
    * @throws DsException
    */
   @Override
   @Nonnull
   public Map<Long, DsDatastore> loadDatastoresMap(Collection<Long> filterByIds) throws DsException {
      Collection<DsDatastore> dsSet = loadDatastores(filterByIds);
      Map<Long, DsDatastore> dsMap = new HashMap<Long, DsDatastore>(dsSet.size(), 1);
      for (DsDatastore ds : dsSet) {
         dsMap.put(ds.getId(), ds);
      }
      return dsMap;
   }

   /**
    * Submit a single task to the conversion queue.
    * TODO: To optimize DB operations, use batch style DB session flushing after N records updated.
    *
    * @see http://docs.jboss.org/hibernate/orm/3.3/reference/en/html/batch.html
    * @param captureRequest a capture request.
    */
   protected void submitSingleTask(CaptureRequestImpl captureRequest) {
      final Application app = _daoFactory.getApplicationDao().find(
            captureRequest.getApplicationId());

      if (app == null) {
         /* Skip bad application IDs */
         _log.error("Application Id " + captureRequest.getApplicationId()
               + " does not exist, skipped");
         return;
      }
      // Set the suggest build name display name, and list of icons.
      captureRequest.setDisplayName(app.getDisplayName());
      captureRequest.setBuildName(app.getSuggestedBuildName());
      captureRequest.setIcons(app.getIcons());

      captureRequest.validateRequiredFields();

      /* Kick off a new task */
      _conversionsQueue.addTask(_taskFactory.newAppConvertTask(captureRequest,
            _conversionsQueue));
      /* Update build request total for this application */
      app.incrementBuildRequestsTotal();
      _daoFactory.getApplicationDao().update(app);

      if (_log.isDebugEnabled()) {
         _log.debug(String.format(
               "Queued a build task for %s with workpoolId=%d recipeId=%d runtimeId=%d datastoreId=%d",
               app.getDisplayName(), captureRequest.getWorkpoolId(), captureRequest.getRecipeId(),
               captureRequest.getRuntimeId(), captureRequest.getDatastoreId()));
      }
   }

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
   @Override
   public void submitTasks(@Nonnull final CaptureRequestImpl[] captureRequests)
      throws DsException, WpException, AfNotFoundException {

      if (captureRequests.length == 1) {
         submitSingleTask(captureRequests[0]);
         return;
      }

      final FutureTask<Void> future = new FutureTask<Void>(new Callable<Void>() {
         @Override
         public Void call() {
            for (CaptureRequestImpl req : captureRequests) {
               submitSingleTask(req);
            }
            String msg = String.format("All %d capture requests are being queued.", captureRequests.length);
            _log.debug(msg);
            NotificationService.INSTANCE.newInfoEvent(msg, Component.builds);
            return null;
         }
      });

      _executor.execute(future);
   }

   /**
    * Service method to flag build as edited and update runtime and hzSupport flag on the build.
    *
    * @param iniRequest
    * @param build
    * @param updateRuntime
    */
   @Override
   public void updateBuildRuntimeHzSupport(IniDataRequest iniRequest, Build build, boolean updateRuntime) {
      // Mark build as being edited and update with latest hzSupport flag and runtime value.
      if (updateRuntime) {
         // Update the runtime with the latest value.
         for (ThinAppRuntime rt : _config.getRuntimes()) {
            if (rt.getBuild() == iniRequest.getRuntimeId()) {
               build.setNewRuntime(rt.getVersion());
               break;
            }
         }
      } else {
         // Remove any previous newRuntime value that may have been applied when ini was previously saved.
         build.setNewRuntime(null);
      }
      build.setHzSupported(iniRequest.isHzSupported());
      build.setSettingsEdited(AfCalendar.Now());

      _daoFactory.getBuildDao().update(build);
   }

   /**
    * Creates a SelectOptions object for ThinApp runtimes.
    *
    * @param setInitialValue - Indicate if the initial value should be set.
    */
   @Override
   public SelectOptions prepareRuntimeSelect(boolean setInitialValue) {
      String defaultRt = _config.getString(ConfigRegistryConstants.THINAPP_RUNTIME_ID);
      if (!setInitialValue || defaultRt == null) {
         defaultRt = StringUtils.EMPTY;
      }

      // TODO Use a client whose url can be changed at runtime. Reusing what we have for now.
      List<ThinAppRuntime> runtimes = _config.getRuntimes();

      // If horizon enabled, display horizon enabled runtimes in its own select group.
      List<EachOption> optionsHorizonEnabled = new ArrayList<EachOption>();
      List<EachOption> optionsAll = new ArrayList<EachOption>();
      boolean hzEnable = _config.getBool(ConfigRegistryConstants.HORIZON_ENABLED);
      for (ThinAppRuntime rt : runtimes) {
         optionsAll.add(new SingleOption(String.valueOf(rt.getBuild()), rt.getVersion()));
      }

      // If Horizon enabled list exist, add them to a new optionGroup.
      if (optionsHorizonEnabled.size() != 0) {
         GroupOption group = new GroupOption(AfConstant.RUNTIME_GROUP_HORIZON,
               AfConstant.RUNTIME_HORIZON_ENABLE, true, optionsHorizonEnabled);
            optionsAll.add(group);
      }
      // Return the select options list.
      return new SelectOptions(defaultRt, optionsAll);
   }


   /**
    * Creates a SelectOptions object for the workpool data.
    * This is used for displaying a dropdown with available workpools on the
    * top and not available workpools in a separate group at the bottom.
    *
    * @param setInitialValue - Indicate if the initial value should be set.
    * @return
    * @throws WpException
    */
   private SelectOptions prepareWorkpoolSelect(boolean setInitialValue)
      throws WpException
   {
      String defaultWp = _config.getString(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL);
      if (!setInitialValue || defaultWp == null) {
         defaultWp = StringUtils.EMPTY;
      }

      List<EachOption> optionAvailable = new ArrayList<EachOption>();
      List<EachOption> optionNotAvailable = new ArrayList<EachOption>();

      List<Workpool> wpList = _wpClient.getAllWorkpools();
      Collections.sort(wpList);

      /* Split the workpool data into 2 groups based on available state */
      for (int ti = 0; ti < wpList.size(); ti++) {
         Workpool wp = wpList.get(ti);
         SingleOption option = new SingleOption(
               wp.getId().toString(),
               wp.getName());
         if (wp.getState() == Workpool.State.available) {
            optionAvailable.add(option);
         }
         else {
            optionNotAvailable.add(option);
            option.setDisabled(true);
         }
      }

      // Not Available workpools exist, add them to the optionGroup.
      if (optionNotAvailable.size() != 0) {
         GroupOption group = new GroupOption(
               AfConstant.NOT_AVAIL,
               true,
               optionNotAvailable);
            optionAvailable.add(group);
      }

      // Provide a nice error as a group name when drop down is displayed.
      if (optionAvailable.size() == 0) {
         optionAvailable.add(new GroupOption(AfConstant.NO_WORKPOOL, true));
      }
      return new SelectOptions(defaultWp, optionAvailable);
   }


   /**
    * Creates a SelectOptions object for the datastore data.
    * This is used for displaying a dropdown with online datastore on the
    * top and not online datastores in a separate group at the bottom.
    *
    * @param setInitialValue - Indicate if the initial value should be set.
    * @return
    * @throws WpException
    */
   @Override
   public SelectOptions prepareDatastoreSelect(boolean setInitialValue) throws DsException
   {
      String defaultDs = _config.getString(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID);
      if (!setInitialValue || defaultDs == null) {
         defaultDs = StringUtils.EMPTY;
      }
      List<EachOption> optionAvailable = new ArrayList<EachOption>();

      for (DsDatastore ds: _dsClient.getWritableDatastores()) {
         long freeBytes = ds.getSize() - ds.getUsed();
         String display = MessageFormat.format(
            "{0} ({1} Free)", // TODO: this needs proper localization!
            ds.getName(),
            StorageUnit.format(freeBytes)
         );
         SingleOption option = new SingleOption(
               ds.getId().toString(),
               display);
         optionAvailable.add(option);
      }

      return new SelectOptions(defaultDs, optionAvailable);
   }
}
