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

package com.vmware.thinapp.manualmode.server;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.vmware.thinapp.common.converter.client.ProjectClient;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.converter.dto.TicketRequest;
import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.thinapp.common.datastore.client.DatastoreClient;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.manualmode.Config;
import com.vmware.thinapp.manualmode.ThreadLocalProperties;
import com.vmware.thinapp.manualmode.Util;
import com.vmware.thinapp.manualmode.util.DownloadResult;
import com.vmware.thinapp.manualmode.util.DriveLetterManager;
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DriveLetter;
import com.vmware.thinapp.manualmode.util.HttpDownloaderService;

import scala.Option;

@Service
public class RequestFactory {
   private static final Logger log = LoggerFactory
         .getLogger(RequestFactory.class);

   public enum RequestScheme {
      http,
      https,
      datastore
   }

   @Autowired
   private Util util;

   @Autowired
   protected Config config;

   @Autowired
   protected DatastoreClient datastoreClient;

   @Autowired
   private ProjectClient projectClient;

   @Autowired
   private HttpDownloaderService httpDownloaderService;

   @Autowired
   private RuntimeManager runtimeManager;

   private static final String PROJECT_LOG_FILE = "Support/taf.log";

   /**
    * Create a new MM or automatic capture request for a given ticket.
    *
    * @param ticketRequest a ticket request.
    * @return an instance of Request.
    */
   public Request create(TicketRequest ticketRequest) {
      DriveLetterManager<String> driveLetterManager =
         DriveLetterManager.getDefault(Capturer.INPUT_MOUNT_DRIVE_FIRST);

      ThinAppRuntime thinAppRuntime = getThinAppRuntime(ticketRequest.getRuntimeId());
      Datastore outputDS =
         datastoreClient.getDatastore(ticketRequest.getOutputDatastore());
      Project project = projectClient.create(outputDS, thinAppRuntime);

      // Output mounter where the conversion results will be written to
      Mounter outputMounter = createOutputMounter(outputDS, project.getSubdir(),
            driveLetterManager);

      // Collection of input mounters
      List<Mounter> inputMounters = new ArrayList<Mounter>();

      // Mapping of drive letter to the files that are stored on that drive
      Multimap<DriveLetter, ProjectFile> driveLetterToFiles = HashMultimap.create();

      List<Future<DownloadResult>> downloadResultFutures =
         new ArrayList<Future<DownloadResult>>();

      // Rage against teh nullz
      Option<DriveLetter> downloadDriveLetter = Option.empty();

      // Unique project directory where all downloaded files get stored
      String projectDir = "project-" + project.getId();
      String projectOutputPath = String.format("%s/%s",
                                 outputDS.getMountPath(), projectDir);
      String projectLogFile = String.format("%s/%s",
                                 projectOutputPath, PROJECT_LOG_FILE);
      ThreadLocalProperties.setProperty(
          ThreadLocalProperties.PER_PROJECT_LOGFILE, projectLogFile);

      // Process each input file in the request
      for (ProjectFile inputFile : ticketRequest.getInputFiles()) {
         String inputUriStr = inputFile.getUrl();
         URI inputUri = AfUtil.toURI(inputUriStr);
         RequestScheme scheme = RequestScheme.valueOf(inputUri.getScheme());

         switch (scheme) {
            case http:
            case https:
               downloadDriveLetter = Option.apply(processDownloadFile(
                     inputFile, downloadDriveLetter, driveLetterManager,
                     outputDS, projectDir, inputMounters,
                     downloadResultFutures));
               break;
            case datastore:
               processDatastoreFile(inputFile, inputUri, driveLetterManager,
                     inputMounters, driveLetterToFiles);
               break;
            default:
               throw new RuntimeException(String.format(
                     "Invalid input URI: %s. Supported input URI schemes are "
                           + "datastore, http, and https", inputUriStr));
         }
      }

      return createRequest(inputMounters, outputMounter, project,
            downloadResultFutures, downloadDriveLetter, driveLetterToFiles,
            ticketRequest.getAutomaticCapture(), ticketRequest.getCommands(),
            ticketRequest.getWorkpool(), projectOutputPath, thinAppRuntime);
   }

