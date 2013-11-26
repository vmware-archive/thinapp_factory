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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.common.collect.Multimap;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.manualmode.util.DownloadResult;
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DriveLetter;
import com.vmware.thinapp.workpool.VmRunCommandBuilder;
import com.vmware.thinapp.workpool.VmRunProgramOutput;

import scala.Option;

/**
 * Capturer implementation that performs automatic captures.
 */
public class AutomaticCapturer extends Capturer {
   // Directory where all input files will be located and where capture
   // commands will be executed
   private static final String JOBDIR = "c:\\jobdir";

   // Path to the setoption.exe utility
   private static final String SET_OPTION_EXE = String.format("%s\\setoption.exe", JOBDIR);

   private final Map<ConversionPhase, CommandList> commands;

   private class DefaultCommandRunner implements CommandRunner {
      @Override
      public void runCommand(Command command, Status status) throws IOException {
         log.debug("Running command '{}' ('{}')",
                 command.getLabel(), command.getCommand());
         status.setLastCommand(command);
         VmRunProgramOutput output = gc.runBatchScriptInGuestWithOutput(
                 VmRunCommandBuilder.RunProgramOptions.defaultOptions(),
                 JOBDIR,
                 getDefaultEnvVarMap(),
                 Arrays.asList(command.getCommand()));
         status.setLastCommand(null);
         output.log(log);
      }
   }

   private class InstallCommandRunner implements CommandRunner {
      @Override
      public void runCommand(Command command, Status status) throws IOException {
         String fullCommand;
         if (downloadResults == null) {
            fullCommand = String.format("\"%s\\%s\"",
                    INPUT_MOUNT_DRIVE_FIRST.driveString(), command.getCommand());
         } else if (command.getCommand().contains("%D")) {
            //XXX: Assuming the first download we're given is the filename to
            //     use to build the full command
            //Replace %D in the given command with the installer name
            fullCommand = String.format("\"%s\\%s\"",
                    INPUT_MOUNT_DRIVE_FIRST.driveString(),
                    downloadResults.get(0).getFilename());
            fullCommand = command.getCommand().replace("%D", fullCommand);
         } else {
            fullCommand = command.getCommand();
         }

         log.debug("Running command '{}' ('{}')",
                 command.getLabel(), fullCommand);
         status.setLastCommand(new Command(command.getLabel(), fullCommand));
         VmRunProgramOutput output = gc.runBatchScriptInGuestWithOutput(
                 VmRunCommandBuilder.RunProgramOptions.defaultOptions(),
                 JOBDIR,
                 getDefaultEnvVarMap(),
                 Arrays.asList(fullCommand));
         status.setLastCommand(null);
         output.log(log);
      }
   }

   private final CommandRunner defaultCommandRunner = new DefaultCommandRunner();
   private final CommandRunner installCommandRunner = new InstallCommandRunner();

   public AutomaticCapturer(List<Future<DownloadResult>> downloadResultFutures,
           List<Mounter> inputMounters, Mounter outputMounter,
           Option<DriveLetter> downloadDriveLetter,
           Multimap<DriveLetter, ProjectFile> driveLetterToFiles,
           Map<ConversionPhase, CommandList> commands,
           ThinAppRuntime thinAppRuntime) {
      super(downloadResultFutures, inputMounters, outputMounter,
	    downloadDriveLetter, driveLetterToFiles, thinAppRuntime);
      this.commands = commands;
   }

   @Override
   protected void preCapture() throws IOException {
      // Provisioning is done, now we're in the pre-capture phase
      status.setCurrentState(State.preCaptureWait);

      // Create the job directory
      checkCancel();
      log.info("Creating job directory");
      VmRunProgramOutput output = gc.runProgramInGuestWithOutput(
              String.format("mkdir %s", JOBDIR));
      output.log(log);

      // Upload setoption.exe
      uploadSetOption(SET_OPTION_EXE);

      // Create copy commands for all input files
      // Copy the downloaded files first, then the datastore files
      List<String> copyCommands = new ArrayList<String>();
      for (DownloadResult downloadResult : downloadResults) {
         copyCommands.add(getCopyCommand(downloadResult.getFilename(),
                 downloadDriveLetter.get()));
      }
      for (DriveLetter driveLetter : driveLetterToFiles.keySet()) {
         for (ProjectFile inputFile : driveLetterToFiles.get(driveLetter)) {
            copyCommands.add(getCopyCommand(inputFile.getFilename(),
                    driveLetter));
         }
      }

      // Run all the commands in the guest to copy all input files from the
      // input fileshares into the job directory
      checkCancel();
      log.info("Copying input files to guest ({})", JOBDIR);
      output = gc.runBatchScriptInGuestWithOutput("c:\\", copyCommands);
      output.log(log);

      log.info("Running precapture commands");
      runCommands(commands.get(ConversionPhase.precapture),
              defaultCommandRunner);
   }

   @Override
   protected void preCaptureSnapshot() throws IOException {
      // Set the state that we've already done precapture customization so
      // Capturer isn't waiting forever
      status.setCurrentState(State.preCaptureDone);
      super.preCaptureSnapshot();
   }

   @Override
   protected void preInstall() throws IOException {
      log.info("Running preinstall commands");
      status.setCurrentState(State.preInstallationWait);
      runCommands(commands.get(ConversionPhase.preinstall),
              defaultCommandRunner);
      status.setCurrentState(State.preInstallationDone);
   }

   /**
    * Calls 'echo test' for a period of time until it succeeds.
    * This ensures the VM is ready to accept interactive commands.
    *
    * @param vm
    * @throws IOException
    */
   @Override
   protected void verifyLoggedIn(boolean autologon) throws IOException {
      if (!autologon) {
         throw new RuntimeException("Automatic capture on a non-autologon VM will fail.");
      }

      if (!performEchoTest()) {
         throw new RuntimeException("Virtual machine marked for autologon is not responding.");
      }
   }

   @Override
   protected void installApplication() throws IOException {
      checkCancel();
      log.info("Running installation commands");
      status.setCurrentState(State.installationWait);
      runCommands(commands.get(ConversionPhase.install), installCommandRunner);
      status.setCurrentState(State.installationDone);
   }

   @Override
   protected void postInstall() throws IOException {
      log.info("Running postinstall commands");
      status.setCurrentState(State.postInstallationWait);
      runCommands(commands.get(ConversionPhase.postinstall), defaultCommandRunner);
      status.setCurrentState(State.postInstallationDone);
   }

   @Override
   protected void preBuild() throws IOException {
      log.info("Running prebuild commands");
      status.setCurrentState(State.preProjectBuildWait);
      runCommands(commands.get(ConversionPhase.prebuild), defaultCommandRunner);
      status.setCurrentState(State.preProjectBuildDone);
   }

   /**
    * Create a copy command that copies the given file from the given drive
    * letter to the guest JOBDIR.
    *
    * @param filename    the name of the file to copy from a mounted drive
    * @param driveLetter the drive letter to copy the file from
    * @return the string for the copy command
    */
   private String getCopyCommand(String filename, DriveLetter driveLetter) {
      // TODO: Need to use xcopy for recursive copy of directories
      return String.format("copy \"%s\\%s\" %s", driveLetter.driveString(),
              filename, JOBDIR);
   }
}
