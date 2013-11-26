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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.VCConfig;

public class VmRunCommandBuilder {
   public static final String STDOUT_FILE = "c:\\stdout.txt";
   public static final String STDERR_FILE = "c:\\stderr.txt";

   private final InstanceInfo vmInfo;
   private final VCConfig vcConfig;

   // XXX: esx for WS targets, need to add this attribute to VCConfig.
   private final String serverType = "vc";

   public static class RunProgramOptions {
      private boolean noWait;
      private boolean activeWindow;
      private boolean interactive;
      private final List<String> masks;

      public RunProgramOptions() {
         this.masks = new ArrayList<String>();
      }

      public boolean isNoWait() {
         return noWait;
      }

      public RunProgramOptions setNoWait(boolean noWait) {
         this.noWait = noWait;
         return this;
      }

      public boolean isActiveWindow() {
         return activeWindow;
      }

      public RunProgramOptions setActiveWindow(boolean activeWindow) {
         this.activeWindow = activeWindow;
         return this;
      }

      public boolean isInteractive() {
         return interactive;
      }

      public RunProgramOptions setInteractive(boolean interactive) {
         this.interactive = interactive;
         return this;
      }

      public List<String> getMasks() {
         return this.masks;
      }

      public RunProgramOptions setMaskStrings(List<String> masks) {
         this.masks.addAll(masks);
         return this;
      }

      /**
       * Default set of options when running a program inside the capture VM.
       *
       * By default always use the interactive option so that we don't get
       * messages on Windows 7 about a program being run on another desktop
       * (inside the Tools context).
       *
       * @return a new instance with interactive mode set
       */
      public static RunProgramOptions defaultOptions() {
         return new RunProgramOptions().setInteractive(true);
      }

      /**
       * Creates a list suitable for passing these arguments on the command line
       *
       * @return list of selected options that vmrun understands
       */
      public List<String> toList() {
         List<String> list = new ArrayList<String>();

         if (isNoWait()) {
            list.add("-noWait");
         }
         if (isActiveWindow()) {
            list.add("-activeWindow");
         }
         if (isInteractive()) {
            list.add("-interactive");
         }

         return list;
      }
   }

   public VmRunCommandBuilder(VCConfig vcConfig, InstanceInfo vmInfo) {
      this.vcConfig = vcConfig;
      this.vmInfo = vmInfo;
   }

   /**
    * Creates a vmrun command line to be run with common options
    *
    * @param command
    *           vmrun command to execute
    * @param args
    *           vmrun arguments to pass to the command
    * @return a {@link List} suitable to pass to {@link ProcessBuilder}
    */
   private List<String> createCommand(String command, String... args) {
      List<String> base =
            Lists.newArrayList("vmrun", "-T", serverType, "-h",
                  String.format("https://%s/sdk", vcConfig.getHost()), "-u",
                  vcConfig.getUsername(), "-p", vcConfig.getPassword(), "-gu",
                  vmInfo.getGuestUsername(), "-gp", vmInfo.getGuestPassword(),
                  command, vmInfo.getVmxPath());

      base.addAll(Arrays.asList(args));
      return base;
   }

   /**
    * Create a command to upload a file from host to guest
    *
    * @param hostSource
    *           source file path
    * @param guestDest
    *           destination file path
    * @return command line to execute
    */
   public List<String> uploadFile(String hostSource, String guestDest) {
      return createCommand("CopyFileFromHostToGuest", hostSource, guestDest);
   }

   public List<String> downloadFile(String guestSource, String hostDest) {
      return createCommand("CopyFileFromGuestToHost", guestSource, hostDest);
   }

   public List<String> listRunningVMs() {
      return createCommand("list");
   }

   public List<String> runProgram(RunProgramOptions runOpts, String guestPath,
         String... args) {
      List<String> runargs = runOpts.toList();
      runargs.add(guestPath);
      runargs.addAll(Arrays.asList(args));

      return createCommand("runProgramInGuest", runargs.toArray(new String[runargs.size()]));
   }

   public List<String> runProgramWithOutput(RunProgramOptions runOpts, String guestPath,
         String... args) {
      List<String> runargs = runOpts.toList();

      String scriptText = String.format("%s %s > %s 2> %s",
            guestPath,
            Joiner.on(" ").join(args),
            VmRunCommandBuilder.STDOUT_FILE,
            VmRunCommandBuilder.STDERR_FILE);

      runargs.add(""); // Uses cmd.exe.
      runargs.add(scriptText);

      return createCommand("runScriptInGuest", runargs.toArray(new String[runargs.size()]));
   }

   public List<String> runScriptInGuest(RunProgramOptions runOpts, String shell, String command) {
      List<String> runargs = runOpts.toList();
      runargs.add(shell);
      runargs.add(command);
      return createCommand("runScriptInGuest", runargs.toArray(new String[runargs.size()]));
   }

   /**
    * Create command for creating a directory inside the guest.
    *
    * @param dest
    * @return
    */
   public List<String> createDirectory(String dest) {
      return createCommand("createDirectoryInGuest", dest);
   }
}
