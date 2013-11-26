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

package com.vmware.appfactory.datastore.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.vmware.appfactory.common.base.SelectedTab;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * Handles all MVC requests for datastores/storage pages.
 */
@Controller
@RequestMapping(value="/datastores")
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class DatastoreController
   extends AbstractUiController
{
   /**
    * Show the HTML page containing the list of datastores.
    * Note the list is initially empty: it will use Ajax to populate it
    * dynamically, so we don't need to fetch data for it here.
    *
    * @param request
    * @param status
    * @param locale
    * @return
    */
   @RequestMapping(
         value={"/index",""},
         method=RequestMethod.GET)
   public ModelAndView index(
         @RequestParam(required=false) Datastore.Status status,
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.STORAGE");
      mm.put("status", status);
      return new ModelAndView("datastores/datastores-index", mm);
   }


   /**
    * Show the HTML for creating a new datastore.
    * We return the same view (datastore-edit) that is used for editing, but
    * we don't put anything into the model.
    *
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(value="/create", method=RequestMethod.GET)
   public ModelAndView create(
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.STORAGE");
      return new ModelAndView("datastores/datastore-edit", mm);
   }


   /**
    * Open the page for editing a datastore.
    *
    * @param dsId
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(value="/edit/{dsId}", method=RequestMethod.GET)
   public ModelAndView edit(
         @PathVariable Long dsId,
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.STORAGE");
      mm.put("datastoreId", dsId);
      return new ModelAndView("datastores/datastore-edit", mm);
   }
}