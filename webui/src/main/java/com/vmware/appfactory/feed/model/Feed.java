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

package com.vmware.appfactory.feed.model;

import java.net.URL;
import java.text.MessageFormat;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.thinapp.common.util.AfCalendar;
/**
 * Data model class for an AppFactory "application feed". A feed has some basic
 * information and a one-to-many relationship to applications.
 */
@Entity
@DiscriminatorValue("feed")
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonIgnoreProperties()
public class Feed extends DataSource
{
   /**
    * The _failure_summary column is set to this value while a feed scan is
    * in progress.
    */
   public static final String SCANNING = "Scanning";

   @NotNull
   private URL _url = null;

   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_contentType", column=@Column(name="_desc_content_type",length=AfText.TYPE_LEN)),
      @AttributeOverride(name="_content", column=@Column(name="_desc_content",length=AfText.CONTENT_LEN))
   } )
   private AfText _description = new AfText();

   @NotNull
   private long _lastRemoteChange = AfCalendar.NEVER;

   /**
    * Create a new feed.
    */
   public Feed()
   {
      super(Type.feed);
   }

   /**
    * Create a new feed based on the input parameters.
    *
    * @param name
    * @param description
    * @param okToScan
    * @param okToConvert
    */
   public Feed(
         String name,
         AfText description,
         boolean okToScan,
         boolean okToConvert)
   {
      super(Type.feed);
      super.setName(name);
      _description = description;
      super.setOkToScan(okToScan);
      super.setOkToConvert(okToConvert);
   }


   /**
    * Get the feed description.
    * @return
    */
   public AfText getDescription()
   {
      return _description;
   }


   /**
    * Set the feed description.
    * @param desc
    */
   public void setDescription(AfText desc)
   {
      _description = desc;
   }


   /**
    * Get the URL where the feed is located.
    * @return
    */
   public URL getUrl()
   {
      return _url;
   }


   /**
    * Set the URL where the feed is located.
    * @param url
    */
   public void setUrl(URL url)
   {
      _url = url;
   }


   /**
    * Set the time when a change in the feed was last detected.
    * @param lastRemoteChange
    */
   public void setLastRemoteChange(long lastRemoteChange)
   {
      _lastRemoteChange = lastRemoteChange;
   }


   /**
    * @return the time when a change in the feed was last detected,
    * in millseconds since the beginning of the epoch.
    */
   public long getLastRemoteChange()
   {
      return _lastRemoteChange;
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
   public String toString()
   {
      return MessageFormat.format("AfFeed[name={0}]", super.getName());
   }
}
