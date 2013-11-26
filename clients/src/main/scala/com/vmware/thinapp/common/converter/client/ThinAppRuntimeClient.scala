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

package com.vmware.thinapp.common.converter.client

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList

import org.springframework.web.client.RestTemplate

import com.vmware.thinapp.common.converter.dto.ThinAppRuntime
import com.vmware.thinapp.common.exception.BaseRuntimeException

class ThinAppRuntimeClient(url: String) {
   val rest = new RestTemplate()

   def list: java.util.List[ThinAppRuntime] = {
      rest.getForObject(url + "/runtimes", classOf[Array[ThinAppRuntime]]).toList
   }

   /**
    * Return a ThinAppRuntime for the id passed, else throws RuntimeException
    */
   def findById(id: Long): ThinAppRuntime = {
      // TODO: BaseRuntimeException is a RuntimeException.  We really shouldn't
     //        return this in the case of an "id not found" error
      list.find(_.id == id).getOrElse(throw new BaseRuntimeException("Runtime not found."))
   }
}