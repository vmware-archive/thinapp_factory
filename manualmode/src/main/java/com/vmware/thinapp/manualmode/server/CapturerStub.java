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
import java.util.List;
import java.util.concurrent.Future;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.manualmode.util.DownloadResult;
import com.vmware.thinapp.manualmode.util.DriveLetterManager.DriveLetter;

import scala.Option;

public class CapturerStub extends Capturer {
   private static final Option<DriveLetter> emptyDriveLetter = Option.empty();

   private static final Multimap<DriveLetter, ProjectFile> emptyMultimap =
      HashMultimap.create();

   public CapturerStub(List<Mounter> inputMounters, Mounter outputMounter, ThinAppRuntime thinAppRuntime) {
      super(new ArrayList<Future<DownloadResult>>(), inputMounters,
            outputMounter, emptyDriveLetter, emptyMultimap, thinAppRuntime);
   }

   @Override
   protected void installApplication()
         throws IOException {
      log.info("Performing capture.");

      status.setCurrentState(State.preCaptureWait);
      status.waitUntil(State.preCaptureDone);
      status.setCurrentState(State.takingPreCaptureSnapshot);
      status.setCurrentState(State.installationWait);
      status.waitUntil(State.installationDone);
   }

   @Override
   protected void buildProject() throws IOException {
      log.info("Capturing package.");
   }

   @Override
   protected void preCapture() throws IOException {
      // Nothing to do
   }

   @Override
   protected void preInstall() throws IOException {
      // Nothing to do
   }

   @Override
   protected void postInstall() throws IOException {
      // Nothing to do
   }

   @Override
   protected void preBuild() throws IOException {
      // Nothing to do
   }

   @Override
   protected void verifyLoggedIn(boolean autologon) throws IOException {
      // Nothing to do
   }
}
