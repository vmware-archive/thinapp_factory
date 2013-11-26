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

package com.vmware.appfactory.cws;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.slf4j.Logger;

import com.vmware.appfactory.common.exceptions.InvalidDataException;

/**
 * Defines CWS project settings related to registry settings.
 *
 * This is based on the CWS API specification: do not edit unless it remains
 * compatible.
 *
 * @author levans
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CwsSettingsRegKey implements Comparable<CwsSettingsRegKey>
{
   /**
    * Each registry key has an isolation mode setting.
    */
   public static enum IsolationMode {
      full,
      merged,
      writecopy,
      sb_only
   }

   private Long _id;

   private String _path;

   private IsolationMode _isolation;

   private final Map<String, CwsSettingsRegSubKeyData> _subkeys
         = new TreeMap<String, CwsSettingsRegSubKeyData>();

   private final Map<String, CwsSettingsRegValue> _values
         = new TreeMap<String, CwsSettingsRegValue>();


   /**
    * Set the full path of this registry key.
    * @param path
    */
   public void setPath(String path) {
      _path = path;
   }


   /**
    * Get the full path of this registry key.
    * @return
    */
   public String getPath() {
      return _path;
   }


   /**
    * Set the isolation mode for this registry key.
    * @param isolation
    */
   public void setIsolation(IsolationMode isolation) {
      _isolation = isolation;
   }


   /**
    * Get the isolation mode for this registry key.
    * @return
    */
   public IsolationMode getIsolation() {
      return _isolation;
   }


   public Map<String, CwsSettingsRegSubKeyData> getSubkeys() {
      return _subkeys;
   }


   public void addSubkey(String name, CwsSettingsRegSubKeyData subKeyData) {
      _subkeys.put(name, subKeyData);
   }


   public void addSubkey(String name, String uri, boolean hasChildren) {
      _subkeys.put(name, new CwsSettingsRegSubKeyData(uri, hasChildren));
   }


   public Map<String, CwsSettingsRegValue> getValues() {
      return _values;
   }


   public void addValue(String key, CwsSettingsRegValue value) {
      _values.put(key, value);
   }


   /**
    * Set the unique ID (defined by CWS) for this registry key.
    * @param registryId
    */
   public void setId(Long registryId)
   {
      _id = registryId;
   }


   /**
    * Get the unique ID (defined by CWS) for this registry key.
    * @return
    */
   public Long getId()
   {
      return _id;
   }


   /**
    * Validate a registry key.
    * If all is well, this returns with no result. If a problem is found,
    * a CwsValidationException is thrown explaining why.
    *
    * TODO: More can be done here.
    *
    * @param log
    * @throws InvalidDataException
    */
   public void validate(Logger log)
      throws InvalidDataException
   {
      log.debug("Validating registry key " + getPath());

      /*
       * Check all the values. Each value must have data whose class matches
       * the data type.
       */
      log.debug("Values...");
      for (String valName : _values.keySet()) {
         CwsSettingsRegValue value = _values.get(valName);
         Class<?> expectedClass = value.getType().dataClass;
         Class<?> actualClass = value.getData().getClass();

         if (actualClass != expectedClass) {
            String msg =
               "Registry value " + valName + " is a " + value.getType() +
               " which expects " + expectedClass +
               " but data is " + actualClass;

            log.error(msg);
            throw new InvalidDataException(msg);
         }
         log.debug(valName + " is OK");
      }

      log.debug("Registry key OK");
   }


   @Override
   public int compareTo(CwsSettingsRegKey o) {
      return new CompareToBuilder()
         .append(this._id, o._id)
         .append(this._path, o._path)
         .append(this._isolation, o._isolation)
         .append(this._subkeys, o._subkeys)
         .append(this._values, o._values)
         .toComparison();
   }
}
