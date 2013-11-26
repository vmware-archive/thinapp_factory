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

package com.vmware.appfactory.build.controller;

import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.build.service.BuildService;
import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.dto.SelectOptions;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.recipe.RecipeMatch;


/**
 * Handles all requests for build-related views. This includes looking at the
 * builds themselves, and also forms for requesting new builds, editing
 * builds and build settings, etc.
 *
 * @author levans
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Projects)
public class BuildController extends AbstractUiController {
   @Resource
   BuildService _buildService;

   private static final String HZ_ENABLED = "horizonEnabled";
   private enum BuildView {
      byapp,      // Grouped by app - existing
      appBuilds,  // Builds per app selected.
      allBuilds   // All builds available.
   }

   /**
    * Get a list of all builds.
    *
    * Note that this only returns an empty view, with an optional model
    * parameter defining a filter. The actual contents are fetched on page
    * load using an Ajax call.
    *
    * NOTE: checkDatastore for published state is used by AppStore.
    *
    * @param request
    * @param status
    * @param view
    * @param locale
    * @return ModelAndView for all builds, or builds grouped by application
    * @throws Exception
    */
   @RequestMapping(value={"/builds","/builds/index","/newui/projects","/projects"},
         method=RequestMethod.GET)
   public ModelAndView index(
         @RequestParam(required=false) Build.Status status,
         @RequestParam(required=false) String view,
         HttpServletRequest request,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.COMMON.TAB_PROJECTS");
      // Default view and builds page.
      String viewName = "builds/builds-index";
      mm.put("buildView", BuildView.allBuilds.name());

      // If VIEW_BY_APP, use the associated view and set buildView param.
      if (BuildView.byapp.name().equals(view)) {
         viewName = "/builds/builds-by-app";
         mm.put(PAGE_TITLE_KEY, tr(locale, "T.BUILDS_BY_APP"));
         mm.put("buildView", BuildView.byapp.name());
      }

      if (status != null) {
         mm.put("status", status.name());
         String statusT = tr(locale, "T.BUILDS.STATUS." + status.name());
         mm.put(PAGE_TITLE_KEY, fr(locale, "T.BUILDS.FOR_STATUS", statusT));
      }

      return new ModelAndView(viewName, mm);
   }

   /**
    * Get the view that shows all builds that are "like" the specified
    * build (i.e. builds for the same application). This includes the
    * specified build.
    *
    * @param request
    * @param buildId
    * @param locale
    * @return ModelAndView for all builds like the specified build.
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/builds/like/{buildId}",
         method=RequestMethod.GET)
   public ModelAndView buildsForApp(
         @PathVariable Long buildId,
         HttpServletRequest request,
         Locale locale) throws AfNotFoundException
   {
      Build build = _daoFactory.getBuildDao().find(buildId);
      if (build == null) {
         throw new AfNotFoundException("No such build " + buildId);
      }

      ModelMap mm = getBaseModel(request);
      mm.put(PAGE_TITLE_KEY, fr(locale, "T.BUILDS.FOR_APP", build.getDisplayName()));

      // Set the appropriate build view.
      mm.put("buildView", BuildView.appBuilds.name());
      mm.put("build", build);
      return new ModelAndView("/builds/builds-all", mm);
   }


   /**
    * Open the view that lets a user edit CWS project settings for
    * a given build, starting with "package.ini" data.
    *
    * @param request
    * @param buildId
    * @param locale
    * @return
    * @throws AfNotFoundException
    *
    */
   @RequestMapping(
         value="/builds/edit/{buildId}",
         method=RequestMethod.GET)
   public ModelAndView editBuildSettings(
         @PathVariable Long buildId,
         HttpServletRequest request,
         Locale locale)
           throws AfNotFoundException {
      return editBuildSettings2(buildId, "packageini", request, locale);
   }


   /**
    * Open the view that lets a user edit CWS project settings for
    * a given build, starting with the specified section (one of
    * "packageini", "registry" or "filesystem").
    *
    * @param request
    * @param buildId
    * @param what
    * @param locale
    * @return ModelAndView for build settings editor.
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/builds/edit/{buildId}/{what}",
         method=RequestMethod.GET)
   public ModelAndView editBuildSettings2(
         @PathVariable Long buildId,
         @PathVariable String what,
         HttpServletRequest request,
         Locale locale) throws AfNotFoundException {
      Build build = _daoFactory.getBuildDao().find(buildId);
      if (build == null) {
         throw new AfNotFoundException("No such build " + buildId);
      }

      ModelMap mm = getBaseModel(request);
      mm.put("yuiSupport",true);
      mm.put("buildId", buildId);
      mm.put("what", what);
      mm.put(PAGE_TITLE_KEY, fr(locale, "T.BUILDS.EDIT_BUILD_OF_APP",
            build.getBuildName(),
            build.getDisplayName()));

      // Get the select options for runtime with default.
      SelectOptions rtSelect = _buildService.prepareRuntimeSelect(true);
      mm.put("runtimeSelect", rtSelect);

      return new ModelAndView("builds/build-edit-home", mm);
   }

   /**
    * Fetch the view that is used in the dialog for creating new registry
    * keys. Used by the build settings editor.
    *
    * @param request
    * @return ModelAndView for defining a new registry key (dialog)
    */
   @RequestMapping(
         value="/builds/regkey/new",
         method=RequestMethod.GET)
   public ModelAndView defineNewRegistryKey(
         HttpServletRequest request)
   {
      ModelMap mm = getBaseModel(request);
      mm.put("yuiSupport",true);
      return new ModelAndView("/builds/build-regkey-edit", mm);
   }


   /**
    * Return the view that lets a user define build parameters for
    * one or more applications.
    *
    * @param request
    * @param locale
    * @param appIds
    * @return ModelAndView for defining new builds for a list of apps.
    */
   @RequestMapping(
         value="/builds/define",
         method=RequestMethod.GET)
   public ModelAndView defineNewBuilds(
         HttpServletRequest request,
         Locale locale,
         @RequestParam("appId") List<Long> appIds)
   {
      ModelMap mm = getBaseModel(request, locale, "T.BUILDS.REQUEST_NEW");
      mm.put("appIds", appIds);
      mm.put("recipeMatchTypes", RecipeMatch.values());
      mm.put("yuiSupport",true);

      // Display hz enabled only if hz allowed or applicable.
      long defaultRt = _config.getLong(ConfigRegistryConstants.THINAPP_RUNTIME_ID);
      mm.put(HZ_ENABLED, false);
      return new ModelAndView("builds/build-define", mm);
   }

   /**
    * Render a HTML form for adding a new datastore.
    *
    * @param request a HttpServletRequest.
    * @param locale a locale.
    * @return ModelAndView for adding a new datastore.
    */
   @RequestMapping(value="/builds/import", method=RequestMethod.GET)
   public ModelAndView create(
         HttpServletRequest request,
         @RequestParam(required=false) boolean dialog,
         Locale locale)
   {
      String view = "builds/builds-import";
      ModelMap mm = getBaseModel(request, locale, "T.IMPORT_PROJECTS");
      if (dialog) {
         mm.put("isDialog", true);
         view = "builds/builds-import-form";
      }
      SelectOptions rtSelect = _buildService.prepareRuntimeSelect(false);
      long defaultRt = _config.getLong(ConfigRegistryConstants.THINAPP_RUNTIME_ID);
      rtSelect.setInitialValue(String.valueOf(defaultRt));
      mm.put("runtimeSelect", rtSelect);
      mm.put(HZ_ENABLED, false);

      return new ModelAndView(view, mm);
   }
}
