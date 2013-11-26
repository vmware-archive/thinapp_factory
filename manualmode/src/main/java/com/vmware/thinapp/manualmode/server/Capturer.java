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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.manualmode.Config;
import com.vmware.thinapp.manualmode.Util;
import com.vmware.thinapp.manualmode.util.DownloadResult;
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DriveLetter;
import com.vmware.thinapp.manualmode.util.JobMonitorService;
import com.vmware.thinapp.manualmode.util.JobMonitorService.JobMonitorTicket;
import com.vmware.thinapp.workpool.CallWithTemporarySnapshot;
import com.vmware.thinapp.workpool.GuestCommands;
import com.vmware.thinapp.workpool.VirtualMachine;
import com.vmware.thinapp.workpool.VirtualMachineException;
import com.vmware.thinapp.workpool.VmRunCommandBuilder;
import com.vmware.thinapp.workpool.VmRunProgramOutput;

import scala.Option;

public abstract class Capturer implements CallWithTemporarySnapshot {
   /** Output mount drive to store ThinApp conversion artifacts. */
   public static final DriveLetter OUTPUT_MOUNT_DRIVE = DriveLetter.O;

   /** Input mount drive to store an application installer. */
   public static final DriveLetter INPUT_MOUNT_DRIVE_FIRST = DriveLetter.I;

   protected final Logger log;

   // Input project INI template
   protected static final String PROJECT_TEMPLATE = "c:\\template.ini";

   // Where the postcapture snapshot should temporarily reside in the guest.
   protected static final String POSTCAPTURE_SNAPSHOT =
           "c:\\postcapture.snapshot";

   // Where the precapture snapshot should temporarily reside in the guest.
   protected static final String PRECAPTURE_SNAPSHOT =
           "c:\\precapture.snapshot";

   // Guest setproxy.exe file destination.
   protected static final String GUEST_SET_PROXY_EXE = "c:\\setproxy.exe";

   // Guest backup eventlogs path
   private static final String GUEST_BACKUPEVENTLOGS_PATH = "O:\\Support\\Capture Machine EventLog\\";

   // Guest backupeventlogs.vbs script destination.
   private static final String GUEST_BACKUPEVENTLOGS_SCRIPT = "c:\\backupeventlogs.vbs";

   public interface CommandRunner {
      void runCommand(Command command, Status status) throws IOException;
   }

   @Autowired
   private Util util;

   @Autowired
   protected Config config;

   @Autowired
   protected JobMonitorService jobMonitor;

   @Autowired
   protected ThinAppRuntimePackager thinAppRuntimePackager;

   // Where the output of the ThinApp project should be generated.
   protected String projectOutput;

   // Connection to VC to remotely execute commands on the capture VM
   protected GuestCommands gc;
   protected List<DownloadResult> downloadResults;

   protected final List<Future<DownloadResult>> downloadResultFutures;
   protected Status status;
   protected JobMonitorTicket ticket;
   protected final List<Mounter> inputMounters;
   protected final Mounter outputMounter;
   protected final Option<DriveLetter> downloadDriveLetter;
   protected final Multimap<DriveLetter, ProjectFile> driveLetterToFiles;
   protected final ThinAppRuntime thinAppRuntime;

   /** Directory inside the guest of the specified runtime. */
   protected String thinappRuntimeDirectory;

   /** Default environment variable map.  Will be filled once output drive is mounted. */
   private final Map<String, String> defaultEnvVarMap;

   /**
    * Common constructor for subclasses of Capturer.
    *
    * @param downloadResultFutures list of eventual application download
    *                              results
    * @param inputMounters         list of fileshares to mount side the capture
    *                              VM
    * @param outputMounter         object for mounting the output fileshare in
    *                              inside the capture VM
    * @param driveLetterToFiles    mapping of drive letter to files on that
    *                              drive
    * @param thinAppRuntime        ThinApp runtime to capture with
    */
   public Capturer(List<Future<DownloadResult>> downloadResultFutures,
           List<Mounter> inputMounters,
           Mounter outputMounter,
           Option<DriveLetter> downloadDriveLetter,
           Multimap<DriveLetter, ProjectFile> driveLetterToFiles,
           ThinAppRuntime thinAppRuntime) {
      // This needs to be created in the constructor instead of statically so
      // that any subclasses have log messages with their class
      this.log = LoggerFactory.getLogger(getClass());
      this.downloadResultFutures = downloadResultFutures;
      this.downloadResults = null;
      this.status = new Status();
      this.inputMounters = inputMounters;
      this.outputMounter = outputMounter;
      this.downloadDriveLetter = downloadDriveLetter;
      this.driveLetterToFiles = driveLetterToFiles;
      this.thinAppRuntime = thinAppRuntime;

      // We cannot fill this as we do not know the project output directory
      // until after the output share has been mounted.  This will be filled
      // as a first step in the preCapture phase.
      this.defaultEnvVarMap = new HashMap<String, String>();
   }

