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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dto.ProjectImportResponse;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.MutableApplicationKey;
import com.vmware.appfactory.cws.CwsSettingsIni;
import com.vmware.appfactory.cws.CwsSettingsIniData;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState.ImportProjectStatus;
import com.vmware.appfactory.taskqueue.tasks.state.builder.ImportProjectStateBuilder;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;

/**
 * This task tracks all the progress of importing projects
 * from a datastore. This task does the following:
 * 1. Invoke projects/import to CWS to initially count the projects and
 *    create new projects in CWS. The response contains a list of project ids.
 * 2. Refresh each projects in CWS.
 * 3. Extract meta-data and update Package.ini if OutDir != bin
 * 4. Save new projects in webui DB.
 * For any failure during the above steps except step 4, it will compensate
 * every failed project.
 *
 * NOTE: Since CWS APIs don't support batch calls yet, step 2 and 3 are potential
 * scalability bottlenecks.
 */
class ImportProjectTask
   extends AbstractTask<
      ImportProjectState,
      ImportProjectStateBuilder,
      ImportProjectState.ImportProjectStatus>
{
   /**
    *
    */
   private static final String DELETED_STATE = "deleted";
   /**
    * The directory to store ThinApp's rebuild output
    */
   private static final String DEFAULT_BUILD_OUTPUT_DIR = "bin";
   /**
    * OutDir parameter of Package.ini
    */
   private static final String OUT_DIR_PARAM = "OutDir";
   /**
    * Default application name
    */
   private static final String DEFAULT_APP_NAME = "Unknown";
   /**
    * InventoryName parameter of Package.ini
    */
   private static final String INVENTORY_NAME_PARAM = "InventoryName";
   /**
    * Application version number regex.
    */
   private static final Pattern APP_VERSION_PATTERN = Pattern.compile("^\\d+[.\\d]+$");
   /**
    * Application version number with architecture regex. E.g. Mozilla FireFox 3.5 (x86 US)
    */
   private static final Pattern APP_VERSION_WITH_ARCH_PATTERN = Pattern.compile("^.+\\s\\d+[.\\d]+\\s\\(.+\\)$");
   private static final Pattern SPACE_SPLITTER = Pattern.compile(" ");

   /* Set progress percentage for each step */
   private final int projectScannedStepProgress = 5;
   private final int projectRefreshedStepProgress = 75;
   private final int projectSavedStepProgress = 100;

   private final Long runtimeId;
   private final boolean enableQR;
   private final String tagQR;
   private final boolean appliedHorizonSupport = false;

   /**
    * Create a new instance of an import task.
    *
    * @param datastore a datastore to scan.
    * @param taskHelperFactory   Accessor for global objects.
    * @param runtimeId    id of the ThinApp runtime to create the project with
    * @param horizonUrl          URL to Horizon organization.
    */
   ImportProjectTask(@Nonnull DsDatastore datastore,
                     @Nonnull TaskHelperFactory taskHelperFactory,
                     @Nonnull Long runtimeId,
                     boolean addHorizonSupport,
                     @Nullable String horizonUrl,
                     boolean enableQR,
                     String tagQR)
   {
      super(taskHelperFactory,
            new ImportProjectStateBuilder()
                  .withNewId(taskHelperFactory.getTaskIdSupplier())
                  .withRecordId(datastore.getId())
                  .withStatus(ImportProjectState.ImportProjectStatus.NEW)
                  .withDescription("Importing projects from " + datastore.getName() + " datastore")
                  .build()
      );
      this.runtimeId = Preconditions.checkNotNull(runtimeId);
      this.enableQR = enableQR;
      this.tagQR = tagQR;
   }

   /**
    * Run this task
    */
   @Override
   public void doRun()
      throws TaskException
   {
      logInfo("Starting a new import project task...", true);

      final Set<Long> failedProjectIds = new HashSet<Long>();

      try {
         /* Step 1 - Create empty projects in CWS */
         updateStatus(ImportProjectState.ImportProjectStatus.CREATING_PROJECTS);
         final ProjectImportResponse response = getTaskHelperFactory().getCws().importProjects(
               getCurrentTaskState().getRecordId(), runtimeId);

         int numValid = response.getNewProjects().size();
         int numInvalid = response.getErrors().size();

         String logMsg = "";
         boolean hasAnyValidProject = false;

         if (numValid == 0 && numInvalid == 0) {
            logMsg = "No projects found in the datastore.";
            logInfo(logMsg, true);
         } else if (numValid == 0 && numInvalid != 0) {
            logMsg = String.format("Found %d invalid project(s) in the datastore.",
                  numInvalid);
            logWarn(logMsg, true);
         } else {
            hasAnyValidProject = true;
            logMsg = String.format("Found %d valid project(s) and %d invalid project(s) in the datastore.",
                  numValid, numInvalid);
            logInfo(logMsg, true);
         }

         final int numFound = numValid + numInvalid;

         if (hasAnyValidProject) {
            update(response, numFound, projectScannedStepProgress, ImportProjectStatus.REFRESHING_PROJECTS);
         } else {
            update(response, numFound, projectSavedStepProgress, ImportProjectStatus.COMPLETE);
            return;
         }

         if (getCurrentTaskState().isAborted()) {
            handleCancel();
            return;
         }

         /* Step 2 - Refresh all imported projects in CWS */
         final int progressStep =  (projectRefreshedStepProgress - projectScannedStepProgress) / numValid;
         for (Long id : response.getNewProjects().keySet()) {
            try {
               getTaskHelperFactory().getCws().refreshProjectSettings(id);
            } catch (Exception e) {
               logErr("Failed to refresh a project in '" + response.getNewProjects().get(id) + "'", false);
               failedProjectIds.add(id);
            }
            incrementProgressBy(progressStep);

            if (getCurrentTaskState().isAborted()) {
               handleCancel();
               return;
            }
         }

         /* Step 3 - Create Web UI projects */
         updateStatusWithProgress(ImportProjectStatus.SAVING_PROJECTS, projectRefreshedStepProgress);
         saveNewProjects(failedProjectIds, response.getNewProjects());
      }
      catch(Exception ex) {
         updateStatus(ImportProjectState.ImportProjectStatus.FAILED);
         logErr("Importing projects failed due to " + ex.getMessage(), false);
      }
      finally {
         if (!failedProjectIds.isEmpty()) {
            setDeletedState(failedProjectIds);
         }
      }
   }

   @Override
   protected void doCleanup() throws TaskException {
      // nothing to do
   }

   /**
    * Update the import project state with details info.
    *
    * @param response a ProjectImportResponse instance.
    * @param numFound a total number of projects found.
    * @param progress an current progress (percentage) of the task.
    * @param status a current status of the task.
    */
   private void update(final ProjectImportResponse response,
         final int numFound, final int progress, final ImportProjectStatus status) {
      updateState(new Function<ImportProjectStateBuilder, ImportProjectState>() {
         @Override
         public ImportProjectState apply(ImportProjectStateBuilder builder) {
            return builder
                  .withNumFound(numFound)
                  .withErrors(reportInvalidProjectErrors(response))
                  .withStatus(status)
                  .withProgress(progress)
                  .build();
         }
      });
   }

   /**
    * Update 'lastError' message of the task.
    * @param errMsg an error message.
    */
   private void error(final String errMsg) {
      getCurrentTaskState().getErrors().add(errMsg);
      updateState(new Function<ImportProjectStateBuilder, ImportProjectState>() {
         @Override
         public ImportProjectState apply(ImportProjectStateBuilder builder) {
            return builder.withLastError(errMsg).build();
         }
      });
   }

   /**
    * Generate a detailed error list from the ProjectImportResponse.
    * @param response a ProjectImportResponse instance.
    * @return a list of errors that found during initial project scan.
    */
   private static List<String> reportInvalidProjectErrors(ProjectImportResponse response) {
      final List<String> errors = new ArrayList<String>();

      for (String prjName : response.getErrors().keySet()) {
         StringBuilder sb = new StringBuilder();
         sb.append('\'' + prjName + "' project was missing ");
         List<String> files = response.getErrors().get(prjName);
         Iterator<String> it = files.iterator();
         while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
               sb.append(" file, ");
            } else {
               sb.append(" file.");
            }
         }
         errors.add(sb.toString());
      }
      return errors;
   }

   /**
    * Delete all WIP imported projects from CWS.
    */
   private void handleCancel()
   {
      updateStatus(ImportProjectState.ImportProjectStatus.CANCELLING);

      setDeletedState(getCurrentTaskState().getImportedProjectIds());
      updateStatus(ImportProjectState.ImportProjectStatus.CANCELLED);
   }


   /**
    * Set all failed projects to the 'deleted' state.
    * Don't call _cws.deleteProject(xx) because that will actually
    * wipe out all the project folders from the file share.
    */
   private void setDeletedState(Iterable<Long> ids) {
      try {
         for (Long id : ids) {
            getTaskHelperFactory().getCws().updateProjectState(id, DELETED_STATE);
         }
      } catch (Exception e) {
         logErr("Failed to update the project's state due to the CWS communication failure.", false);
      }
   }

   /**
    * Create a new build for an imported project.
    * @param failedProjectIds a set of failed project ids.
    * @param importedProjects a map of imported projects {id -> project dir}
    */
   private void saveNewProjects(final Set<Long> failedProjectIds, final Map<Long, String> importedProjects) {
      final Collection<Project> projects = new ArrayList<Project>(importedProjects.size());

      for (Long id : importedProjects.keySet()) {
         if (failedProjectIds.contains(id)) {
            continue;
         }
         Project project;
         try {
            project = getTaskHelperFactory().getCws().getProjectStatus(id);
         } catch (Exception e) {
            logErr("Failed to look up status of project '" + importedProjects.get(id) + "'", false);
            failedProjectIds.add(id);
            continue;
         }
         projects.add(project);
      }

      if (getCurrentTaskState().isAborted()) {
         handleCancel();
         return;
      }

      for (Project project: projects) {
         Application app = getAppMetaDataAndUpdatePackageIni(project);
         if (app == null) {
            failedProjectIds.add(project.getId());
            continue;
         }

         ThinAppRuntime newRuntime = getTaskHelperFactory().getCachedRuntime(project.getRuntimeId());
         Build build = Build.newFromProject(app, project, app.getName() + " -Import", "N/A",
               newRuntime.getVersion(), appliedHorizonSupport, Build.Source.IMPORT);
         getTaskHelperFactory().getDaoFactory().getBuildDao().create(build);
      }

      final int numSuccess = importedProjects.size() - failedProjectIds.size();
      logInfo(numSuccess + " projects were successfully imported.", true);

      updateState(new Function<ImportProjectStateBuilder, ImportProjectState>() {
         @Override
         public ImportProjectState apply(ImportProjectStateBuilder builder) {
            return builder.withNumImported(numSuccess)
                  .withStatus(ImportProjectState.ImportProjectStatus.COMPLETE)
                  .withProgress(projectSavedStepProgress)
                  .build();
         }
      });
   }

   /**
    * Extract meta-data from Package.ini and update it if needed.
    *
    * @param project - a project instance.
    * @return a new application instance if parsing name from 'InventoryName'
    *  was successful. So, version and icon are optional. Otherwise, it will
    *  return null for any failure.
    */
   private Application getAppMetaDataAndUpdatePackageIni(Project project)
   {
      final Application app = new Application();
      String errTemplate = "Project '%s' was missing '%s' in the Package.ini file.";

      try {
         CwsSettingsIni ini = getTaskHelperFactory().getCws().getProjectPackageIni(project.getId());
         if (!ini.containsKey(CwsSettingsIni.BUILD_OPTIONS)) {
            logErr(String.format(errTemplate, project.getSubdir(), CwsSettingsIni.BUILD_OPTIONS), true);
            return null;
         }

         CwsSettingsIniData buildOptions = ini.get(CwsSettingsIni.BUILD_OPTIONS);
         /** Get App Name and Version from 'InventoryName' */
         if (!buildOptions.containsKey(INVENTORY_NAME_PARAM)) {
            logErr(String.format(errTemplate, project.getSubdir(), INVENTORY_NAME_PARAM), true);
            return null;
         }
         parseAppNameAndVersion(app, buildOptions.get(INVENTORY_NAME_PARAM));

         boolean hasIniChanges = false;
         /** Ensure 'OutDir' is set to 'bin' in the Package.ini */
         if (buildOptions.containsKey(OUT_DIR_PARAM)) {
            String outDir = buildOptions.get(OUT_DIR_PARAM);
            if (!outDir.equals(DEFAULT_BUILD_OUTPUT_DIR)) {
               // Fix 'outDir' setting.
               buildOptions.put(OUT_DIR_PARAM, DEFAULT_BUILD_OUTPUT_DIR);
               hasIniChanges = true;
            }
         } else {
            logErr(String.format(errTemplate, project.getSubdir(), "OutDir"), true);
         }

         hasIniChanges = hasIniChanges || applyQualityReporting(project, buildOptions);

         if (hasIniChanges) {
            updatePackageIni(project.getId(), ini, app.getName());
         }
      } catch (Exception e) {
         logErr("Failed to extract application meta-data from Packge.ini of project '" + project.getSubdir() + "'", true);
         return null;
      }

      return app;
   }

   /**
    * Apply quality reporting when a project has EXE files under bin and enableQR flag is ON.
    *
    * @param project a project instance.
    * @param buildOptions a buildOption section of Package.ini
    * @return true if the given project needs to update its Package.ini file to enable quality
    * reporting; otherwise, it will return false.
    */
   private boolean applyQualityReporting(Project project, CwsSettingsIniData buildOptions) {
      /** If a project has executable binary files or the QR flag is off, then stop here! **/
      if (!this.enableQR || !CollectionUtils.isEmpty(project.getFiles())) {
         return false;
      }
      boolean hasQRKeys = buildOptions.containsKey(CwsSettingsIni.QR_ENABLED_KEY) &&
         buildOptions.containsKey(CwsSettingsIni.QR_TAG_KEY);
      boolean hasQRValues = "1".equals(buildOptions.get(CwsSettingsIni.QR_ENABLED_KEY)) &&
         this.tagQR.equals(buildOptions.get(CwsSettingsIni.QR_TAG_KEY));

      if (!hasQRKeys || !hasQRValues) {
         buildOptions.put(CwsSettingsIni.QR_ENABLED_KEY, "1");
         buildOptions.put(CwsSettingsIni.QR_TAG_KEY, this.tagQR);
         return true;
      } else {
         /**
          * If both QR keys are present and have the right settings that means the project
          * is already enabled for quality reporting, then it returns false so that we don't
          * need to call CWS to update Package.ini.
          */
         return false;
      }
   }

   /**
    * Update the Package.ini of the given project id.
    *
    * @param projectId a project id.
    * @param ini a CwsSettingsIni(Package.ini) instance.
    * @param projectName a project name
    * @return true if the update package ini CWS call was successful; otherwise, it
    *    will return false.
    */
   private boolean updatePackageIni(Long projectId, CwsSettingsIni ini, String projectName)
      throws Exception
   {
      try {
         getTaskHelperFactory().getCws().updateProjectPackageIni(projectId, ini);
         return true;
      } catch (Exception e) {
         logErr("Failed to update Package.ini's of " + projectName, true);
         return false;
      }
   }

   /**
    * Parse application name and version from the given string.
    * ThinApp uses this format to store app meta-data in 'InventoryName':
    *    {NAME} {VERSION} {OTHERS}
    *
    * If the string is:
    *  1. empty, then set default app name.
    *  2. ...NO_DIGIT_STRING..., then set the string as app name.
    *  3. XX_X...X_Y_ZZZ or XX_X..X_Y , set all Xs to app name and Y to app version.
    * @param app - an application instance.
    * @param nameAndVersion - a string to be parsed.
    */
   private static void parseAppNameAndVersion(MutableApplicationKey app, String nameAndVersion)
   {
      if (StringUtils.isBlank(nameAndVersion)) {
         app.setName(DEFAULT_APP_NAME);
         return;
      }

      String[] str = SPACE_SPLITTER.split(nameAndVersion);
      /* Assume the last token is version. E.g. 7-Zip 1.0 */
      int versionToken = str.length - 1;
      /* However, if the string matches this format - {NAME} {VERSION} ({ARCHITECTURE} {LOCALE}) */
      if (APP_VERSION_WITH_ARCH_PATTERN.matcher(nameAndVersion).matches()) {
         versionToken = str.length - 3;
      }
      for (int i = 0; i < str.length; i++) {
         String token = str[i];
         if (StringUtils.isNotBlank(token)) {
            /* The first token always goes to the App Name */
            if (StringUtils.isBlank(app.getName())) {
               app.setName(token);
            } else {
               if (i == versionToken && APP_VERSION_PATTERN.matcher(token).matches()) {
                  app.setVersion(token);
                  break;
               }
               app.setName(app.getName() + ' ' + token);
            }
         }
      }
   }

   private void logErr(String msg, boolean alert)
   {
      _log.error(msg);
      if (alert) {
         NotificationService.INSTANCE.newErrorEvent(
               "Task[" + getCurrentTaskState().getId() + "] - " + msg,
               Component.builds);
      }
      error(msg);
   }

   private void logInfo(String msg, boolean alert)
   {
      _log.info(msg);
      if (alert) {
         NotificationService.INSTANCE.newInfoEvent(
               "Task[" + getCurrentTaskState().getId() + "] - " + msg,
               Component.builds);
      }
   }

   private void logWarn(String msg, boolean alert)
   {
      _log.warn(msg);
      if (alert) {
         NotificationService.INSTANCE.newWarnEvent(
               "Task[" + getCurrentTaskState().getId() + "] - " + msg,
               Component.builds);
      }
   }
}
