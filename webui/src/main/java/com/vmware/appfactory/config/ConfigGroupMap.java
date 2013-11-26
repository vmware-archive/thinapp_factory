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

package com.vmware.appfactory.config;

import java.util.Map;
import java.util.TreeMap;


/**
 * Groups mappings of parameter-to-value together, using the parameter group
 * name. Since this is based on a TreeSet, the keys (group names) are always
 * sorted (alphabetical order). Further, the parameter-to-value maps are
 * also TreeMaps, so parameters within each group are sorted too.
 */
public class ConfigGroupMap
   extends TreeMap<String,Map<ConfigParam,String>>
{
   private static final long serialVersionUID = 1L;

   /**
    * Add a parameter-to-value mapping into this map.
    * If no map for this parameter group yet exists, one will be created.
    * Else, this mapping is added to the existing group.
    *
    * @param param Configuration parameter.
    * @param value Current parameter value.
    */
   public void add(ConfigParam param, String value)
   {
      /* Get settings for this group. */
      Map<ConfigParam,String> groupSettings = get(param.getGroup());

      if (groupSettings == null) {
         /* No settings yet: create some. */
         groupSettings = new TreeMap<ConfigParam, String>();
         put(param.getGroup(), groupSettings);
      }

      groupSettings.put(param, value);
   }

   /**
    * Return a Map of AfConfigParams that are editable.
    *
    * @param group
    * @return Map<AfConfigParam,String> - shalow copy / null if empty.
    */
   public Map<ConfigParam,String> getEditableConfigParam(String group)
   {
      if (group == null) {
         return null;
      }

      Map<ConfigParam, String> map = get(group);
      if (map == null) {
         return null;
      }

      // Create a shallow copy of only the applicable ones.
      Map<ConfigParam, String> result = new TreeMap<ConfigParam, String>();
      for (ConfigParam param : map.keySet()) {
         if (!param.isUserEditable()){
            result.put(param, map.get(param));
         }
      }
      return (result.size() == 0)? null : result;
   }
}
