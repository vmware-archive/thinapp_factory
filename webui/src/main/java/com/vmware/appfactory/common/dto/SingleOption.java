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

package com.vmware.appfactory.common.dto;

/**
 * This object represents Select input field option details.
 * This is the simplest form of option.
 *
 * @see GroupOption
 * @see EahOption
 * @see SelectOptions
 */
public class SingleOption extends EachOption
{
   /** _key is the key returned upon selection of this dropdown. */
   private String _key;

   /** disable this option from selection */
   private boolean _disabled;

   /**
    * Create a new option with the key, the display value, and a flag
    * indicating if the display needs to be translated before display.
    *
    * @param key
    * @param display
    * @param translate
    */
   public SingleOption(String key, String display, boolean translate)
   {
      this._key = key;
      this._display = display;
      this._translate = translate;
   }


   /**
    * Set the key as the display value as well and there will not be any translation.
    *
    * @param key
    */
   public SingleOption(String key)
   {
      this._key = key;
      this._display = key;
      this._translate = false;
      this._disabled = false;
   }


   /**
    * Set the options key and display, and no translate is not set.
    *
    * @param key
    * @param display
    */
   public SingleOption(String key, String display)
   {
      this._key = key;
      this._display = display;
      this._translate = false;
   }


   /**
    * @return the key
    */
   public String getKey()
   {
      return _key;
   }


   /**
    * @param key the key to set
    */
   public void setKey(String key)
   {
      this._key = key;
   }


   /**
    * @return the _disabled
    */
   public boolean isDisabled()
   {
      return _disabled;
   }


   /**
    * @param _disabled the disable to set
    */
   public void setDisabled(boolean disabled)
   {
      this._disabled = disabled;
   }
}
