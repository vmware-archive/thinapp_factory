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

import org.springframework.web.client.RestTemplate
import com.vmware.thinapp.common.converter.dto.{ConversionResponse, ConversionRequest, ConversionJobStatus}

class ConversionClient(baseUrl: String) {
   private val rest = new RestTemplate

   def get(conversionId: Long): ConversionJobStatus = {
      rest.getForObject("%s/%d" format (baseUrl, conversionId), classOf[ConversionJobStatus])
   }

   def create(conversionRequest: ConversionRequest): ConversionResponse = {
      rest.postForObject(baseUrl, conversionRequest, classOf[ConversionResponse])
   }
}