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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

/**
 * Responsible for executing a specific installation.
 * <p/>
 * Current delegates to command line helper.
 */
@Component("installRunner")
@Scope("prototype")
public class InstallRunnerImpl implements InstallRunner {
   private static final Logger log = LoggerFactory.getLogger(InstallRunnerImpl.class);

   @Value("#{workpoolProperties['createvm']}")
   private String CREATE_VM;
   private InstallRequest installRequest;

   public InstallRunnerImpl(InstallRequest installRequest) {
      this.installRequest = installRequest;
   }

   @Override
   public Result run() throws IOException, InterruptedException {
      log.debug("Using create-vm script from: {}.", CREATE_VM);
      File ini = File.createTempFile("create-vm", null);
      try {
         String rawContents = installRequest.toIni(InstallRequest.nullScrubber);
         String redactedContents = installRequest.toIni(InstallRequest.redactedScrubber);
         log.info("\n" + redactedContents);
         FileCopyUtils.copy(rawContents.getBytes(Charsets.UTF_8), ini);
         log.debug("Logging status to {}.", installRequest.getLogFile());
         log.info("Executing create-vm request.");

         // Have to set PYTHON_EGG_CACHE because otherwise Python will try to extract temporary files to
         // /usr/share/tomcat6 which will not work.
         ProcessBuilder pb = new ProcessBuilder(CREATE_VM, ini.getAbsolutePath());
         pb.environment().put("PYTHON_EGG_CACHE", "/tmp/tomcat-egg-cache");
         Process p = pb.start();

         int ret = p.waitFor();
         log.info("create-vm exited with code: {}.", ret);

         // Relog log file.
         for (String line : Files.readLines(installRequest.getLogFile(), Charsets.UTF_8)) {
            log.info(line);
         }

         String stderr = CharStreams.toString(
                 new InputStreamReader(p.getErrorStream(), Charsets.UTF_8)).trim();

         if (StringUtils.hasLength(stderr)) {
            ret = -1; // suppress content (if any) from stdout and enforce the error handling code path
            log.error(stderr);
         }

         installRequest.getLogFile().delete();

         String moid = "";

         if (ret == 0) {
            moid = CharStreams.toString(
                    new InputStreamReader(p.getInputStream(), Charsets.UTF_8)).trim();
            log.debug("Received installed VM with moid {}.", moid);
         }

         return new Result(moid, StringUtils.hasLength(moid), stderr);
      } finally {
         if (!ini.delete()) {
            log.error("Failed to delete file: {}.", ini);
         }
      }
   }
}
