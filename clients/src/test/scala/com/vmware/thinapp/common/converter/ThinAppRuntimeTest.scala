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

package com.vmware.thinapp.common.converter

import com.vmware.thinapp.common.converter.dto.ThinAppRuntime
import client.ThinAppRuntimeClient
import org.junit.Test
import org.junit.Assert

class ThinAppRuntimeTest {
   @Test
   def testListRuntimes {
      println(System.getProperties.get("java.class.path"))
      val client = new ThinAppRuntimeClient("http://localhost:9090/mm")
      val rtList: java.util.List[ThinAppRuntime] = client.list
      println(rtList)
   }

   @Test
   def testFetchByRuntimeId {
      val client = new ThinAppRuntimeClient("http://localhost:9090/mm")
      val rtList: java.util.List[ThinAppRuntime] = client.list
      println("Size of runtime list: " + rtList.size())
      if (rtList.size > 0) {
         val rt = client.findById(rtList.get(0).getId())
         Assert.assertEquals(rt.getBuild(), rtList.get(0).getBuild())
         println("Loaded single runtime: " + rt)
      }
   }
}