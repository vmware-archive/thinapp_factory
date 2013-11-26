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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.workpool.GuestCommands;
import com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions;
import com.vmware.thinapp.workpool.VmRunProgramOutput;

public class Mounter {
   private static final Logger log = LoggerFactory.getLogger(Mounter.class);
   private final String drive;
   private final String username;
   private final String uncPath;
   private final String password;
   private boolean mounted = false;

   public Mounter(String uncPath, String username, String password, String drive) {
      this.uncPath = uncPath;
      this.username = username;
      this.password = password;
      this.drive = drive;
   }

   /**
    * Execute the mount operation in the virtual machine
    *
    * @param vm
    *           virtual machine to execute in
    * @return path to mounted location
    * @throws IOException
    */
   public String mount(GuestCommands vm) throws IOException {
      try {
         VmRunProgramOutput output =
            vm.runProgramInGuestWithOutput(
                  // Must be interactive or else the drive will be in state "unavailable".
                  RunProgramOptions.defaultOptions().setMaskStrings(
                     Arrays.asList(new String[] {password})),
                     "net", "use", drive,
                     '"' + uncPath + '"',
                     '"' + password + '"',
                     "/user:" + username);
         output.log(log);
      } catch (Exception e) {
         String lastError = e.getMessage()
            .replace("\r", "").replace("\n\n", "\n");

         String msg = String.format(
            "Failed to mount %s to drive %s in capturer: %s",
            uncPath, drive, lastError);

         log.error(msg);
         throw new IOException(msg);
      }

      mounted = true;

      return drive;
   }

   /**
    * Execute the unmount operation in the virtual machine
    *
    * @param vm
    *           virtual machine to execute in
    * @throws IOException
    */
   public void unmount(GuestCommands vm) throws IOException {
      if (mounted) {
         VmRunProgramOutput output =
               vm.runProgramInGuestWithOutput(
                     "net", "use", drive, "/delete", "/yes");
         output.log(log);
      }
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this).
         setExcludeFieldNames(new String[] {"password"}).toString();
   }
}
