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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class provides an object used for displaying drop downs and can be
 * consumed via the velocity templates or the javascript library.
 *
 * @TODO extend this for multi-select.
 */
public class SelectOptions
{
   /** Array of options that are displayed. */
   private EachOption[] _eachOptionArray;

   /** Initial value the drop down should start with. */
   private String _initialValue;


   /**
    * Default empty constructor
    */
   public SelectOptions()
   {
      /* Empty constructor*/
   }


   /**
    * Constructs SelectOptions when passed with an object Array of EachOption
    * @param initialValue
    * @param optionArray
    */
   public SelectOptions(String initialValue, EachOption[] optionArray)
   {
      this._initialValue = initialValue;
      this._eachOptionArray = optionArray;
   }


   /**
    * Constructs SelectOptions when passed with an object list of EachOption.
    *
    * @param initialValue
    * @param optionList
    */
   public SelectOptions(String initialValue, List<EachOption> optionList)
   {
      this._initialValue = initialValue;
      if (CollectionUtils.isNotEmpty(optionList)) {
         this._eachOptionArray =
            optionList.toArray(new EachOption[optionList.size()]);
      }
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


   /**
    * @return the _initialValue
    */
   @JsonProperty("value")
   public String getInitialValue()
   {
      return _initialValue;
   }


   /**
    * @param initialValue the _initialValue to set
    */
   @JsonProperty("value")
   public void setInitialValue(String initialValue)
   {
      this._initialValue = initialValue;
   }


   @Override
   public String toString()
   {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }
}
