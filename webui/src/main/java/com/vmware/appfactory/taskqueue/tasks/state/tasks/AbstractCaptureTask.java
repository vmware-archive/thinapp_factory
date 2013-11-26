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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.AbstractCaptureStateBuilder;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * This is an abstraction layer for all ThinApp capture tasks. The common
 * components of managing capture history, etc will be implemented here.
 */
public abstract class AbstractCaptureTask
      <T extends AbstractCaptureState<T,S,E>,
       S extends AbstractCaptureStateBuilder<T,S,E>,
       E extends Enum<E>>
   extends AbstractTask<T,S,E> {
   protected final TaskQueue _conversionsQueue;
   protected final String _horizonUrl;

   /**
    * Constructor to set initial task state (including
    * the capture request).
    *
    * @param taskHelperFactory     Accessor to other factory objects.
    * @param initialState  The initial state of the task.  This can be obtained
    *                      later by calling getCurrentTaskState().
    *
    *                      This can only be updated by calling updateState or
    *                      updateStatus on the base class.
    * @param conversionsQueue queue to use when rebuilding
    * @param horizonUrl Horizon URL to configure captured project with
    */
   AbstractCaptureTask(TaskHelperFactory taskHelperFactory,
                       T initialState,
                       @Nonnull TaskQueue conversionsQueue,
                       @Nullable String horizonUrl)
   {
      super(taskHelperFactory, initialState);
      _conversionsQueue = conversionsQueue;
      _horizonUrl = horizonUrl;
   }

   /**
    * @return the _captureRequest
    */
   public CaptureRequest getCaptureRequest() {
      return getCurrentTaskState().getCaptureRequest();
   }

   /**
    * Create the AppBuildRequest entry with the data passed
    * and available in the captureRequest.
    *
    * @param workpool
    * @param runtimeId
    * @param isManualMode
    * @return null if build request was not created
    */
   protected final void createAppBuildRequest(Workpool workpool, Long runtimeId, boolean isManualMode) {
      // Set the appropriate app this request is associated with.
      Application app = new Application();
      app.setId(getCaptureRequest().getApplicationId());

      // Now create the AppBuildRequest object here.
      AppBuildRequest buildRequest = new AppBuildRequest();
      buildRequest.setOsType(workpool.getOsType().getOsTypeName());
      buildRequest.setOsVariant(workpool.getOsType().getOsVariantName());
      buildRequest.setManualMode(isManualMode);
      buildRequest.setRequestStage(RequestStage.created);
      buildRequest.setRecipeId(getCaptureRequest().getRecipeId());
      buildRequest.setDatastoreId(getCaptureRequest().getDatastoreId());
      buildRequest.setApplication(app);
      buildRequest.setRuntime(loadRuntimeStringById(runtimeId));

      // Create a record and store the record Id to send to the client
      Long buildRequestId = getTaskHelperFactory().getDaoFactory().getAppBuildRequestDao()
            .create(buildRequest);

      // Set the buildRequestId on this TaskState for later use.
      getCurrentTaskState().setBuildRequestId(buildRequestId);
   }

   /**
    * Update the requestStage for this capture record.
    *
    * @param requestStage
    */
   protected final void updateAppBuildRequestStage(RequestStage requestStage) {
      // Do not propagate when fails. Used as history and works without it.
      try {
         getTaskHelperFactory().getDaoFactory().getAppBuildRequestDao()
            .updateBuildRequestStage(getCurrentTaskState().getBuildRequestId(), requestStage);
      }
      catch (Exception e) {
         _log.error(e.getMessage(), e);
      }
   }


   /**
    * Create a new build for converted application.
    *
    * @param project
    * @param isManualMode - Boolean to distinguish captures: auto from manual
    * @return a new build id or null on failure.
    */
   protected final Long createNewBuild(Project project, boolean isManualMode) throws AfNotFoundException {
      final Application app = getTaskHelperFactory().getDaoFactory().getApplicationDao().find(
            getCaptureRequest().getApplicationId());

      /* Check if conversion finished but the app was removed */
      if (app == null) {
         throw new AfNotFoundException ("Conversion of " + getCaptureRequest().getDisplayName() +
               " finished, but application deleted.");
      }

      Build.Source src = isManualMode ? Build.Source.MANUAL_CAPTURE : Build.Source.AUTO_CAPTURE;
      Build build = Build.newFromProject(app, project, getCaptureDisplayName(),
            loadRuntimeStringById(getCaptureRequest().getRuntimeId()), null,
            getCaptureRequest().isAddHorizonIntegration(), src);

      // Get the OsType info and set it on the build.
      Workpool wp = getTaskHelperFactory().getCachedWorkpool(getCaptureRequest().getWorkpoolId());
      OsType osType = wp.getOsType();
      build.setOsType(osType.getOsTypeName());
      build.setOsVariant(osType.getOsVariantName());

      Long buildId = getTaskHelperFactory().getDaoFactory().getBuildDao().create(build);

      updateBuildIconUrls(build);

      getTaskHelperFactory().getDaoFactory().getBuildDao().update(build);
      // Update the buildRequest record with this buildId and fire notification.
      notifyAndUpdateAppBuildRequestBuildId(buildId, isManualMode);

      if (_log.isDebugEnabled()) {
         _log.debug("A new build {} created.", buildId);
      }

      return buildId;
   }

   /**
    * Update the localUrl fields of all BuildIcons that are part of the given build.
    * The URLs will be updated to reference the build API instead of the app API.
    *
    * @param build the build to update
    */
   protected final void updateBuildIconUrls(Build build) {
      List<? extends AfIcon> icons = build.getIcons();
      if (icons != null) {
         for(int iconPos = 0; iconPos < icons.size(); iconPos++) {
            AfIcon icon = icons.get(iconPos);
            if (icon != null) {
               String localUrl = buildIconUrl("builds", build.getId(), iconPos, icon.getIconHash());
               icon.setLocalUrl(localUrl);
            }
         }

         // Persist all changes that were made to the local URLs
         getTaskHelperFactory().getDaoFactory().getBuildDao().update(build);
      }
   }

   /**
    * This method updated the build request record with the newly created buildId.
    *
    * @param buildId
    * @param isManualMode
    */
   protected final void notifyAndUpdateAppBuildRequestBuildId(Long buildId, boolean isManualMode) {
      if (buildId == null) {
         return;
      }

      // Notify of a successful build.
      NotificationService.INSTANCE.newInfoEvent(String.format("Project %s of %s is ready.",
            getCaptureDisplayName(), getCaptureRequest().getDisplayName()),
            (isManualMode? Component.manualCapture : Component.autoCapture));

      try {
         getTaskHelperFactory().getDaoFactory().getAppBuildRequestDao()
            .updateBuildRequestBuildId(getCurrentTaskState().getBuildRequestId(), buildId);
      }
      catch (Exception e) {
         _log.error(e.getMessage(), e);
      }
   }

   /**
    * This method notifies the user of the failure and updates failCount on the app table.
    *
    * @param isManualMode
    */
   protected final void notifyFailureAndUpdateApp(boolean isManualMode) {
      // Notify a build failure.
      NotificationService.INSTANCE.newErrorEvent(String.format("Project %s of %s failed to build.",
            getCaptureDisplayName(), getCaptureRequest().getDisplayName()),
            (isManualMode? Component.manualCapture : Component.autoCapture));

      ApplicationDao dao = getTaskHelperFactory().getDaoFactory().getApplicationDao();
      Application app = dao.find(getCaptureRequest().getApplicationId());
      if (app != null) {
         app.setFailCount(app.getFailCount() + 1);
         dao.update(app);
      }
   }

   /**
    * Helper method to get runtime string by runtimeId
    *
    * @param runtimeId
    * @return
    */
   private String loadRuntimeStringById(Long runtimeId) {
      String rtVersion = StringUtils.EMPTY;
      // Do not propagate when it fails. Used for history and work without it.
      ThinAppRuntime rt = getCaptureRequest().getRuntime();
      if (null == rt) {
         _log.error("Could not get runtime when creating build request " + runtimeId);
      } else{
         rtVersion = rt.getVersion();
      }
      return rtVersion;
   }

   /**
    * Creates a unique build name for this capture.
    * @return
    */
   private String getCaptureDisplayName() {
      String buildName = getCaptureRequest().getBuildName();

      if (getCurrentTaskState().getBuildRequestId() != null) {
         buildName += "." + getCurrentTaskState().getBuildRequestId();
      }
      return buildName;
   }
}
