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

package com.vmware.appfactory.workpool.controller;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * Handles all MVC requests for workpool pages.
 *
 * NOTE: cloneSupported flag is needed to create a WorkpoolManager.
 */
@Controller
@RequestMapping(value="/workpools")
@SelectedTab(value= SelectedTab.Tabs.Settings)
public class WorkpoolController
   extends AbstractUiController
{
   /**
    * Show the HTML page containing the list of workpools.
    * Note the list is initially empty: it will use Ajax to populate it
    * dynamically, so we don't need to fetch data for it here.
    *
    * @param request
    * @param locale
    * @throws AfServerErrorException if cloneSupport cannot be figured.
    * @throws WpException
    * @return
    */
   @RequestMapping(
         value={"","/index"},
         method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         @RequestParam(required=false) String subtab,
         Locale locale)
      throws AfServerErrorException, WpException
   {
      ModelMap mm = getBaseModel(request, locale, "T.WORKPOOL");
      mm.addAttribute("cloneSupported", isCloneSupported());
      mm.addAttribute("subtab",subtab);
      return new ModelAndView("workpool/workpool-index", mm);
   }


   /**
    * Show the HTML for creating a new workpool.
    * We return the same view (workpool-edit) that is used for editing, but
    * we don't put anything into the model to indicate we are creating a
    * new workpool.
    *
    * @param request
    * @param locale
    * @throws AfServerErrorException if cloneSupport cannot be figured.
    * @throws WpException
    * @return
    */
   @RequestMapping(value="/create", method=RequestMethod.GET)
   public ModelAndView create(
         HttpServletRequest request,
         Locale locale)
   throws AfServerErrorException, WpException
   {
      boolean isWS = _viClient.isVITypeWorkstation(true);
      ModelMap mm = getBaseModel(request, locale, "T.WORKPOOL");
      mm.addAttribute("allVMImages", loadAllVmImages());
      mm.addAttribute("cloneSupported", isCloneSupported());
      mm.addAttribute("isWS", isWS);
      mm.addAttribute("yuiSupport", true);
      mm.addAttribute("defaultMaxInstance", isWS ?
                                            1 : _config.getInteger(ConfigRegistryConstants.WORKPOOL_DEFAULT_MAX_INSTANCE));
      mm.addAttribute("yuiSupport", true);
      return new ModelAndView("workpool/workpool-edit", mm);
   }


   /**
    * Call to check if clone is supported.
    *
    * @return whether the VI supports clone or not.
    * @throws AfServerErrorException - if cloneSupport cannot be figured.
    * @throws WpException
    */
   private boolean isCloneSupported()
      throws AfServerErrorException, WpException
   {
      return _viClient.checkCloneSupport(true);
   }


   /**
    * Call to check if vi type is workstation.
    *
    * @return
    * @throws AfServerErrorException - if vi type cannot be figured.
    * @throws WpException
    */
   private boolean isWS()
      throws AfServerErrorException, WpException
   {
      return _viClient.isVITypeWorkstation(true);
   }


   /**
    * Open the page for editing a workpool.
    *
    * @param workpoolId
    * @param request
    * @param locale
    * @throws AfServerErrorException if cloneSupport cannot be figured.
    * @throws WpException
    * @return
    */
   @RequestMapping(value="/edit/{workpoolId}", method=RequestMethod.GET)
   public ModelAndView edit(
         @PathVariable Long workpoolId,
         HttpServletRequest request,
         Locale locale)
   throws AfServerErrorException, WpException
   {
      ModelMap mm = getBaseModel(request, locale, "T.WORKPOOL");
      mm.put("workpoolId", workpoolId);
      mm.addAttribute("cloneSupported", isCloneSupported());
      mm.addAttribute("isWS", isWS());
      // Set to 1, it will be replaced by the workpool's max instance value.
      mm.addAttribute("defaultMaxInstance", 1);
      mm.addAttribute("yuiSupport", true);
      return new ModelAndView("workpool/workpool-edit", mm);
   }


   /**
    * Before, this would show an HTML page containing the list of vmimages.
    *
    * Now that they're both displayed on the same page, just redirect
    * to the appropriate settings page.
    *
    * @return a redirect to the page that currently shows the list of VM images.
    */
   @RequestMapping(
         value="/image",
         method=RequestMethod.GET)
   public String listImage()
   {
      return "redirect:/settings?subtab=image#0";
   }


   /**
    * Helper method to load the list of VmImages
    *
    * @return List<VmImage>
    * @throws AfServerErrorException
    */
   private List<VmImage> loadAllVmImages() throws AfServerErrorException
   {
      try {
         List<VmImage> imageList = _wpClient.getAllImages();
         return CollectionUtils.isEmpty(imageList)?
               null : imageList;
      }
      catch (WpException e) {
         throw new AfServerErrorException(e);
      }
   }

}