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
import com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions;
import com.vmware.thinapp.workpool.VmRunProgramOutput;

import scala.Option;

/**
 * Capturer implementation that performs manual captures.
 */
public class ManualCapturer extends Capturer {
   // Path to the setoption.exe utility
   private static final String SET_OPTION_EXE = "C:\\setoption.exe";

   private class DefaultCommandRunner implements CommandRunner {
      @Override
      public void runCommand(Command command, Status status) throws IOException {
         log.debug("Running command '{}' ('{}')",
                 command.getLabel(), command.getCommand());
         VmRunProgramOutput output = gc.runBatchScriptInGuestWithOutput(
                 VmRunCommandBuilder.RunProgramOptions.defaultOptions(),
                 "C:\\",
                 getDefaultEnvVarMap(),
                 Arrays.asList(command.getCommand()));
         output.log(log);
      }
   }

   private final Map<ConversionPhase, CommandList> commands;
   private final CommandRunner defaultCommandRunner = new DefaultCommandRunner();

   public ManualCapturer(List<Future<DownloadResult>> downloadResultFutures,
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
      // Upload setoption.exe
      uploadSetOption(SET_OPTION_EXE);
   }

   @Override
   protected void preInstall() throws IOException {
      // Nothing to do
   }

   /**
    * Waits for the state to change into needsLoginDone.
    *
    * @param vm
    * @throws IOException
    */
   @Override
   protected void verifyLoggedIn(boolean autologon) throws IOException {
      // Wait for the user to say "I've logged in."
      if (!autologon) {
         status.setCurrentState(State.needsLoginWait);
         status.waitUntil(State.needsLoginDone);
      }

      // We must still wait for the login state to accept interactive commands.
      if (!performEchoTest()) {
         throw new RuntimeException("Guest login for manual capture did not become interactive");
      }
   }

   @Override
   protected void installApplication() throws IOException {
      VmRunProgramOutput output;

      // Pop open an Explorer window pointing to the installation files.
      output = gc.runProgramInGuestWithOutput(
              RunProgramOptions.defaultOptions().setActiveWindow(true).setNoWait(true),
              "explorer", INPUT_MOUNT_DRIVE_FIRST.driveString());
      output.log(log);

      // XXX: This will hold onto a thread for each client which is
      // unfortunate.
      checkCancel();
      log.info("Doing installation");
      status.setCurrentState(State.installationWait);
      status.waitUntil(State.installationDone);

      // Must do after waitUntil since request may have been canceled.
      checkCancel();
   }

   @Override
   protected void postInstall() throws IOException {
      // Nothing to do
   }

   @Override
   protected void preBuild() throws IOException {
      log.info("Running prebuild commands");
      // TODO: Note that we only run the prebuild commands and no others in the conversion
      // request.  This is to support adding Quality Reporting.  For full recipe support
      // in manual captures, the automatic capture command running code should be used by
      // the manual and automatic capturer.
      runCommands(commands.get(ConversionPhase.prebuild), defaultCommandRunner);
   }
}
