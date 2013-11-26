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

package com.vmware.thinapp.manualmode.web

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ResponseBody, RequestMethod, RequestMapping}

import com.vmware.thinapp.manualmode.server.RuntimeManager

import com.vmware.thinapp.common.converter.dto.ThinAppRuntime

@Controller
@RequestMapping(Array("/runtimes"))
class RuntimeController @Autowired()(val runtimeManager: RuntimeManager) {
   @RequestMapping(method = Array(RequestMethod.GET))
   @ResponseBody
   def list: java.util.List[ThinAppRuntime] = {
      runtimeManager.getRuntimes
   }
}