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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.build.dto.BuildAllResponse;
import com.vmware.appfactory.build.dto.BuildComponents;
import com.vmware.appfactory.build.dto.BuildDefineResponse;
import com.vmware.appfactory.build.dto.BuildGroupByApp;
import com.vmware.appfactory.build.dto.BuildGroupByAppResponse;
import com.vmware.appfactory.build.dto.BuildRequest;
import com.vmware.appfactory.build.dto.IniDataRequest;
import com.vmware.appfactory.build.dto.ProjectImportRequest;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.build.service.BuildService;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.CwsRegistryRequest;
import com.vmware.appfactory.cws.CwsSettingsDir;
import com.vmware.appfactory.cws.CwsSettingsIni;
import com.vmware.appfactory.cws.CwsSettingsIniData;
import com.vmware.appfactory.cws.CwsSettingsRegKey;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.datastore.exception.DsNameInUseException;
import com.vmware.appfactory.recipe.dao.RecipeDao;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles all the project-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@SuppressWarnings("MissortedModifiers")
@Controller
public class BuildApiController
   extends AbstractApiController
{
   @Resource
   BuildService _buildService;

   /**
    * Submit new build requests.
    *
    * Accepts a list of BuildRequest instances that defines one or more
    * applications to build, and the settings for each.
    *
    * @see #getDefaultBuildDefinitions
    *
    * @param captureRequests List of new builds that are being requested.
    * @throws AfBadRequestException
    */
   @RequestMapping(
         value="/builds",
         method=RequestMethod.POST)
   public @ResponseBody void buildApplications(
         @RequestBody CaptureRequestImpl[] captureRequests)
      throws AfNotFoundException, WpException, DsException, AfBadRequestException {
      /** Check batch size */
      if (captureRequests.length > _config.getMaxProjectsPerBatch()) {
         throw new AfBadRequestException(
               String.format("The maximum limit is %d projects/batch, but you submitted %d projects in this batch.",
                     _config.getMaxProjectsPerBatch(), captureRequests.length));
      }
      /** Submit the build requests to the conversion queue. */
      _buildService.submitTasks(captureRequests);
   }


   /**
    * Get a list of all builds, optionally filtered by status value.
    * The results can be sorted (according to the natural sort order of
    * AfBuild).
    *
    * @param status If specified, returns builds with this status only.
    * @param checkDatastore If specified and true, only return builds that
    *                       exist on valid datastores.
    *
    * @return A list of all builds.
    *
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/builds",
         method=RequestMethod.GET)
   @Nullable
   public @ResponseBody BuildAllResponse getBuilds(
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response,
         @RequestParam(required=false) Build.Status status,
         @RequestParam(required=false) boolean checkDatastore)
         throws AfServerErrorException, IOException, DsException {

      BuildDao buildDao = _daoFactory.getBuildDao();

      Map<Long, DsDatastore> dsMap = _buildService.loadDatastoresMap(null);
      if (checkModified(request,response, dsMap.values(), buildDao)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      return new BuildAllResponse(
            false,
            getBuildsInternal(status, checkDatastore, dsMap),
            dsMap);
   }

   private List<Build> getBuildsInternal(Build.Status status, boolean checkDatastore, Map<Long, DsDatastore> dsMap)
   throws DsException {
      BuildDao buildDao = _daoFactory.getBuildDao();
      List<Build> builds;

      if (status == null) {
         /* Get all the builds */
         builds = buildDao.findAll();
      }
      else {
         /* Get all builds with the given status */
         builds = buildDao.findForStatus(status);
      }

      if (checkDatastore) {
         builds = removeInvalidDatastoreBuilds(builds, dsMap);
      }

      return builds;
   }


   /**
    * Get a list of all builds, optionally filtered by status value.
    * The results can be sorted (according to the natural sort order of
    * AfBuild).
    *
    * @param status If specified, returns builds with this status only.
    *
    * @return BuildGroupByAppResponse containing grouped builds by app and
    *         related datastores.
    *
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/builds/group/app",
         method=RequestMethod.GET)
   @Nullable
   public @ResponseBody BuildGroupByAppResponse getBuildGroupByApp(
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response,
         @RequestParam(required=false) Build.Status status)
         throws AfServerErrorException, DsException, IOException {

      BuildDao buildDao = _daoFactory.getBuildDao();

      if (checkModified(request,response, null, buildDao)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      // Get the builds sorted so grouping uses fewer iterations.
      List<Build> builds = getBuildsInternal(status, false, null);

      if (CollectionUtils.isEmpty(builds)) {
         return new BuildGroupByAppResponse();
      }
      Set<Long> dsIdSet = new HashSet<Long>();
      Map<AbstractApp, BuildGroupByApp> buildGroupMap =
         new HashMap<AbstractApp, BuildGroupByApp>();

      // Group builds by app and keep track of all the datastores
      for (Build build : builds) {
         // Keep track of the list of datastores.
         dsIdSet.add(build.getDatastoreId());
         AbstractApp.AppIdentity identity = new AbstractApp.AppIdentity(build);
         BuildGroupByApp temp = buildGroupMap.get(identity);
         if (temp == null) {
            buildGroupMap.put(identity, new BuildGroupByApp(identity, build));
         }
         else {
            temp.updateBuildGroup(build);
         }
      }

      // Create the response object and return it.
      return new BuildGroupByAppResponse(
            buildGroupMap.values(),
            _buildService.loadDatastores(dsIdSet));
   }


   /**
    * Get a list of all builds for the same application as the specified
    * build.
    *
    * @param request - Servlet request.  Set by spring.
    * @param response - Servlet response.  Set by spring.
    * @param checkDatastore If specified and true, only return builds that
    *                       exist on valid datastores.
    * @param buildId Return all builds for the same application as this one.
    *
    * @return A list of all builds.
    *
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/builds/like/{buildId}",
         method=RequestMethod.GET)
   @Nullable
   public @ResponseBody BuildAllResponse getBuildsLike(
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response,
         @RequestParam(required=false) boolean checkDatastore,
         @PathVariable Long buildId)
         throws AfServerErrorException, IOException, DsException {
      /* Get builds matching 'likeBuildId' */
      BuildDao buildDao = _daoFactory.getBuildDao();

      Map<Long, DsDatastore> dsMap = _buildService.loadDatastoresMap(null);
      if (checkModified(request,response, dsMap.values(), buildDao)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      Build likeBuild = buildDao.find(buildId);
      List<Build> builds;
      // see bug 836875: if we have just deleted the build,
      // we won't see it.  Be graceful and return an empty
      // list instead of an NPE.
      if (null != likeBuild) {
         builds = buildDao.findForApp(likeBuild);
      } else {
         builds = Collections.emptyList();
      }

      // If builds exist, then remove the ones that are applicable.
      if (checkDatastore) {
         builds = removeInvalidDatastoreBuilds(builds, dsMap);
      }
      BuildAllResponse result = new BuildAllResponse(
            _config.getBool(ConfigRegistryConstants.HORIZON_ENABLED),
            builds, dsMap);

      return result;
   }


   /**
    * Get a single build.
    *
    * @param buildId
    * @return The specified build.
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/builds/{buildId}",
         method=RequestMethod.GET)
   public @ResponseBody Build getBuild(
         ServletWebRequest webRequest,
         @PathVariable Long buildId)
      throws AfNotFoundException
   {
      BuildDao buildDao = _daoFactory.getBuildDao();
      Build build = buildDao.find(buildId);

      if (build == null) {
         throw new AfNotFoundException("No such build ID " + buildId);
      }

      long lastModified = build.getModified();
      if (0 != lastModified && webRequest.checkNotModified(lastModified)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      return build;
   }


   /**
    * Get the icon with the given hash at position iconId for the build with the given id.
    *
    * @param buildId id of application
    * @param iconId index of icon to access
    * @param iconHash hash of the icon to access
    * @param response
    * @return binary data for the build's icon
    * @throws AfNotFoundException, IOException
    */
   @ResponseBody
   @RequestMapping(
         value = "/builds/{buildId}/icon/{iconId}/{iconHash}",
         method = RequestMethod.GET)
   public byte[] getBuildIcon(
         @PathVariable Long buildId,
         @PathVariable Integer iconId,
         @PathVariable String iconHash,
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response)
      throws AfNotFoundException, IOException
   {
      return processIconRequest(buildId, iconId, iconHash, _daoFactory.getBuildDao(), request, response);
   }

   /**
    * Change the state of a build.
    *
    * @param id
    * @param status
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{id}/status/{status}",
         method=RequestMethod.PUT)
   public @ResponseBody void setBuildStatus(
         @PathVariable Long id,
         @PathVariable Build.Status status)
           throws AfNotFoundException, AfBadRequestException, AfConflictException, DsException, CwsException {
      if (status == null) {
         throw new AfBadRequestException("Invalid build status");
      }

      Build build = findBuildAndValidate(id);
      build.setStatus(status);
      _daoFactory.getBuildDao().update(build);
   }

   /**
    * Delete a build.
    *
    * @param id
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws CwsException
    */
   @RequestMapping(
         value="/builds/{id}",
         method=RequestMethod.DELETE)
   public @ResponseBody void deleteBuild(@PathVariable Long id)
      throws AfNotFoundException, AfConflictException, CwsException
   {
      Build build = findBuildAndValidate(id);

      /* Request project deletion from CWS */
      Long projectId = build.getConverterProjectId();
      try {
         _cwsClient.deleteProject(projectId);
      }
      catch(AfNotFoundException ex) {
         /*
          * CWS doesn't know about the project, so it's probably gone already.
          * Therefore, issue a warning but carry on with our own delete.
          */
         _log.warn(
               "Deleting build " + id + ": " +
               "CWS project id " + projectId + " not known");
      }

      /* Delete from our own database */
      BuildDao buildDao = _daoFactory.getBuildDao();
      buildDao.delete(build);
   }


   /**
    * Rebuild a build.
    *
    * @param id
    * @throws AfNotFoundException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{id}/rebuild",
         method=RequestMethod.POST)
   public @ResponseBody void rebuildBuild(@PathVariable Long id)
      throws AfNotFoundException, AfConflictException
   {
      Build build = findBuildAndValidate(id);

      AppFactoryTask task = _taskFactory.newRebuildTask(build);
      _conversionsQueue.addTask(task);
      _log.debug(
            "Queued rebuild task for build " + build.getBuildId() +
                  " (CWS project id = " + build.getConverterProjectId() + ')');
   }


   /**
    * Get all settings for a build.
    *
    * @see #getBuildSettingsPackageIni
    *
    * @param buildId
    * @return A map containing the keys "dirRoot", "registryRoot", and "packageIni"
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/settings",
         method=RequestMethod.GET)
   public @ResponseBody Map<String,Object> getBuildSettings(
         @PathVariable Long buildId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);
      Map<String,Object> map = new HashMap<String, Object>();

      try {
         Long projectId = build.getConverterProjectId();
         boolean embedAuth = false; // because it doesn't work for IE

         map.put("dirRoot", _cwsClient.getProjectDirectoryRoot(projectId));
         map.put("registryRoot", _cwsClient.getProjectRegistryRoot(projectId));
         map.put("iniDataRequest", loadIniAndRelatedData(build));
         map.put("projectDir", build.getLocationUrl(_dsClient, embedAuth));

         return map;
      }
      catch(CwsException ex) {
         throw new AfServerErrorException(ex);
      }
      catch(DsException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get package.ini (only) settings for a build.
    *
    * @see #getBuildSettings(Long)
    *
    * @param buildId
    * @return
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/settings/packageini",
         method=RequestMethod.GET)
   public @ResponseBody IniDataRequest getBuildSettingsPackageIni(
         @PathVariable Long buildId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);
      return loadIniAndRelatedData(build);
   }


   /**
    * Helper method to consolidate ini data together as IniDataRequest object.
    *
    * TODO Good to abstract this method into buildService and hide implementation detail.
    *
    * @param build
    * @return
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    */
   private IniDataRequest loadIniAndRelatedData(Build build) throws AfNotFoundException, AfServerErrorException {
      try {
         Long projectId = build.getConverterProjectId();
         CwsSettingsIni packageIni = _cwsClient.getProjectPackageIni(projectId);
         Project project = _cwsClient.getProjectStatus(build.getConverterProjectId());

         IniDataRequest iniData = new IniDataRequest(packageIni, false, project.getRuntimeId());
         return iniData;
      }
      catch(CwsException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get directory root (only) settings for a build.
    *
    * @see #getBuildSettings(Long)
    *
    * @param buildId
    * @return
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/settings/directory",
         method=RequestMethod.GET)
   public @ResponseBody Map<String,Object> getBuildSettingsDirectoryRoot(
         @PathVariable Long buildId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);
      Map<String,Object> map = new HashMap<String, Object>();

      try {
         Long projectId = build.getConverterProjectId();
         boolean embedAuth = false; // because it doesn't work for IE

         map.put("dirRoot", _cwsClient.getProjectDirectoryRoot(projectId));
         map.put("projectDir", build.getLocationUrl(_dsClient, embedAuth));
         return map;
      }
      catch(CwsException ex) {
         throw new AfServerErrorException(ex);
      }
      catch(DsException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get registry root (only) settings for a build.
    *
    * @see #getBuildSettings(Long)
    *
    * @param buildId
    * @return
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/settings/registry",
         method=RequestMethod.GET)
   public @ResponseBody CwsSettingsRegKey getBuildSettingsRegistryRoot(
         @PathVariable Long buildId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);

      try {
         Long projectId = build.getConverterProjectId();
         return _cwsClient.getProjectRegistryRoot(projectId);
      }
      catch(CwsException ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get registry settings for a build project.
    * Since this data is loaded in sections (by key), you must specify
    * which key is needed by passing a registryId value.
    *
    * @param buildId
    * @param registryId
    * @return The specified registry key settings.
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/registry/{registryId}",
         method=RequestMethod.GET)
   public @ResponseBody CwsSettingsRegKey getBuildSettingsRegistryKey(
         @PathVariable Long buildId,
         @PathVariable Long registryId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);

      Long projectId = build.getConverterProjectId();

      try {
         CwsSettingsRegKey regKey = _cwsClient.getProjectRegistryKey(
               projectId,
               registryId);
         return regKey;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to get registry key for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get directory settings (files, attrs, subdirs) for a build project.
    * Since this data is loaded in sections (by directory), you must specify
    * which directory is needed by passing a directoryId value.
    *
    * @param buildId
    * @param directoryId
    * @return The specified directory.
    *
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/directory/{directoryId}",
         method=RequestMethod.GET)
   public @ResponseBody CwsSettingsDir getBuildSettingsDirectory(
         @PathVariable Long buildId,
         @PathVariable Long directoryId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);
      Long projectId = build.getConverterProjectId();

      try {
         CwsSettingsDir dir = _cwsClient.getProjectDirectory(projectId, directoryId);
         return dir;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to get directory for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Update (replace) package.ini settings for a build project.
    *
    * @param buildId
    * @param newPackageIni
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/packageIni",
         method=RequestMethod.PUT)
   public @ResponseBody IniDataRequest updateBuildPackageIni(
         @PathVariable Long buildId,
         @RequestBody IniDataRequest iniRequest)
      throws AfNotFoundException, AfServerErrorException, AfBadRequestException, AfConflictException
   {
      if (iniRequest.getRuntimeId() == null) {
         throw new AfBadRequestException("Missing runtimeId for this request");
      }

      iniRequest.setHzSupported(false);

      CwsSettingsIni newPackageIni = iniRequest.getPackageIni();
      Build build = findBuildAndValidate(buildId);

      try {
         Project project = _cwsClient.getProjectStatus(build.getConverterProjectId());
         boolean rtChanged = (iniRequest.getRuntimeId() != project.getRuntimeId());

         // Update CWS project settings
         if (rtChanged) {
            _cwsClient.updateThinAppRuntime(build.getConverterProjectId(), iniRequest.getRuntimeId());
         }

         /* Update CWS project settings */
         boolean iniChanged = _cwsClient.updateProjectPackageIni(
               build.getConverterProjectId(),
               newPackageIni);


         /* Update the last edited timestamp only if the state has changed. */
         if (iniChanged || rtChanged) {
            _buildService.updateBuildRuntimeHzSupport(iniRequest, build, rtChanged);
         }
         // Return the updated iniData back to user. This may contain updated info about hzSupported and hz entries.
         return iniRequest;
      }
      catch(CwsException ex) {
         throw new AfServerErrorException(ex);
      }
   }

   /**
    * API call which is used to create a new registry key for a
    * build's settings.
    *
    * The request must be a JSON object with the following properties:
    * parentId:
    * key:
    * @param requestData
    * @param buildId
    * @return The URL to the registry resource
    * @throws AfNotFoundException
    * @throws CwsException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/registry/new",
         method=RequestMethod.POST)
   public @ResponseBody String createBuildRegistryKey(
         @RequestBody CwsRegistryRequest requestData,
         @PathVariable Long buildId)
      throws
         AfNotFoundException,
         CwsException,
         AfConflictException
   {
      Build build = findBuildAndValidate(buildId);

      Long projectId = build.getConverterProjectId();

      /* Read request from client */
      return _cwsClient.createProjectRegistryKey(
            projectId,
            requestData);
   }


   /**
    * Update a project registry key.
    *
    * @param regKey
    * @param buildId
    * @param registryId
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    * @throws InvalidDataException
    */
   @RequestMapping(
         value="/builds/{buildId}/registry/{registryId}",
         method=RequestMethod.PUT)
   public @ResponseBody void updateBuildRegistryKey(
         @RequestBody CwsSettingsRegKey regKey,
         @PathVariable Long buildId,
         @PathVariable Long registryId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException,
             InvalidDataException
   {
      Build build = findBuildAndValidate(buildId);

      Long projectId = build.getConverterProjectId();

      try {
         boolean changed = _cwsClient.updateProjectRegistryKey(
               projectId,
               registryId,
               regKey);

         /* Update the last edited timestamp only if the state has changed */
         if (changed) {
            build.setSettingsEdited(AfCalendar.Now());
            BuildDao buildDao = _daoFactory.getBuildDao();
            buildDao.update(build);
         }

         return;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to update settings for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Update a project directory.
    *
    * @param dir
    * @param buildId
    * @param directoryId
    *
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    * @throws InvalidDataException
    */
   @RequestMapping(
         value="/builds/{buildId}/directory/{directoryId}",
         method=RequestMethod.PUT)
   public @ResponseBody void updateBuildDirectory(
         @RequestBody CwsSettingsDir dir,
         @PathVariable Long buildId,
         @PathVariable Long directoryId)
      throws
         AfNotFoundException, AfServerErrorException, AfConflictException,
         InvalidDataException
   {
      Build build = findBuildAndValidate(buildId);

      Long projectId = build.getConverterProjectId();

      try {
         boolean changed = _cwsClient.updateProjectDirectory(
               projectId,
               directoryId,
               dir);

         /* Update the last edited time stamp only if the state has changed */
         if (changed) {
            build.setSettingsEdited(AfCalendar.Now());
            BuildDao buildDao = _daoFactory.getBuildDao();
            buildDao.update(build);
         }

         return;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to update settings for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Delete a project registry key.
    *
    * @param buildId
    * @param registryId
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @RequestMapping(
         value="/builds/{buildId}/registry/{registryId}",
         method=RequestMethod.DELETE)
   public @ResponseBody void deleteBuildRegistryKey(
         @PathVariable Long buildId,
         @PathVariable Long registryId)
      throws AfNotFoundException, AfServerErrorException, AfConflictException
   {
      Build build = findBuildAndValidate(buildId);

      try {
         Long projectId = build.getConverterProjectId();
         _cwsClient.deleteProjectRegistryKey(projectId, registryId);
         return;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to update settings for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Force CWS to refresh it's internal state of a project's settings.
    * This is necessary, for example, if a user edits the project directory
    * by hand.
    *
    * @param buildId
    * @throws AfConflictException
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/builds/{buildId}/settings/refresh",
         method=RequestMethod.PUT)
   public @ResponseBody void refreshBuildSettings(
         @PathVariable Long buildId)
      throws AfNotFoundException, AfConflictException, AfServerErrorException
   {
      Build build = findBuildAndValidate(buildId);

      try {
         Long projectId = build.getConverterProjectId();
         _cwsClient.refreshProjectSettings(projectId);
         return;
      }
      catch(CwsException ex) {
         /* Internal error we weren't expecting */
         _log.error("Failed to refresh settings for build", ex);
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get a BuildDefineResponse instance from a set of applications that the
    * user wants to build. The returned instance will contain global data
    * and defaults for each application. The UI will use this to create a list of
    * build requests, and then call buildApplications().
    *
    * @see #buildApplications
    *
    * @param appIds
    * @return
    * @throws AfServerErrorException
    */
   @RequestMapping(
         value="/builds/define",
         method=RequestMethod.GET)
   public @ResponseBody BuildDefineResponse getDefaultBuildDefinitions(
         @RequestParam("appId") List<Long> appIds)
      throws AfServerErrorException
   {
      ApplicationDao appDao = _daoFactory.getApplicationDao();
      RecipeDao recipeDao = _daoFactory.getRecipeDao();
      BuildDefineResponse response = new BuildDefineResponse();

      /* Create a build request per app, which the user can modify */
      Collection<Application> apps = appDao.findAll(appIds, false);
      for (Application app : apps) {
         CaptureRequest cr = new CaptureRequestImpl(
               app.getId(),
               app.getIcons(),
               app.getDisplayName(),
               app.getSuggestedBuildName());
         BuildRequest br = new BuildRequest(
               app.getIcons(),
               cr,
               recipeDao.findMatchesForApp(app).toIdMap(false));

         response.getRequests().add(br);
      }

      /* Get the list of all workpools */
      try {
         BuildComponents buildComponents = _buildService.loadBuildComponents(false);
         response.setBuildComponents(buildComponents);
      }
      catch(WpException ex) {
         throw new AfServerErrorException(ex);
      }
      catch (DsException dse) {
         throw new AfServerErrorException(dse);
      }

      /* A list of all available recipes */
      response.setRecipes(_daoFactory.getRecipeDao().findAll());
      response.setHorizonEnabled(false);

      return response;
   }


   /**
    * Create a new datastore and import all projects from the datastore.
    *
    * @param request a new datastore request
    *
    * @throws AfBadRequestException
    * @throws AfConflictException If the requested name is already in use
    * @throws DsException
    */
   @RequestMapping(
         value="/builds/import",
         method=RequestMethod.POST)
   public @ResponseBody void importProjectsFromDatastore(
         @RequestBody ProjectImportRequest request)
      throws AfBadRequestException, DsException, AfConflictException
   {
      try {
         final DsDatastore ds = DsUtil.fromDTO(request.getDatastore());
         if (ds == null) {
            throw new AfBadRequestException("Unknown datastore type " + request.getDatastore().getType());
         }
         ds.setMountAtBoot(true);
         _dsClient.createDatastore(ds, true);

         final AppFactoryTask task = _taskFactory.newImportProjectTask(
               ds, request.getRuntimeId(), false);

         _conversionsQueue.addTask(task);
         _log.debug("Queued a new import project task [{}] from {} datastaore",
               task.getCurrentTaskState().getId(), ds.getName());
      }
      catch (DsNameInUseException dEx) {
         throw new AfConflictException("The given datastore name [" + request.getDatastore().getName() + "] is already in use!");
      }
      catch (Exception ex) {
         throw new AfBadRequestException(ex);
      }
   }

   /**
    * Helper utility used to verify if the buildId actually points
    * to a valid build.
    *
    * Also checks to see if the build is currently being rebuilt.
    *
    * @param buildId
    * @return
    * @throws AfNotFoundException if invalid buildId
    * @throws AfConflictException if rebuilding.
    */
   private Build findBuildAndValidate(Long buildId)
         throws AfNotFoundException, AfConflictException
   {
      // Get the build we're interested in
      BuildDao buildDao = _daoFactory.getBuildDao();
      Build build = buildDao.find(buildId);

      if (build == null) {
         throw new AfNotFoundException("Invalid build id " + buildId);
      }

      validateRebuildState(build);
      return build;
   }

   /**
    * Helper method to check if the build is currently being rebuilt.
    *
    * @param build
    * @throws AfConflictException if build state is REBUILDING
    */
   private void validateRebuildState(Build build)
         throws AfConflictException
   {
      // if build is rebuilding, throw exception.
      if (Build.Status.REBUILDING == build.getStatus()) {
         _log.info("No CRUD while rebuilding buildId: {}", build.getBuildId() );
         throw new AfConflictException("NO_CRUD_DURING_REBUILD");
      }
   }

   /**
    * Return builds that exists on known online datastores.
    *
    * @param builds Builds to look through
    * @return A filtered subset of builds on valid & online datastores
    */
   private List<Build> removeInvalidDatastoreBuilds(List<Build> builds, Map<Long, DsDatastore> datastoresMap)
   throws DsException {
      Iterator<Build> it = builds.iterator();
      List<Build> matches = new ArrayList<Build>();

      // Load all ds to avoid repeated loads.
      Map<Long, DsDatastore> dsMap = (datastoresMap == null)?
            _buildService.loadDatastoresMap(null) : datastoresMap;

      while (it.hasNext()) {
         Build build = it.next();

         DsDatastore ds = dsMap.get(build.getDatastoreId());
         if (ds == null || ds.getStatus() != Datastore.Status.online) {
            continue;
         }

         matches.add(build);
      }
      return matches;
   }
}
