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

package com.vmware.appfactory.application.model;

import java.net.URISyntaxException;
import java.net.URL;

import javax.persistence.Entity;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.appfactory.common.InputFile;

/**
 * This class describes the download instructions for an application
 * that appears in an AppFactory feed. This is just a specific use
 * case of a generic feed input file.
 *
 * @see InputFile
 */
@Entity
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class AppDownload
   extends InputFile
{
   /**
    * Create an application download from a URL.
    * @param url - URL to the download file
    * @return
    * @throws URISyntaxException
    */
   public static AppDownload createFromURL(URL url)
      throws URISyntaxException
   {
      AppDownload file = new AppDownload();
      file.setURI(url.toURI());
      return file;
   }
}
