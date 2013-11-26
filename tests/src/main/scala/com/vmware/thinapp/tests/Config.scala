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

import org.codehaus.jackson.map.ObjectMapper

import com.vmware.thinapp.common.datastore.client.DatastoreClient
import com.vmware.thinapp.common.workpool.client.WorkpoolService
import com.vmware.thinapp.common.converter.client.ConversionClient

import io.Source

object Config {
   private val json = new ObjectMapper

   val root = json.readTree(Source.fromFile(System.getProperty("tests.paramsFile")).mkString)

   /**
    * Get the deployed URL.
    */
   def getUrl = System.getProperty("tafUrl")

   /**
    * Construct a URL based on either an overriden value or the deployed instance.
    *
    * @param prop property to check for the overriden url
    * @param withBaseUrl string to format with the deployed URL if there is no overriden value
    */
   def getDefaultUrl(prop: String, withBaseUrl: String): String = {
      Option(System.getProperty(prop)).getOrElse(withBaseUrl format getUrl)
   }

   // XXX: Maybe pull these from the JSON instead?
   def getDatastoreClient = new DatastoreClient(getDefaultUrl("datastoreClientUrl", "%s:5000/storage"))

   def getWorkpoolClient = new WorkpoolService(getDefaultUrl("workpoolServiceUrl", "%s:8080/mm/workpool"))

   def getConversionClient = new ConversionClient(getDefaultUrl("conversionClientUrl", "%s:8080/mm/conversions"))
}