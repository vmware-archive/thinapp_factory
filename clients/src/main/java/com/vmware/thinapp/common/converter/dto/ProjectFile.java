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

package com.vmware.thinapp.common.converter.dto;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This object is created when a job completes successfully.
 * A successful job generates a Project, and a project contains one or
 * more ProjectFile objects.
 */
public class ProjectFile {

   private String url;
   private String filename;
   private long size;

   /**
    * Default constructor
    */
   public ProjectFile() {
      /* Empty */
   }

   /**
    * Constructor to set the filename and url
    * @param filename
    * @param url
    */
   public ProjectFile(String filename, String url) {
      this.filename = filename;
      this.url = url;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public String getFilename() {
      return filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public long getSize() {
      return size;
   }

   public void setSize(long size) {
      this.size = size;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this,
            ToStringStyle.MULTI_LINE_STYLE);
   }
}

