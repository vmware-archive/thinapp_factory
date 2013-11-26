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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


/**
 * Describes a configuration parameter used generally in AppFactory for
 * all kinds of settings. The actual configuration parameters available are
 * defined in AfConfigRegistry.
 *
 * @see ConfigRegistry
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class ConfigParam
   implements Comparable<ConfigParam>
{
   /**
    * Every parameter has one of these data types.
    */
   public static enum Type {
      /** Long integer */
      LONG,
      /** Short integer */
      INTEGER,
      /** String */
      STRING,
      /** Boolean */
      BOOLEAN,
      /** One of a set */
      SINGLE_SELECT
   }

   private final String _group;

   private final String _key;

   private final String _translateKey;

   private final ConfigParam.Type _type;

   private final int _ordinal;

   private String _units = null;

   private ConfigParamOptions _options = null;

   private boolean _userEditable = true;

   /**
    * Define a new STRING parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @return
    */
   public static ConfigParam newStringInstance(
         String group,
         int ord,
         String key,
         boolean userEditable)
   {
      return new ConfigParam(
            group, ord, key,
            ConfigParam.Type.STRING,
            userEditable, null);
   }


   /**
    * Define a new LONG parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @param units
    * @return
    */
   public static ConfigParam newLongInstance(
         String group,
         int ord,
         String key,
         boolean userEditable,
         String units)
   {
      return new ConfigParam(
            group, ord, key,
            ConfigParam.Type.LONG,
            userEditable, units);
   }


   /**
    * Define a new INTEGER parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @param units
    * @return
    */
   public static ConfigParam newIntegerInstance(
         String group,
         int ord,
         String key,
         boolean userEditable,
         String units)
   {
      return new ConfigParam(
            group, ord, key,
            ConfigParam.Type.INTEGER,
            userEditable, units);
   }


   /**
    * Define a new BOOLEAN parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @return
    */
   public static ConfigParam newBooleanInstance(
         String group,
         int ord,
         String key,
         boolean userEditable)
   {
      return newBooleanInstance(group, ord, key, userEditable, null);
   }

   /**
    * Define a new BOOLEAN parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @return
    */
   public static ConfigParam newBooleanInstance(
         String group,
         int ord,
         String key,
         boolean userEditable,
         String units)
   {
      return new ConfigParam(
            group, ord, key,
            ConfigParam.Type.BOOLEAN,
            userEditable, units);
   }


   /**
    * Define a new SINGLE_SELECT parameter.
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param userEditable
    * @param options
    * @return
    */
   public static ConfigParam newSelectInstance(
         String group,
         int ord,
         String key,
         boolean userEditable,
         ConfigParamOptions options)
   {
      ConfigParam param = new ConfigParam(
            group, ord, key,
            ConfigParam.Type.SINGLE_SELECT,
            userEditable, null);

      param._options = options;
      return param;
   }


   /**
    * Create a new parameter instance.
    *
    *
    * @param group Configuration group (for UI display).
    * @param ord Ordinal (for UI display).
    * @param key Unique key for this parameter.
    * @param units
    */
   private ConfigParam(
         String group,
         int ord,
         String key,
         ConfigParam.Type type,
         boolean userEditable,
         String units)
   {
      _group = group;
      _ordinal = ord;
      _key = key;
      _type = type;
      _units = units;
      _userEditable = userEditable;
      _translateKey = "T.CONFIG." + StringUtils.upperCase(key);
   }


   /**
    * Get the UI group that this parameter belongs to.
    * @return
    */
   public String getGroup()
   {
      return _group;
   }


   /**
    * @return
    */
   public String getKey()
   {
      return _key;
   }


   /**
    * Get the translation key for the display name of this parameter.
    * @return
    */
   public String getTranslationKey()
   {
      return _translateKey;
   }


   /**
    * @return
    */
   public ConfigParam.Type getType()
   {
      return _type;
   }


   /**
    * Get units, if any.
    * @return The units, or null if none.
    */
   public String getUnits()
   {
      return _units;
   }


   /**
    * Get the _userEditable flag.
    * @return
    */
   public boolean isUserEditable()
   {
      return _userEditable;
   }


   /**
    * Sets the _editable flag.
    * @param userEditable
    */
   public void setUserEditable(boolean userEditable)
   {
      this._userEditable = userEditable;
   }


   /**
    * Get the set of options (for SINGLE_SELECT parameters).
    * @return
    */
   public ConfigParamOptions getOptions()
   {
      return _options;
   }


   /**
    * Set the set of options (for SINGLE_SELECT parameters).
    * @param options
    */
   public void setOptions(ConfigParamOptions options)
   {
      _options = options;
   }


   /**
    * Get the parameter's ordinal.
    * @return
    */
   public int getOrdinal()
   {
      return _ordinal;
   }


   @Override
   public int compareTo(ConfigParam other)
   {
      return _ordinal - other._ordinal;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}