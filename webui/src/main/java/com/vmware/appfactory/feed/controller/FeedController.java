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

package com.vmware.appfactory.feed.controller;

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
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.model.Feed;


/**
 * Handles all requests related to viewing, adding, and editing
 * application feeds.
 */
@Controller
@RequestMapping(value="/feeds")
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class FeedController
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
   @RequestMapping(value={"","/index"}, method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.FEEDS");
      return new ModelAndView("feeds/feeds-index", mm);
   }


   /**
    * Show the HTML for creating a new feed.
    * We return the same view (feed-edit) that is used for editing, but
    * we don't put anything into the model.
    *
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(value="/create", method=RequestMethod.GET)
   public ModelAndView create(
         HttpServletRequest request,
         @RequestParam(required=false) boolean dialog,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.FEEDS");
      String view = "feeds/feed-edit";
      if (dialog) {
         mm.put("isDialog",true);
         view = "feeds/feed-form";
      }
      return new ModelAndView(view, mm);
   }


   /**
    * Open the feed editor for the specified feed.
    *
    * @param id ID of feed to edit.
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
      FeedDao feedDao = _daoFactory.getFeedDao();
      Feed feed = feedDao.find(id);

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed id " + id);
      }

      ModelMap mm = getBaseModel(request, locale, "T.FEEDS");
      mm.put("feedId", feed.getId());

      return new ModelAndView("feeds/feed-edit", mm);
   }
}