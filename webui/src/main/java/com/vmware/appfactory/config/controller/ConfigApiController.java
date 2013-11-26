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

package com.vmware.appfactory.config.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.VelocityMultipleLayoutAndToolsViewResolver;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.dto.EachOption;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigGroupMap;
import com.vmware.appfactory.config.ConfigParam;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.config.model.ConfigChangeEvent;
import com.vmware.thinapp.common.util.AfJson;


/**
 * This controller handles all the configuration-related API calls to
 * AppFactory. All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class ConfigApiController
   extends AbstractApiController
{
    @Autowired
    VelocityMultipleLayoutAndToolsViewResolver resolver;

   /**
    * Get a map that groups all configuration data into groups. Each group name
    * maps to an array of objects; each object contains the configuration
    * parameter AND it's value.
    *
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = "/config",
         method = RequestMethod.GET)
   public Map<String,List<Map<String,Object>>> indexAsMap()
   {
      /* Get all the parameters */
      boolean limitedEditable = !_af.isDevModeDeploy();
      ConfigGroupMap map = _config.getGroupMap(limitedEditable);

      /*
       * Need to create a custom JSON response: each key is a group name,
       * and that maps to an array of objects that contain both parameter
       * data and the value.
       */
      Map<String,List<Map<String,Object>>> response = new HashMap<String,List<Map<String,Object>>>();

      for (String cat : map.keySet()) {
         Map<ConfigParam,String> paramVals = map.get(cat);

         /* Create list of custom JSON objects */
         List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
         response.put(cat, list);

         for (ConfigParam param : paramVals.keySet()) {
            // Skip through the parameters that should be hidden from user.
            if (ConfigRegistryConstants.HIDDEN_CONFIG_KEYS.contains(param.getKey())) {
               continue;
            }
            String value = paramVals.get(param);

            /* Put custom object (containing parameter and value) into list */
            Map<String,Object> obj = new HashMap<String,Object>();
            obj.put("name", param.getTranslationKey());
            obj.put("key", param.getKey());
            obj.put("units", param.getUnits());
            obj.put("type", param.getType());
            obj.put("ordinal", Integer.valueOf(param.getOrdinal()));
            obj.put("value", value);

            if (param.getType() == ConfigParam.Type.SINGLE_SELECT) {

               List<EachOption> opts;
               try {
                     opts = param.getOptions().getValues(_config);
                  } catch (Exception e) {
                     opts = Collections.emptyList();
                     _log.error("Could not add options for config param=" + param, e);
                  }
               obj.put("options", opts);
            }

            list.add(obj);
         }
      }

      return response;
   }

   /**
    * Get the value for a specific configuration parameter.
    * @param key
    * @return The current value for the given key
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/{key}",
         method = RequestMethod.GET)
   public Object getValue(@PathVariable String key)
      throws AfBadRequestException
   {
      // Make sure none of the hidden parameters can be requested by the user.
      if (ConfigRegistryConstants.HIDDEN_CONFIG_KEYS.contains(key)) {
         throw new AfBadRequestException("Invalid parameter " + key);
      }

      boolean devView = _af.isDevModeDeploy();
      try {
         ConfigParam param = _config.getParam(key);
         if (!devView && !param.isUserEditable()) {
            throw new AfBadRequestException("Invalid parameter " + key);
         }
         switch(param.getType()) {
            case STRING: return _config.getString(key);
            case LONG: return Long.valueOf(_config.getLong(key));
            case INTEGER: return Integer.valueOf(_config.getInteger(key));
            case BOOLEAN: return Boolean.valueOf(_config.getBool(key));
            case SINGLE_SELECT: return _config.getString(key);
            default:
               throw new AfBadRequestException("Invalid parameter " + key);
         }
      }
      catch(IllegalArgumentException ex) {
         throw new AfBadRequestException("Invalid parameter " + key);
      }
   }

   /**
    * Set values for one or more parameters.
    * @param request
    * @throws AfBadRequestException
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(
         value = "/config",
         method = RequestMethod.POST)
   public void setValues(HttpServletRequest request)
      throws AfBadRequestException, AfServerErrorException
   {

      if (null != resolver) {
         // TODO(rude): remove this when the old UI is gone
         resolver.clearCache();
      }


      try {
         /* Read JSON data */
         JsonNode node = AfJson.ObjectMapper().readTree(request.getInputStream());

         if (node != null && node.isObject()) {
            Iterator<String> it = node.getFieldNames();
            while (it.hasNext()) {
               String key = it.next();
               String value = node.get(key).getValueAsText();
               _config.setValue(key, value);
            }
         }
         else {
            throw new AfBadRequestException("Badly formed JSON");
         }
      }
      catch(JsonProcessingException ex) {
         throw new AfBadRequestException("Badly formed JSON");
      }
      catch(IOException ex) {
         throw new AfServerErrorException(ex);
      }

      publishEvent(new ConfigChangeEvent(_config));
   }

   /**
    * Set EULA to true. We need this method in addition to setValues()
    * so that it can be invoked prior to the user authentication.
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(
         value = "/config/eula",
         method = RequestMethod.POST)
   public void acceptEula() throws AfServerErrorException
   {
      if (null != resolver) {
         // TODO(rude): remove this when the old UI is gone
         resolver.clearCache();
      }
      try {
         _config.setValue(ConfigRegistryConstants.GEN_EULA_ACCEPTED, Boolean.TRUE.toString());
      }
      catch(Exception ex) {
         throw new AfServerErrorException(ex);
      }
   }
}