   private ThinAppRuntime getThinAppRuntime(final Long runtimeId) {
      Collection<ThinAppRuntime> runtime = Collections2.filter(runtimeManager.getRuntimes(),
              new Predicate<ThinAppRuntime>() {
                 @Override
                 public boolean apply(ThinAppRuntime thinAppRuntime) {
                    return thinAppRuntime.getId() == runtimeId;
                 }
              });

      assert runtime.size() == 1;

      return Iterables.get(runtime, 0);
   }

   /**
    * Spawns off an asynchronous download task for the given download input
    * file.  Also performs one-time processing when the given drive letter is
    * not defined.
    */
   private DriveLetter processDownloadFile(ProjectFile downloadFile,
         Option<DriveLetter> downloadDriveLetter,
         DriveLetterManager<String> driveLetterManager, Datastore outputDS,
         String projectDir, List<Mounter> inputMounters,
         List<Future<DownloadResult>> downloadResultFutures) {
      DriveLetter reservedDriveLetter;

      /** /installers/project-x */
      String installersProjectDir = config.getInstallersDir() + projectDir;

      /**
       * [Internal DS's mountPath -> /home/user/packages ]
       * [Other DS's mountPath    -> /mnt/cifs/{id}      ]
       * [Internal -> /home/user/packages/installers/project-x ]
       * [Others   -> /mnt/cifs/{id}/installers/project-x      ]
       */
      String downloadPath = outputDS.getMountPath() + installersProjectDir;

      // Perform one-time processing if we haven't reserved a drive letter
      if (downloadDriveLetter.isEmpty()) {
         // Reserve a drive letter for the download input mounter
         reservedDriveLetter = driveLetterManager.reserve();

         // Get an input mounter for the download directory
         inputMounters.add(createInputMounter(outputDS, installersProjectDir,
               reservedDriveLetter));

         // Create a download dir with unique project id. e.g.
         // /home/user/packages/installers/project-xxxxx (internal)
         // /mnt/cifs/{id}/installers/project-xxxxx (Others)
         log.debug(
               "Checking for existence of installer download directory {}",
               downloadPath);
         File dir = new File(downloadPath);
         if (!dir.exists()) {
            log.debug(
                  "Installer download directory {} does not exist, creating...",
                  downloadPath);
            if (!dir.mkdirs()) {
               throw new IllegalArgumentException(
                     "Failed to create required directory - " + downloadPath);
            }
         }
      } else {
         reservedDriveLetter = downloadDriveLetter.get();
      }

      // Trigger an HTTP download for the input file
      log.info("Start downloading from {} and save into {}", downloadFile,
            downloadPath);
      downloadResultFutures.add(httpDownloaderService.asyncDownload(
            downloadFile.getUrl(), downloadPath, downloadFile.getFilename()));
      return reservedDriveLetter;
   }

   /**
    * Process the given datastore file.  Will create input mounters as needed.
    */
   private void processDatastoreFile(ProjectFile datastoreFile, URI inputUri,
         DriveLetterManager<String> driveLetterManager, List<Mounter> inputMounters,
         Multimap<DriveLetter, ProjectFile> driveLetterToFiles) {
      // Get the datastore the input file is stored on
      String datastoreId = inputUri.getHost();
      Datastore inputDS = datastoreClient.getDatastore(
            datastoreId);
      String parentPath = null;
      try {
         parentPath = AfUtil.parentUri(inputUri).getPath();
      } catch (URISyntaxException ex) {
         throw new ConverterException("Invalid file URI", ex);
      }
      String datastoreKey = datastoreId + parentPath;

      // Get a drive letter for the datastore
      DriveLetter driveLetter;
      if (driveLetterManager.containsKey(datastoreKey)) {
         driveLetter = driveLetterManager.reserveWithKey(datastoreKey);
      } else {
         // Reserve a new drive letter
         driveLetter = driveLetterManager.reserveWithKey(datastoreKey);

         // Create the input mounter for the datastore
         inputMounters.add(createInputMounter(inputDS, parentPath, driveLetter));
      }

      // Extract the filename from the full datastore URL if the name is null
      if (StringUtils.isBlank(datastoreFile.getFilename())) {
         try {
            URI datastoreURI = new URI(datastoreFile.getUrl());
            datastoreFile.setFilename(AfUtil.getFilenameFromURI(datastoreURI));
         } catch (URISyntaxException ex) {
            throw new ConverterException(String.format("Invalid datastore file URI: %s", datastoreFile.getUrl()));
         }
      }

      // Associate the input file with the datastore's drive letter
      driveLetterToFiles.get(driveLetter).add(datastoreFile);
   }

