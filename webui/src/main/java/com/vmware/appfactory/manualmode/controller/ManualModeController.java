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

package com.vmware.appfactory.manualmode.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.base.AbstractUiController;

/**
 * This controller serves all manual mode related UI views.
 *
 * @author saung
 * @since v1.0 5/10/2011
 */
@Controller
@RequestMapping(value="/manualMode")
public class ManualModeController extends AbstractUiController {


   /**
    * Get Manual mode index Velocity template.
    *
    * @param appId - an applicatoin id.
    * @param ticketId - a Manual mode ticket id.
    * @param request - a Httpservlet request.
    * @return a model map and view.
    * @throws Exception if any error raised while loading manualmode.vm.
    */
   @RequestMapping(value="/index", method=RequestMethod.GET)
   public ModelAndView index(
         @RequestParam(required=false) Long appId,
         @RequestParam(required=false) Long ticketId,
         HttpServletRequest request)
      throws Exception
   {
      _log.debug("appId=" + appId + ":ticketId=" + ticketId);
      ModelMap mm = getBaseModel(request);
      mm.put("appId", appId);
      mm.put("ticketId", ticketId);

      if (appId != null) {
         ApplicationDao appDao = _daoFactory.getApplicationDao();
         final Application app = appDao.find(appId);
         if (app == null) {
            _log.error("app is null for appId {}", appId);
         } else {
            mm.put("appName", app.getDisplayName());
         }
      } else {
         _log.warn("Invalid app id {}", appId);
      }


      return new ModelAndView("apps/manualmode", mm);
   }

}
