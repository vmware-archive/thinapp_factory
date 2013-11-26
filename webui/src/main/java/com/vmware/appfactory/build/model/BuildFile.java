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

package com.vmware.appfactory.build.model;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.vmware.appfactory.common.base.AbstractRecord;


/**
 * A build file is an executable ThinApp file.
 *
 * Builds contain one or more of these files: each file corresponds to an
 * application that can be run. Users may or may not be able to download
 * individual files from a build (if not, they can download the build,
 * which will include all the files).
 */
@Entity
public class BuildFile
   extends AbstractRecord
{
   @NotNull
   private String _url;

   @NotNull
   private long _size = 0;

   @NotNull
   private String _exeName = "";


   /**
    * Create a new instance.
    */
   public BuildFile()
   {
      /* Nothing to do */
   }


   /**
    * Get the file ID (same as the record ID).
    * @return
    */
   @JsonProperty("file_id")
   public Long getFileId()
   {
      return getId();
   }


   /**
    * Set the URL to the file.
    * @param url
    */
   public void setUrl(String url)
   {
      if (url == null) {
         throw new IllegalArgumentException();
      }

      _url = url;
   }


   /**
    * Get the URL to the file.
    * @return URL to the file.
    */
   public String getUrl()
   {
      return _url;
   }


   /**
    * Set the size of this file.
    * @param size
    */
   public void setSize(long size)
   {
      _size = size;
   }


   /**
    * Get the file size.
    * This is the value set by setSize(); it is not computed.
    * @return
    */
   public long getSize()
   {
      return _size;
   }


   /**
    * Set the name of the ".exe" file.
    * @param exeName
    */
   public void setExeName(String exeName)
   {
      if (exeName == null) {
         throw new IllegalArgumentException();
      }

      _exeName = exeName;
   }


   /**
    * Get the name of the ".exe" file.
    * @return
    */
   public String getExeName()
   {
      return _exeName;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      BuildFile other = (BuildFile) record;
      int numChanges = 0;

      if (!StringUtils.equals(getUrl(), other.getUrl())) {
         setUrl(other.getUrl());
         numChanges++;
      }

      if (getSize() != other.getSize()) {
         setSize(other.getSize());
         numChanges++;
      }

      if (!StringUtils.equals(getExeName(), other.getExeName())) {
         setExeName(other.getExeName());
         numChanges++;
      }

      return numChanges;
   }
}
