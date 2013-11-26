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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;
import com.vmware.thinapp.workpool.VmRunCommandBuilder;
import com.vmware.thinapp.workpool.VmRunCommandBuilder.RunProgramOptions;
import com.vmware.thinapp.workpool.WorkpoolStub;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:TestVmRunCommandBuilder-context.xml")
public class VmRunCommandBuilderTest {

   public static final String VC = "https://vc.your.company.com/sdk";
   public static final String VC_USER = "vc-user";
   public static final String VC_PWD = "vc-password";
   public static final String GUEST_USER = "guest-user";
   public static final String GUEST_PWD = "guest-password";
   public static final String DS_LOC = "[LUN1] appfactory-dev Instance/appfactory-dev Instance.vmx";

   @Test
   public void testUploadFile() throws Exception {
      VmRunCommandBuilder v = new VmRunCommandBuilder(WorkpoolStub.vcInfo,
            WorkpoolStub.vmInfo);

      // NOTE: Destination directory must already exist.
      List<String> expected = ImmutableList.of(
            "vmrun",
            "-T", "vc",
            "-h", VC,
            "-u", VC_USER,
            "-p", VC_PWD,
            "-gu", GUEST_USER,
            "-gp", GUEST_PWD,
            "CopyFileFromHostToGuest",
            DS_LOC,
            "/tmp/file.exe",
            "C:\\tmp\\dest.exe");

      List<String> cmd = v.uploadFile("/tmp/file.exe", "C:\\tmp\\dest.exe");

      assertEquals(expected, cmd);
   }

   @Test
   public void testDownloadFile() throws Exception {
      VmRunCommandBuilder v = new VmRunCommandBuilder(WorkpoolStub.vcInfo,
            WorkpoolStub.vmInfo);

      // NOTE: Destination directory must already exist.
      List<String> expected = ImmutableList.of(
            "vmrun",
            "-T", "vc",
            "-h", VC,
            "-u", VC_USER,
            "-p", VC_PWD,
            "-gu", GUEST_USER,
            "-gp", GUEST_PWD,
            "CopyFileFromHostToGuest",
            DS_LOC,
            "C:\\tmp\\dest.exe",
            "/tmp/file.exe");

      List<String> cmd = v.downloadFile("C:\\tmp\\dest.exe", "/tmp/file.exe");

      assertEquals(expected, cmd);
   }

   @Test
   public void testRunProgram() throws Exception {
      VmRunCommandBuilder v = new VmRunCommandBuilder(WorkpoolStub.vcInfo,
            WorkpoolStub.vmInfo);

      // NOTE: Destination directory must already exist.
      List<String> expected = ImmutableList.of(
            "vmrun",
            "-T", "vc",
            "-h", VC,
            "-u", VC_USER,
            "-p", VC_PWD,
            "-gu", GUEST_USER,
            "-gp", GUEST_PWD,
            "CopyFileFromHostToGuest",
            "runProgramInGuest",
            DS_LOC,
            "-interactive",
            "C:\\Windows\\notepad.exe",
            "input.txt");


      RunProgramOptions runOptions = new RunProgramOptions();
      runOptions.setInteractive(true);

      List<String> cmd = v.runProgram(runOptions, "C:\\Windows\\notepad.exe", "input.txt");

      assertEquals(expected, cmd);
   }

   @Test
   public void testRunScriptInGuest() throws Exception {
      VmRunCommandBuilder v = new VmRunCommandBuilder(WorkpoolStub.vcInfo,
            WorkpoolStub.vmInfo);

      // NOTE: Destination directory must already exist.
      List<String> expected = ImmutableList.of(
            "vmrun",
            "-T", "vc",
            "-h", VC,
            "-u", VC_USER,
            "-p", VC_PWD,
            "-gu", GUEST_USER,
            "-gp", GUEST_PWD,
            "runScriptInGuest",
            DS_LOC,
            "-interactive",
            "",
            "dir /s > c:\\stdout.txt 2> c:\\stderr.txt");

      RunProgramOptions runOptions = new RunProgramOptions().setInteractive(true);
      List<String> cmd = v.runProgramWithOutput(runOptions, "dir", "/s");
      assertEquals(expected, cmd);
   }
}
