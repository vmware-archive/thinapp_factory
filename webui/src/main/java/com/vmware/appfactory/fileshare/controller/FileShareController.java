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

package com.vmware.appfactory.fileshare.controller;

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
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.model.FileShare;


/**
 * The controller for handling File Share's Add, Edit, Delete and Reset.
 */
@Controller
@RequestMapping(value={"/fileshare","/fileshares"})
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class FileShareController extends AbstractUiController {

   /**
    * Show the HTML page containing the list of file shares.
    * Note the list is initially empty: it will use Ajax to populate it
    * dynamically, so we don't need to fetch data for it here.
    *
    * @param request
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value={"/index",""}, method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.FILE_SHARES");
      return new ModelAndView("fileshare/fileshare-index", mm);
   }


   /**
    * Show the HTML for creating a new file share.
    * We return the same view (fileshare-edit) that is used for editing, but
    * we don't put anything into the model.
    *
    * @param request
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value="/create",method=RequestMethod.GET)
   public ModelAndView create(
         @RequestParam(required=false) boolean dialog,
         HttpServletRequest request,
         Locale locale) throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.FILE_SHARES");
      String view = "fileshare/fileshare-edit";
      if (dialog) {
         mm.put("isDialog",true);
         view = "fileshare/fileshare-form";
      }
      return new ModelAndView(view, mm);
   }


   /**
    * Edit an exiting file share.
    *
    * @param id
    * @param request
    * @param locale
    * @return
    * @throws AfNotFoundException
    */
   @RequestMapping(value="/edit/{id}", method=RequestMethod.GET)
   public ModelAndView edit(
         @PathVariable Long id,
         HttpServletRequest request,
         Locale locale)
      throws AfNotFoundException
   {
      FileShareDao fileShareDao = _daoFactory.getFileShareDao();
      FileShare fileshare = fileShareDao.find(id);

      if (fileshare == null) {
         throw new AfNotFoundException("File share " + id + " not found");
      }

      ModelMap mm = getBaseModel(request, locale, "T.FILE_SHARES");
      mm.put("fileShareId", fileshare.getId());

      return new ModelAndView("fileshare/fileshare-edit", mm);
   }
}
