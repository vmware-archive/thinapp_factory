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

package com.vmware.thinapp.workpool

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import tests.fixtures.Defaults

@RunWith(classOf[JUnit4])
class CloneRequestTest {
   @Test
   def testThisThat: Unit = {
      val c = new CloneRequest(
         Defaults.vmLocation,
         Defaults.vcConfig,
         Defaults.vmImage,
         Defaults.workpoolName,
         "password")
      println(c.toIni(CloneRequest.nullScrubber))
      println(c.toIni(CloneRequest.redactedScrubber))
   }
}
