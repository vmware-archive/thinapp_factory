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

package com.vmware.appfactory.datasource.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;


/**
 * Handles all requests related to viewing, adding, and editing
 * application feeds.
 */
@Controller
@RequestMapping(value="/sources")
public class DataSourceController
   extends AbstractUiController
{
   /**
    * Show the HTML page containing the list of feeds.
    * Note the list is initially empty: it will use Ajax to populate it
    * dynamically, so we don't need to fetch data for it here.
    * @param request
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value="/index", method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.DATA_SOURCES");
      return new ModelAndView("sources/sources-index", mm);
   }
}