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

package com.vmware.appfactory.dashboard.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.vmware.appfactory.common.base.SelectedTab;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.config.ConfigRegistryConstants;


/**
 * Handles all the MVC dashboard requests.
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Dashboard)
public class DashboardController
   extends AbstractUiController
{
   /**
    * Return the dashboard.
    *
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(
         value={"/dashboard","/dashboard/index","/newui","/newui/dashboard"},
         method=RequestMethod.GET)
   public ModelAndView dashboard(
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.DASHBOARD");
      mm.put("showAlert", _config.getBool(ConfigRegistryConstants.WORKPOOL_SHOW_SETUP_ALERT));

      return new ModelAndView("dashboard/dashboard-index", mm);
   }
}