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

package com.vmware.appfactory.common.base;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.vmware.appfactory.common.AppFactory;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.CwsClientService;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.util.AfJson;

/**
 * Serves as a superclass for any @Service classes that provide a client
 * interface to other appliance services.
 *
 * The CWS client, Datastore client and Workpool client all inherit from this.
 * @see CwsClientService
 * @see DatastoreClientService
 * @see WorkpoolClientService
 */
public abstract class AbstractRestClient
{
   @Resource
   protected ConfigRegistry _config;

   @Resource
   protected AppFactory _appFactory;


   protected Logger _log;

   protected final RestTemplate _rest = new RestTemplate();

   /* Trying to POST or PUT a null request gets you a 411 error */
   protected static final HttpEntity<String> EMPTY_REQUEST = new HttpEntity<String>("empty");

   private final String _baseUrlProp;


   /**
    * Instantiate this base class.
    * Configures a logger specific to the most-derived child class.
    */
   protected AbstractRestClient(String baseUrlProperty)
   {
      _log = LoggerFactory.getLogger(getClass());
      _baseUrlProp = baseUrlProperty;
   }


   /**
    * Construct the full API URL from the configured base and the
    * specified relative components.
    *
    * @return
    */
   protected String baseUrl()
   {
      return _config.getString(_baseUrlProp);
   }


   /**
    * Check to see whether JSON input/output should be logged or not,
    * and if so, call logJson with the given message and object.
    * This provides more detailed information for debugging.
    *
    * @param message Debug message to print before JSON.
    * @param obj Object to convert to JSON and log.
    */
   protected void debugLogJson(String message, Object obj)
   {
      if (_log.isDebugEnabled() && _config.getBool(ConfigRegistryConstants.DEBUG_JSON_LOGGING)) {
         logJson(message, obj);
      }
   }


   /**
    * Convert the object into JSON and log it.
    *
    * @param message Debug message to print before JSON.
    * @param obj Object to convert to JSON and log.
    */
   protected void logJson(String message, Object obj) {
      _log.debug(message);
      try {
         String json = AfJson.ObjectMapper().writeValueAsString(obj);
         _log.debug(json);
      }
      catch(Exception ex) {
         _log.debug("Failed to convert to JSON for debugging");
      }
   }
}
