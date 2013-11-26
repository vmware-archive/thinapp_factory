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

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This is a utility class that provides utility methods to manage upload progress of a file upload.
 * This is achieved by setting a unique Id into the init method, which in turn will be used to associate
 * is stored in HttpSession.
 *
 * This class provides methods to initialize, add, remove and fetch ProgressListener associated with the
 * CustomMultipartResolverWithProgress for tracking upload progress.
 *
 * @see ProgressListener
 * @see CustomMultipartResolverWithProgress
 * @author Keerthi Singri
 */
public class ProgressReporter {
   private static final Logger _log = LoggerFactory.getLogger(ProgressReporter.class);

   // The key whose value will be used for storing the ProgressListener in the users session.
   private static final String UPLOAD_REQUEST_ID = "TAF_uploadId";

   // An id prefix against which ProgressListener is stored.
   private static final String PROGRESS_KEY_PREFIX = "TAF_pListener_";

   /**
    * This method initializes the progress reporting by creating a new uploadId and storing it in the
    * user session for use when a file upload happens.
    *
    * @param request
    * @param uploadId
    */
   public static void initProgressListener(HttpServletRequest request, String uploadId) {
      if (StringUtils.isEmpty(uploadId)) {
         // If no key, we will not initiate any upload tracking session variable.
         _log.warn("No uploadKey passed, ignore this request.");
         return;
      }
      request.getSession().setAttribute(UPLOAD_REQUEST_ID, uploadId);
      _log.debug("added uploadKey: {} into session", uploadId);
   }

   /**
    * This method adds the ProgressListener to the session based on the keyPrefix as available.
    * This method can be invoked only by CustomMultipartResolverWithProgress and hence the limited visibility.
    *
    * @param request
    * @param progressListener
    */
   static void addProgressListener(HttpServletRequest request, ProgressListener progressListener) {
      String uploadId = (String)request.getSession().getAttribute(UPLOAD_REQUEST_ID);
      if (StringUtils.isEmpty(uploadId)) {
         // If no key, we cant store the progressListener in session and hence no upload progress.
         _log.warn("No uploadId available, hence we do not track the ProgressListener");
         return;
      }

      String sessionKey = createSessionKey(uploadId);

      // Add the progressListener onto the session for progress reporting.
      request.getSession().setAttribute(sessionKey, progressListener);

      // Remove this session variable(if exists), as its not needed anymore.
      request.getSession().removeAttribute(UPLOAD_REQUEST_ID);
   }

   /**
    * Get the progress of the upload in the form of a clone copy of ProgressListenerImpl.
    *
    * @param request - HttpServletRequest
    * @param uploadId - a key representing the progress copy in session.
    * @return implementation of ProgressListener
    */
   public static ProgressListener getProgressListener(HttpServletRequest request, String uploadId) {
      if (uploadId != null) {
         ProgressListener pListener = (ProgressListener)request.getSession()
               .getAttribute(createSessionKey(uploadId));

         // Return a clone copy of the listener.
         if (pListener instanceof ProgressListenerImpl) {
            return ((ProgressListenerImpl) pListener).clone();
         }
      }

      /* The upload process may have already finished and AppApiController.uploadAndCreate()
       * may have removed ProgressListener instance from session already. Hence return null.
       */
      return null;
   }

   /**
    * Removes the ProgressListener from the HttpSession associated with the input parameter uploadId.
    *
    * @param request
    * @param uploadId
    */
   public static void removeProgressListener(HttpServletRequest request, String uploadId) {
      if (uploadId != null) {
         String sessionKey = createSessionKey(uploadId);
         request.getSession().removeAttribute(sessionKey);
         _log.trace("Remove session attr with key: " + sessionKey);
      }
   }

   /**
    * Concatenates the unique key with prefix to make the key used to store the ProgressListener.
    *
    * @param uploadId
    * @return
    */
   private static String createSessionKey(String uploadId) {
      return PROGRESS_KEY_PREFIX + uploadId;
   }
}
