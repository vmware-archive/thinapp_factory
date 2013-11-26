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

package com.vmware.thinapp.manualmode.util;

public class DownloadResult {

   private final boolean result;
   private final String url;
   private final String downloadDir;
   private final String filename;
   private final String message;

   public DownloadResult(String url, String downloadDir, String filename) {
      this.result = true;
      this.url = url;
      this.downloadDir = downloadDir;
      this.filename = filename;
      this.message = null;
   }

   public DownloadResult(String url, String downloadDir, Throwable t) {
      this.result = false;
      this.url = url;
      this.downloadDir = downloadDir;
      this.filename = null;
      String tName = t.getClass().getName();
      this.message = tName.substring(tName.lastIndexOf(".") + 1) +
                     " - " + t.getMessage();
   }

   public boolean getResult() {
      return result;
   }

   public String getUrl() {
      return url;
   }

   public String getDownloadDir() {
      return downloadDir;
   }

   public String getFilename() {
      return filename;
   }

   public String getMessage() {
      return message;
   }
}
