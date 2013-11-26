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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vmware.thinapp.manualmode.ThreadLocalFileAppender;
import com.vmware.thinapp.manualmode.ThreadLocalProperties;

/**
 * A Spring singleton bean to support concurrent download via a background
 * thread pool from executor service.
 *
 * @author saung
 * @since M7 - Jul 7, 2011
 */
@Service
public class HttpDownloaderService {
   /** get the logger */
   private static final Logger log = LoggerFactory
         .getLogger(HttpDownloaderService.class);

   @Autowired
   private ExecutorService executor;

   private class DownloadTask implements Callable<DownloadResult> {
      private final String url;
      private final String downloadDir;
      private final String defaultFilename;
      private final String logFile;

      public DownloadTask(String url, String downloadDir, String defaultFilename) {
         super();
         this.url = url;
         this.downloadDir = downloadDir;
         this.defaultFilename = defaultFilename;
         this.logFile = ThreadLocalProperties.getProperty(
                        ThreadLocalProperties.PER_PROJECT_LOGFILE);
      }

      @Override
      public DownloadResult call() throws Exception {
         try {
            ThreadLocalFileAppender.set(logFile);
            log.debug("Attempting to download from url {}", url);
            return new DownloadResult(
                  url, downloadDir,
                  HttpDownloader.downloadToFile(url, downloadDir, defaultFilename));
         } catch (Throwable t) {
            /**
             * Eat the exception because it needs to return the status
             * regardless of the result.
             */
            log.error("Failed to download " + url + " into " + downloadDir, t);
            return new DownloadResult(url, downloadDir, t);
         } finally {
            ThreadLocalFileAppender.remove();
         }
      }
   }

   /**
    * Async download to a given file.
    *
    * @param url
    *           - a url to download.
    * @param downloadDir
    *           - a download dir.
    * @param defaultFilename
    *           - a default filename to use if no meta-data found in the
    *           response header or url.
    * @return a boolean download status
    */
   public Future<DownloadResult> asyncDownload(String url, String downloadDir,
         String defaultFilename) {
      return executor
            .submit(new DownloadTask(url, downloadDir, defaultFilename));
   }

   /**
    * Graceful orderly shutdown to kill all running threads.
    */
   @PreDestroy
   public void destroy() {
      executor.shutdown();
   }
}
