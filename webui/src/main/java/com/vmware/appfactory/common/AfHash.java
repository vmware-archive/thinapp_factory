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

package com.vmware.appfactory.common;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 * This class represents a hash function that is used to verify a
 * successful application download.
 *
 * This class is persisted by embedding into the table for AfApplication
 * so do not rename the fields without updating that class also.
 *
 * TODO: Pass hash to CWS so it can be used.
 */
@Embeddable
public class AfHash
{
   /**
    * Hash functions that are supported.
    */
   public static enum Function
   {
      /** A dummy hash function */
      NONE,
      /** SHA1 */
      SHA1;
   }

   /* Do not rename: see AfApplication */
   @NotNull
   private Function _function = Function.NONE;

   /* Do not rename: see AfApplication */
   @NotNull
   private String _value = "";


   /**
    * Create a new AfHash instance.
    */
   public AfHash()
   {
      /* Nothing to do */
   }


   /**
    * Set the hash function.
    * @param function
    */
   public void setFunction(Function function)
   {
      if (function == null) {
         throw new IllegalArgumentException();
      }

      _function = function;
   }


   /**
    * Get the hash function.
    * @return
    */
   public Function getFunction()
   {
      return _function;
   }


   /**
    * Set the hash value.
    * @param value
    */
   public void setValue(String value)
   {
      if (value == null) {
         throw new IllegalArgumentException();
      }

      _value = value;
   }


   /**
    * Get the hash value.
    * @return
    */
   public String getValue()
   {
      return _value;
   }


   @Override
   public AfHash clone()
   {
      AfHash clone = new AfHash();
      clone._function = _function;
      clone._value = _value;
      return clone;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }

      if (obj == this) {
         return true;
      }

      if (!(obj instanceof AfHash)) {
         return false;
      }

      AfHash other = (AfHash) obj;
      return _function == other._function &&
             _value.equals(other._value);
   }


   @Override
   public int hashCode()
   {
      return (_function + _value).hashCode();
   }


   /**
    * Null-safe equality check for two AfHash instances.
    * @return
    */
   public static boolean equals(AfHash hash1, AfHash hash2)
   {
      return (hash1 == null ? hash2 == null : hash1.equals(hash2));
   }
}
