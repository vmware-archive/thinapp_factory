
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

package com.vmware.appfactory.admin.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractUiController;


/**
 * This controller handles all the UI requests for the administration
 * pages. Note that some requests for getting and updating administration
 * parameters are handled by the AdminApiController.
 */
@Controller
@RequestMapping(value="/log")
public class LogController
   extends AbstractUiController
{
   /**
    * Log all POSTed log messages.
    *
    * @param request
    */
   @ResponseBody
   @RequestMapping(
         value = "/index",
         method = RequestMethod.POST)
   public void index(
         HttpServletRequest request)
   {
      String message = request.getParameter("message");
      String level = request.getParameter("level");
      _log.debug(level + ": " + message);
   }
}
