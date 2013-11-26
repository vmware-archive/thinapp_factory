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

package com.vmware.thinapp.workpool.tests.unit

import org.junit.Test
import com.vmware.thinapp.workpool.InstallRequest
import com.vmware.thinapp.workpool.tests.fixtures.Defaults

class InstallRequestTest {
   @Test
   def testToIni = {
      val req = new InstallRequest(
         "test-vm",
         Defaults.vcConfig,
         Defaults.vmPattern,
         Defaults.vmHardware,
         Defaults.vmLocation, "thinstalled!")
      println(req.toIni(InstallRequest.nullScrubber))
      println(req.toIni(InstallRequest.redactedScrubber))
   }
}
