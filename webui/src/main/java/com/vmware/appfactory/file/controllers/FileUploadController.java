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

package com.vmware.appfactory.file.controllers;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.ProgressListener;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.file.upload.ProgressReporter;

/**
 * This controller provides updates on the upload progress of any file.
 *
 * The status of the upload is monitored in the users session under the session
 * variable "pListener_${ProgressReporter.UPLOAD_REQUEST_ID}"
 *
 * @see ProgressReporter
 */
@Controller
public class FileUploadController extends AbstractApiController
{
   /**
    * This method provides with the status of an ongoing upload.
    *
    * @param uploadId - Unique Id represents the upload.
    * @param request
    * @return ProgressListenerImpl contains progress.
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(method = RequestMethod.GET,
                   value = "/upload/{uploadId}/progress")
   public ProgressListener fetchUploadProgress(
         @PathVariable String uploadId,
         HttpServletRequest request)
      throws AfBadRequestException
   {
      if (StringUtils.hasLength(uploadId)) {
         ProgressListener pl = ProgressReporter.getProgressListener(request, uploadId);
         return pl;
      }

      /*
       * This can happen if the uploadId was a bogus, or if the client is
       * requesting status on something that has already finished since the
       * last time they requested.
       */
      throw new AfBadRequestException("Invalid upload Id: " + uploadId);
   }
}