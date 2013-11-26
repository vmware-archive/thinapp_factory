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

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Defines CWS project settings related to registry sub key data.
 *
 * This is based on the CWS API specification: do not edit unless it remains
 * compatible.
 *
 * @author Keerthi Singri
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CwsSettingsRegSubKeyData implements Comparable<CwsSettingsRegSubKeyData>
{
   private String _url;

   private boolean _hasChildren;

   /**
    * Empty default constructor.
    */
   public CwsSettingsRegSubKeyData()
   {  // empty default constructor.
   }

   /**
    * @param url
    * @param hasChildren
    */
   public CwsSettingsRegSubKeyData(String url, boolean hasChildren)
   {
      _url = url;
      _hasChildren = hasChildren;
   }

   /**
    * @return the url
    */
   public String getUrl()
   {
      return _url;
   }

   /**
    * @param url the _url to set
    */
   public void setUrl(String url)
   {
      this._url = url;
   }

   /**
    * @return the _hasChildren
    */
   public boolean isHasChildren()
   {
      return _hasChildren;
   }

   /**
    * @param hasChildren the _hasChildren to set
    */
   public void setHasChildren(boolean hasChildren)
   {
      this._hasChildren = hasChildren;
   }


   @Override
   public int compareTo(CwsSettingsRegSubKeyData o) {
      return new CompareToBuilder()
         .append(this._url, o._url)
         .append(this._hasChildren, o._hasChildren)
         .toComparison();
   }


}
