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

package com.vmware.appfactory.feed.dto;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * Data submitted to the FeedApiController in order to create a new
 * feed.
 */
public class FeedRequest
{
   /** Feed name */
   public String name;

   /** Feed description (text only) */
   public String description;

   /** Feed URL */
   public String url;

   /** If true, feed will be scanned at the default interval */
   public boolean okToScan;

   /** If true, feed applications will be auto converted as needed */
   public boolean okToConvert;

   /** If true, feed URL needs user/password */
   public boolean authRequired;

   /** Feed authentication */
   public String authUsername;

   /** Feed authentication */
   public String authPassword;

   /** Map of applications to include or exclude from auto conversion */
   public Map<Long, Boolean> appIncludes;

   /** The "url" field parsed into a URL class after validation */
   public URL realUrl;

   /**
    * Check this instance contains valid data.
    * @throws InvalidDataException
    */
   public void validate()
      throws InvalidDataException
   {
      /* Name and URL are required */
      if (AfUtil.anyEmpty(name)) {
         throw new InvalidDataException("Required field missing.");
      }

      /* Authorization user and pass are required */
      // XXX empty password should be OK
      if (authRequired) {
         if (AfUtil.anyEmpty(authUsername, authPassword)) {
            throw new InvalidDataException("Required field missing.");
         }
      }
      /* URL must be good */
      try {
         realUrl = new URL(url);
      }
      catch(MalformedURLException ex) {
         throw new InvalidDataException("URL " + url + " is invalid");
      }
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}