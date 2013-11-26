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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This is the other type of EachOption. This group can hold more options
 * within itself.
 *
 * @see GroupOption
 * @see EachOption
 */
public class GroupOption extends EachOption
{
   /** Array of options that are part of this group. */
   private EachOption[] _eachOptionArray;

   /**
    * Create a new groupOption with the display value with translate
    * turned off.
    *
    * @param display
    */
   public GroupOption(String display)
   {
      this._display = display;
      this._translate = false;
   }


   /**
    * Create a new groupOption with the display value, and a flag
    * indicating if the display needs to be translated before display.
    *
    * @param display
    * @param translate
    */
   public GroupOption(String display, boolean translate)
   {
      this._display = display;
      this._translate = translate;
   }


   /**
    * Create a new groupOption with the display value, list of EachOption,
    * and a flag indicating if the display needs to be translated before
    * display.
    *
    * @param display
    * @param translate
    * @param optionList
    */
   public GroupOption(String display, boolean translate, List<EachOption> optionList)
   {
      this._display = display;
      this._translate = translate;
      if (CollectionUtils.isNotEmpty(optionList)) {
         this._eachOptionArray =
            optionList.toArray(new EachOption[optionList.size()]);
      }
   }

   /**
    * Create a new groupOption with the id, display value, list of EachOption,
    * and a flag indicating if the display needs to be translated before
    * display.
    *
    * @param id
    * @param display
    * @param translate
    * @param optionList
    */
   public GroupOption(String id, String display, boolean translate, List<EachOption> optionList)
   {
      this._id = id;
      this._display = display;
      this._translate = translate;
      if (CollectionUtils.isNotEmpty(optionList)) {
         this._eachOptionArray =
            optionList.toArray(new EachOption[optionList.size()]);
      }
   }


   /**
    * Create a new groupOption with the display value, list of EachOption,
    * and a flag indicating if the display needs to be translated before
    * display.
    *
    * @param display
    * @param translate
    * @param optionArray
    */
   public GroupOption(String display, boolean translate, EachOption[] optionArray)
   {
      this._display = display;
      this._translate = translate;
      this._eachOptionArray = optionArray;
   }


   /**
    * @return the _eachOptionArray
    */
   @JsonProperty("options")
   public EachOption[] getEachOptionArray()
   {
      return _eachOptionArray;
   }


   /**
    * @param eachOptionArray the _eachOptionArray to set
    */
   @JsonProperty("options")
   public void setEachOptionArray(EachOption[] eachOptionArray)
   {
      this._eachOptionArray = eachOptionArray;
   }
}
