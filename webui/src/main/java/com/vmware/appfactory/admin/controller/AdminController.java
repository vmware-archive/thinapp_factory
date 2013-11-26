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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistryConstants;



/**
 * This controller handles all the UI requests for the administration
 * pages. Note that some requests for getting and updating administration
 * parameters are handled by the AdminApiController.
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class AdminController
   extends AbstractUiController
{
   /**
    * Open the EULA page.
    * XXX Need to pick one based on locale.
    *
    * @param request
    * @return
    */
   @RequestMapping(
         value="/admin/eula",
         method=RequestMethod.GET)
   public ModelAndView eula(HttpServletRequest request)
   {
      ModelMap mm = getBaseModel(request);
      mm.addAttribute("eulaAccepted", _config.getBool(ConfigRegistryConstants.GEN_EULA_ACCEPTED));
      return new ModelAndView("admin/admin-eula", mm);
   }

   /**
    * Get a view of all the administration options.
    *
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(
         value={"/admin/index","/admin"},
         method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.ADMIN");
      return new ModelAndView("admin/admin-index",mm);
   }


   /**
    * Open the view showing the "About" screen.
    * @param request
    * @param locale
    * @return Model and view for the 'about' page.
    */
   @RequestMapping(
         value="/admin/about",
         method=RequestMethod.GET)
   public ModelAndView about(
         HttpServletRequest request,
         Locale locale)
   {
      ModelMap mm = getBaseModel(request, locale, "T.ABOUT_TAF");

      /* Get the host IP address, not the IP address that the request carries.
       * At times it could use 127.0.0.1 especially when using a web server.
       */
      mm.addAllAttributes(getServerIPAndHost(request));

      return new ModelAndView("admin/admin-about", mm);
   }


   /**
    * Open the view showing the "Help" screen.
    * @param locale
    * @return Model and view for the 'help' page.
    */
   @RequestMapping(
         value="/admin/help",
         method=RequestMethod.GET)
   public String help(Locale locale)
   {
      String lang = locale.getLanguage();

      /* Non-English languages that we have help files for: */
      if (lang.equals(Locale.FRENCH.getLanguage())) {
         return "redirect:/static/help/help_fr.html";
      }

      /* Everything else, including English */
      return "redirect:/static/help/help.html";
   }


   /**
    * Open the view showing all the settings pages.
    *
    * cloneSupported flag is needed for the workpool tab.
    *
    * @param request
    * @param locale
    * @return Model and view for the 'help' page.
    */
   @RequestMapping(
         value={"/admin/settings", "/newui/settings", "/settings"},
         method=RequestMethod.GET)
   public ModelAndView settings(
         HttpServletRequest request,
         @RequestParam(required=false) String subtab,
         Locale locale)
      throws AfServerErrorException
   {
      ModelMap mm = getBaseModel(request, locale, "T.SETTINGS");
      if (!_config.isNewUI()) {
         mm.addAttribute("cloneSupported", _viClient.checkCloneSupport(true));
      }
      mm.addAttribute("subtab",subtab);
      return new ModelAndView("settings/settings-index", mm);
   }


   /**
    * Get the IP address and the host name of the system running this webUI.
    *
    * The IP may not be the same if webui runs behind a web server,
    * which would likely use localhost.
    *
    * @param request
    * @return Map<String,String> IP address & hostname as values
    */
   private Map<String,String> getServerIPAndHost(HttpServletRequest request) {

      Map<String,String> hostAddressModel = new HashMap<String,String>();
      try {
         // Get IP Address
         InetAddress addr = InetAddress.getLocalHost();
         hostAddressModel.put("serverAddressNotLocal", addr.getHostAddress());
         hostAddressModel.put("serverNameNotLocal", addr.getCanonicalHostName());

      } catch (UnknownHostException e) {
        //  issue getting to host, default to request values.
         hostAddressModel.put("serverAddressNotLocal", request.getLocalAddr());
         hostAddressModel.put("serverNameNotLocal", request.getLocalName());
      }
      return hostAddressModel;
   }

}
