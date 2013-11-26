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

import org.apache.commons.lang.StringUtils;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.state.RebuildState;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * A task specific to the CWS rebuild of a project.
 */
class RebuildTask extends AbstractTask<
      RebuildState,
      RebuildState.Builder,
      RebuildState.RebuildStatus>
{
   /** current build instance */
   private final Build build;

   /**
    * Create a new instance of a rebuild task for the given build.
    *
    * @param build Build to rebuild.
    * @param taskHelperFactory Accessor to other factory objects.
    */
   RebuildTask(@Nonnull Build build,
               @Nonnull TaskHelperFactory taskHelperFactory)
   {
      super(taskHelperFactory,
            new RebuildState.Builder()
                  .withNewId(taskHelperFactory.getTaskIdSupplier())
                  .withRecordId(build.getId())
                  .withStatus(RebuildState.RebuildStatus.NEW)
                  .withDescription(
                        new StringBuilder(120)
                              .append("Rebuild of project for ")
                              .append(build.getName())
                              .append(" (")
                              .append(build.getVersion())
                              .append(") [")
                              .append(build.getBuildName())
                              .append(']')
                              .toString()
                  )
                  .build());
      this.build = build;
   }

   public void initialize()
      throws TaskException
   {
      try {
         /* Submit the rebuild request to CWS */
         getTaskHelperFactory().getCws().rebuildProject(build.getConverterProjectId());

         /* Request OK: mark the build as 'rebuilding' */
         build.setStatus(Build.Status.REBUILDING);
         getTaskHelperFactory().getDaoFactory().getBuildDao().update(build);
         updateStatus(RebuildState.RebuildStatus.REBUILDING);
      }
      catch(AfNotFoundException e) {
         /*
          * CWS doesn't know about the project, so it's probably been deleted
          * by someone else?  Fail in this case for now.
          */
         failTask();
         throw new TaskException(this, "Rebuilding build " + build.getBuildId() + ": " +
               "CWS project id " + build.getConverterProjectId() + " not known");
      }
      catch (CwsException e) {
         failTask();
         throw new TaskException(this, e);
      }
   }


   /**
    * Do the work.
    * This is the reason for the task; use this method to perform the entire
    * operation. Tasks are run in threads, so this will not be called again.
    *
    * @throws com.vmware.appfactory.taskqueue.exception.TaskException
    *
    */
   @Override
   protected void doRun() throws TaskException {
      initialize();

      boolean success = false;

      /*
       * We loop forever, checking periodically with CWS to see if there
       * are any updates. When CWS is done, so are we.
       */
      try {
         success = true;
         while (updateFromCws()) {
            sleepUntilNextUpdate();

            if (getCurrentTaskState().isAborted()) {
               // XXX send to CWS too!
               failTask();
               success = false;
               break;
            }
         }
      }
      catch(TaskException ex) {
         failTask();
         throw ex;
      }
      catch(InterruptedException ex) {
         // XXX send to CWS too!
         updateStatus(RebuildState.RebuildStatus.CANCELLED);
         _log.debug("Rebuild cancelled for project: {}", build.getBuildName());
         success = false;
      }
      finally {
         /* Update the status of the build. */

         // Flip to Staged so we are not in REBUILDING state for eternity.
         build.setStatus(Build.Status.STAGED);

         // Update the built time only if rebuild was successful.
         if (success) {
            build.setBuilt(AfCalendar.Now());
            if (StringUtils.isNotEmpty(build.getNewRuntime())) {
               // If another runtime is applied, newRuntime will be set. If it exists, make it the current runtime.
               build.setRuntime(build.getNewRuntime());
               build.setNewRuntime(null);
            }
         }
         // TODO Exception handler?
         getTaskHelperFactory().getDaoFactory().getBuildDao().update(build);
      }
   }

   @Override
   protected void doCleanup() throws TaskException {
      // nothing to do
   }


   /**
    * Ask CWS for rebuild status, and update our own status to match.
    * As long as the job is still waiting or working, we return true. As soon
    * as it stops, we return false.
    *
    * @return
    * @throws TaskException
    */
   private boolean updateFromCws()
      throws TaskException
   {
      boolean running;

      try {
         /* Get the rebuild status from CWS */
         Project project = getTaskHelperFactory().getCws().getProjectStatus(build.getConverterProjectId());
         Project.State projState = project.getState();

         switch(projState) {
            case dirty:
               /* The rebuild failed as we went from rebuild to dirty */
               updateStatus(RebuildState.RebuildStatus.FAILED);
               NotificationService.INSTANCE.newErrorEvent(
                     build.getBuildName() + "'s rebuild failed!",
                     Component.builds);
               throw new TaskException(this, "Rebuild failed!");

            case available:
               running = false;
               /** The rebuild succeeded, we then update the build with the new binary files from the project */
               build.extractAndReplaceBuildFiles(project);
               updateStatus(RebuildState.RebuildStatus.COMPLETE);
               NotificationService.INSTANCE.newInfoEvent(
                     "Project " + build.getBuildName() + " rebuild is ready!", Component.builds);
               break;

            case rebuilding:
               /* Still rebuilding, nothing to do */
               running = true;
               break;

            default:
               /* All other CwsProject states are unexpected, fail */
               NotificationService.INSTANCE.newErrorEvent(
                     build.getBuildName() + "'s rebuild failed!",
                     Component.builds);
               throw new TaskException(
                     this,
                     "Unexpected CWS state \"" + projState + '"');
         }
      }
      catch(Exception ex) {
         NotificationService.INSTANCE.newErrorEvent(
               "Converter update request failed!",
               Component.builds);
         throw new TaskException(this, "CWS update failed: " + ex.getMessage());
      }

      return running;
   }

   protected void failTask() {
      updateStatus(RebuildState.RebuildStatus.FAILED);
   }
}
