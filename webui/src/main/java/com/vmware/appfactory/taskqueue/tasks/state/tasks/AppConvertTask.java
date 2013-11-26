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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.cws.CwsHelper;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.AppConvertStateBuilder;
import com.vmware.appfactory.taskqueue.tasks.state.util.StallDetector;
import com.vmware.thinapp.common.converter.dto.ConversionJobStatus;
import com.vmware.thinapp.common.converter.dto.ConversionRequest;
import com.vmware.thinapp.common.converter.dto.ConversionResponse;
import com.vmware.thinapp.common.converter.dto.ConversionResult;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * A task specific to the CWS conversion of an application for the first
 * time (i.e. not a project rebuild).
 */
class AppConvertTask
   extends AbstractCaptureTask<AppConvertState,AppConvertStateBuilder,AppConvertState.AppConvertStatus>
{
   private static final Set<AppConvertState.AppConvertStatus> COMPLETED_STATES =
         ImmutableSet.of(
               AppConvertState.AppConvertStatus.failed,
               AppConvertState.AppConvertStatus.complete,
               AppConvertState.AppConvertStatus.cancelled
         );
   private final StallDetector stallDetector;
   private final boolean enableQR;
   private final String tagQR;

   /**
    * Create a new instance for the conversion of the specified application.
    * Nothing will be done until this task is added to the task queue.
    *
    * @param captureRequest      App which we are to build
    * @param taskHelperFactory   Accessor for global objects.
    * @param conversionsQueue
    * @param horizonUrl
    * @param stallCpu
    * @param stallNet
    * @param stallDisk
    * @param stallTimeout
    */
   AppConvertTask(@Nonnull CaptureRequest captureRequest,
                  @Nonnull TaskHelperFactory taskHelperFactory,
                  @Nonnull TaskQueue conversionsQueue,
                  @Nullable String horizonUrl,
                  long stallCpu,
                  long stallNet,
                  long stallDisk,
                  long stallTimeout,
                  boolean enableQR,
                  String tagQR) {
      super(taskHelperFactory,
            new AppConvertStateBuilder()
                  .withNewId(taskHelperFactory.getTaskIdSupplier())
                  .withRecordId(captureRequest.getApplicationId())
                  .withCaptureRequest(captureRequest)
                  .withStatus(AppConvertState.AppConvertStatus.newtask)
                  .withDescription("Conversion of " + captureRequest.getDisplayName())
                  .build(), conversionsQueue, horizonUrl);
      stallDetector = new StallDetector(_log,
                                        stallCpu,
                                        stallNet,
                                        stallDisk,
                                        stallTimeout,
                                        POLL_FREQ_SECS);
      this.enableQR = enableQR;
      this.tagQR = tagQR;
   }

   public static AppConvertState.AppConvertStatus fromConverterState(ConversionJobStatus status) {
      switch(status.getState()) {
         case created: return AppConvertState.AppConvertStatus.created;
         case downloading: return AppConvertState.AppConvertStatus.downloading;
         case provisioning: return AppConvertState.AppConvertStatus.provisioning;
         case precapture: return AppConvertState.AppConvertStatus.precapture;
         case preinstall: return AppConvertState.AppConvertStatus.preinstall;
         case install: return AppConvertState.AppConvertStatus.install;
         case postinstall: return AppConvertState.AppConvertStatus.postinstall;
         case postcapture: return AppConvertState.AppConvertStatus.postcapture;
         case projectgen: return AppConvertState.AppConvertStatus.generate;
         case prebuild: return AppConvertState.AppConvertStatus.prebuild;
         case projectbuild: return AppConvertState.AppConvertStatus.build;
         case projectrefresh: return AppConvertState.AppConvertStatus.refresh;
         case finishing: return AppConvertState.AppConvertStatus.finishing;
         case finished:
            if (ConversionResult.Disposition.failed.equals(status.getResult().getDisposition())) {
               return AppConvertState.AppConvertStatus.failed;
            }
            return AppConvertState.AppConvertStatus.complete;
         case cancelling: return AppConvertState.AppConvertStatus.cancelling;
         case cancelled: return AppConvertState.AppConvertStatus.cancelled;
      }
      throw new IllegalStateException("Unknown converter state: " + status.getState());
   }

   protected void initialize() throws TaskException {
      try {
         /* Get the recipe, if any */
         CaptureRequest request = getCurrentTaskState().getCaptureRequest();
         Recipe recipe = null;
         if (request.getRecipeId() != null) {
            recipe = getTaskHelperFactory().getDaoFactory().getRecipeDao().find(request.getRecipeId());
            if (recipe == null) {
               throw new TaskException(this,
                     "The specified recipe #" + request.getRecipeId() + " does not exist.");
            }
         }

         /* Get the latest download and installs */
         ApplicationDao appDao = getTaskHelperFactory().getDaoFactory().getApplicationDao();
         Application app = appDao.find(request.getApplicationId());

         if (app == null) {
            throw new TaskException(
                  this,
                  "Can't convert " + getCurrentTaskState().getCaptureRequest().getDisplayName() + ": " +
                        "Application no longer exists");
         }

         Workpool workpool = getTaskHelperFactory().getCachedWorkpool(request.getWorkpoolId());

         /* Use a helper method to create a ConversionRequest object */
         ConversionRequest jobRequest = CwsHelper.generateConversionRequest(
               app.getDownload(),
               app.getInstalls(),
               recipe,
               request.getRecipeVariableValues(),
               request.getDatastoreId(),
               workpool,
               request.getRuntimeId(),
               enableQR,
               tagQR);

         /* Submit the CwsJobRequest to CWS */
         final ConversionResponse jobResponse =
               getTaskHelperFactory().getCws().submitJobRequest(jobRequest);

         updateState(
               new Function<AppConvertStateBuilder, AppConvertState>() {
                  @Override
                  public AppConvertState apply(AppConvertStateBuilder builder) {
                     return builder.withConverterId(jobResponse.getJobId()).build();
                  }
               }
         );

         // Invoke creation of the AppBuildRequest history record.
         createAppBuildRequest(
               workpool,
               request.getRuntimeId(),
               false);
      } catch (Exception e) {
         _log.error("Could not initialize task: " + e.getMessage(), e);
         throw new TaskException(this, "Could not initialize task", e);
      }
   }


   @Override
   protected void doRun() throws TaskException {

      try {
         initialize();

         // Store the running stage for AppBuildRequest
         updateAppBuildRequestStage(RequestStage.running);

         /*
         * We loop forever, checking periodically with CWS to see if there
         * are any updates. When CWS is done, so are we.
         */
         while (true) {
            updateFromCws();
            AppConvertState.AppConvertStatus status = getCurrentTaskState().getStatus();
            if (COMPLETED_STATES.contains(status)) {
               break;
            }
            sleepUntilNextUpdate();

            if (getCurrentTaskState().isAborted()) {
               handleTaskCancel();
               break;
            }
         }

         if (getCurrentTaskState().getStatus().equals(AppConvertState.AppConvertStatus.complete)) {
            try {
               // When a CWS job is complete, create a build from the CWS project.
               final Project project = getTaskHelperFactory().getCws().getProjectStatus(
                     getCurrentTaskState().getProjectId());
               final Long buildId = createNewBuild(project, false);
               updateState(
                     new Function<AppConvertStateBuilder, AppConvertState>() {
                        @Override
                        public AppConvertState apply(AppConvertStateBuilder builder) {
                           return builder.withBuildId(buildId)
                                         .build();
                        }
                     }
               );
            } catch(Exception ex) {
               throw new TaskException(
                     this,
                     "Creation of build from conversion failed!",
                     ex
               );
            }
         }
      }
      catch (TaskException e) {
         handleBuildFailure(e.getMessage());
         throw e;
         // set failed
      }
      catch(InterruptedException ex) {
         handleTaskCancel();
      }
      finally {
         // Store the cancel / fail / success stage for AppBuildRequest
         RequestStage reqStage = (getCurrentTaskState().getStatus() == AppConvertState.AppConvertStatus.failed)?
               RequestStage.failed : (getCurrentTaskState().getStatus() == AppConvertState.AppConvertStatus.cancelled)?
                        RequestStage.cancelled : RequestStage.successful;
         updateAppBuildRequestStage(reqStage);
      }
   }

   @Override
   protected void doCleanup() throws TaskException {
      doCleanupConversionTask(getCurrentTaskState().getStatus(),
            AppConvertState.AppConvertStatus.complete,
            getCurrentTaskState().getProjectId(),
            getCaptureRequest().getDisplayName());
   }

   /**
    * Unstall this conversion task by clearing out the performance data buffer.
    */
   @Override
   public void unstall() {
      _log.debug(
            String.format("Attempting to unstall conversion task %s",
                  getCaptureRequest().getDisplayName()));
      stallDetector.unstall();
   }

   /**
    * Ask CWS for job status, and update our own status to match.
    * As long as the job is still waiting or working, we return true. As soon
    * as it stops, we return false.
    *
    * @throws TaskException      if we failed to query the state of the task at
    *                            any point, or the call we made threw an exception
    *                            from the backend.
    */
   private void updateFromCws()
      throws TaskException
   {
      final ConversionJobStatus cwsStatus;
      try {
         /* Get the update from CWS */
         final CwsClientService cws = getTaskHelperFactory().getCws();
         cwsStatus = cws.getConversionStatus(getCurrentTaskState().getConverterId());
      }
      catch(Exception ex) {
         throw new TaskException(
               this,
               "CWS update failed for task " + getCurrentTaskState().getId() + ": " + ex.getMessage(),
               ex);
      }

      updateState(new Function<AppConvertStateBuilder, AppConvertState>() {
         @Override
         public AppConvertState apply(@Nullable AppConvertStateBuilder builder) {
            return update(cwsStatus, builder);
         }
      });
   }

   protected AppConvertState update(ConversionJobStatus cwsStatus,
                                  AppConvertStateBuilder builder) {

      AppConvertState.AppConvertStatus newStatus = fromConverterState(cwsStatus);
      builder.withIsStalled(checkForStall(cwsStatus, newStatus));

      builder.withStatus(newStatus);
      switch(newStatus) {
         case newtask:
         case queued:
         case created:
            builder.withProgress(-1);
            break;

         case downloading:
         case provisioning:
         case precapture:
         case preinstall:
         case install:
         case postinstall:
         case postcapture:
         case generate:
         case prebuild:
         case build:
         case refresh:
         case finishing:
         case cancelling:
            builder.withProgress(cwsStatus.getPercent());
            break;

         case complete:
            builder.withProgress(100);
            break;

         case cancelled:
            break;

         case failed:
            builder.withLastCommand(cwsStatus.getResult().getLastCommand())
                   .withLastError(cwsStatus.getResult().getLastError())
                   .withLastRunningState(cwsStatus.getResult().getLastRunningState());
      }

      builder.withProjectId(cwsStatus.getProjectId());
      return builder.build();
   }

   private boolean checkForStall(ConversionJobStatus cwsStatus,
                                 AppConvertState.AppConvertStatus appConvertStatus) {

      PerformanceData perfData = cwsStatus.getPerformanceData();
      try {
         stallDetector.addPerformanceData(perfData);
      }
      catch (Exception ex) {
         // Ignore: monitoring isn't critical enough to fail the job
         _log.error("Performance data update failed.", ex);
         return false;
      }

      // If the buffer is full, we've seen a full sequence of performance
      // data over the defined time limit and all data was below the
      // defined threshold: we've detected a stall!
      if (appConvertStatus != AppConvertState.AppConvertStatus.complete &&
            appConvertStatus !=  AppConvertState.AppConvertStatus.failed &&
            stallDetector.isStalled()) {

         // Only log that the job is stalled when we first transition to the
         // stalled state
         if (!getCurrentTaskState().getIsStalled()) {
            _log.debug("Performance data buffer is full, conversion job is stalled.");
         }
         return true;
      }
      return false;
   }

   private void handleTaskCancel()
   {
      CwsException exception = null;
      try {
         _log.debug("Cancelling conversion task {}", getCaptureRequest().getDisplayName());
         getTaskHelperFactory().getCws().cancelConversion(getCurrentTaskState().getConverterId());
      } catch (CwsException e) {
         // eat the exception, nom nom nom
         exception = e;
      }
      final boolean cancelFailed = (null != exception);

      updateState(
            new Function<AppConvertStateBuilder, AppConvertState>() {
               @Override
               public AppConvertState apply(AppConvertStateBuilder builder) {

                  if (cancelFailed) {
                     builder.withStatus(AppConvertState.AppConvertStatus.failed);
                  } else {
                     builder.withStatus(AppConvertState.AppConvertStatus.cancelled);
                  }
                  return builder.build();
               }
            }
      );
   }

   /**
    * Update _status and increment _application.failCount and
    * throw TaskException
    *
    * @param exceptionMessage
    * @throws TaskException
    */
   private void handleBuildFailure(final String exceptionMessage) throws TaskException
   {
      updateState(
            new Function<AppConvertStateBuilder, AppConvertState>() {
               @Override
               public AppConvertState apply(AppConvertStateBuilder builder) {

                  builder.withStatus(AppConvertState.AppConvertStatus.failed)
                        .withProgress(-1);
                  return builder.build();
               }
            }
      );

      /*
       * Update the current record in the database, not our
       * cached copy, in case it was updated elsewhere.
       */
      notifyFailureAndUpdateApp(false);
      throw new TaskException(this, exceptionMessage);
   }
}
