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

package com.vmware.appfactory.fileshare.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datasource.AppsAndRecipes;
import com.vmware.appfactory.datasource.DataSourceObject;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsDatastoreCifs;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.fileshare.CifsHelper;
import com.vmware.appfactory.fileshare.IFeedConverter;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.dto.ApplicationInfoDelta;
import com.vmware.appfactory.fileshare.dto.FileShareRequest;
import com.vmware.appfactory.fileshare.dto.FileShareSyncResponse;
import com.vmware.appfactory.fileshare.dto.FileShareSyncResponses;
import com.vmware.appfactory.fileshare.exception.FeedConverterException;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * This class implements Cifs file share scanning, meta-data mapping based on pre-defined
 * directory layout and handles user's meta-data overrides. It also supports
 * deleting and updating existing file shares.
 *
 * @author saung
 * @since M7 7/26/2011
 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService
{
   /**
    * get the logger
    */
   private static final Logger _log = LoggerFactory.getLogger(FileShareServiceImpl.class);

   @Resource
   protected FileShareDao _fileShareDao;

   @Resource(name="cifsToAppsFeedConverter")
   protected IFeedConverter<Application> _appFeedConverter;

   @Resource(name="cifsToRecipesFeedConverter")
   protected IFeedConverter<Recipe> _recipeFeedConverter;

   @Resource
   protected DatastoreClientService _dsClient;

   @Resource
   protected ConfigRegistry _config;

   /**
    * @see com.vmware.appfactory.fileshare.service.FileShareService#scan(com.vmware.appfactory.fileshare.model.FileShare)
    */
   @Override
   public AppsAndRecipes scan(FileShare fileshare)
      throws AfNotFoundException, AfForbiddenException, AfServerErrorException
   {
      fileshare.setLastScan(AfCalendar.Now());
      String smbFormatUrl = CifsHelper.getSMBUrl(fileshare.getServer(), fileshare.getPath());
      String[] domainUser = CifsHelper.parseDomainAndUsername(fileshare.getUsername());
      String domain = domainUser[0];
      String username = domainUser[1];
      _log.debug("Scanning SMB URL " + smbFormatUrl + ". [domain:username]=[" + domain + ":" + username + "]");

      try {
         /* Scan for applications */
         final List<Application> appList = _appFeedConverter.scanObjects(smbFormatUrl,
               domain, username, fileshare.getPassword(),
               fileshare.getLastScan(), fileshare.getDatastoreId());
         if (appList != null) {
            _log.debug("Found " + appList.size() + " applications");
         }

         /* Scan for recipes */
         final List<Recipe> recipeList = _recipeFeedConverter.scanObjects(smbFormatUrl,
               domain, username, fileshare.getPassword(),
               fileshare.getLastScan(), fileshare.getDatastoreId());
         if (recipeList != null) {
            _log.debug("Found " + recipeList.size() + " recipes");
         }

         fileshare.setStatus(FileShare.Status.SCANNED);

         AppsAndRecipes data = new AppsAndRecipes();
         data.setApplications(appList);
         data.setRecipes(recipeList);
         return data;
      }
      catch(FeedConverterException ex) {
         switch(ex.getErrorCode()) {
            case AccessDenied:
               throw new AfForbiddenException(ex);
            case NotFound:
               throw new AfNotFoundException(ex);
            case Other:
               throw new AfServerErrorException(ex);
         }
         // Should not reach this, but just in case...
         throw new AfServerErrorException(ex);
      }
   }

   /**
    * @see com.vmware.appfactory.fileshare.service.FileShareService#createFileShare(FileShareRequest) )
    */
   @Override
   public FileShare createFileShare(FileShareRequest request)
      throws AfServerErrorException, AfForbiddenException {

      try {
         /* Create a new file share */
         final FileShare fileshare = new FileShare();
         fileshare.setName(request.getName());
         fileshare.setOkToScan(true);
         fileshare.setOkToConvert(request.isOkToConvert());
         setAuthentication(request, fileshare);
         fileshare.setServerPath(request.getServerPath());
         fileshare.setDatastoreName(request.getName());

         /**
          * Need to create new datastore first before app scan so that
          * the right datastore id will be set in _absoluteurl of appDownload.
          */
         DsDatastore ds = new DsDatastoreCifs(
               fileshare.getDatastoreName(),
               fileshare.getServer(),
               fileshare.getPath(),
               null, // no domain
               fileshare.getUsername(),
               fileshare.getPassword(),
               null); // No mountPath yet, but it will be set after creating a new datastore in CWS.

         // XXX: Creating datastore without going online due to bug 789053 to allow us
         // to finish creating this fileshare.
         _dsClient.createDatastore(ds, false);
         fileshare.setDatastoreId(ds.getId());

         AppsAndRecipes data = scan(fileshare);
         updateAppsWithDelta(data.getApplications(), request.getAppsToSkip(), request.getAppDeltas());

         fileshare.setApplications(data.getApplications());
         fileshare.setRecipes(data.getRecipes());

         /* Now create a new fileshare */
         _fileShareDao.create(fileshare);
         return fileshare;
      } catch(AfForbiddenException e) {
         throw e;
      } catch(Exception ex) {
         throw new AfServerErrorException(ex);
      }

   }

   /**
    * @throws AfServerErrorException
    * @see com.vmware.appfactory.fileshare.service.FileShareService#updateFileShare(FileShareRequest)
    */
   @Override
   public FileShare updateFileShare(FileShareRequest request)
      throws AfNotFoundException, AfConflictException, AfForbiddenException, AfServerErrorException
   {
      if (request == null) {
         throw new IllegalArgumentException("request is null");
      }

      /* Get fileshare */
      final FileShare fileshare = _fileShareDao.find(request.getFileShareId());
      if (fileshare == null) {
         throw new AfNotFoundException("Invalid file share ID.");
      }

      /* Update basic file share settings */
      updateFileShareSettings(request, fileshare);

      // Rescan fileshare
      AppsAndRecipes newData = scan(fileshare);

      // Update applications in the fileshare
      final List<Application> existingApps = fileshare.getApplications();
      final FileShareSyncResponse<Application> result = reconcile(
            existingApps,
            newData.getApplications(),
            new ApplicationHelper(),
            isForceOverride());
      if (result != null) {
         final List<Application> newAndUpdatedApps = result.getUpdatedItems();
         newAndUpdatedApps.addAll(result.getNewItems());
         updateAppsWithDelta(
               newAndUpdatedApps,
               request.getAppsToSkip(),
               request.getAppDeltas());
      }

      // Update recipes in the fileshare
      final List<Recipe> existingRecipes = fileshare.getRecipes();
      final FileShareSyncResponse<Recipe> result2 = reconcile(
            existingRecipes,
            newData.getRecipes(),
            new RecipeHelper(),
            true);
      if (result2 != null) {
         final List<Recipe> newAndUpdatedRecipes = result2.getUpdatedItems();
         newAndUpdatedRecipes.addAll(result2.getNewItems());
      }

      /* Save the fileshare */
      _fileShareDao.update(fileshare);
      return fileshare;
   }

   /**
    * Update file share settings like description, share path and login info.
    * @param request
    * @param fileshare
    * @throws AfConflictException
    */
   private void updateFileShareSettings(FileShareRequest request,
         FileShare fileshare) throws AfConflictException {
      /* Make sure the name is not used by another fileshare */
      FileShare namedFileShare = _fileShareDao.findByName(request.getName());
      if (namedFileShare != null && !namedFileShare.getId().equals(request.getFileShareId())) {
         throw new AfConflictException("Fileshare name already in use.");
      }

      /* Update the fileshare: basics */
      fileshare.setName(request.getName());
      fileshare.setDescription(request.getDescription());
      fileshare.setServerPath(request.getServerPath());

      /* Update the file share: Options */
      fileshare.setOkToScan(true);
      fileshare.setOkToConvert(request.isOkToConvert());

      setAuthentication(request, fileshare);
   }

   /**
    * @see com.vmware.appfactory.fileshare.service.FileShareService#deleteFileShare(Long)
    */
   @Override
   @Nullable
   public FileShare deleteFileShare(Long fileShareId)
      throws AfNotFoundException, AfBadRequestException, AfServerErrorException {
      final FileShare fileshare = _fileShareDao.find(fileShareId);
      if (fileshare == null) {
         throw new AfNotFoundException("Invalid file share ID.");
      }

      boolean removeFromDb;
      try {
         removeFromDb = deleteDataStore(fileshare.getDatastoreId());
      } catch (AfNotFoundException e) {
         // if the backend doesn't know about it, but it is in the
         // frontend database, then just remove it from our database
         removeFromDb = true;
      }
      if (removeFromDb) {
         _fileShareDao.delete(fileshare);
         return fileshare;
      }
      return null;
   }

   /**
    * Set the file share: authentication.
    * @param spec
    * @param fileshare
    */
   protected void setAuthentication(FileShareRequest spec,
         FileShare fileshare) {
      if (spec.isAuthRequired()) {
         fileshare.setUsername(spec.getAuthUsername());
         fileshare.setPassword(spec.getAuthPassword());
      } else {
         fileshare.setUsername("");
         fileshare.setPassword("");
      }
   }

   /**
    * Update application info with delta.
    * @param apps - a list of apps from a file share.
    * @param toBeSkipped - a list of installers (with full path) to be skipped.
    *    E.g. /path/to/installer.exe (path cannot start with 'datastore://')
    * @param delta - a list of app info changes that will overwrite the app info from
    *   file share's directory-layout-to-meta-data mapping.
    */
   private final void updateAppsWithDelta(
         List<Application> apps,
         List<String> toBeSkipped,
         List<ApplicationInfoDelta> delta) {
      if (CollectionUtils.isEmpty(apps)) {
         return;
      }
      Iterator<Application> it = apps.iterator();
      Application app = null;

      while (it.hasNext()) {
         app = it.next();
         boolean hasSkip = !CollectionUtils.isEmpty(toBeSkipped);
         boolean hasDelta = !CollectionUtils.isEmpty(delta);

         if (!hasSkip && !hasDelta) {
            /* if both toBeSkipped and delta lists are empty, then exit the loop.*/
            break;
         } else if (hasSkip && toBeSkipped.remove(app.getDownload().getURI().getPath())) {
            it.remove();
         } else if (hasDelta) {
               ApplicationInfoDelta tempDelta = new ApplicationInfoDelta();
               // Metadata is keyed on full path to installer name
               String installerPath = app.getDownload().getURI().getPath();
               tempDelta.setKey(installerPath);

               int index = delta.indexOf(tempDelta);
               if (index > -1) {
                  tempDelta = delta.get(index);
                  tempDelta.copyToApplication(app);
                  app.setOverrideMetadata(true);
                  delta.remove(index);
               }
         }
      } // end while loop

   }

   /**
    * FIXME: merge into DatastoreClientService.
    * Delete data store from CWS.
    *
    * @param dsId - a datastore ID
    * @return true if the deletion was successful; return false otherwise.
    *
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws AfServerErrorException
    */
   private final boolean deleteDataStore(Long dsId)
      throws AfNotFoundException, AfBadRequestException, AfServerErrorException {
      DsDatastore ds = null;
      try {
         ds = _dsClient.findDatastore(dsId, false);

         if (ds == null) {
            throw new AfNotFoundException("DataStore:" + dsId + " not found.");
         }

         /* Can't delete if it's the default. */
         if (_dsClient.isDefault(ds.getId())) {
            throw new AfBadRequestException("Can't delete the default datastore");
         }

         /* Check if it's online. */
         if (ds.getStatus() == Datastore.Status.online) {
            /**
             * Force to switch it to offline. CWS should throw an
             * exception if the datastore is being used by any conversion request.
             */
            _log.info("Trying to switch datastore to offline.");
            _dsClient.setOffline(dsId);
            _log.info("Datastore is now offline");
         }

         _dsClient.deleteDatastore(dsId);
         return true;
      } catch (DsException e) {
         _log.error("Failed to delete the datastore - " + dsId , e);
         throw new AfServerErrorException("Failed to delete the datastore - " + dsId);
      }
   }

   /**
    * @see com.vmware.appfactory.fileshare.service.FileShareService#sync(FileShareRequest) )
    */
   @Override
   public FileShareSyncResponses sync(FileShareRequest request)
      throws AfNotFoundException, AfConflictException, AfForbiddenException, AfServerErrorException
   {
      if (request == null) {
         throw new IllegalArgumentException("request is null!");
      }
      final FileShare fs = _fileShareDao.find(request.getFileShareId());
      if (fs == null) {
         throw new IllegalArgumentException("Invalid file share id " + request.getFileShareId());
      }

      if (StringUtils.hasLength(request.getServerPath())) {
         fs.setServerPath(request.getServerPath());
      }
      if (request.isAuthRequired() &&
            StringUtils.hasLength(request.getAuthUsername())) {
         fs.setUsername(request.getAuthUsername());
         fs.setPassword(request.getAuthPassword());
      }
      // Load existing applications from the DB.
      final List<Application> existingApps = fs.getApplications();
      final List<Recipe> existingRecipes = fs.getRecipes();

      // Scan apps and recipes from file share.
      AppsAndRecipes newData = scan(fs);

      // Reconcile new and existing data
      FileShareSyncResponses result = new FileShareSyncResponses();

      result.appSyncResponse = reconcile(
            existingApps,
            newData.getApplications(),
            new ApplicationHelper(),
            isForceOverride());

      result.recipeSyncResponse = reconcile(
            existingRecipes,
            newData.getRecipes(),
            new RecipeHelper(),
            true);

      return result;
   }


   /**
    * Reconcile existing application meta-data with updates from the current scan.
    * It uses absolute path with installer name as unique key to differentiate apps.
    * If a given scanned apps list is empty, then return null, but it will return newly
    * scanned apps as existing apps if there is no existing app. Otherwise, it will do:
    *   1. Go through scanned apps and update meta-data of existing apps
    *    if new meta-data isn't empty.
    *   2. Add all new apps that don't exist in the existing database to the new apps list.
    *   3. Add all existing apps that aren't not found in the newly scanned list to the deleted list.
    *
    * @param existingItems - a list of existing apps from the database.
    * @param scannedItems - a list of apps from a recent file share scan.
    * @return a FileShareSyncResponse instance.
    */
   private final <T extends DataSourceObject> FileShareSyncResponse<T> reconcile(
         List<T> existingItems,
         List<T> scannedItems,
         DataSourceObjectHelper<T> helper,
         boolean overrideExistingItems)
   {
      // If no items found in the file share scan, then return null.
      if (CollectionUtils.isEmpty(scannedItems)) {
         return null;
      }

      final List<T> newItems = new ArrayList<T>();

      // Convert existing items list into a map for efficient lookup.
      final Map<String, T> existingItemsMap = helper.toKeyMap(existingItems);

      for (T item : scannedItems) {
         // Check if it already exists.
         String key = helper.getMapKey(item);
         if (existingItemsMap.containsKey(key)) {
            /**
             * Update meta-data if either of the conditions is true:
             *  1. Existing item's metadata is never updated.
             *  2. The 'overrideExistingItems' flag is true.
             */
            T existingItem = existingItemsMap.get(key);
            if (overrideExistingItems || !helper.isMetadataModified(existingItem)) {
               helper.copy(item, existingItem);
            }
            // Remove existing app from the map.
            existingItemsMap.remove(key);
         } else {
            // Put it into the new apps list
            newItems.add(item);
         }
      } // end for loop

      // Add remaining apps from the map to the deleted list.
      final List<T> deletedApps = new ArrayList<T>(existingItemsMap.values());
      // Remove deleted apps from the existing apps list.
      existingItems.removeAll(deletedApps);

      return new FileShareSyncResponse<T>(newItems, existingItems, deletedApps);
   }


   /**
    * Since DataSourceObject instances could be pretty much anything, we need
    * a helper to implement various methods we need during file share scanning.
    * @param <T>
    */
   abstract static class DataSourceObjectHelper<T extends DataSourceObject>
   {
      public Map<String,T> toKeyMap(List<T> items)
      {
         final Map<String, T> map = new HashMap<String, T>();

         if (items != null) {
            for (T item : items) {
               map.put(getMapKey(item), item);
            }
         }

         return map;
      }

      /**
       * Return a key which we can use to create a map of all items, each
       * item having it's own unique key.
       * @param item
       * @return
       */
      public abstract String getMapKey(T item);

      /**
       * Perform a copy from one DataSourceObject item to another.
       * @param source
       * @param dest
       */
      public abstract void copy(T source, T dest);

      /**
       * Check whether a given item's metadata is modified.
       * @param item
       * @return true if the metadata is modified; otherwise, return false.
       */
      public abstract boolean isMetadataModified(T item);
   }


   static final class ApplicationHelper
      extends DataSourceObjectHelper<Application>
   {
      @Override
      public String getMapKey(Application app) {
         return app.getDownload().getURI().toString();
      }

      @Override public void copy(Application source, Application dest) {
         dest.deepCopy(source);
      }

      @Override public boolean isMetadataModified(Application app) {
         return app.isOverrideMetadata();
      }
   }


   static final class RecipeHelper
      extends DataSourceObjectHelper<Recipe>
   {
      @Override
      public String getMapKey(Recipe recipe) {
         return recipe.getName();
      }

      @Override public void copy(Recipe source, Recipe dest) {
         dest.deepCopy(source);
      }

      @Override public boolean isMetadataModified(Recipe recipe) {
         return false;
      }
   }

   /**
    * Check whether metadata override flag is ON or not.
    * @return
    */
   private boolean isForceOverride()
   {
      return _config.getBool(ConfigRegistryConstants.FILESHARE_OVERRIDE_APP_INFO_IN_RESCAN);
   }

}
