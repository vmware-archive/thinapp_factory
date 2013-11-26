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

import java.util.ArrayList;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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
public class CwsSettingsRegValue implements Comparable<CwsSettingsRegValue>
{
   /**
    * Every registry value has a type.
    * These correspond to all the Windows registry data types, or at least
    * those that are supported by ThinApp.
    *
    * See http://msdn.microsoft.com/en-us/library/ms724884(v=vs.85).aspx
    */
   public static enum Type {
      /** A null-terminated string */
      REG_SZ(String.class),

      /** A string with unexpanded variable refs in it (e.g. "%PATH%") */
      REG_EXPAND_SZ(String.class),

      /** A sequence of null-terminated strings, terminated by empty string */
      REG_MULTI_SZ(ArrayList.class),

      /** 32-bit integer */
      REG_DWORD(Integer.class),

      /** 32-bit integer */
      REG_DWORD_LITTLE_ENDIAN(Integer.class),

      /** 64-bit integer */
      REG_QWORD(Integer.class),

      /** Binary data (array of integers) */
      REG_BINARY(ArrayList.class);

      Class<?> dataClass;

      Type(Class<?> thisDataClass) {
         this.dataClass = thisDataClass;
      }
   }

   private Object _data;

   private Type _type;

   private boolean _nameExpand;

   private boolean _dataExpand;


   /**
    * Set the data for this registry value.
    * How the data should be interpreted is defined by the type.
    * @param data
    */
   public void setData(Object data)
   {
      _data = data;
   }


   /**
    * Get the data for this registry value.
    * How the data should be interpreted is defined by the type.
    * @return
    */
   public Object getData()
   {
      return _data;
   }


   /**
    * Set the data type for this registry value.
    * @param type
    */
   public void setType(Type type)
   {
      _type = type;
   }


   /**
    * Get the data type for this registry value.
    * @return
    */
   public Type getType()
   {
      return _type;
   }


   /**
    * Set the "nameExpand" property.
    * @param nameExpand If true, ThinApp will expand variables within the name.
    */
   public void setNameExpand(boolean nameExpand)
   {
      _nameExpand = nameExpand;
   }


   /**
    * Get the "nameExpand" property.
    * @return True if ThinApp will expand variables within the name.
    */
   public boolean isNameExpand()
   {
      return _nameExpand;
   }


   /**
    * Set the "dataExpand" property.
    * @param dataExpand If true, ThinApp will expand variables within the data.
    */
   public void setDataExpand(boolean dataExpand)
   {
      _dataExpand = dataExpand;
   }


   /**
    * Get the "dataExpand" property.
    * @return True if ThinApp will expand variables within the data.
    */
   public boolean isDataExpand()
   {
      return _dataExpand;
   }


   @Override
   public int compareTo(CwsSettingsRegValue o) {
      return new CompareToBuilder()
         .append(this._data, o._data)
         .append(this._type, o._type)
         .toComparison();
   }
}
