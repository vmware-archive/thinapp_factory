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

package com.vmware.appfactory.config.model;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * Data model class for an AppFactory configuration setting value.
 */
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"_key"}))
public class ConfigSetting
extends AbstractRecord
{
   @NotNull
   private String _key = "";

   @Nullable
   private String _value = null;


   /**
    * Creates a new instance.
    */
   public ConfigSetting()
   {
      /* Nothing to do */
   }


   /**
    * Set the configuration parameter key.
    * @param key
    */
   public void setKey(String key)
   {
      if (key == null) {
         throw new IllegalArgumentException();
      }

      _key = key;
   }


   /**
    * Gets the configuration key.
    * @return
    */
   public String getKey()
   {
      return _key;
   }


   /**
    * Set the value.
    * Note: all values persisted as strings, regardless of type.
    * @param value
    */
   public void setValue(@Nullable String value)
   {
      _value = value;
   }


   /**
    * Get the value.
    * Note: all values persisted as strings, regardless of type.
    * @return null if no value is set for this property, or a value
    * if one is.
    */
   @Nullable
   public String getValue()
   {
      return _value;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      ConfigSetting other = (ConfigSetting) record;
      int numChanges = 0;

      if (!StringUtils.equals(getKey(), other.getKey())) {
         setKey(other.getKey());
         numChanges++;
      }

      if (!StringUtils.equals(getValue(), other.getValue())) {
         setValue(other.getValue());
         numChanges++;
      }

      return numChanges;
   }
}
