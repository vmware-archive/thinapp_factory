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

package com.vmware.appfactory.file;

import java.io.InputStream;

/**
 * This class represents a binary file's data and HTTP headers.
 */
public class FileData
{
   private final String contentDisposition;
   private final String contentType;
   private final int contentLength;
   private final InputStream is;

   /**
    * Create a new FileData instance.
    */
   public FileData(
         String contentDisposition,
         String contentType,
         int contentLength,
         InputStream is)
   {
      this.contentDisposition = contentDisposition;
      this.contentType = contentType;
      this.contentLength = contentLength;
      this.is = is;
   }


   public String getContentDisposition()
   {
      return contentDisposition;
   }


   public int getContentLength()
   {
      return contentLength;
   }


   public InputStream getInputStream()
   {
      return is;
   }


   public String getContentType()
   {
      return contentType;
   }
}
