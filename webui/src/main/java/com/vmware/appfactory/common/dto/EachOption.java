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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * This is an abstract option denoting each option object used for a
 * drop down. There are 2 types, the single one and the group option.
 *
 * @see GroupOption
 * @see SingleOption
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleOption.class),
        @JsonSubTypes.Type(value = GroupOption.class)})
public abstract class EachOption
{
   /** _display is the display text used in the drop downs. */
   protected String _display;

   /** _translate indicates if the display text should be translated. */
   protected boolean _translate;

   /** the id value for the option. */
   protected String _id;

   /**
    * @return the display
    */
   public String getDisplay()
   {
      return _display;
   }


   /**
    * @param display the display to set
    */
   public void setDisplay(String display)
   {
      this._display = display;
   }


   /**
    * @return the translate
    */
   public boolean isTranslate()
   {
      return _translate;
   }


   /**
    * @param translate the translate to set
    */
   public void setTranslate(boolean translate)
   {
      this._translate = translate;
   }


   /**
    * @return the id
    */
   public String getId()
   {
      return _id;
   }


   /**
    * @param id the id to set
    */
   public void setId(String id)
   {
      _id = id;
   }


   @Override
   public String toString()
   {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }
}
