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

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.datasource.AppsAndRecipes;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.dto.FileShareRequest;
import com.vmware.appfactory.fileshare.dto.FileShareSyncResponses;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.appfactory.fileshare.service.FileShareService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This controller handles all the fileshare-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class FileShareApiController extends AbstractApiController {

   @Resource
   protected FileShareDao _fileShareDao;

   @Resource
   protected FileShareService _fileShareService;

   /**
    * Get a list of all the known fileshares.
    * @return
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(value = "/fileshare", method = RequestMethod.GET)
   @Nullable
   public List<FileShare> index(
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response
   )
         throws AfServerErrorException, IOException {
      if (checkModified(request,response, null, _fileShareDao)) {
         // shortcut exit - no further processing necessary
         return null;
      }

      try {
         List<FileShare> list = _fileShareDao.findAll();
         return list;
      } catch(Exception ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get a single fileshare, including all its applications.
    * The path variable can either be a fileshare ID or a fileshare name.
    * @param idOrName
    * @return
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/{idOrName}",
         method = RequestMethod.GET)
   public FileShare getFileShare(
         @PathVariable String idOrName) throws AfNotFoundException
   {
      FileShare fileshare = null;

      /* First see if we were given an ID */
      try {
         Long id = Long.valueOf(idOrName);
         fileshare = _fileShareDao.find(id);
      }
      catch(NumberFormatException ex) {
          /* Was not a number: perhaps a name? */
      }

      /* Then see if it is a name */
      if (fileshare == null) {
         fileshare = _fileShareDao.findByName(idOrName);
      }

      if (fileshare == null) {
         throw new AfNotFoundException("Invalid file share ID or name.");
      }
      return fileshare;
   }


   /**
    * Delete a file share.
    * @param id
    * @throws AfNotFoundException
    * @throws AfServerErrorException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/{id}",
         method = RequestMethod.DELETE)
   public void deleteFileShare(
         @PathVariable Long id)
      throws AfNotFoundException, AfBadRequestException, AfServerErrorException
   {
      deleteAppSourceRelated(_fileShareService.deleteFileShare(id));
   }


   /**
    * Update a file share.
    * @param id
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws AfForbiddenException
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/{id}",
         method = RequestMethod.PUT)
   public void updateFileShare(
         @PathVariable Long id,
         @RequestBody FileShareRequest request)
         throws AfBadRequestException, AfNotFoundException, AfConflictException, AfForbiddenException,
         AfServerErrorException, DsException, WpException {
      request.setFileShareId(id);

      try {
         /* Basic spec validation */
         request.validate();
      }
      catch(InvalidDataException ex) {
         throw new AfBadRequestException(ex);
      }
      final FileShare fs = _fileShareService.updateFileShare(request);
      if (request.isOkToConvert()) {
         createTasksForAllApps(fs);
      }
   }

   /**
    * Create a new fileshare.
    * @param fsRequest
    * @param request
    * @throws AfBadRequestException
    * @throws AfConflictException
    * @throws AfServerErrorException
    * @throws AfForbiddenException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/create",
         method = RequestMethod.POST)
   public void createFileShare(
         @RequestBody FileShareRequest fsRequest,
         HttpServletRequest request)
         throws AfBadRequestException, AfConflictException, AfServerErrorException, AfForbiddenException, DsException,
         AfNotFoundException, WpException {
      /* Basic spec validation */
      try {
         fsRequest.validate();
      }
      catch(InvalidDataException ex) {
         throw new AfBadRequestException(ex);
      }

      /* Name must be unique */
      if (_fileShareDao.findByName(fsRequest.getName()) != null) {
         throw new AfConflictException("Fileshare name already in use.");
      }

      final FileShare fs = _fileShareService.createFileShare(fsRequest);
      if (fsRequest.isOkToConvert()) {
         createTasksForAllApps(fs);
      }
   }

   /**
    * Scan a new fileshare.
    * @param spec
    * @param request
    * @return
    * @throws AfBadRequestException
    * @throws AfConflictException
    * @throws AfServerErrorException
    * @throws AfForbiddenException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/scan",
         method = RequestMethod.POST)
   public AppsAndRecipes scan(
         @RequestBody FileShareRequest spec,
         HttpServletRequest request)
      throws AfBadRequestException, AfConflictException, AfNotFoundException, AfServerErrorException, AfForbiddenException
   {
      /* Basic spec validation */
      try {
         spec.validate();
      } catch(Exception ex) {
         throw new AfBadRequestException(ex);
      }

      /* Name must be unique */
      if (_fileShareDao.findByName(spec.getName()) != null) {
         throw new AfConflictException("Fileshare name already in use.");
      }

      final FileShare fileshare = new FileShare();
      fileshare.setServerPath(spec.getServerPath());
      if (spec.isAuthRequired()) {
         fileshare.setUsername(spec.getAuthUsername());
         fileshare.setPassword(spec.getAuthPassword());
      }
      final AppsAndRecipes data = _fileShareService.scan(fileshare);
      return data;
   }

   /**
    * Re-scan an existing fileshare.
    * @param request
    * @return
    * @throws AfBadRequestException
    * @throws AfConflictException
    * @throws AfServerErrorException
    * @throws AfForbiddenException
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/sync",
         method = RequestMethod.POST)
   public FileShareSyncResponses sync(@RequestBody FileShareRequest request)
      throws AfBadRequestException, AfConflictException, AfServerErrorException, AfForbiddenException, AfNotFoundException
   {
      if (request.getFileShareId() != null) {
         _log.info("Syncing file share id - {}" , request.getFileShareId());
      }
      /* Basic spec validation */
      try {
         request.validate();
      } catch(Exception ex) {
         throw new AfBadRequestException(ex);
      }

      return _fileShareService.sync(request);

   }

   /**
    * Re-scan an existing fileshare.
    * @param fileshareId
    * @throws AfNotFoundException
    * @throws AfForbiddenException
    * @throws AfServerErrorException
    * @throws AfConflictException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/{fileshareId}/sync",
         method = RequestMethod.PUT)
   public void sync(
         @PathVariable Long fileshareId)
      throws AfNotFoundException, AfForbiddenException, AfConflictException, AfServerErrorException
   {
      FileShare fs = _daoFactory.getFileShareDao().find(fileshareId);
      if (fs == null) {
         throw new AfNotFoundException("Invalid fileshare id " + fileshareId);
      }

      FileShareRequest request = new FileShareRequest();
      request.setFileShareId(fileshareId);
      _fileShareService.sync(request);
   }


   /**
    * Reset a file share.
    * This will set a feed back to the state it was in when it was first
    * created. This means any error is removed, and the previous scan and
    * convert times are set to "never".
    *
    * @param id Feed to reset
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/fileshare/{id}/reset",
         method = RequestMethod.PUT)
   public void resetFileShare(
         @PathVariable Long id)
      throws AfNotFoundException
   {
      FileShareDao dao = _daoFactory.getFileShareDao();
      FileShare fileshare = dao.find(id);

      if (fileshare == null) {
         throw new AfNotFoundException("Invalid fileshare ID " + id);
      }

      fileshare.setFailure(null);
      fileshare.setLastConversion(AfCalendar.NEVER);
      fileshare.setLastScan(AfCalendar.NEVER);

      dao.update(fileshare);
   }

   private void createTasksForAllApps(FileShare fs) throws AfNotFoundException, WpException, DsException {
      for (Application app : fs.getApplications()) {
         CaptureRequestImpl cr = new CaptureRequestImpl(
               app.getId(),
               app.getIcons(),
               app.getDisplayName(),
               fs.getName(),
               _wpClient.getDefault().getId(),
               _dsClient.getDefaultOutputDatastore().getId(),
               _config.getDefaultRuntime(),
               false,   // Horizon is not enabled by default.
               _taskFactory.getTaskHelperFactory()
         );
         AppFactoryTask task = _taskFactory.newAppConvertTask(cr, _conversionsQueue);
         _conversionsQueue.addTask(task);
         _log.debug("Task for " + app.getDisplayName() + " added to queue");
      }
   }
}