   /**
    * Get the pre-quoted path to the snapshot tool inside the guest.
    *
    * @return
    */
   public String getSnapshotExe() {
      if (thinappRuntimeDirectory == null) {
         throw new ConverterException("The ThinApp runtime directory was not set.");
      }
      return String.format("\"%s\\snapshot.exe\"", thinappRuntimeDirectory);
   }

   /**
    * Return the capture status.
    *
    * @return the status of this capturer
    */
   public Status getStatus() {
      return status;
   }

   protected abstract void preCapture() throws IOException;

   protected abstract void preInstall() throws IOException;

   protected abstract void installApplication() throws IOException;

   protected abstract void postInstall() throws IOException;

   protected abstract void preBuild() throws IOException;

   protected abstract void verifyLoggedIn(boolean autologon) throws IOException;

   @Override
   public void call(VirtualMachine vm, Lease lease) throws Exception {
      gc = util.autowire(new GuestCommands(
            vm.getVcConfig(),
            vm.getVmInfo(),
            status.getProjectId()));

      // We wait to set the lease here since the VM may have needed to go
      // through stages to snapshot to cache state which messes with
      // VMRC (requires reconnecting it).
      status.setLease(lease);
      status.setCurrentState(State.vmAcquired);

      try {
         prepareVm(vm);
         // Monitor VM performance once we have verified tools is running and
         // the VM has a valid IP
         monitorVm(vm.getVmInfo().getMoid());
         verifyLoggedIn(vm.getVmInfo().getAutologon());
         stageThinApp();
         // Download files before mounting the input/output file shares
         // This allows the installer directory to be created before it is
         // mounted, if necessary.
         downloadFiles();
         mountInOut();
         // Create default environment variable map
         createDefaultEnvVarMap();
         preCapture();
         preCaptureSnapshot();
         preInstall();
         installApplication();
         postInstall();
         postCaptureSnapshot();
         preBuild();
         buildProject();
      } catch (Exception e) {
         if (status.isRequestCancelling()) {
            log.debug("Task was interrupted", e);
            throw new CaptureRequestCanceled();
         }
         status.setLastError(e.getMessage());
         throw e;
      } finally {
         if (log.isDebugEnabled() || log.isTraceEnabled() ||
             status.getStates().contains(State.failure)) {
            downloadGuestEventlogs();
         }

         // Carefully unmount inputs
         for (Mounter inputMounter : inputMounters) {
            try {
               inputMounter.unmount(gc);
            } catch (Exception e) {
               log.error("Unable to unmount input.  Continuing.", e);
            }
         }

         // Carefully unmount output
         try {
            outputMounter.unmount(gc);
         } catch (Exception e) {
            log.error("Unable to unmount output.  Continuing.", e);
         }

         // Stop monitoring the job
         jobMonitor.stopMonitoring(ticket);
      }

      checkCancel();

      log.info("Tasks inside virtual machine completed.");
   }

   private void prepareVm(VirtualMachine vm) throws IOException {
      checkCancel();
      status.setCurrentState(State.poweringOnVm);
      log.info("Powering on the virtual machine.");
      vm.powerOn();

      checkCancel();
      status.setCurrentState(State.waitingForTools);
      log.info("Waiting for VMware Tools to respond.");
      vm.waitForTools();
      checkCancel();
      log.info("Setting screen to 800x600");
      vm.setScreenResolution(800, 600);
      checkCancel();
      log.info("Waiting for Guest Ip");
      vm.waitForGuestIp();
      checkCancel();
      setGuestProxy();
   }

