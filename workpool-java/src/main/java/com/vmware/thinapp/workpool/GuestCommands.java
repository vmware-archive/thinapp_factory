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

package com.vmware.thinapp.workpool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions;

/**
 * Wrapper around running different types of commands inside the guest.
 */
public class GuestCommands {
   private static final Logger log = LoggerFactory
           .getLogger(GuestCommands.class);
   private final VmRunCommandBuilder vmRun;

   // Unique identifier for all commands that are run by this instance
   private final Long commandId;

   @Autowired
   private ProcessMonitorService processMonitor;

   public GuestCommands(
         VCConfig vcConfig,
         InstanceInfo vmInfo,
         Long commandId) {
      vmRun = new VmRunCommandBuilder(vcConfig, vmInfo);
      this.commandId = commandId;
   }

   private abstract class RunCommand {
      private final List<String> cmd, displayCmd;
      private final Logger log =
         LoggerFactory.getLogger(RunCommand.class);
      protected String stdout = "";

      public RunCommand(List<String> cmd) {
         this(cmd, new ArrayList<String>());
      }

      public RunCommand(List<String> cmd, List<String> masks) {
         this.cmd = cmd;
         this.displayCmd = new ArrayList<String>(this.cmd.size());
         boolean maskNext = false;
         for (String arg : this.cmd) {
            if (arg != null && (arg.equals("-p") || arg.equals("-gp"))) {
               maskNext = true;
            } else if (maskNext) {
               arg = "********";
               maskNext = false;
            } else {
               for (String mask : masks) {
                  if (arg != null) {
                     arg = arg.replace(mask, "********");
                  }
               }
            }
            this.displayCmd.add(arg);
         }
      }

      private int run() throws IOException {
         log.info("Running command: {}.", displayCmd);
         ProcessBuilder processBuilder = new ProcessBuilder(cmd);
         processBuilder.redirectErrorStream(true);
         Process process = processBuilder.start();
         BufferedReader bufferedReader = null;

         try {
            processMonitor.add(commandId, process);
            bufferedReader = new BufferedReader(new InputStreamReader(
                            process.getInputStream()));

            while (true) {
               String line = bufferedReader.readLine();

               if (line != null) {
                  stdout = stdout + line + "\n";
                  log.error(line);
               } else {
                  break;
               }
            }

            try {
               int exitCode = process.waitFor();
               handleExit(exitCode);
               return exitCode;
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            } finally {
               // Clear the thread interrupted flag:
               // Process.waitFor() doesn't follow the normal convention of
               // clearing this flag when it throws an InterruptedException, so
               // we need to do it manually.
               Thread.interrupted();
            }
         } finally {
            processMonitor.remove(commandId);

            // As suggested in the following blog post, we need to be very explicit
            // cleaning up the file streams provided by the Process.  We close in, out,
            // err, and then explicitly destroy it just to be safe: http://kylecartmell.com/?p=9
            if (bufferedReader != null) {
               Closeables.closeQuietly(bufferedReader);
            }
            if (process != null) {
               Closeables.closeQuietly(process.getInputStream());
               Closeables.closeQuietly(process.getOutputStream());
               Closeables.closeQuietly(process.getErrorStream());
               process.destroy();
            }
         }
      }

      protected abstract void handleExit(int exitCode);
   }

