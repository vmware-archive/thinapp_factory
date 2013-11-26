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

package com.vmware.thinapp.workpool.tests.functional

import org.junit.Test
import org.junit.Assert._
import com.vmware.thinapp.common.workpool.dto.{VCConfig}
import com.vmware.thinapp.common.workpool.client.WorkpoolService

class VcConfigTest {
   @Test
   def setAndGet = {
      val wp = new WorkpoolService("http://localhost:8080/workpool")
      val vc = new VCConfig()
      vc.setHost("test host")
      vc.setUsername("jcamp")
      vc.setPassword("ca$hc0w")
      vc.setDatacenter("Prime DC")
      wp.setConfig(vc)

      assertEquals(vc, wp.getConfig())
   }
}