   /**
    * Stages the requested ThinApp runtime package inside the VM.
    *
    * @throws IOException
    */
   private void stageThinApp() throws IOException {
      File runtimePackage = thinAppRuntimePackager.createPackage(thinAppRuntime);

      try {
         checkCancel();
         status.setCurrentState(State.installingThinApp);
         String guestdir = String.format("c:\\thinapp-%05x", new Integer(new Random().nextInt(0xfffff)));
         String guestfile = String.format("%s\\package.exe", guestdir);
         log.debug(String.format("Creating guest directory for ThinApp runtime package: %s.", guestdir));
         gc.createDirectory(guestdir);
         log.debug(String.format("Uploading ThinApp runtime package %s to %s.", runtimePackage, guestfile));
         gc.uploadFileToGuest(runtimePackage.getAbsolutePath(), guestfile);
         log.debug("Extracting ThinApp runtime package.");
         gc.runBatchScriptInGuestWithOutput(
               guestdir,
               Arrays.asList("package.exe"));
         thinappRuntimeDirectory = guestdir;
      } finally {
         if (!runtimePackage.delete()) {
            log.error(String.format("Unable to delete ThinApp runtime package: %s.", runtimePackage));
         }
      }
   }

   private void monitorVm(String vmMoid) {
      try {
         // Attempt to monitor the VM
         ticket = jobMonitor.startMonitoring(vmMoid, status);
         status.setRefreshRate(ticket.getRefreshRate());
         PerformanceData perfData = status.getPerfData();
         if (perfData != null) {
            // Update the data again since we now have the refresh rate as well
            // as the performance data
            status.update(perfData.getDate(), perfData.getValues());
         }
      } catch (Exception ex) {
         // VC may not be ready or we're trying to connect to Workstation (which
         // isn't supported yet)
         log.debug("Unable to begin monitoring VM {}.", vmMoid, ex);
      }
   }

   /**
    * Set guest proxy if either httpProxyHost or httpsProxyHost is set in the
    * system environment.
    *
    * @throws IOException if any error raised while setting proxy in the guest.
    */
   private void setGuestProxy() throws IOException {
      /**
       * If installer download already started, then we need to set the proxy
       * config in Guest VM. This is because some installers are wrapped with
       * download manager that downloads actual installer bits when it runs.
       * TODO: Remove this null check if we always want to set guest proxy.
       */
      final List<String> proxySettings = getProxySettings();
      if (proxySettings == null) {
         log.info("Not setting guest proxy settings...");
         return;
      }
      log.info("Uploading setproxy.exe to the guest...");
      gc.uploadFileToGuest(config.getSetProxyExePath(), GUEST_SET_PROXY_EXE);
      log.info("Uploaded setproxy.exe to the guest...");
      log.info("Running setproxy.exe in the guest...");
      gc.runProgramInGuest(
            // Don't run with default commands (-interactive) because at this
            // point we may not yet be logged in.
            new VmRunCommandBuilder.RunProgramOptions(),
            GUEST_SET_PROXY_EXE,
            proxySettings.toArray(new String[proxySettings.size()]));
      log.info(String.format("Successfully set HTTP proxy in the guest to" +
              " %s:%d with proxy bypass %s", config.getHttpProxyHost(),
              Integer.valueOf(config.getHttpProxyPort()),
              config.getHttpNonProxyHosts()));
   }

   /**
    * Get all proxy settings, and it conditionally adds nonProxyHosts if
    * httpNonProxyHosts is not empty.
    *
    * @return a list of settings in an input format to setproxy.exe. E.g:
    *         C:\\setproxy.exe --http proxy.mycompany.com 1234 C:\\setproxy.exe
    *         --http proxy.mycompany.com 1234 "*.internal.mycompany.com" If
    *         httpProxyHost is empty or httpProxyPort is not greater than zero,
    *         then it will return null.
    */
   private final List<String> getProxySettings() {
      if (StringUtils.hasLength(config.getHttpProxyHost()) &&
              config.getHttpProxyPort() > 0) {
         final List<String> settings = new ArrayList<String>();
         settings.add("--http");
         settings.add(config.getHttpProxyHost());
         settings.add(String.valueOf(config.getHttpProxyPort()));
         if (StringUtils.hasLength(config.getHttpNonProxyHosts())) {
            settings.add(config.getHttpNonProxyHosts());
         }
         return settings;
      }
      return null;
   }

