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

package com.vmware.appfactory.datastore.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datasource.dto.DatastoreUsageResponse;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.datastore.exception.DsNameInUseException;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.thinapp.common.datastore.dto.Datastore;


/**
 * This controller handles all the datastore-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@SuppressWarnings("MissortedModifiers")
@Controller
public class DatastoreApiController
   extends AbstractApiController
{
   /**
    * Get a JSON object containing all datastores, plus other related data.
    * @param status
    * @return A map containing keys "datastores" (array of datastores) and
    *         "default", the name of the default output datastore.
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores",
         method=RequestMethod.GET)
   public @ResponseBody Map<String,Object> getAllDatastores(
         @RequestParam(required=false) final Datastore.Status status)
      throws DsException
   {
      /* Get datastores, filter on status */
      final List<DsDatastore> filteredDs = new ArrayList<DsDatastore>();
      boolean hideOffLine = !_config.getBool(ConfigRegistryConstants.DATASTORE_SHOW_OFFLINE);

      for (DsDatastore ds : _dsClient.getAllDatastores()) {
             if (status != null && ds.getStatus() != status) {
                continue;
             }
             if (hideOffLine && Datastore.Status.offline == ds.getStatus()) {
                continue;
             }
             filteredDs.add(ds);
          }

      // XXX fix this!
      //Collections.sort(filteredDs);

      /* Put datastores, and other stuff, into the results map */
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("datastores", filteredDs);
      map.put("defaultId", _config.getString(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID));

      return map;
   }


   /**
    * Get a JSON description of a single datastore by passing its name.
    * If there is no datastore with that name, a 404 error is returned.
    *
    * @param id
    * @return
    * @throws AfNotFoundException
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores/{id}",
         method=RequestMethod.GET)
   public @ResponseBody DsDatastore getDatastore(
         @PathVariable Long id)
      throws AfNotFoundException, DsException
   {
      DsDatastore ds = _dsClient.findDatastore(id, false);
      if (ds == null) {
         throw new AfNotFoundException("Datastore \"" + id + "\" not found");
      }

      return ds;
   }


   /**
    * Create a new datastore.
    *
    * The request body must contain JSON object representing the common
    * datastore DTO.
    *
    * @param request
    *
    * @throws AfBadRequestException
    * @throws AfConflictException If the requested name is already in use
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores",
         method=RequestMethod.POST)
   public @ResponseBody void createDatastore(
         @RequestBody Datastore request)
      throws AfBadRequestException, DsException, AfConflictException
   {
      try {
         /*
          * Webui tracks a richer datastore class than that provided by the
          * common library: create one of those from the request.
          */
         DsDatastore ds = DsUtil.fromDTO(request);
         if (ds == null) {
            throw new AfBadRequestException("Unknown datastore type " + request.getType());
         }

         _dsClient.createDatastore(ds, true);
      }
      catch (DsNameInUseException dEx) {
         throw new AfConflictException("The given datastore name [" + request.getName() + "] is already in use!");
      }
   }


   /**
    * Update an existing datastore.
    *
    * If the specified datastore name is not found, a 404 error is returned.
    * Otherwise, that datastore is updated with the 'JsonDatastoreRequest'
    * from the request body.
    *
    * NOTE: Only some fields can be changed. Any fields in the request that
    * can't be changed are ignored.
    *
    * @param id
    * @param request
    * @throws AfBadRequestException
    * @throws AfNotFoundException
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores/{id}",
         method=RequestMethod.PUT)
   public @ResponseBody void updateDatastore(
         @PathVariable Long id,
         @RequestBody Datastore request)
      throws AfBadRequestException, AfNotFoundException, DsException
   {
      DsDatastore ds = _dsClient.findDatastore(id, false);

      if (ds == null) {
         throw new AfNotFoundException("Datastore \"" + id + "\" not found");
      }

      /* Update what we can in the datastore */
      ds.setUsername(request.getUsername());
      ds.setPassword(request.getPassword());
      ds.setMountAtBoot(request.isMountAtBoot());

      /* Save the DS */
      _dsClient.updateDatastore(ds);
   }


   /**
    * Delete a datastore.
    *
    * Deletes the named datastore. If the name is not recognized, a SC_NOT_FOUND
    * error is returned. If the datastore is online, or is the default
    * datastore, it cannot be deleted, and a SC_BAD_REQUEST error is returned.
    * @param id
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores/{id}",
         method=RequestMethod.DELETE)
   public @ResponseBody void deleteDatastore(
         Locale locale,
         @PathVariable Long id)
      throws AfNotFoundException, AfBadRequestException, AfForbiddenException, DsException
   {
      /* Check whether any file share is using this datastore or not. */
      final FileShare fs = _daoFactory.getFileShareDao().findByDatastoreId(id);
      if (fs != null) {
         throw new AfForbiddenException("'" + fs.getName()
               + "' file share is still using this datastore. " +
               "Please delete the file share first.");
      }

      DsDatastore ds = _dsClient.findDatastore(id, false);
      if (ds == null) {
         throw new AfNotFoundException(fr(locale, "M.STORAGE.ID_NOT_FOUND", id.toString()));
      }

      /* Can't delete if it's online. */
      if (ds.getStatus() == Datastore.Status.online) {
         throw new AfBadRequestException(tr(locale, "M.STORAGE.CANT_DELETE_IF_NOT_OFFLINE"));
      }

      /* Can't delete if it's the default. */
      if (_dsClient.isDefault(ds.getId())) {
         throw new AfBadRequestException(tr(locale, "M.STORAGE.CANT_DELETE_IF_DEFAULT"));
      }

      /** Delete all uploaded apps and projects from the database. **/
      _daoFactory.getApplicationDao().deleteUploadedApps(id);
      _daoFactory.getBuildDao().deleteAll(id);
      _dsClient.deleteDatastore(id);
   }


   /**
    * Change the status of a datastore, or make it the default. Only "online",
    * "offline" and "default" are accepted as valid values for "statusStr".
    *
    * The datastore status is changed to the one specified.
    * If the name if not the name of a known datastore, SC_NOT_FOUND is
    * returned. If the status is anything other than "online", and the datastore
    * is the default one, SC_BAD_REQUEST is returned. For any invalid status
    * value, SC_BAD_REQUEST is returned.
    *
    * @param id
    * @param statusStr
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws DsException
    */
   @RequestMapping(
         value="/datastores/{id}/{statusStr}",
         method=RequestMethod.PUT)
   public @ResponseBody void setDatastoreStatus(
         @PathVariable Long id,
         @PathVariable String statusStr)
      throws AfNotFoundException, AfBadRequestException, DsException
   {
      if (id == null) {
         throw new AfBadRequestException("Missing/invalid datastore ID");
      }

      DsDatastore ds = _dsClient.findDatastore(id, false);

      /* Make sure datastore exists */
      if (ds == null) {
         throw new AfNotFoundException("Datastore \"" + id + "\" not found");
      }

      if (statusStr.equals("default")) {
         _config.setValue(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID, ""+ds.getId());
      }
      else if (statusStr.equals("online")) {
         _dsClient.setOnline(id);
      }
      else if (statusStr.equals("offline")) {
         if (_dsClient.isDefault(ds.getId())) {
            /* Can't go offline if it's the default. */
            throw new AfBadRequestException("Default datastore can't be set to " + statusStr);
         }
         int numWaitingAndRunningProjects = _conversionsQueue.countActiveTasksByDatastoreId(id);
         if (numWaitingAndRunningProjects > 0) {
            throw new AfBadRequestException(
               String.format("You have %d work in progress task(s) using '%s' datastore. " +
               "So, switching to offline is not allowed.",
               numWaitingAndRunningProjects,
               ds.getName()));
         }
         _dsClient.setOffline(id);
      }
      else {
         throw new AfBadRequestException("Invalid status \"" + statusStr + "\"");
      }
   }

   /**
    * Check the datastore usage.
    * @param id a datastore id.
    * @return a DatastoreUsageResponse instance.
    */
   @RequestMapping(value = "/datastores/{id}/usage", method = RequestMethod.GET)
   public @ResponseBody DatastoreUsageResponse usage(@PathVariable Long id) {
      return new DatastoreUsageResponse(
            _daoFactory.getBuildDao().findByDatastore(id).size(),
            _daoFactory.getApplicationDao().findUploadedApps(id).size());
   }
}
