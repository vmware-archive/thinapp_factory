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

package com.vmware.thinapp.manualmode.tests

import org.springframework.test.context.ContextConfiguration
import org.junit.Test
import com.vmware.thinapp.manualmode.server.ThinAppRuntimePackager
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.beans.factory.annotation.{Value, Autowired}
import java.io.File

@ContextConfiguration
@RunWith(classOf[SpringJUnit4ClassRunner])
class ThinAppRuntimePackagerTest {
   @Autowired
   val thinappRuntimePackager: ThinAppRuntimePackager = null

   @Value("#{manualModeProperties.runtimesPath}")
   var runtimesPath: String = null

   @Test
   def testCreatePackage = {
      val runtime = ThinAppRuntime("4.0.4", 216012, new File(runtimesPath, "4.0.4-216012").getAbsolutePath)
      println(thinappRuntimePackager.createPackage(runtime))
   }
}