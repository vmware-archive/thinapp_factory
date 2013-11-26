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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

public class VmRunProgramOutput {
   private final String stdout;
   private final String stderr;

   public VmRunProgramOutput(String stdout, String stderr) {
      this.stdout = stdout;
      this.stderr = stderr;
   }

   public String getStderr() {
      return stderr;
   }

   public String getStdout() {
      return stdout;
   }

   public void log(final Logger logger) {
      if (logger == null) {
         return;
      }

      if (StringUtils.isNotEmpty(getStdout())) {
         logger.info(getStdout());
      }
      if (StringUtils.isNotEmpty(getStderr())) {
         logger.error(getStderr());
      }
   }

}
