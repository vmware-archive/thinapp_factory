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

package com.vmware.appfactory.file.upload;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * Overwrite CommonsMultipartResolver to provide progress tracking. The
 * progress can be tracked by ProgressReporter utility.
 *
 * @see ProgressListenerImpl
 * @see ProgressReporter
 * @see CommonsMultipartResolver
 *
 * @author Keerthi Singri
 */
public class CustomMultipartResolverWithProgress extends CommonsMultipartResolver
{
   private static ThreadLocal<ProgressListener> progressListenerLocal =
      new ThreadLocal<ProgressListener>();

   private static final Logger _log = LoggerFactory.getLogger(CustomMultipartResolverWithProgress.class);

   @Override
   public void cleanupMultipart(MultipartHttpServletRequest request) {
      // Set upload finished flag on ProgressListenerImpl.
      ProgressListener pListener = progressListenerLocal.get();
      if (pListener instanceof ProgressListenerImpl) {
         ((ProgressListenerImpl)pListener).setFinished();
      }
      super.cleanupMultipart(request);
   }

   /**
    * Set ProgressListener to the file Upload so progress gets updated.
    */
   @Override
   protected FileUpload newFileUpload(FileItemFactory fileItemFactory)
   {
      FileUpload fileUpload = super.newFileUpload(fileItemFactory);
      fileUpload.setProgressListener(progressListenerLocal.get());
      _log.debug("Assigning progressListener to fileUpload.");
      return fileUpload;
   }

   /**
    * Create a threadlocal ProgressListener and initialize tracking capability
    * by adding it to HttpSession via ProgressReporter
    */
   @Override
   public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
   throws MultipartException {
      ProgressListener pListener = new ProgressListenerImpl();

      // Associate the progressListener
      progressListenerLocal.set(pListener);

      // Let the ProgressReporter keep track of this ProgressListener
      ProgressReporter.addProgressListener(request, pListener);
      _log.trace("Create new progressListener and associate to user session.");

      return super.resolveMultipart(request);
   }

   /*
   @Override
   // NOTE: This method can also be used to inject progress listener to FileUpload
   // or debug while the file upload is being prepared.
   protected FileUpload prepareFileUpload(String encoding) {
       FileUpload fileUpload = super.prepareFileUpload(encoding);

       // This is called much later after resolveMultipart() is invoked.
       ProgressListener progressListener = progressListenerLocal.get();
       fileUpload.setProgressListener(progressListener);
       _log.info("\n Assigning progressListener to fileUpload. (Another way to assign progressListener)");
       return fileUpload;
   }*/
}
