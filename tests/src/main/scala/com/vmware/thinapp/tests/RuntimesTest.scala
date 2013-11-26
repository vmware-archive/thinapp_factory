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

package com.vmware.thinapp.tests

import org.testng.Assert

import com.vmware.thinapp.common.converter.client.ThinAppRuntimeClient
import org.testng.annotations.Test

class RuntimesTest {
   @Test(description = "Verify that the installed ThinApp runtimes can be listed.")
   def verifyRuntimesPresent {
      val url = "%s/mm" format Config.getUrl
      printf("Using URL: %s.\n", url)
      val client = new ThinAppRuntimeClient(url)
      val runtimes = client.list
      println(runtimes)
      Assert.assertEquals(runtimes.size, 7)
   }
}
