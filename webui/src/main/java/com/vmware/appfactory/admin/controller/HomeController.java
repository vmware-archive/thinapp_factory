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

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vmware.appfactory.common.base.AbstractUiController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * A Spring MVC controller for top-level or home page views only.
 */
@Controller
public class HomeController
   extends AbstractUiController
{
   /**
    * Return the AppFactory home page.
    * @param request    Http servlet request
    * @return           Main page of legacy UI
    */
   @RequestMapping("/")
   public ModelAndView home(HttpServletRequest request)
   {
      ModelMap mm = getBaseModel(request);
      return new ModelAndView("home/home", mm);
   }
}
