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

package com.vmware.appfactory.application.controller;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.application.AppHelper;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dto.BuildComponents;
import com.vmware.appfactory.build.service.BuildService;
import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.thinapp.common.util.AfJson;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles view and UI requests for the Applications pages.
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Installers)
public class AppController
   extends AbstractUiController
{
   @Resource
   private BuildService _buildService;

   /**
    * Show the HTML page containing the list of all applications.
    * Note the list is initially empty: it will use Ajax to populate it
    * dynamically, so we don't need to fetch data for it here.
    *
    * @param category
    * @param request
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value={"/apps","/apps/index","/newui/installers","/installers"}, method=RequestMethod.GET)
   public ModelAndView index(
         @RequestParam(required=false) String category,
         HttpServletRequest request,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.APPS.ALL");

      /* If a specific category is requested, add to the model */
      if (StringUtils.isNotEmpty(category)) {
         mm.put("category", category);
         mm.put(PAGE_TITLE_KEY,
               fr(locale, "T.APPS.APPS_FOR_CATEGORY",
               (category.equals(AppApiController.NO_CATEGORY_REQUEST) ? tr(locale, "T.APPS.NO_CATEGORY") :
               category)));
      }

      return new ModelAndView("apps/apps-index", mm);
   }

   /**
    * Open the view to add a new application by hand.
    *
    * @param request
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value="/apps/add", method=RequestMethod.GET)
   public ModelAndView add(
         HttpServletRequest request,
         @RequestParam(required=false) boolean dialog,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.APPS");
      String view = "apps/app-add";

      mm.put("datastoreOptions", _buildService.prepareDatastoreSelect(true));
      if (dialog) {
         mm.put("isDialog",true);
         view = "apps/app-add-form";
      }

      return new ModelAndView(view, mm);
   }

   /**
    * Edit an existing application meta-data info.
    *
    * @param request - a HttpServlet request.
    * @param appId - an valid application id.
    * @return
    * @throws Exception
    */
   @RequestMapping(value="/apps/edit/{appId}", method=RequestMethod.GET)
   public ModelAndView edit(
         HttpServletRequest request,
         @PathVariable Long appId,
         @RequestParam(required=false) Long recipeId,
         @RequestParam(required=false) String selectionData,
         Locale locale)
      throws Exception
   {
      Application app = _daoFactory.getApplicationDao().find(appId);
      if (app == null) {
         throw new AfNotFoundException("No such application " + appId);
      }
      // replace datastore://xx/ path with real share path (server/path).
      String serverPath = null;
      if (DataSource.Type.fileshare.equals(app.getDataSourceType())) {
         Long fileShareId = app.getDataSource().getId();
         FileShare fs = _daoFactory.getFileShareDao().find(fileShareId);
         if (fs != null) {
            serverPath = fs.getServerPath();
         }
      } else if (DataSource.Type.upload.equals(app.getDataSourceType())) {
         // Upload App doesn't have data source link. So, extract DS id from the uri.
         Long dsId = AppHelper.extractDatastoreIdFromDownloadUri(app);
         if (dsId != null) {
            DsDatastore ds =_dsClient.findDatastore(dsId, true);
            if (ds != null) {
               serverPath = ds.getServer() + DsUtil.FILE_SEPARATOR + ds.getShare();
            }
         }
      }

      if (serverPath != null) {
         String datastoreUri = app.getDownload().getURI().toString();
         app.getDownload().setURI(URI.create(AppHelper.replaceInternalDSSchemeWithServerPath(datastoreUri, serverPath)));
      }

      ModelMap mm = getBaseModel(request, locale, "T.APPS");
      mm.put("app", app);
      mm.put("recipeId", recipeId);
      mm.put("selectionData", selectionData);

      return new ModelAndView("apps/app-edit", mm);
   }

   /**
    * Display application related data in detail.
    * Fetch application details, associated recipe info if recipeId is passed.
    * It also fetches the entities required for capturing a build.
    *
    * @param request - a HttpServlet request.
    * @param appId - an valid application id.
    * @param recipeId - a valid recipe id
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(value="/apps/detail/{appId}", method=RequestMethod.GET)
   public ModelAndView detail(
         HttpServletRequest request,
         @PathVariable Long appId,
         @RequestParam(required=false) Long recipeId,
         @RequestParam(required=false) String selectionData,
         Locale locale)
      throws Exception
   {
      Application app = _daoFactory.getApplicationDao().find(appId);
      if (app == null) {
         throw new AfNotFoundException("No such application " + appId);
      }
      Recipe recipe = null;
      long otherRecipeCount = _daoFactory.getRecipeDao().countAll();
      if (recipeId != null) {
         recipe = _daoFactory.getRecipeDao().find(recipeId);
      }

      // Subtract 1 from otherRecipeCount coz we have a valid recipe.
      if (recipe != null) {
         otherRecipeCount--;
      }

      // Find other versions of apps with the same name.
      List<Application> otherVersions = _daoFactory.getApplicationDao()
         .findOtherVersionsIncluded(app);
      Application newerApp = null;
      if (CollectionUtils.isNotEmpty(otherVersions)) {
         // Sort by name, version, ... and get the most recent version to the top.
         Collections.sort(otherVersions);
         Collections.reverse(otherVersions);
         newerApp = findNewerVersionApp(app, otherVersions);
      }

      ModelMap mm = getBaseModel(request, locale, "T.APPS");
      mm.put("app", app);
      mm.put("icons", AfJson.ObjectMapper().writeValueAsString(app.getIcons()));
      mm.put("recipe", recipe);
      mm.put("otherRecipeCount", otherRecipeCount);
      mm.put("newerApp", newerApp);
      mm.put("appVersions", otherVersions);

      // Additionally load the build components and set any defaults if they were set.
      loadBuildComponentsAndSetDefault(mm, selectionData);

      return new ModelAndView("apps/app-detail", mm);
   }

   /**
    * Helper function to load build components and set the corresponding defaults for the build
    * components that were selected earlier.
    *
    * @param mm
    * @param selection is in the format of: <wpId>.<dsId>.<rtId>.hzFlag where hzFlag is true/false.
    * @throws DsException
    * @throws WpException
    */
   private void loadBuildComponentsAndSetDefault(ModelMap mm, String selectionData) throws DsException, WpException {
      BuildComponents buildComponents = _buildService.loadBuildComponents(false);

      boolean has4Values = false;
      String[] values = null;
      Long runtimeId = null;

      if (StringUtils.isNotEmpty(selectionData)) {
         values = selectionData.trim().split("\\.");
         has4Values = (values.length == 4);
      }

      if (has4Values) {
         // If the input param comes with the value for horizon setting, use it.
         boolean hzChecked = Boolean.TRUE.toString().equalsIgnoreCase(values[3]);
         mm.put("horizonChecked", hzChecked);

         // Set the default values for workpool, runtime and datastore if they exist.
         if (StringUtils.isNotEmpty(values[0])) {
            buildComponents.getWorkpoolOptions().setInitialValue(values[0]);
         }
         if (StringUtils.isNotEmpty(values[1])) {
            buildComponents.getDatastoreOptions().setInitialValue(values[1]);
         }

         /* The hz enabled is based on runtime, hence compute the hzEnabled based on display value */
         if (StringUtils.isNotEmpty(values[2])) {
            try {
               runtimeId = Long.valueOf(values[2]);
            }
            catch (NumberFormatException e) {
               // Dont worry, use the default runtime value if passed info is invalid.
               runtimeId = _config.getLong(ConfigRegistryConstants.THINAPP_RUNTIME_ID);
            }
         }
      } else {
         runtimeId = _config.getLong(ConfigRegistryConstants.THINAPP_RUNTIME_ID);
      }
      // Apply the computed runtimeId and corresponding hzEnabled value.
      buildComponents.getRuntimeOptions().setInitialValue(runtimeId.toString());
      mm.put("buildComponents", buildComponents);
      mm.put("horizonEnabled", false);
   }

   /**
    * This method finds a newer version of an app if one exists.
    *
    * @param app - base app to compare against.
    * @param otherVersions - other version to look in for the newer version.
    * @return - the newer version of the app or null
    */
   private Application findNewerVersionApp(Application app, List<Application> otherVersions)
   {
      Application newerApp = null;
      if (CollectionUtils.isNotEmpty(otherVersions)) {
         // Get a newer version app if exists?
         String bestNewerVersion = otherVersions.get(0).getVersion();
         if (AfUtil.alnumCompare(bestNewerVersion, app.getVersion()) > 0) {
            newerApp = otherVersions.get(0);
         }
      }
      return newerApp;
   }
}