   /**
    * Download file from the guest to the host.
    *
    * @param guestSource
    * @param hostDest
    * @throws IOException
    */
   public void downloadFileFromGuest(String guestSource, String hostDest)
           throws IOException {
      List<String> cmd = vmRun.downloadFile(guestSource, hostDest);

      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Download file exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "Download file exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }

   /**
    * Upload file to the guest from the host.
    *
    * @param hostSource
    * @param guestDest
    * @throws IOException
    */
   public void uploadFileToGuest(String hostSource, String guestDest)
           throws IOException {
      List<String> cmd = vmRun.uploadFile(hostSource, guestDest);

      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Upload file exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "Upload file exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }

   /**
    *
    */
   public void listRunningVMs() throws IOException {
      List<String> cmd = vmRun.listRunningVMs();

      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("List running VMs exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "List running VMs exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }

   /**
    * Run program in guest using default run options (interactive mode).
    *
    * @see #runProgramInGuest(com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions, String, String...)
    */
   public void runProgramInGuest(String guestPath, String... args) throws IOException {
      runProgramInGuest(RunProgramOptions.defaultOptions(), guestPath, args);
   }

   /**
    * Run program in guest.
    *
    * @param options
    * @param guestPath
    * @param args
    * @throws IOException
    */
   public void runProgramInGuest(RunProgramOptions options, String guestPath,
           String... args) throws IOException {
      List<String> cmd = vmRun.runProgram(options, guestPath, args);

      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Run program exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "Run program exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }

   /**
    * Run batch script inside guest with output.
    *
    * @see #runBatchScriptInGuestWithOutput(com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions, String, java.util.Map, java.util.List)
    */
   public VmRunProgramOutput runBatchScriptInGuestWithOutput(
           String workingDirectory,
           List<String> commands) throws IOException {
      return runBatchScriptInGuestWithOutput(RunProgramOptions.defaultOptions(), workingDirectory,
              new HashMap<String, String>(), commands);
   }

   /**
    * Runs the given command with the given arguments within a batch script.
    * Before the command is run the script will cd to the given working directory.
    * <p/>
    * This is implemented by first creating a local temporary batch script,
    * uploading the script to the guest, then running the script.
    *
    * @param runProgramOptions
    * @param workingDirectory  the working directory the given command should be run
    * @param envVars           environmental variables to set when launching the process
    * @param commands          commands to run
    * @return
    */
   public VmRunProgramOutput runBatchScriptInGuestWithOutput(
           RunProgramOptions runProgramOptions,
           String workingDirectory,
           Map<String, String> envVars,
           List<String> commands) throws IOException {
      // Create the temporary batch script file
      File tempFile = File.createTempFile("tmpBatch", ".bat");
      log.debug("Got temporary batch filename: {}", tempFile.toString());

      // Build the batch script text
      StringBuilder sb = new StringBuilder();
      // cd to the given working directory
      sb.append(String.format("cd /d %s\r\n", workingDirectory));
      // Define any given environment variables
      for (String varName : envVars.keySet()) {
         sb.append(String.format("set %s=%s\r\n", varName, envVars.get(varName)));
      }
      for (String command : commands) {
         sb.append(String.format("%s\r\n", command));
      }

      try {
         // Copy the generated batch file contents to the temp file
         FileCopyUtils.copy(sb.toString().getBytes(Charsets.US_ASCII), tempFile);

         // Upload the batch file to the guest
         String guestBatchFilename = String.format("c:\\%s", tempFile.getName());
         log.debug("Name of batch file in guest: {}", guestBatchFilename);
         this.uploadFileToGuest(tempFile.toString(), guestBatchFilename);

         // Run the batch file in the guest
         log.debug("Running batch file in guest...");
         VmRunProgramOutput batchOutput =
            this.runProgramInGuestWithOutput(runProgramOptions, guestBatchFilename);

         // Delete the batch file from the guest
         log.debug("Deleting batch file from guest...");
         this.runScriptInGuest(
               "",
               String.format("del %s", guestBatchFilename));

         return batchOutput;
      } finally {
         tempFile.delete();
      }
   }

   /**
    * Run program inside guest with output.
    * @see #runProgramInGuestWithOutput(com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions, String, String...)
    */
   public VmRunProgramOutput runProgramInGuestWithOutput(String guestPath, String... args) throws IOException {
      return runProgramInGuestWithOutput(RunProgramOptions.defaultOptions(), guestPath, args);
   }

   /**
    * Run program inside guest with output.
    *
    * @param runProgramOptions program run options
    * @param guestPath program to run
    * @param args program arguments
    * @return program output inside guest
    * @throws IOException
    */
   public VmRunProgramOutput runProgramInGuestWithOutput(
           RunProgramOptions runProgramOptions, String guestPath, String... args)
           throws IOException {
      List<String> cmd =
              vmRun.runProgramWithOutput(runProgramOptions, guestPath, args);

      // Run command
      RunCommand runCommand = new RunCommand(cmd, runProgramOptions.getMasks()) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Run program with output exited with code: {} ({}).",
                     exitCode, this.stdout);
            if (exitCode == 255 || exitCode == -1) {
               /*
                * A non-0 exitCode could be:
                *
                * - the VMRUN was able to login the guest and start the command
                * in the guest OS, but the command itself returned non-zero
                * (not necessary an error). In this case, VMRUN returns the
                * command's exitCode as is but not its stderr error. User
                * should retrieve the command's from guest stderr. In this case,
                * We should defer the exception till the error detail is
                * retrieved from guest's stderr.
                *
                * - the VMRUN failed to login into the guest (due to VC access
                * failure or guest login failure). In this case, VMRUN returns
                * 255 (or -1) and has the error write to stdout. In this case,
                * user should retrieve the VMRUN's error message from host
                * stdout. We can simply raise the exception (with the error
                * just retrieved from the host's stdout) here in this case.
                */
               throw new VirtualMachineException(String.format(
                  "Run program with output exited with code: %d (%s).",
                  exitCode, this.stdout));
            }
         }
      };

      int exitCode = runCommand.run();

      File tempFile = File.createTempFile("guestOutput", null);

      try {
         downloadFileFromGuest("c:\\stdout.txt", tempFile.toString());
         String stdout =
                 Files.toString(tempFile, Charsets.US_ASCII /* XXX: What do we need to support here? */);

         downloadFileFromGuest("c:\\stderr.txt", tempFile.toString());
         String stderr =
                 Files.toString(tempFile, Charsets.US_ASCII /* XXX: What do we need to support here? */);
         if (exitCode != 0 && StringUtils.isNotEmpty(stderr)) {
            stderr = stderr.replace("\r", "").replace("\n\n", "\n");
            throw new IOException(stderr);
         }

         return new VmRunProgramOutput(stdout, stderr);
      } finally {
         tempFile.delete();
      }
   }

   /**
    * Run script inside guest.
    *
    * @see #runScriptInGuest(com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions, String, String)
    */
   public void runScriptInGuest(String shell, String command)
           throws IOException {
      runScriptInGuest(RunProgramOptions.defaultOptions(), shell, command);
   }

   /**
    * Run script inside guest.
    *
    * @param runopts program run options
    * @param shell   empty string for cmd.exe, otherwise a shell path
    * @param command command line to run
    * @throws IOException
    */
   public void runScriptInGuest(RunProgramOptions runopts, String shell, String command)
           throws IOException {
      List<String> cmd = vmRun.runScriptInGuest(runopts, shell, command);

      // Run command
      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Run script in guest exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "Run script in guest exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }

   /**
    * Create a directory inside the guest.
    *
    * @param dest
    */
   public void createDirectory(String dest) throws IOException {
      List<String> cmd = vmRun.createDirectory(dest);

      // Run command
      RunCommand runCommand = new RunCommand(cmd) {
         @Override
         protected void handleExit(int exitCode) {
            log.info("Create directory in guest exited with code: {} ({}).", exitCode, stdout);
            if (exitCode != 0) {
               throw new VirtualMachineException(String.format(
                       "Create directory in guest exited with code: %d (%s).", exitCode, stdout));
            }
         }
      };

      runCommand.run();
   }
}