   protected boolean performEchoTest() throws IOException {
      /*
       * Run 'echo test' for a short time until it succeeds.
       * It may take a while since Tools begins servicing these requests
       * prior to the guest being ready to accept commands.
       */
      final int TEST_COUNT = 15;
      final int TEST_INTERVAL_MS = 10000;
      boolean isLoggedIn = false;

      log.debug("Performing echo test to verify that the user is logged in...");
      for (int i = 1; i < TEST_COUNT && !isLoggedIn; i++) {
         try {
            gc.runScriptInGuest("", "echo test");
            isLoggedIn = true;
         } catch (VirtualMachineException e) {
            log.debug("Unable to run command, assuming user is not logged into guest.");
            // Cool off for a couple seconds.
            try {
               log.debug("Waiting a few seconds to try the command again.");
               Thread.sleep(TEST_INTERVAL_MS);
            } catch (InterruptedException ie) {
               throw new RuntimeException("Got nterruptedException in verifyLoggedIn", ie);
            }
         }
      }

      return isLoggedIn;
   }

   private void downloadFiles() {
      downloadResults = waitInstallerDownloadIfRequired();

      // Did we actually need to download anything?
      if (!CollectionUtils.isEmpty(downloadResults)) {
         status.setCurrentState(State.downloading);

         // Get the results for each download
         for (DownloadResult result : downloadResults) {
            checkCancel();
            if (result.getResult()) {
               log.info("Successfully downloaded file {} to directory {}",
                       result.getFilename(), result.getDownloadDir());
            } else {
               // Installer download failed, sad panda -:(
               status.setCurrentState(State.installerDownloadFailed);
               throw new RuntimeException(String.format(
                  "Failed to download %s to directory %s: %s",
                     result.getUrl(),
                     result.getDownloadDir(),
                     result.getMessage()));
            }
         }
      }
   }

   /**
    * Check if there are any installer downloads that have started and wait
    * until they are done.
    *
    * @return true if either there was no prior installer download or the
    *         installer download was successful; otherwise, return false.
    */
   private List<DownloadResult> waitInstallerDownloadIfRequired() {
      List<DownloadResult> results = new ArrayList<DownloadResult>();

      if (!CollectionUtils.isEmpty(downloadResultFutures)) {
         for (Future<DownloadResult> downloadResultFuture :
                 downloadResultFutures) {
            DownloadResult result = null;
            try {
               log.info("Waiting for an installer download to complete if " +
                       "not done yet...");
               result = downloadResultFuture.get();
               if (result != null) {
                  results.add(result);
               }
            } catch (InterruptedException e) {
               log.debug("Waiting on installer download raised " +
                       "InterruptedException, ignoring...", e);
            } catch (ExecutionException e) {
               log.debug("Waiting on installer download raised " +
                       "ExecutionException, ignoring...", e);
            }
         }
      }

      return results;
   }

   private void mountInOut() throws IOException {
      checkCancel();
      status.setCurrentState(State.mountingFileSharesToGuest);
      for (Mounter inputMounter : inputMounters) {
         inputMounter.mount(gc);
      }
      projectOutput = outputMounter.mount(gc);
   }

   /**
    * Uploads the setoption.exe utility to the guest so it is available for use
    * by recipe commands.
    *
    * @throws IOException
    */
   protected void uploadSetOption(String setOptionExe) throws IOException {
      log.info("Uploading setoption.exe to the guest at path {}...", setOptionExe);
      gc.uploadFileToGuest(config.getSetOptionExePath(), setOptionExe);
   }

   protected void preCaptureSnapshot() throws IOException {
      checkCancel();
      status.setCurrentState(State.preCaptureWait);
      status.waitUntil(State.preCaptureDone);

      deleteTempOutputFiles();

      checkCancel();
      status.setCurrentState(State.takingPreCaptureSnapshot);
      log.info("Taking precapture snapshot.");
      VmRunProgramOutput output = gc.runProgramInGuestWithOutput(
            getSnapshotExe(),
            PRECAPTURE_SNAPSHOT);
      output.log(log);
   }

