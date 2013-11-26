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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * An utility class to download contents from a HTTP server and stream to a
 * file. It uses Spring RestTemplate to invoke HTTP GET method.
 *
 * @author saung
 * @since M7 - Jul 6, 2011
 */
public class HttpDownloader {
   /** A RestTemplate instance */
   private static final RestTemplate REST = new RestTemplate();

   /** OS-specific file separator */
   private static final String FILE_SEPARATOR = System
         .getProperty("file.separator");

   /**
    * Download url and save it into a file.
    *
    * @param urlStr a url to download.
    * @param downloadDir a directory to save the download file.
    * @param defaultFilename a default filename to use unless it can extract
    *                        from the response header or the url.
    * @return the filename of the file that was downloaded
    */
   public static String downloadToFile(final String urlStr,
         final String downloadDir, final String defaultFilename) {
      final URI url = AfUtil.toURI(urlStr);

      final RequestCallback request = new RequestCallback() {
         @Override
         public void doWithRequest(ClientHttpRequest request)
               throws IOException {
            /** do nothing */
         }
      };

      final ResponseExtractor<String> response = new ResponseExtractor<String>() {
         @Override
         public String extractData(ClientHttpResponse response)
               throws IOException {
            String filename =
               AfUtil.getFilenameFromHttpResponse(response, url, defaultFilename);
            try {
               streamToFile(downloadDir, response, filename);
            } catch (IOException ex) {
               throw new ConverterException(String.format("Failed to download %s.", url));
            }
            return filename;
         }
      };

      // TODO: This isn't the best way to handle file downloads, but it works for now.
      return REST.execute(url, HttpMethod.GET, request, response);
   }

   /**
    * Stream content from http response to a file.  This method requires the
    * downloadDir to have been previously created.
    *
    * @param downloadDir a download dir.
    * @param response a ClientHttpResponse.
    * @throws IOException if any error raised while saving into a file.
    * @throws FileNotFoundException if the given file not found.
    */
   private static final void streamToFile(
         final String downloadDir,
         ClientHttpResponse response,
         String filename) throws IOException {
      // Make sure the given download directory actually exists
      final File dir = new File(downloadDir);
      if (!dir.exists()) {
         throw new IllegalArgumentException(
               String.format("The given download directory %s does not exist",
                     downloadDir));
      }

      // Get binary from the http response body
      final InputStream in = response.getBody();

      String filePath = null;
      if (StringUtils.endsWithIgnoreCase(downloadDir, FILE_SEPARATOR)) {
         filePath = downloadDir + filename;
      } else {
         filePath = downloadDir + FILE_SEPARATOR + filename;
      }

      final File tofile = new File(filePath);
      final FileOutputStream out = new FileOutputStream(tofile);
      if (FileCopyUtils.copy(in, out) == 0) {
         throw new IOException("Given HTTP response is empty.");
      }
   }
}
