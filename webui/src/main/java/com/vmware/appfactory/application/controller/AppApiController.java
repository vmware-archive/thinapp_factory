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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.vmware.appfactory.application.AppHelper;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.dto.AppCaptureHistory;
import com.vmware.appfactory.application.dto.AppCaptureHistory.ActionType;
import com.vmware.appfactory.application.dto.ApplicationCreateRequest;
import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.dto.SimpleResponse;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.file.FileHelper;
import com.vmware.appfactory.file.upload.ProgressReporter;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfJson;


/**
 * This controller handles all the feed-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class AppApiController
   extends AbstractApiController
{
   /** A value for "category" when we query for apps without a category */
   public static final String NO_CATEGORY_REQUEST = "false";

   private static final String SES_APP_REQUEST = "SES_APP_REQUEST";

   /**
    * Get a list of all the applications, in our own JSON format.
    *
    * @param category Optional, to return apps from that category only.
    * @param sort Optional, to sort the results.
    * @param request - Servlet request.  Set by spring.
    * @param response - Servlet response.  Set by spring.
    * @throws IOException - if the etag headers could not be written
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps",
         method = RequestMethod.GET)
   @Nullable
   public List<Application> getAllApplications(
         @RequestParam(required=false) final String category,
         @RequestParam(required=false) boolean sort,
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response)
           throws IOException {

      /* Get apps */
      ApplicationDao appDao = _daoFactory.getApplicationDao();

      // the e-tag for the Apps table needs to be based on not only the apps
      // db table itself, but also:
      //  - the feeds table
      //  - the fileshares table
      // The reason is that the Application object has a dataSourceName property,
      // so if the name of a feed or fileshare changed, we need a new e-tag.
      if (checkModified(request,
                        response,
                        null,
                        appDao,
                        _daoFactory.getFeedDao(),
                        _daoFactory.getFileShareDao())) {
         // shortcut exit - no further processing necessary
         return null;
      }

      final List<Application> apps = appDao.findAllIncluded();

       /**
        * TODO: if we continue to need to filter on categories,
        * just do a proper select here.
        */
      /* Filter on category */
      if (StringUtils.isNotEmpty(category)) {
         CollectionUtils.filter(apps, new Predicate() {
            @Override
            public boolean evaluate(Object obj) {
               Application app = (Application) obj;
               return
                  (app.getCategories().isEmpty() && category.equals(NO_CATEGORY_REQUEST)) ||
                  (app.belongsToCategory(category));
            }
         });
      }

      if (sort) {
         Collections.sort(apps);
      }

      return apps;
   }

   /**
    * Get a single application, in our own JSON format.
    *
    * @param id Application ID.
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{id}",
         method = RequestMethod.GET)
   @Nullable
   public Application getApplication(@PathVariable Long id, @Nonnull WebRequest webRequest)
   throws AfNotFoundException {
      // Finding lastUpdated on this table is slower than fetching the record by PK.
      Application app = _daoFactory.getApplicationDao().find(id);
      if (app == null) {
         throw new AfNotFoundException("Invalid app Id " + id);
      }
      long lastModified = app.getModified();
      if (0 != lastModified && webRequest.checkNotModified(lastModified)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      return app;
   }

   /**
    * Get the icon with the given hash at position iconId for the application with the given id.
    *
    * @param appId id of application
    * @param iconId index of icon to access
    * @param iconHash hash of the icon to access
    * @param response
    * @return binary data for the application's icon
    * @throws AfNotFoundException, IOException
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{appId}/icon/{iconId}/{iconHash}",
         method = RequestMethod.GET)
   public byte[] getAppIcon(
         @PathVariable Long appId,
         @PathVariable Integer iconId,
         @PathVariable String iconHash,
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response)
      throws AfNotFoundException, IOException
   {
      return processIconRequest(appId, iconId, iconHash, _daoFactory.getApplicationDao(), request, response);
   }

   /**
    * Change the "skipped" attribute for an application.
    * Skipped applications are not considered for automatic conversion from a
    * feed and do not show up in the application inventory.
    *
    * @param appId
    * @param skip
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{appId}/skip/{skip}",
         method = RequestMethod.PUT)
   public void setApplicationSkip(
         @PathVariable Long appId,
         @PathVariable boolean skip)
      throws AfNotFoundException
   {
      /* Get the app */
      ApplicationDao appDao = _daoFactory.getApplicationDao();
      final Application app = appDao.find(appId);

      if (app == null) {
         throw new AfNotFoundException("No such application id " + appId);
      }

      app.setSkipped(skip);
      appDao.update(app);

      return;
   }

   /**
    * Edit an existing application info.
    *
    * @param appId - a valid appId.
    * @param appRequest - an ApplicationCreateRequest instance.
    * @throws AfNotFoundException if the request's appId not found in the database.
    * @throws AfBadRequestException if appRequest is null or either appName or appVersion
    *    is empty.
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{appId}",
         method = RequestMethod.PUT)
   public void edit(
         @PathVariable Long appId,
         @RequestBody ApplicationCreateRequest appRequest)
      throws AfNotFoundException, AfBadRequestException
   {
      /* Get the app */
      final ApplicationDao appDao = _daoFactory.getApplicationDao();
      final Application app = appDao.find(appId);

      if (app == null) {
         throw new AfNotFoundException("No such application id " + appId);
      }

      try {
         appRequest.validateNameAndVersion();
      } catch (Exception e) {
         throw new AfBadRequestException(e);
      }

      if (AppHelper.trimWhiteSpaceAndCheckDiff(app, appRequest)) {
         app.setName(appRequest.getAppName());
         app.setVersion(appRequest.getAppVersion());
         app.setVendor(appRequest.getAppVendor());
         app.setInstallerRevision(appRequest.getAppRevision());
         app.setLocale(appRequest.getAppLocale());

         AppInstall install = null;
         if (StringUtils.isNotBlank(appRequest.getAppInstallCommandOption())) {
            install = new AppInstall();
            install.setCommand(appRequest.getAppInstallCommandOption());
         }
         app.setInstalls(install);

         app.setOverrideMetadata(true);
         appDao.update(app);
      }

      return;
   }

   /**
    * Update the application's failCount to 0.
    *
    * By resetting it, if the feeds are enabled with auto convert,
    * the next cycle of app conversion kicks in for the feeds
    *
    * @param appId
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{appId}/resetFailCount",
         method = RequestMethod.POST)
   public void resetAppFailureCount(
         @PathVariable Long appId)
      throws AfNotFoundException
   {
      /* Get the app */
      ApplicationDao appDao = _daoFactory.getApplicationDao();
      Application app = appDao.find(appId);

      if (app == null) {
         throw new AfNotFoundException("No such application id " + appId);
      }
      if (app.getFailCount() != 0) {
         // update only if reset is needed.
         app.setFailCount(0);
         appDao.update(app);
      }

      return;
   }


   /**
    * This method fetches the list of build requests by appId. The build requests
    * lifecycle is closely tied to that of the app itself.
    *
    * @param appId
    * @return
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/{appId}/buildRequests",
         method = RequestMethod.GET)
   public List<AppCaptureHistory> getBuildRequestsByApp(
         ServletWebRequest webRequest,
         @PathVariable Long appId)
      throws AfNotFoundException
   {
      /* Get the app */
      ApplicationDao appDao = _daoFactory.getApplicationDao();
      Application app = appDao.find(appId);

      if (app == null) {
         throw new AfNotFoundException("No such application id " + appId);
      }

      List<AppBuildRequest> builds = _daoFactory.getAppBuildRequestDao()
         .findBuildRequestForApp(appId);

      // Compute the created/lastUpdated based on AppBuildRequest and related build table entries.
      long lastUpdated = 0;
      List<AppCaptureHistory> captures = new ArrayList<AppCaptureHistory>(builds.size());
      for (AppBuildRequest request : builds) {
         AppCaptureHistory capture = createAppCaptureHistory(request);
         captures.add(capture);
         if (capture.getLastUpdated() > lastUpdated) {
            lastUpdated = capture.getLastUpdated();
         }
      }

      // Return data only if there are changes yet to be sent.
      return webRequest.checkNotModified(lastUpdated)? null : captures;
   }


   /**
    * This is a helper method that performs the necessary lookups on build
    * and recipe to get related info.
    *
    * @param request
    * @return
    */
   private AppCaptureHistory createAppCaptureHistory(AppBuildRequest request)
   {
      // If the recipeId is set and that recipe exists, fetch it for the name.
      String recipeName = StringUtils.EMPTY;
      if (request.getRecipeId() != null) {
         Recipe r = _daoFactory.getRecipeDao().find(request.getRecipeId());
         if (r != null) {
            recipeName = r.getName();
         }
      }

      long lastUpdated = (request.getModified() == 0)? request.getCreated() : request.getModified();
      Build.Status status = null;
      ActionType type = ActionType.NONE;
      // If build is successful, fetch the current status of the build.
      if (request.getRequestStage() == RequestStage.successful) {
         Build b = _daoFactory.getBuildDao().find(request.getBuildId());
         if (b != null) {
            // The build may not exist, and only show the status and link if build exists.
            status = b.getStatus();
            type = ActionType.BUILD;

            // If build's last updated timestamp is recent, store it.
            if (b.getModified() > lastUpdated) {
               lastUpdated = b.getModified();
            }
         }
         else {
            type = ActionType.BUILD_DELETED;
         }
      }
      else if (request.getRequestStage() == RequestStage.created
            || request.getRequestStage() == RequestStage.running) {
         type = ActionType.TASK;
      }

      return new AppCaptureHistory(request,recipeName, type, lastUpdated, status);
   }


   /**
    * Step 1: Validate the destination datastore for uploading an application
    * installer.
    *
    * @param request - HttpServletRequest request.
    * @param appRequest - ApplicationCreateRequest json object
    * @throws AfBadRequestException if the serverPath in the request is not reachable.
    * @throws AfServerErrorException
    *
    * @see #uploadAndCreate(MultipartFile, HttpServletRequest)
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/create",
         method = RequestMethod.POST)
   public void validateCreate(
         @RequestBody ApplicationCreateRequest appRequest,
         HttpServletRequest request)
      throws AfBadRequestException, AfServerErrorException {
      // Perform all possible validations upfront, so they can be avoided after
      // the file is uploaded.
      validateAppAddRequest(appRequest);
      request.getSession().setAttribute(SES_APP_REQUEST, appRequest);

      // Store the unique uploadId parameter into the user session which will be consumed by
      // CustomMultipartResolverWithProgress to assist with upload progress management.
      ProgressReporter.initProgressListener(request, appRequest.getUploadId());
   }

   /**
    * Step 2: Upload the installer and create the new file share application.
    *
    * Failure or success will respond with a json with a success flag. The Http status type does not
    * matter much, as this response is recieved in an iframe, and is not transposed over to the js callback.
    *
    * Docs: http://jquery.malsup.com/form/#file-upload
    *
    * @param uploadFile - multipart file being uploaded.
    * @param request - HttpServletRequest
    */
   @ResponseBody
   @RequestMapping(
         value = "/apps/upload",
         method = RequestMethod.POST)
   public ResponseEntity<String> uploadAndCreate(
         @RequestParam("uploadFile") MultipartFile uploadFile,
         HttpServletRequest request) {
      ApplicationCreateRequest uploadApp = null;
      try {
         // Load and validate required fields.
         uploadApp = loadSessionUploadApp(uploadFile, request);
         final List<String> installerDirs = generateFolderNameListForUploadApp(uploadApp);

         // Get the destination ds and copy the installer.
         DsDatastore ds = _dsClient.findDatastore(uploadApp.getDsId(), true);

         // Developer mode! Copy the upload file to a local directory.
         if (_af.isDevModeDeploy() && StringUtils.isNotEmpty(_af.getDevModeUploadDir())) {

            // Path to file's directory
            String newPath = _af.getDevModeUploadDir();
            for (String dir : installerDirs) {
               newPath = FileHelper.constructFilePath2(File.separator, newPath, dir);
            }
            FileUtils.forceMkdir(new File(newPath));

            // Full path to file
            String newFile = FileHelper.constructFilePath2(
                  File.separator, newPath, uploadFile.getOriginalFilename());

            _log.debug("DEV MODE! Uploading " + uploadFile.getOriginalFilename() +
                  " size " + uploadFile.getSize() + " to " + newFile);

            // Copy it
            uploadFile.transferTo(new File(newFile));
         }
         else { // Production mode! Copy the upload file to the datastore.
            // Full path to directory where file will be copied to.
            String destFile = ds.createDirsIfNotExists(installerDirs.toArray(new String[installerDirs.size()]));
            destFile = destFile + uploadFile.getOriginalFilename();

            _log.debug("Uploading " + uploadFile.getOriginalFilename() +
               " size " + uploadFile.getSize() + " to " + destFile);

            ds.copy(uploadFile, destFile, null);
         }

         // create the application now that uploaded installer has been copied.
         createUploadApp(installerDirs, uploadFile.getOriginalFilename(), request, uploadApp);
         _log.info("upload file & app: {} creation complete.", uploadApp.getAppName());
         return respondUploadMessage("Installer was uploaded and saved.", true, request);

      } catch (AfBadRequestException e) {
         _log.error("Saving file to ds error: " + e.getMessage(), e);
         return respondUploadMessage("Selected datastore couldnt not be accessed.", false, request);
      } catch (URISyntaxException urie) {
         _log.error("Application create error: " + urie.getMessage(), urie);
         return respondUploadMessage("Saving the application after installer upload failed.", false, request);
      } catch (IllegalStateException e) {
         _log.error("Save file to ds error: " + e.getMessage(), e);
         return respondUploadMessage("Installer couldnt be saved onto the datastore.", false, request);
      } catch (IOException e) {
         _log.error("Create folder /save file to ds error: " + e.getMessage(), e);
         return respondUploadMessage("Installer couldnt be saved onto the datastore.", false, request);
      } catch (DsException ds) {
         _log.error("Saving file to ds error: " + ds.getMessage(), ds);
         return respondUploadMessage("Selected datastore could not be accessed.", false, request);
      } catch (RuntimeException rts) {
         // This is the default runtime exception case that needs to be handled. The client handler can only
         // handle json response, and hence we catch all other runtime exceptions here.
         _log.error("Uploading installer failed with error: " + rts.getMessage(), rts);
         return respondUploadMessage("Uploading and creating an application failed.", false, request);
      }
      finally {
         if (uploadApp != null) {
            // Cleanup the progress listener session variable.
            ProgressReporter.removeProgressListener(request, uploadApp.getUploadId());
         }
      }
   }

   /**
    * This method generates a folder name list for the following dir layout:
    * "<taf-upload-folder>/vendor/name/version/locale/revision"
    *
    * @param uploadApp
    * @return
    */
   private List<String> generateFolderNameListForUploadApp(ApplicationCreateRequest uploadApp) {
      // default dir layout is "vendor/name/version/locale/revision"
      final Map<String, String> metaMap = Application.createMetadataMap(
            FileHelper.DEFAULT_APPLICATION_DIR_LAYOUT.split("/"), uploadApp.getAppVendor(),
            uploadApp.getAppName(), uploadApp.getAppVersion(), uploadApp.getAppLocale(), uploadApp.getAppRevision());

      List<String> installerDirs = FileHelper.parseDirs(FileHelper.DEFAULT_APPLICATION_DIR_LAYOUT, metaMap);

      // Add 'upload' dir to the dir list.
      installerDirs.add(0, FileHelper.UPLOAD_DIR);
      return installerDirs;
   }

   /**
    * This method responds with a string response containing json data. If success is set, it means the request
    * was successful, else a failure. For IE8, IE9 compatibility, the contentType=text/plain is set explicitly.
    *
    * @param message
    * @param success
    * @param request
    * @return
    */
   private ResponseEntity<String> respondUploadMessage(String message, boolean success, HttpServletRequest request) {
      // wrap json in a textarea only if the request did not come from xhr
      /* Only required for json or script response types for jquery.form.js uploads.
      _log.trace("X-Requested-With: " + request.getHeader("X-Requested-With"));
      boolean isXhr = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
      if (!isXhr) {
         message = String.format("<textarea>%s</textarea>", message);
      } */

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.TEXT_PLAIN);
      SimpleResponse simpleResponse = new SimpleResponse(success, message);
      String response = StringUtils.EMPTY;
      try {
         response = AfJson.ObjectMapper().writeValueAsString(simpleResponse);
      }
      catch (JsonMappingException e) {
         // This should not occur, the object is simple enough with boolean, string.
         _log.error("Json convertion error: ", e);
         response = message;
      }
      catch (IOException e) {
         // This should not occur under normal circumstances.
         _log.error("Json convertion error: ", e);
         response = message;
      }
      return new ResponseEntity<String>(response, responseHeaders, HttpStatus.OK);
   }

   /**
    * Helper method to validate the input params, and throw appropriate exceptions.
    *
    * @param uploadFile
    * @param uploadApp
    * @throws AfBadRequestException
    */
   private ApplicationCreateRequest loadSessionUploadApp(MultipartFile uploadFile, HttpServletRequest request)
   throws AfBadRequestException {
      ApplicationCreateRequest uploadApp = (ApplicationCreateRequest)
      request.getSession().getAttribute(SES_APP_REQUEST);

      // Cleanup session variables created during step 1 which is not required on the session anymore.
      request.getSession().removeAttribute(SES_APP_REQUEST);

      if (uploadApp == null) {
         _log.warn("uploadApp details are not found in session");
         throw new AfBadRequestException("Upload details are not found in session.");
      }

      if (StringUtils.isBlank(uploadFile.getOriginalFilename())) {
         throw new AfBadRequestException("Upload file has no filename.");
      }

      if (uploadFile.getSize() == 0) {
         throw new AfBadRequestException("Upload file has no size.");
      }
      return uploadApp;
   }

   /**
    * Delete AppBuildRequest by id.
    * @param id an AppBuildRequest id.
    */
   @RequestMapping(
         value = "/apps/buildRequests/{id}",
         method = RequestMethod.DELETE)
   public @ResponseBody void deleteBuildRequest(@PathVariable Long id) {
      AppBuildRequest appBuildRequest = new AppBuildRequest();
      appBuildRequest.setId(id);
      _daoFactory.getAppBuildRequestDao().delete(appBuildRequest);
   }

   /**
    * Helper method to assist in creating the application that is being
    * uploaded.
    *
    * @param installerDirs - Uploaded file parent directory list
    * @param uploadFileName - Uploaded file name.
    * @param request - HttpServletRequest
    * @param uploadApp - Application creation request
    */
   private void createUploadApp(List<String> installerDirs, String uploadFileName,
         HttpServletRequest request, ApplicationCreateRequest uploadApp) throws URISyntaxException {
      // Create the app object.
      Application app = new Application(uploadApp.getAppName(), uploadApp.getAppVersion(),
            uploadApp.getAppLocale(), uploadApp.getAppRevision(), uploadApp.getAppVendor());

      // Construct download file
      List<String> pathParts = new ArrayList<String>();
      pathParts.addAll(installerDirs);
      pathParts.add(uploadFileName);
      URI uri = DsUtil.generateDatastoreURI(uploadApp.getDsId(), pathParts.toArray(new String[pathParts.size()]));

      // Construct download location
      AppDownload dl = new AppDownload();
      dl.setURI(uri);
      app.setDownload(dl);

      // Use the user passed installer command when passed, else use the default applicable.
      List<AppInstall> appInstallList = null;
      if (StringUtils.isNotEmpty(uploadApp.getAppInstallCommandOption())) {
         AppInstall appInstall = new AppInstall(uploadApp.getAppInstallCommandOption());
         appInstallList = Collections.singletonList(appInstall);
      } else {
         appInstallList = AppHelper.createInstallsFromFile(uploadFileName);
      }

      // Construct install command
      app.setInstalls(appInstallList);

      // Because this app uses user supplied meta-data.
      app.setOverrideMetadata(true);

      // Tie app to the "upload" data source by assigning no data source
      app.setDataSource(null);
      app.setLastRemoteUpdate(System.currentTimeMillis());

      // Create the application
      _daoFactory.getApplicationDao().create(app);
      _log.debug("Created upload application {}, id = {}", app.getDisplayName(), app.getId());
   }


   /**
    * Helper method to check if datastore is valid, and if so, bring it online if offline.
    *
    * @param appRequest
    * @throws AfServerErrorException
    */
   private void validateAppAddRequest(ApplicationCreateRequest appRequest)
   throws AfServerErrorException
   {
      try {
         DsDatastore ds = _dsClient.findDatastore(appRequest.getDsId(), true);
         // Bring the datastore online if its offline. Needed for uploading binary.
         if (ds.getStatus() == Datastore.Status.offline) {
            _dsClient.setOnline(appRequest.getDsId());
         }
      }
      catch (DsException e) {
         throw new AfServerErrorException("The selected datastore is unavailable at the moment.");
      }
   }
}
