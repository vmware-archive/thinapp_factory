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

package com.vmware.appfactory.auth.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractNewUiController;

/**
 * This controller handles all login, logout and session timeout requests.
 */
@Controller
@RequestMapping("/auth")
public class AuthController extends AbstractNewUiController {

   @RequestMapping(value = "/login", method = RequestMethod.GET)
   public ModelAndView login(
         HttpServletRequest request, Locale locale) {
      ModelMap mm = getBaseModel(request, locale, "T.LOGIN");
      return new ModelAndView("admin/login", mm);
   }
}
