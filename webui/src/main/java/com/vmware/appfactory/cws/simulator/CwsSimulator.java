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

package com.vmware.appfactory.cws.simulator;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.CwsRegistryRequest;
import com.vmware.appfactory.cws.CwsServerInfo;
import com.vmware.appfactory.cws.CwsSettings;
import com.vmware.appfactory.cws.CwsSettingsDir;
import com.vmware.appfactory.cws.CwsSettingsIni;
import com.vmware.appfactory.cws.CwsSettingsIniData;
import com.vmware.appfactory.cws.CwsSettingsRegKey;
import com.vmware.appfactory.cws.CwsSettingsRegSubKeyData;
import com.vmware.appfactory.cws.CwsSettingsRegValue;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.ConversionJobStatus;
import com.vmware.thinapp.common.converter.dto.ConversionJobStatus.JobState;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.converter.dto.ConversionRequest;
import com.vmware.thinapp.common.converter.dto.ConversionResponse;
import com.vmware.thinapp.common.converter.dto.ConversionResult;
import com.vmware.thinapp.common.converter.dto.DsLocation;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.Status;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * Fake controller which pretends to be the Converter Web Service. This can be
 * used for UI testing; it does not actually "convert" anything.
 */
@Controller
@RequestMapping(value = {"/cws", "/webui/cws"})
public class CwsSimulator
   extends AbstractApiController
{
   /** How many (max) fake exe's to create per conversion job */
   private static final int MAX_EXES_PER_JOB = 4;

   /** How often to fail a job */
   private static final float REBUILD_FAILURE_RATE = 0.005f;

   /** How often we provide performance data */
   private static final int PERF_DATA_REFRESH_RATE = 10;

   private static final int REBUILD_MAX_TICKS = 10;

   private static long NEXT_ID = 1;

   private static final Random RANDOM = new Random();

   /** Running jobs: map job IDs to job status */
   private static final Map<Long, ConversionJobStatus> CONVERSION_ID_TO_STATUS_MAP =
      new HashMap<Long, ConversionJobStatus>();

   /** Running jobs: map job IDs to original requests */
   private static final Map<Long, ConversionRequest> CONVERSION_ID_TO_REQUEST_MAP =
         new HashMap<Long, ConversionRequest>();

   /** Running rebuilds: map project ids to projects being rebuilt */
   private static final Map<Long, Project> REBUILD_QUEUE = new HashMap<Long, Project>();

   /** Rebuild times: map project ids to elapsed rebuild time in 'ticks' */
   private static final Map<Long, Integer> REBUILD_TICKS = new HashMap<Long, Integer>();

   /** Completed jobs: map job ids to projects */
   private static final Map<Long, Project> PROJECT_MAP = new HashMap<Long, Project>();

   /** Completed jobs: map project ids to settings */
   private static final Map<Long, CwsSettings> SETTINGS_MAP = new HashMap<Long, CwsSettings>();

   /** A collection of ALL registry nodes we create */
   private static final Map<Long, CwsSettingsRegKey> REGISTRY_CACHE = new HashMap<Long, CwsSettingsRegKey>();

   /** A boolean to store time synchronization status */
   private boolean _timesync = false;

   /**
    * A license expiration date which will disable any WRITE calls to AppFactory
    * ISO8601 formatter for date without time zone.
    * The format used is <tt>yyyy-MM-dd</tt>.
    */
   private static final String LICENSE_EXPIRATION_DATE = "2020-02-20";


   public CwsSimulator() {
      ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
      executor.scheduleWithFixedDelay(new CwsSimulatorDaemon(), 1, 1, TimeUnit.SECONDS);
   }

   /**
    * Create a new conversion request.
    *
    * @param cwsRequest
    */
   @ResponseBody
   @RequestMapping(
         value = "/conversions",
         method = RequestMethod.POST)
   public ConversionResponse createConversion(
         @RequestBody ConversionRequest cwsRequest)
   {
      /* Assign this "job" a new ID. */
      cwsRequest.setId(Long.valueOf(NEXT_ID++));

      /* Create a successful status */
      // TODO: Randomly fail a CWS request
      ConversionJobStatus status = new ConversionJobStatus();
      status.setJobId(cwsRequest.getId());
      status.setState(ConversionJobStatus.JobState.created);

      /* Add to the queue of things to "work" on */
      CONVERSION_ID_TO_STATUS_MAP.put(cwsRequest.getId(), status);
      CONVERSION_ID_TO_REQUEST_MAP.put(cwsRequest.getId(), cwsRequest);

      /* Write the response. */
      return new ConversionResponse(cwsRequest.getId());
   }


   /**
    * Get the status of a conversion request.
    *
    * @param jobId
    */
   @ResponseBody
   @RequestMapping(
         value = "/conversions/{id}",
         method = RequestMethod.GET)
   public ConversionJobStatus getConversionSatus(
         @PathVariable("id") Long jobId)
      throws AfNotFoundException
   {
      ConversionJobStatus result = CONVERSION_ID_TO_STATUS_MAP.get(jobId);
      if (null != result) {
         return result;
      }

      /* There was no such job id */
      throw new AfNotFoundException("Invalid job ID " + jobId);
   }


   /**
    * Cancel (abort) an ongoing conversion request.
    *
    * @param conversionId
    */
   @ResponseBody
   @RequestMapping(
         value = "/conversions/{id}/cancel",
         method = RequestMethod.POST)
   public void cancelConversion(
         @PathVariable("id") Long conversionId)
      throws AfNotFoundException
   {
      /* Mark it as aborting; the update timer will handle it from there. */
      ConversionJobStatus status = CONVERSION_ID_TO_STATUS_MAP.get(conversionId);
      if (null != status) {
         status.setState(ConversionJobStatus.JobState.cancelling);
         status.setPercent(0);
         return;
      }

      /* There was no such job id */
      throw new AfNotFoundException("Invalid job ID " + conversionId);
   }


   /**
    * This is the simulator method to create dummy thinApp runtime.
    *
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = "/runtimes",
         method = RequestMethod.GET)
   public ThinAppRuntime[] getThinAppRuntimes()
   {
      ThinAppRuntime[] tas = {
            new ThinAppRuntime("4.6.2", 456451, "/wherever/4.6.2", 456451L),
            new ThinAppRuntime("4.7", 519532, "/wherever/4.7", 519532L) };
      return tas;
   }


   @ResponseBody
   @RequestMapping(
         value = "/projects/{id}",
         method = RequestMethod.GET)
   public Project getProjectStatus(
         @PathVariable("id") Long projectId)
      throws AfNotFoundException
   {
      Project proj = PROJECT_MAP.get(projectId);

      if (proj == null) {
         /* There was no such project id */
         throw new AfNotFoundException("Invalid project ID " + projectId);
      }

      return proj;
   }


   /**
    * Rebuild a project.
    *
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{id}/rebuild",
         method = RequestMethod.POST)
   public void rebuildProject(
         @PathVariable("id") Long projectId)
      throws AfNotFoundException
   {
      if (!PROJECT_MAP.containsKey(projectId)) {
         /* There was no such project id */
         throw new AfNotFoundException("Invalid project ID " + projectId);
      }

      _log.debug("Project rebuild for " + projectId);

      /* Get the project and change its state to rebuilding */
      Project project = PROJECT_MAP.get(projectId);
      project.setState(Project.State.rebuilding);

      /* Add the project to the collection of projects currently being rebuilt */
      REBUILD_QUEUE.put(project.getId(), project);
      REBUILD_TICKS.put(project.getId(), Integer.valueOf(0));

      return;
   }


   /**
    * Refresh a project.
    * This tells CWS to rescan the project output directory and refresh the
    * contents; used when the user modifies the output directory contents
    * manually when editing a build.
    *
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{id}/refresh",
         method = RequestMethod.POST)
   public void refreshProject(
         @PathVariable("id") Long projectId)
      throws AfNotFoundException
   {
      if (!PROJECT_MAP.containsKey(projectId)) {
         /* There was no such project id */
         throw new AfNotFoundException("Invalid project ID " + projectId);
      }

      _log.debug("Project refresh for " + projectId);

      /* There is nothing we simulator for this */

      return;
   }


   /**
    * Delete a project.
    * In the real CWS service, this deletes the files associated with a
    * project.
    *
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}",
         method = RequestMethod.DELETE)
   public void deleteProject(
         @PathVariable Long projectId)
      throws AfNotFoundException
   {
      /* We have nothing to do, and nothing to return */

      if (!PROJECT_MAP.containsKey(projectId)) {
         /* There was no such project id */
         throw new AfNotFoundException("Invalid project ID " + projectId);
      }
   }


   /**
    * Update the project with one of:
    * 1. ThinApp runtime to a different version.
    * 2. State
    *
    * @param projectId
    * @param runtimeId
    * @param state
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}",
         method = RequestMethod.PUT)
   public void updateProject(
         @PathVariable Long projectId,
         @RequestParam(required=false) String state,
         @RequestParam(required=false) Long runtimeId)
      throws AfNotFoundException
   {
      Project project = PROJECT_MAP.get(projectId);
      if (project == null) {
         /* There was no such project id */
         throw new AfNotFoundException("Invalid project ID " + projectId);
      }

      if (runtimeId != null) {
         project.setRuntimeId(runtimeId);
      }
   }


   /**
    * Get package.ini settings for a project.
    *
    * @param request
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/packageini",
         method = RequestMethod.GET)
   public CwsSettingsIni getProjectPacakgeIni(
         HttpServletRequest request,
         @PathVariable Long projectId)
      throws AfNotFoundException
   {
      CwsSettings settings = getProjectSettings(cwsUrlRoot(request), projectId);
      return settings.getPackageIni();
   }


   /**
    * Update package.ini settings for a project.
    *
    * @param request
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/packageini",
         method = RequestMethod.PUT)
   public HashMap<String,Boolean> updateProjectPackageIni(
         @RequestBody CwsSettingsIni newPackageIni,
         HttpServletRequest request,
         @PathVariable Long projectId)
      throws AfNotFoundException
   {
      CwsSettings settings = getProjectSettings(cwsUrlRoot(request), projectId);

      /* Update packageIni and we're done */
      boolean changed = settings.setPackageIniIfChanged(newPackageIni);
      _log.debug("Update packageini of project " + projectId + ", changed = " + changed);
      HashMap<String,Boolean> res = new HashMap<String,Boolean>();
      res.put("modified", Boolean.valueOf(changed));
      return res;
   }


   /**
    * Get root directory settings for a project.
    *
    * @param request
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/directory",
         method = RequestMethod.GET)
   public CwsSettingsDir getProjectDirectoryRoot(
         HttpServletRequest request,
         @PathVariable Long projectId)
      throws AfNotFoundException
   {
      CwsSettings settings = getProjectSettings(cwsUrlRoot(request), projectId);
      return settings.getDirRoot();
   }


   /**
    * Get child directory settings for a project.
    *
    * @param request
    * @param projectId
    * @param childNum
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/directory/{childNum}",
         method = RequestMethod.GET)
   public CwsSettingsDir getProjectDirectory(
         HttpServletRequest request,
         @PathVariable Long projectId,
         @PathVariable Long childNum)
      throws AfNotFoundException
   {
      CwsSettings settings = getProjectSettings(cwsUrlRoot(request), projectId);

      /* Get the root, but make up subkeys on the fly */
      CwsSettingsDir root = settings.getDirRoot();
      return createDummyDirectories(cwsUrlRoot(request), projectId, root);
   }


   // TODO: GET /projects/{projectId}/file/{fileId}


   // TODO: PUT /projects/{projectId}/directory/{directoryId}


   // TODO: POST /projects/{projectId}/directory/new


   // TODO: POST /projects/{projectId}/file/new


   // TODO: DELETE /projects/{projectId}/directory/{directoryId}


   // TODO: DELETE /projects/{projectId}/file/{fileId}


   /**
    * Get root registry settings for a project.
    *
    * From the API spec:
    * "Returns all top level keys (registry 'folders') modified in ThinApp
    * capture."
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/registry",
         method = RequestMethod.GET)
   public CwsSettingsRegKey getProjectRegistryRoot(
         HttpServletRequest request,
         @PathVariable Long projectId)
      throws AfNotFoundException
   {
      CwsSettings settings = getProjectSettings(cwsUrlRoot(request), projectId);
     return settings.getRegistryRoot();
   }


   /**
    * Get child registry settings for a project.
    *
    * From the API spec:
    * "Returns the key, a list of its subkeys and URIs to access the contents
    * of said subkeys, and all values immediately under that key."
    *
    * @param request
    * @param response
    * @param projectId
    * @param registryId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/registry/{registryId}",
         method = RequestMethod.GET)
   public CwsSettingsRegKey getProjectRegistrySubkey(
         HttpServletRequest request,
         HttpServletResponse response,
         @PathVariable Long projectId,
         @PathVariable Long registryId)
      throws AfNotFoundException
   {
      getProjectSettings(cwsUrlRoot(request), projectId);
      return REGISTRY_CACHE.get(registryId);
   }


   /**
    * Delete a registry key for a project.
    *
    * @param request
    * @param projectId
    * @param registryId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/registry/{registryId}",
         method = RequestMethod.DELETE)
   public void deleteProjectRegistrySubkey(
         HttpServletRequest request,
         @PathVariable Long projectId,
         @PathVariable Long registryId)
      throws AfNotFoundException
   {
      getProjectSettings(cwsUrlRoot(request), projectId);

      // TODO: Find the parent and remove the child. For now, just log it
      _log.debug("** DELETED REGISTRY NODE:");
      logRegistryKey(REGISTRY_CACHE.get(registryId));
      // REGISTRY_CACHE.remove(registryId);
   }


   /**
    * Replace a registry key for a project.
    *
    * @param request
    * @param projectId
    * @param registryId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/registry/{registryId}",
         method = RequestMethod.PUT)
   public HashMap<String,Boolean> updateProjectRegistrySubkey(
         HttpServletRequest request,
         @RequestBody CwsSettingsRegKey regKey,
         @PathVariable Long projectId,
         @PathVariable Long registryId)
      throws AfNotFoundException
   {
      getProjectSettings(cwsUrlRoot(request), projectId);

      /* Update the given registry ID with the given key */
      boolean changed = updateRegistryKey(registryId, regKey);
      _log.debug("Update packageini of project " + projectId + ", changed = " + changed);
      HashMap<String,Boolean> res = new HashMap<String,Boolean>();
      res.put("modified", Boolean.valueOf(changed));
      return res;
   }


   /**
    * Create a new registry key for a project.
    *
    * @param request
    * @param requestData
    * @param projectId
    */
   @ResponseBody
   @RequestMapping(
         value = "/projects/{projectId}/registry/new",
         method = RequestMethod.POST)
   public Map<String,String> createProjectRegistryKey(
         HttpServletRequest request,
         @PathVariable Long projectId,
         @RequestBody CwsRegistryRequest requestData)
      throws AfNotFoundException, AfServerErrorException
   {
      getProjectSettings(cwsUrlRoot(request), projectId);

      try {
         /* Remember this node */
         Long regId = Long.valueOf(NEXT_ID++);
         REGISTRY_CACHE.put(regId, requestData.getKey());
         String regUrl = cwsUrlRoot(request) + "/projects/" + projectId + "/registry/" + regId;

         // TODO: Do something with the data. For now, just log it
         _log.debug("Created registry node:");
         logRegistryKey(requestData.getKey());

         /* Create the result structure and return it */
         Map<String,String> result = new HashMap<String,String>();
         result.put("url", regUrl);
         return result;
      }
      catch(Exception ex) {
         _log.error("Registry key creation failed", ex);
         throw new AfServerErrorException(ex.getMessage());
      }
   }


   /**
    * Reboot the simulated appliance.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/reboot",
         method = RequestMethod.POST)
   public void reboot()
   {
      _log.debug("Reboot command issued");
   }


   /**
    * Return the byte array for a test zip file.
    *
    * @param response
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/logs",
         method = RequestMethod.GET)
   public byte[] logs(HttpServletResponse response)
   {
      _log.debug("Get ZIP appliance system log file, returning byte[] with " +
            "length = " + CwsSimulator._zipFileBytes.length);

      /* Note: See AdminApiController.getLogs for an explanation of why we must
       * set these headers manually.
       */
      response.setHeader(AfUtil.CONTENT_LENGTH, Integer.toString(_zipFileBytes.length));
      response.setHeader(AfUtil.CONTENT_DISPOSITION, "attachment; filename=\"sample_logs.zip\"");

      /* Return the file data. */
      return CwsSimulator._zipFileBytes;
   }


   /**
    * Return the simulated date/time and uptime.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/info",
         method = RequestMethod.GET)
   public CwsServerInfo getInfo()
   {
      _log.debug("GET /config/info");
      CwsServerInfo info = new CwsServerInfo();
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      info.date = format.format(new Date()).replace(' ', 'T') + "Z";
      info.uptime = AfCalendar.Now() / 1000;
      return info;
   }


   /**
    * Return the simulated state of host/guest time synchronization.
    *
    * @throws InterruptedException
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/timesync",
         method = RequestMethod.GET)
   public boolean getTimeSync()
      throws InterruptedException{
      _log.debug("GET /config/timesync, state = " + _timesync);

      //Wait for 2 seconds to simulate CWS being slowsauce
      Thread.sleep(2000);

      return _timesync;
   }


   /**
    * Enable simulated host/guest time synchronization.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/timesync/enable",
         method = RequestMethod.POST)
   public void enableTimeSync()
   {
      _log.debug("POST /config/timesync/enable");
      _timesync = true;
   }


   /**
    * Disable simulated host/guest time synchronization.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/timesync/disable",
         method = RequestMethod.POST)
   public void disableTimeSync()
   {
      _log.debug("POST /config/timesync/disable");
     _timesync = false;
   }

   @Service
   public class CwsSimulatorDaemon implements Runnable {
      @Override
      public void run()
      {
         try {
            if (_config.getBool(ConfigRegistryConstants.CWS_PAUSED)) {
               return;
            }

            updateConversions();
            updateRebuilds();
         } catch (Throwable t) {
            _log.error("Simulator thread exited with error: ", t);
         }
      }
   }

   /**
    * Return the simulated license expiration date time.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/expire",
         method = RequestMethod.GET)
   public Map<String, String> getLicenseExpirationDate() {
      _log.trace("GET /config/expire, expiration date = " + LICENSE_EXPIRATION_DATE);
      Map<String, String> resMap = new HashMap<String, String>();
      resMap.put("date", LICENSE_EXPIRATION_DATE);
      return resMap;
   }

   /**
    * Update any 'running' conversion jobs.
    */
   private void updateConversions() {
      for (Map.Entry<Long,ConversionJobStatus> entry : CONVERSION_ID_TO_STATUS_MAP.entrySet()) {

         Long jobId = entry.getKey();
         ConversionJobStatus status = entry.getValue();

         if (null == status.getState()) {
            status.setState(ConversionJobStatus.JobState.created);
         }

         ConversionRequest request = CONVERSION_ID_TO_REQUEST_MAP.get(jobId);

         int newPercent = 0;
         switch(status.getState()) {
            case created:
               status.setState(ConversionJobStatus.JobState.provisioning);
               status.setPercent(newPercent);
               break;

            case provisioning:
               processCaptureStage(request, status, status.getState().getNext());
               break;
            case downloading:
               if (status.getPercent() == 0) {
                  dumpDownloads(request);
               }
               processCaptureStage(request, status, status.getState().getNext());
               break;

            case precapture:
            case preinstall:
               processCaptureStage(request, status, status.getState().getNext());
               break;

            case install:
               if (status.getPercent() == 0) {
                  dumpCommands(request);
               }
               processCaptureStage(request, status, status.getState().getNext());
               break;

            case postinstall:
            case postcapture:
            case projectgen:
            case prebuild:
            case projectbuild:
            case projectrefresh:
               processCaptureStage(request, status, status.getState().getNext());
               break;

            case finishing:
               newPercent = status.getPercent() + (request.hashCode() % 3) + 1;
               status.setPercent(newPercent);
               if (status.getPercent() >= 100) {
                  /* finishing -> finished */
                  finishConversion(request);
               }
               break;

            case cancelling:
               status.setState(ConversionJobStatus.JobState.cancelled);
               status.setPercent(newPercent);
               break;

            case cancelled:
            case finished:
               /* Nothing to do for any of these*/
               break;
         }
      }
   }


   /**
    * Process the a simulated capture during one of the main phases.
    */
   private void processCaptureStage(
         ConversionRequest request,
         ConversionJobStatus status,
         ConversionJobStatus.JobState nextState,
         int multiplier)
   {
      if (status.getState() == JobState.install && commandLabelContains("Stall", request.getSteps().values())) {
         // Start providing performance data once we have started the install phase and the app is
         // being converted with a command label that contains the string "Stall".
         status.setPerformanceData(PerformanceData.create(
               PERF_DATA_REFRESH_RATE, new Date(), getPerfThresholdsOver()));
      } else if (status.getState() == JobState.install && commandLabelContains("Fail", request.getSteps().values())) {
         // Fail the job if a command label contains the string "Fail"
         ConversionResult res = new ConversionResult(
               ConversionResult.Disposition.failed,
               Status.State.installerDownloadFailed,
               new Command("Test command", "echo test"));
         status.setResult(res);
         status.setState(ConversionJobStatus.JobState.finished);
         status.setPercent(0);
      } else {
         int newPercent = status.getPercent() + (multiplier * (request.hashCode() % 3) + 1);
         status.setPercent(newPercent);
         if (status.getPercent() >= currentStateEndpoint(status.getState())) {
            status.setState(nextState);
         }
      }
   }

   private boolean commandLabelContains(final String text, Collection<CommandList> commandLists) {
      final Predicate<Command> anyLabelInCommand = new Predicate<Command>() {
         @Override
         public boolean apply(Command command) {
            return command.getLabel() != null && command.getLabel().contains(text);
         }
      };

      Predicate<CommandList> anyCommandInList = new Predicate<CommandList>() {
         @Override
         public boolean apply(CommandList commandList) {
            return Iterables.any(commandList.getCommands(), anyLabelInCommand);
         }
      };

      return Iterables.any(commandLists, anyCommandInList);
   }

   private Map<String, Long> getPerfThresholdsOver() {
      return ImmutableMap.of(
            PerformanceData.CPU_USAGE_AVERAGE_SPEC,
               _config.getLong(ConfigRegistryConstants.CWS_STALL_CPU) * 100,
            PerformanceData.DISK_USAGE_AVERAGE_SPEC,
               _config.getLong(ConfigRegistryConstants.CWS_STALL_NET) * 100,
            PerformanceData.NET_USAGE_AVERAGE_SPEC,
               _config.getLong(ConfigRegistryConstants.CWS_STALL_DISK) * 100);
   }

   private int currentStateEndpoint(ConversionJobStatus.JobState jobState) {
      return ((jobState.ordinal() +1) * 100) / (ConversionJobStatus.JobState.finished.ordinal() + 1);
   }

   private void processCaptureStage(
         ConversionRequest request,
         ConversionJobStatus status,
         ConversionJobStatus.JobState nextState)
   {
      processCaptureStage(request, status, nextState, 1);
   }


   /**
    * Update any 'running' project rebuilds.
    *
    * XXX: It would be nice to clean this up using a class similar to
    * CwsJobStatus.  This would encapsulate details such as
    * REBUILD_MAX_TICKS, etc.
    */
   private void updateRebuilds() {
      List<Long> completedRebuilds = new ArrayList<Long>();
      List<Long> failedRebuilds = new ArrayList<Long>();

      /* Determine what project rebuilds have completed or failed */
      for(Long projectId : REBUILD_QUEUE.keySet()) {
         _log.debug("Updating rebuild for project id = " + projectId);

         /* Occasionally fail a rebuild */
         if (Math.random() < REBUILD_FAILURE_RATE) {
            failedRebuilds.add(projectId);
            continue;
         }
         /* Complete rebuilds that have run for REBUILD_MAX_TICKS */
         else if(REBUILD_TICKS.get(projectId) == Integer.valueOf(REBUILD_MAX_TICKS)){
            completedRebuilds.add(projectId);
         }
         /* Note that time has elapsed for the rebuild */
         else {
           REBUILD_TICKS.put(projectId, Integer.valueOf(REBUILD_TICKS.get(projectId).intValue()+1));
         }
      }

      /* Mark all failed rebuilds as dirty */
      for(Long projectId : failedRebuilds) {
         _log.debug("Rebuild failed for projectId = " + projectId);

         Project project = REBUILD_QUEUE.get(projectId);
         project.setState(Project.State.dirty);
         REBUILD_QUEUE.remove(projectId);
         REBUILD_TICKS.remove(projectId);
      }

      /* Mark all completed rebuilds as available */
      for(Long projectId : completedRebuilds) {
         _log.debug("Rebuild complete for projectId = " + projectId);

         Project project = REBUILD_QUEUE.get(projectId);
         project.setState(Project.State.available);
         REBUILD_QUEUE.remove(projectId);
         REBUILD_TICKS.remove(projectId);
      }
   }

   private void finishConversion(ConversionRequest request)
   {
      /* Get the status of this job */
      ConversionJobStatus status = CONVERSION_ID_TO_STATUS_MAP.get(request.getId());

      /* Create fake project */
      Long projectId = new Long(NEXT_ID++);
      Project project = new Project();
      project.setId(projectId);
      project.setState(Project.State.available);
      project.setSubdir("fake-" + projectId);
      project.setRuntimeId(request.getRuntimeId());
      PROJECT_MAP.put(projectId, project);

      /* If request output was a datastore, put that into the project */
      DsLocation outputPath = request.getOutput();
      if (outputPath.isDatastorePath()) {
         // The path we create is "datastore://<dsName>"
         String fullPath = outputPath.getUrl();
         Long dsId = Long.valueOf(fullPath.substring(12));
         project.setDatastoreId(dsId);
      }

      /* Put fake files into the project */
      int numFiles = 1 + (int) (Math.random() * MAX_EXES_PER_JOB - 1);
      List<ProjectFile> files = new ArrayList<ProjectFile>();
      for (int i = 0; i < numFiles; i++) {
         try {
            String fakeFileName = "Dummy File #" + i + ".exe";
            URI uri = new URI(
                  "http",
                  "localhost",
                  "/" + fakeFileName,
                  null);

            ProjectFile file = new ProjectFile(
                  fakeFileName, uri.toASCIIString());
            file.setSize(10000 * (i + 1));
            files.add(file);
         }
         catch(URISyntaxException ex) {
            /* Safe to ignore in this case */
         }
      }
      project.setFiles(files);

      /* Create job result */
      ConversionResult result = new ConversionResult(
            ConversionResult.Disposition.succeeded,
            Status.State.refreshingProjectDone,
            null);

      /* Update job status */
      status.setResult(result);
      status.setProjectId(projectId);
      status.setState(ConversionJobStatus.JobState.finished);
   }


   /**
    * Updates the given registry ID with the given key.
    *
    * @param registryId
    * @param regKey
    * @return true if the given registry key was different from the existing
    *         value, false otherwise
    */
   private boolean updateRegistryKey(Long registryId, CwsSettingsRegKey regKey) {
      if(REGISTRY_CACHE.get(registryId).equals(regKey)) {
         return false;
      }

      REGISTRY_CACHE.put(registryId, regKey);
      return true;
   }


   /**
    * Get settings for a project.
    *
    * Since the simulator holds all job and project state in memory only,
    * it all gets lost when the webapp reloads. This is a pain when developing.
    * So, we treat ALL project IDs as valid. Otherwise, you need to convert
    * an app each time you want to edit a project.
    *
    * @param cwsUrlRootStr
    * @param projectId
    * @return
    */
   private synchronized CwsSettings getProjectSettings(
         String cwsUrlRootStr,
         Long projectId)
   {
      CwsSettings settings = SETTINGS_MAP.get(projectId);

      if (settings == null) {
         /* Make new settings (dummy values) */
         settings = createDummySettings(cwsUrlRootStr, projectId);
         SETTINGS_MAP.put(projectId, settings);
      }

      return settings;
   }


   /**
    * Create a dummy set of project settings. These are used for the
    * 'default' values for our simulated CWS projects.
    *
    * @param cwsUrlRootStr
    * @param projectId
    * @return
    */
   private static final CwsSettings createDummySettings(
         String cwsUrlRootStr,
         Long projectId)
   {
      CwsSettings settings = new CwsSettings();

      Long regRootId = createDummyRegistry(cwsUrlRootStr, projectId, null, "", 0);
      CwsSettingsRegKey regRoot = REGISTRY_CACHE.get(regRootId);

      settings.setPackageIni(createDummyIni());
      settings.setRegistryRoot(regRoot);
      settings.setDirRoot(createDummyDirectories(cwsUrlRootStr, projectId, null));

      return settings;
   }


   /**
    * Create a dummy INI structure.
    *
    * @return
    */
   private static final CwsSettingsIni createDummyIni()
   {
      CwsSettingsIni ini = new CwsSettingsIni();
      int numSections = RANDOM.nextInt(10)+10;

      for (int i = 1; i <= numSections; i++) {
         String sectionName = "Section" + i;
         CwsSettingsIniData section = new CwsSettingsIniData();
         ini.put(sectionName, section);

         int numPairs = RANDOM.nextInt(5)+5;
         for (int j = 0; j <= numPairs; j++) {
            String key = sectionName + "-Key-" + j;
            section.put(key, AfUtil.randomString());
         }
      }

      return ini;
   }


   /**
    * Create a dummy directory structure.
    *
    * @return
    */
   private static final CwsSettingsDir createDummyDirectories(
         String cwsUrlRootStr,
         Long projectId,
         CwsSettingsDir parent)
   {
      CwsSettingsDir dir = new CwsSettingsDir();

      /*
       * Directory path:
       * If root, it's empty, else it has one component (good enough...)
       */
      if (parent == null) {
         dir.setPath("");
      } else {
         dir.setPath(parent.getPath() + "/pathSegment");
      }

      /*
       * Files:
       * A random number from 1 to 5
       */
      int numFiles = RANDOM.nextInt(5) + 1;
      for (int f = 1; f <= numFiles; f++) {
         dir.addFile(
               "file-" + f,
               cwsUrlRootStr + "/projects/" + projectId + "/file/" + f);
      }

      /*
       * Sub-directories:
       * If root, there are 3. Else a 50% chance of none and a 50% chance
       * of 1 or 2 (good enough)
       */
      int numDirs = 0;
      if (parent == null) {
         numDirs = 3;
      }
      else {
         numDirs = RANDOM.nextInt(4) - 2;
      }

      if (numDirs > 0) {
         for (int d = 1; d <= numDirs; d++) {
            dir.addDirectory(
                  "directory-" + d,
                  cwsUrlRootStr + "/projects/" + projectId + "/directory/" + d);
         }
      }

      /*
       * Attributes
       * Just a dummy INI structure (good enough)
       */
      dir.setAttributes(createDummyIni());

      return dir;
   }


   /**
    * Create a dummy registry structure.
    *
    * @param cwsUrlRootStr
    * @param projectId
    * @param parent
    * @return
    */
   private static final Long createDummyRegistry(
         String cwsUrlRootStr,
         Long projectId,
         CwsSettingsRegKey parent,
         String pathName,
         int depth)
   {
      Long regId = Long.valueOf(NEXT_ID++);

      /* Create a new registry key and cache it */
      CwsSettingsRegKey reg = new CwsSettingsRegKey();
      reg.setId(regId);
      REGISTRY_CACHE.put(regId, reg);

      String indent = "";

      for (int i = 0; i < depth; i++)
         indent += "  ";

      /* Isolation mode: random */
      reg.setPath(pathName);
      reg.setIsolation((RANDOM.nextBoolean() ?
            CwsSettingsRegKey.IsolationMode.full :
            CwsSettingsRegKey.IsolationMode.merged));

      // Initialize the hasChildren variable that will be set when the child
      // registry is created.
      boolean hasChildren = false;

      if (parent == null) {
         /*
          * Make fixed subkeys, one per hive.
          */
         for (String hive : new String[] {
                  "HKEY_LOCAL_MACHINE",
                  "HKEY_CURRENT_USER",
                  "HKEY_USERS" }) {
            Long hiveId = createDummyRegistry(
                  cwsUrlRootStr,
                  projectId,
                  reg,
                  hive,
                  depth + 1);

            // Compute hasChildren from the child registry in REGISTRY_CACHE.
            CwsSettingsRegKey childReg = REGISTRY_CACHE.get(hiveId);
            hasChildren = childReg.getSubkeys().size() > 0;

            reg.addSubkey(
                  hive,
                  cwsUrlRootStr + "/projects/" + projectId + "/registry/" + hiveId,
                  hasChildren);
         }
      }
      else {
         /* Key has a path */
         StringBuilder path = new StringBuilder(parent.getPath());
         if (path.length() > 0) {
            path.append("/");
         }
         path.append(pathName);
         reg.setPath(path.toString());

         /* Key has subkeys? */
         int numSubkeys = (
               depth == 1 ? RANDOM.nextInt(4) + 1 :
               depth < 10 ? RANDOM.nextInt(10) - 5 :
               0);

         for (int i = 1; i <= numSubkeys; i++) {
            String keyName = "KEY-" + AfUtil.randomString();
            Long registryId = createDummyRegistry(
                  cwsUrlRootStr,
                  projectId,
                  reg,
                  keyName,
                  depth + 1);

            // Compute hasChildren from the child registry in REGISTRY_CACHE.
            CwsSettingsRegKey childReg = REGISTRY_CACHE.get(registryId);
            hasChildren = childReg.getSubkeys().size() > 0;

            reg.addSubkey(
                  keyName,
                  cwsUrlRootStr + "/projects/" + projectId + "/registry/" + registryId,
                  hasChildren);
         }


         /* Key has values: one for every type! */
         for (CwsSettingsRegValue.Type type : CwsSettingsRegValue.Type.values()) {
            CwsSettingsRegValue val = new CwsSettingsRegValue();
            val.setType(type);

            /* Make data according to type */
            switch(val.getType()) {
               case REG_BINARY: {
                  int data[] = new int[RANDOM.nextInt(20) + 1];
                  for (int d = 0; d < data.length; d++) {
                     data[d] = RANDOM.nextInt(256);
                  }
                  val.setData(data);
               } break;

               case REG_MULTI_SZ: {
                  String data[] = new String[RANDOM.nextInt(5) + 2];
                  for (int d = 0; d < data.length; d++) {
                     data[d] = AfUtil.randomString();
                  }
                  val.setData(data);
               } break;

               case REG_SZ:
               case REG_EXPAND_SZ:
                  val.setData(AfUtil.randomString());
                  break;

               case REG_DWORD:
               case REG_DWORD_LITTLE_ENDIAN:
               case REG_QWORD:
                  val.setData(Integer.valueOf(RANDOM.nextInt(1000)));
                  break;
            }

            String key = AfUtil.randomString();
            reg.addValue(key, val);
         }
      }

      return regId;
   }


   /**
    * For URI resources, we need to point back to ourself. This method
    * computes the correct absolute URL (since CWS will provide absolute
    * URLs).
    *
    * @param request
    * @return
    */
   private static String cwsUrlRoot(HttpServletRequest request)
   {
      StringBuilder url = new StringBuilder("http://");

      url
         .append(request.getServerName())
         .append(":").append(request.getServerPort())
         .append(request.getContextPath())
         .append("/cws");

      return url.toString();
   }


   /**
    * Log a registry key.
    * Primarily for debugging.
    */
   private void logRegistryKey(CwsSettingsRegKey registry)
   {
      _log.debug("Path ...... " + registry.getPath());
      _log.debug("IsoMode ... " + registry.getIsolation());

      Map<String,CwsSettingsRegValue> vals = registry.getValues();
      _log.debug("Values .... " + vals.size());
      for (String name : vals.keySet()) {
         _log.debug("   " + name + " = " + vals.get(name).getData());
      }

      Map<String,CwsSettingsRegSubKeyData> subs = registry.getSubkeys();
      _log.debug("Subkeys ... " + subs.size());
      for (String key : subs.keySet()) {
         _log.debug("   " + key + " = " + subs.get(key));
      }
   }


   private void dumpDownloads(ConversionRequest request)
   {
      _log.info("Conversion Downloads for request #" + request.getId());

      for (ProjectFile file : request.getFiles()) {
         _log.info("   " + file.getUrl());
      }
   }


   private void dumpCommands(ConversionRequest request)
   {
      _log.info("Conversion Commands for request #" + request.getId());

      for (ConversionPhase phase : request.getSteps().keySet()) {
         CommandList cmds = request.getSteps().get(phase);
         for (Command cmd : cmds.getCommands()) {
            _log.info("   " + phase.toString().toUpperCase() + " : " + cmd.getCommand());
         }
      }
   }


   /**
    * Raw bytes for a sample ZIP file containing a single text (.log) file
    */
   private static final byte[] _zipFileBytes={
         0x50, 0x4B, 0x03, 0x04, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x16,
         0x74, (byte) 0x9D, 0x3E, (byte) 0xA5, 0x0F, (byte) 0xBC, 0x06, 0x19, 0x00, 0x00, 0x00,
         0x19, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x1C, 0x00, 0x73, 0x61, 0x6D,
         0x70, 0x6C, 0x65, 0x2E, 0x6C, 0x6F, 0x67, 0x55, 0x54, 0x09, 0x00,
         0x03, 0x7C, 0x2E, (byte) 0xBB, 0x4D, 0x7D, 0x2E, (byte) 0xBB, 0x4D, 0x75, 0x78,
         0x0B, 0x00, 0x01, 0x04, (byte) 0xF6, 0x01, 0x00, 0x00, 0x04, 0x14, 0x00,
         0x00, 0x00, 0x54, 0x68, 0x69, 0x73, 0x20, 0x69, 0x73, 0x20, 0x61,
         0x20, 0x64, 0x65, 0x6D, 0x6F, 0x20, 0x6C, 0x6F, 0x67, 0x20, 0x66,
         0x69, 0x6C, 0x65, 0x2E, 0x0A, 0x50, 0x4B, 0x01, 0x02, 0x1E, 0x03,
         0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x16, 0x74, (byte) 0x9D, 0x3E, (byte) 0xA5,
         0x0F, (byte) 0xBC, 0x06, 0x19, 0x00, 0x00, 0x00, 0x19, 0x00, 0x00, 0x00,
         0x0A, 0x00, 0x18, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
         0x00, (byte) 0x80, (byte) 0x81, 0x00, 0x00, 0x00, 0x00, 0x73, 0x61, 0x6D, 0x70,
         0x6C, 0x65, 0x2E, 0x6C, 0x6F, 0x67, 0x55, 0x54, 0x05, 0x00, 0x03,
         0x7C, 0x2E, (byte) 0xBB, 0x4D, 0x75, 0x78, 0x0B, 0x00, 0x01, 0x04, (byte) 0xF6,
         0x01, 0x00, 0x00, 0x04, 0x14, 0x00, 0x00, 0x00, 0x50, 0x4B, 0x05,
         0x06, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x50, 0x00,
         0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00};
}