   private void postCaptureSnapshot() throws IOException {
      deleteTempOutputFiles();

      checkCancel();
      log.info("Taking postcapture snapshot");
      status.setCurrentState(State.takingPostCaptureSnapshot);
      VmRunProgramOutput output = gc.runProgramInGuestWithOutput(
              getSnapshotExe(),
              POSTCAPTURE_SNAPSHOT);
      output.log(log);

      checkCancel();
      log.info("Generating default project template.");
      status.setCurrentState(State.generatingProject);
      output = gc.runProgramInGuestWithOutput(
              getSnapshotExe(),
              PRECAPTURE_SNAPSHOT,
              "-SuggestProject",
              POSTCAPTURE_SNAPSHOT,
              PROJECT_TEMPLATE);
      output.log(log);

      checkCancel();
      log.info("Generating project directory");
      // Any programs that have to access the mounted drives must be run
      // with setInteractive(true) otherwise it doesn't have access to
      // the mounted drives.
      output = gc.runProgramInGuestWithOutput(
              getSnapshotExe(),
              PROJECT_TEMPLATE,
              "-GenerateProject",
              projectOutput);
      output.log(log);
   }

   protected void buildProject() throws IOException, ConverterException {
      checkCancel();
      log.info("Building project.");
      status.setCurrentState(State.buildingProject);
      VmRunProgramOutput output = gc.runBatchScriptInGuestWithOutput(
            VmRunCommandBuilder.RunProgramOptions.defaultOptions(),
            projectOutput,
            ImmutableMap.of("PROJECT_PATH", projectOutput),
            Arrays.asList("build.bat"));
      output.log(log);

      // Check if build.bat left behind package.vo.tvr* files. This indicates a build failure.
      validateBuildOutput();
   }

   private void validateBuildOutput() throws IOException, ConverterException {
      VmRunProgramOutput output = gc.runProgramInGuestWithOutput(
              "cmd.exe", "/c", "dir",  projectOutput + "\\bin /b | find \"package.ro.tvr\" /i /c");
      output.log(log);
      if (!output.getStdout().startsWith("0")) {
         // Build didnt succeed due to left over package.ro.tvr* files. Hence flag as build failure.
         throw new ConverterException("Build incomplete. Found package.ro.tvr* files.");
      }
	}

   protected void deleteTempOutputFiles() {
      checkCancel();
      log.debug("Deleting temporary command output files...");
      try {
         gc.runScriptInGuest("",
               String.format("del %s %s",
                     VmRunCommandBuilder.STDOUT_FILE,
                     VmRunCommandBuilder.STDERR_FILE));
      } catch (IOException ex) {
         // Ignore: Deleting temp files failed, but we don't care
      } catch (VirtualMachineException ex) {
         // Ignore: Deleting temp files failed, but we don't care
      }
   }

   protected void checkCancel() {
      if (status.isRequestCancelling()) {
         throw new CaptureRequestCanceled();
      }
   }

   private void downloadGuestEventlogs() {
      try {
         log.debug("Uploading backupeventlogs.vbs to the guest...");
         gc.uploadFileToGuest(config.getBackupEventlogsScriptPath(),
                              GUEST_BACKUPEVENTLOGS_SCRIPT);
         log.debug("Uploaded backupeventlogs.vbs to the guest...");
         log.debug("Running backupeventlogs.vbs in the guest...");
         gc.runScriptInGuest("",
             String.format("cscript \"%s\" \"%s\"",
                           GUEST_BACKUPEVENTLOGS_SCRIPT,
                           GUEST_BACKUPEVENTLOGS_PATH));
         log.debug("Successfully downloaded event logs from guest.");
      } catch (Exception e) {
         log.debug("Failed to download guest event logs.", e);
      }
   }

   /**
    * Constructs the default mapping for environment variables to be set when
    * running capture commands.
    */
   private void createDefaultEnvVarMap() {
      defaultEnvVarMap.put("ProjectDirectory", projectOutput);
   }

   /**
    * Return the default environment variable mapping.  This can only be called
    * following the preCapture phase.
    */
   protected Map<String, String> getDefaultEnvVarMap() {
      if (CollectionUtils.isEmpty(defaultEnvVarMap)) {
         throw new ConverterException("The default environment variable mapping" +
                 "has not been filled yet.");
      }
      return defaultEnvVarMap;
   }

   protected void runCommands(CommandList commandsToRun,
           CommandRunner commandRunner) throws IOException {
      if (commandsToRun != null && commandsToRun.getCommands() != null) {
         for (Command command : commandsToRun.getCommands()) {
            checkCancel();
            commandRunner.runCommand(command, status);
         }
      }
   }
}