   /**
    * Create a MM capture workflow.
    *
    * @param inputMounters a list of input mounters.
    * @param outputMounter an output mounter.
    * @param project a project.
    * @param downloadResultFutures a collection of futures for download results.
    * @param downloadDriveLetter drive letter reserved for file downloads
    * @param driveLetterToFiles mapping of drive letters to files on that drive
    * @param automaticCapture true for automatic capture request,
    *                         false for manual capture request
    * @param commands list of commands to run at each conversion phase
    * @param workpool workpool instance to use
    * @param projectOutputPath the project's output path
    *                          (/home/user/packages/project-xxx)
    * @param thinAppRuntime runtime to capture with
    * @return a new Request instance.
    */
   protected Request createRequest(List<Mounter> inputMounters,
           Mounter outputMounter, Project project,
           List<Future<DownloadResult>> downloadResultFutures,
           Option<DriveLetter> downloadDriveLetter,
           Multimap<DriveLetter, ProjectFile> driveLetterToFiles,
           boolean automaticCapture,
           Map<ConversionPhase, CommandList> commands,
           Workpool workpool,
           String projectOutputPath,
           ThinAppRuntime thinAppRuntime) {
      Capturer capturer;

      if (automaticCapture) {
         capturer = util.autowire(new AutomaticCapturer(downloadResultFutures,
               inputMounters, outputMounter, downloadDriveLetter,
               driveLetterToFiles, commands, thinAppRuntime));
      } else {
         capturer = util.autowire(new ManualCapturer(downloadResultFutures,
               inputMounters, outputMounter, downloadDriveLetter,
               driveLetterToFiles, commands, thinAppRuntime));
      }

      log.info("Using output mounter: {}", outputMounter);
      log.info("Using input mounters: {}", inputMounters);

      String logFile = String.format("%s/%s", projectOutputPath, PROJECT_LOG_FILE);
      return util.autowire(
         new Request(workpool, project, capturer, capturer.getStatus(), logFile));
   }

   /**
    * Create an input mounter.
    *
    * @param inDS an input datastore instance.
    * @param inputUncPath UNC path to the input datastore.
    * @return a new Mounter instance.
    */
   protected Mounter createInputMounter(Datastore inDS, String inputUncPath,
         DriveLetter driveLetter) {
      if (inDS == null) {
         throw new IllegalArgumentException("Input datastore is null");
      }

      // Strip all leading path elements so that joining them doesn't
      // result in multiple separators which confuses Windows.
      String server = StringUtils.strip(inDS.getServer(), "/\\");
      String share = null;
      if (StringUtils.isNotEmpty(inDS.getShare())) {
         share = StringUtils.strip(inDS.getShare(), "/\\");
      }
      String path = null;
      if (inputUncPath != null) {
         path = StringUtils.strip(inputUncPath, "/\\");
      }
      String inputUnc = AfUtil.toUNC(server, share, path);

      log.debug("Input datastore is: {} [UNC:{}]", inDS, inputUnc);

      return new Mounter(inputUnc, inDS.getUsername(), inDS.getPassword(),
            driveLetter.driveString());
   }

   /**
    * Create an output mounter.
    *
    * @param outDS an output datastore.
    * @param projectSubDir a directory to store project build output.
    * @return a new Mounter instance.
    */
   protected Mounter createOutputMounter(Datastore outDS, String projectSubDir,
         DriveLetterManager<String> driveLetterManager) {
      if (outDS == null) {
         throw new IllegalArgumentException("outDS is null");
      }

      String projectUnc = AfUtil.toUNC(outDS.getServer(), outDS.getShare(),
            projectSubDir);

      if (log.isDebugEnabled()) {
         log.debug("Output datastore is: {} [UNC:{}]", outDS, projectUnc);
      }

      driveLetterManager.reserve(Capturer.OUTPUT_MOUNT_DRIVE);

      return new Mounter(projectUnc, outDS.getUsername(), outDS.getPassword(),
            Capturer.OUTPUT_MOUNT_DRIVE.driveString());
   }
}
