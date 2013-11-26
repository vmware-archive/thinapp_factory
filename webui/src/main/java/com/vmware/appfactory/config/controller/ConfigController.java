
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

package com.vmware.appfactory.config.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.config.dto.HorizonConfig;

/**
 * This controller handles all the UI requests for the configuration
 * pages. Note that some requests for getting and updating configuration
 * parameters are handled by the ConfigApiController.
 */
@Controller
@RequestMapping(value="/config")
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class ConfigController
   extends AbstractUiController
{
   /**
    * Get a view of all the configuration parameters.
    *
    * @param request
    * @param locale
    * @return
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value={"","/index"},
         method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         Locale locale)
      throws AfNotFoundException
   {
      ModelMap mm = getBaseModel(request, locale, "T.CONFIG");
      return new ModelAndView("config/config-index", mm);
   }

   @RequestMapping(value="/horizon", method=RequestMethod.GET)
   public ModelAndView configHorizon(HttpServletRequest request, Locale locale) {
      ModelMap mm = getBaseModel(request, locale, "T.CONFIG");
      // Load the horizon configuration.
      HorizonConfig config = new HorizonConfig(
         _config.getString(ConfigRegistryConstants.HORIZON_URL),
         _config.getString(ConfigRegistryConstants.HORIZON_IDP_ACTIVATION_TOKEN),
         _config.getBool(ConfigRegistryConstants.HORIZON_IGNORE_CERT_WARN));

      mm.put("horizonConfig",config);
      return new ModelAndView("config/config-horizon-form", mm);
   }
}