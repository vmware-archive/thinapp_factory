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

package com.vmware.appfactory.store.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.build.model.BuildFile;
import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.thinapp.common.util.AfJson;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * The Spring MVC controller that handles all requests for the store.
 *
 * This includes the view of the store that a user sees, plus the requests
 * made by the store page and the plugin to fetch the bootstrapper and the
 * JSON data is requests.
 */
@Controller
public class StoreController
   extends AbstractUiController
{
   private static final String BOOTSTAPPER_INSTALLER_FILE = "setup.exe";
   private static final String BOOTSTAPPER_INSTALLER_PATH = "classpath:" + BOOTSTAPPER_INSTALLER_FILE;

   @Autowired
   private org.springframework.context.ApplicationContext _springContext;


   /**
    * Returns the end-user view of the application store.
    *
    * This includes all applications that are marked as PUBLISHED, organized
    * by category. Applications on a bad datastore are not included.
    *
    * @param request
    * @return
    */
   @RequestMapping(
         value="/store",
         method=RequestMethod.GET)
   public ModelAndView showAvailableApps(
         HttpServletRequest request)
   {
      ModelMap mm = getBaseModel(request);
      return new ModelAndView("store/store-front", mm);
   }


   /**
    * Returns the end-user view of the application store.
    *
    * This includes all applications that are marked as PUBLISHED, organized
    * by category. Applications on a bad datastore are not included.
    *
    * @param request
    * @return
    */
   @RequestMapping(
         value="/store/installed",
         method=RequestMethod.GET)
   public ModelAndView showInstalledApps(
         HttpServletRequest request)
   {
      ModelMap mm = getBaseModel(request);
      return new ModelAndView("/store/store-installed", mm);
   }


   /**
    * Handles requests to download packages.
    *
    * Note that we use a "bootstrapper" (browser plug-in) to actually download
    * the packages. A request for this URL means the bootstrapper is not yet
    * installed, so we must send it to the client as an attachment. We also
    * embed the actual download URL into the executable so the bootstrapper can
    * do it's thing immediately after being installed.
    *
    * @param packageIds List of build (ids) to install or uninstall
    * @param action install or uninstall.
    * @param request
    * @param response
    * @throws IOException
    * @throws URISyntaxException
    */
   @RequestMapping(
         value="/store/bootstrap",
         method=RequestMethod.GET)
   public void downloadBootstrapper(
         @RequestParam("id") List<String> packageIds,
         @RequestParam String action,
         HttpServletRequest request,
         HttpServletResponse response)
      throws IOException, URISyntaxException
   {
      if (action.equals("install")) {
         _log.debug("Request to download; redirecting to bootstapper installer");

         /* Create custom URL for bootstrapper.
          * Note this is not a real URL we have to handle: the bootstrapper
          * picks it apart to decide what it needs to install, and calls
          * "/captures.json" to get a "dictionary" of all packages and where
          * they live.
          */
         String params = StringUtils.collectionToDelimitedString(packageIds, ".");
         if ( params.length() > 0) {
            params = "capture=" + params;
         }
         URI newUrl = new URI("thinapp", null, request.getServerName(), request.getServerPort(),
               request.getContextPath() + "/install", params, null);
         _log.debug("URL to embed = " + newUrl);

         Resource template = _springContext.getResource(BOOTSTAPPER_INSTALLER_PATH);

         File srcFile = template.getFile();
         _log.debug("Template file = " + srcFile);
         File dstFile = File.createTempFile("af-", ".exe");

         try {
            FileCopyUtils.copy(srcFile, dstFile);
            _log.debug("Copied " + srcFile + " to " + dstFile);

            long fileSize = AfUtil.binaryReplace(
                  dstFile,
                  "ThinApp Chain URL goes here",
                  newUrl.toString(),
                  "UTF-16BE");

            if (fileSize < 0) {
               throw new IOException("Failed to replace URL text in installer template");
            }

            /* Redirect to the installer path */
            response.setContentType("application/octet-stream");
            response.setContentLength((int) fileSize);
            response.setHeader(AfUtil.CONTENT_DISPOSITION, "attachment; filename=\"setup.exe\"");

            FileInputStream is = new FileInputStream(dstFile);
            ServletOutputStream os = response.getOutputStream();

            // Closes streams when finished.
            FileCopyUtils.copy(is, os);
         } finally {
            dstFile.delete();
         }

         return;
      }

      _log.error("Unknown install action \"" + action + "\"");

      /* Redirect to the store */
      response.sendRedirect(getContextPath(request) + "/store/index");
   }


   /**
    * This is the request send by the bootstrapper when it asks for the
    * information needed to download and install ThinApp packages.
    * The response is a custom JSON format.
    *
    * TODO It would be nice if the bootstrapper understood AppFactory classes
    * TODO Should be an API call (BuildApiController?)
    *
    * @param response
    * @throws IOException
    */
   @RequestMapping("/captures.json")
   public void getBuildSummary(HttpServletResponse response)
      throws IOException
   {
      BuildDao buildDao = _daoFactory.getBuildDao();
      List<Build> published = buildDao.findForStatus(Build.Status.PUBLISHED);
      List<Map<String,?>> json = new ArrayList<Map<String,?>>();

      for (Build build : published) {
         Map<String,Object> buildInfo = new HashMap<String,Object>();

         buildInfo.put("name", build.getName());
         buildInfo.put("app_version", build.getVersion());
         buildInfo.put("package_id", build.getId());
         buildInfo.put("application_id", build.getId());
         buildInfo.put("icon_url", build.getBestIconForSize(32).getUrl());
         buildInfo.put("id", build.getId());

         List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
         for (BuildFile file : build.getBuildFiles()) {
            Map<String,Object> fileInfo = new HashMap<String,Object>();

            fileInfo.put("url", file.getUrl());
            fileInfo.put("file_id", file.getId());
            fileInfo.put("filename", file.getExeName());
            fileInfo.put("size", Long.valueOf(file.getSize()));
            fileInfo.put("id", file.getId());
            files.add(fileInfo);
         }

         buildInfo.put("thinapps", files);
         json.add(buildInfo);
      }

      /*
       * Note: bootstrapper fails if no Content-Length is specified, so we have
       * to convert JSON to a string before writing the HTTP response.
       */

      String s = AfJson.ObjectMapper().writeValueAsString(json);
      response.setContentLength(s.length());
      response.getWriter().write(s);
      return;
   }
}
