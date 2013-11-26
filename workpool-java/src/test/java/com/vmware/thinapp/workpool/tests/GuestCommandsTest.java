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

package com.vmware.thinapp.workpool.tests;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.vmware.thinapp.workpool.GuestCommands;
import com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions;
import com.vmware.thinapp.workpool.VmRunProgramOutput;
import com.vmware.thinapp.workpool.WorkpoolStub;

import static org.junit.Assert.assertEquals;

public class GuestCommandsTest {
   GuestCommands vm;

   @Before
   public void setup() {
      vm = new GuestCommands(WorkpoolStub.vcInfo, WorkpoolStub.vmInfo, null);
   }

   @Test
   public void testUploadDownloadFile() throws Exception {
      vm.uploadFileToGuest("/home/user/uploadFile.txt", "c:\\uploaded.txt");

      vm.downloadFileFromGuest("c:\\uploaded.txt", "/tmp/downloaded.txt");

      String original =
            Files.toString(new File("/home/user/uploadFile.txt"),
                  Charsets.UTF_8);
      String downloaded =
            Files.toString(new File("/tmp/downloaded.txt"), Charsets.UTF_8);

      assertEquals(original, downloaded);
   }

   @Test
   @Ignore("Uploading to a directory that doesn't yet exists doesn't work.")
   public void testUploadFileToDirectory() throws Exception {
      vm.uploadFileToGuest("/home/user/uploadFile.txt", "c:\\dir\\uploaded.txt");
   }

   @Test
   // Note that this test blocks in the VM!
   public void testRunProgram() throws Exception {
      vm.runProgramInGuest(new RunProgramOptions().setActiveWindow(true)
            .setInteractive(true), "C:\\windows\\notepad.exe");
   }

   @Test
   public void testRunProgramInGuestWithOutput() throws Exception {
      VmRunProgramOutput output;

      output = vm.runProgramInGuestWithOutput(new RunProgramOptions(), "echo", "100");

      String stdout = output.getStdout();
      assertEquals("100", stdout.trim());

      output = vm.runProgramInGuestWithOutput(new RunProgramOptions(), "echo 100 1>&2");
      String stderr = output.getStdout();
      assertEquals("100", stderr.trim());
   }
}
