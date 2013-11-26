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

package com.vmware.appfactory.datastore;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.vmware.appfactory.common.base.AbstractRestClient;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.datastore.exception.DsNameInUseException;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This is a client to the remote DataStore service.
 *
 * This client wraps all the RESTful HTTP API's provided by a datastore
 * service and provides a simple interface to datastore querying and
 * manipulation.
 *
 * Note: Since this is a client for webui specifically, all methods
 * accept and return DsDatastore instances. Internally, the Datastore DTO
 * class might be used, but is not exposed.
 */
@Service("datastoreClient")
public class DatastoreClientService
   extends AbstractRestClient
{
   /**
    * Datastore cache map: <datastore_id,datastore_instance>
    */
   @Nonnull
   private final Map<Long, DsDatastore> _dsCache;


   /**
    * Create a new Datastore client using the URL to the datastore service
    * as defined in the configuration instance.
    * Wherever possible use the bean instance instead.
    */
   public DatastoreClientService()
   {
      super(ConfigRegistryConstants.DATASTORE_SERVICE_URL);
      _dsCache = new HashMap<Long, DsDatastore>();
   }


   /**
    * When using the bean instance is not possible, this constructor
    * will create a new instance.
    *
    * @param config
    */
   public DatastoreClientService(ConfigRegistry config)
   {
      this();
      _config = config;
   }


   /**
    * Get a list of all datastores.
    *
    * @return A list of all datastores.
    * @throws DsException
    */
   public DsDatastore[] getAllDatastores()
      throws DsException
   {
      try {
         /* Fetch latest list from the server */
         Datastore[] dtos = _rest.getForObject(
               baseUrl() + "/storage",
               Datastore[].class);

         /* Cache these for later */
         DsDatastore[] datastores = DsUtil.fromDTO(dtos);
         _dsCache.clear();
         for (DsDatastore ds : datastores) {
            _dsCache.put(ds.getId(), ds);
         }

         return datastores;
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }

   @Nonnull
   public Collection<DsDatastore> getCachedDatastores() {
      return Collections.unmodifiableCollection(_dsCache.values());
   }

   /**
    * Get all writable (online and non-system) datastores from CWS.
    *
    * @return a list of DsDatastore.
    * @throws DsException if any error raised while invoking CWS.
    */
   public List<DsDatastore> getWritableDatastores() throws DsException {
      Builder<DsDatastore> dsBuilder = ImmutableList.builder();
      for (DsDatastore ds: getAllDatastores()) {
         boolean isOnLine = (Datastore.Status.online == ds.getStatus());
         if(!"system".equals(ds.getName()) && isOnLine) {
            dsBuilder.add(ds);
         }
      }
      return dsBuilder.build();
   }

   /**
    * See if a datastore exists with a given unique id.
    *
    * @param id Datastore id
    * @param useCache If true, look in the cache first.
    * @return True if the datastore id exists, false otherwise.
    * @throws DsException
    */
   public boolean datastoreExists(Long id, boolean useCache)
      throws DsException
   {
      if (useCache && _dsCache.containsKey(id)) {
         return true;
      }

      try {
         DsDatastore ds = findDatastore(id, useCache);
         return (ds != null);
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }


   /**
    * Get a datastore.
    * If 'useCache' is true, then a local cache is checked first, and returned
    * from there if found. If not found, or 'useCache' is false, the datastore
    * service is queried.
    *
    * @param id
    * @param useCache
    * @return The named datastore, or null if no such datastore exists.
    * @throws DsException
    */
   public DsDatastore findDatastore(Long id, boolean useCache)
      throws DsException
   {
      if (useCache && _dsCache.containsKey(id)) {
         return _dsCache.get(id);
      }

      try {
         Datastore dto = _rest.getForObject(
               baseUrl() + "/storage/{id}",
               Datastore.class,
               id);

         /* Convert DTO into a real datastore and cache it. */
         DsDatastore ds = DsUtil.fromDTO(dto);
         if (ds != null) {
            _dsCache.put(id, ds);
         }
         return ds;
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return null;
         }
         throw new DsException(ex);
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }


   /**
    * Delete a datastore.
    *
    * @param id - a unique datastore id to be deleted.
    * @throws DsException
    */
   public void deleteDatastore(Long id)
      throws DsException
   {
      try {
         _rest.delete(
               baseUrl() + "/storage/{id}",
               id);
         _dsCache.remove(id);
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }


   /**
    * Mark a datastore as being "online".
    *
    * @param id - a unique datastore id.
    * @throws DsException
    */
   public void setOnline(Long id)
      throws DsException
   {
      try {
         _rest.postForLocation(
               baseUrl() + "/storage/{id}/online",
               EMPTY_REQUEST,
               id);
         _dsCache.remove(id);
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }


   /**
    * Mark a datastore as being "offline".
    *
    * @param id - a unique datastore id.
    * @throws DsException
    */
   public void setOffline(Long id)
      throws DsException
   {
      try {
         _rest.postForLocation(
               baseUrl() + "/storage/{id}/offline",
               EMPTY_REQUEST,
               id);
         _dsCache.remove(id);
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }


   /**
    * Make a HTTP POST request to CWS to save the given
    * data store.
    *
    * @param datastore - a data store object.
    * @param makeOnline a boolean flag to make the datastore online.
    * @return new datastore id or -1 if the name is in use.
    * @throws DsException
    */
   public Long createDatastore(DsDatastore datastore, boolean makeOnline)
      throws DsException
   {
      /* Is the name in use? */
      if (isNameInUse(datastore.getName())) {
         throw new DsNameInUseException("The datastore [" + datastore.getName()
               + "] is in use!");
      }

      try {
         /* Create datastore, get URL to it */
         String endpoint = (makeOnline) ? "/storage?online=1" : "/storage";
         Datastore dto = (Datastore) datastore;
         URI newDatastoreUri = _rest.postForLocation(
               baseUrl() + endpoint,
               dto);

         /* What new ID was assigned? */
         String idStr = AfUtil.extractLastURIToken(newDatastoreUri);
         datastore.setId(Long.valueOf(idStr));

         /* Cache the actual new datastore */
         _dsCache.put(datastore.getId(), datastore);
         return datastore.getId();
      }
      catch (Exception ex) {
         throw new DsException(ex);
      }
   }

   /**
    * Make a HTTP POST request to CWS to save the given
    * data store.
    *
    * @param datastore - a data store object.
    * @throws DsException
    */
   public void updateDatastore(DsDatastore datastore)
      throws DsException
   {
      try {
         Datastore dto = (Datastore) datastore;
         _rest.put(baseUrl() + "/storage/" + dto.getId(), dto);
         _dsCache.remove(datastore.getId());
      }
      catch (RestClientException ex) {
         throw new DsException(ex);
      }
   }

   /**
    * Get default data store name.
    *
    * @return a data store name
    * @throws DsException
    */
   @Nullable
   public final DsDatastore getDefaultOutputDatastore()
      throws DsException
   {
      final DsDatastore[] allDs = getAllDatastores();
      for (DsDatastore ds : allDs) {
         setInternalDatastoreAsDefaultIfNotExist(ds);
         if (isDefault(ds.getId())) {
            return ds;
         }
      }

      return null;
   }


   /**
    * Check to see if a datastore is the default (for output).
    * @param dsId Datastore to check.
    * @return True if a default has been selected, and it's this one.
    */
   public boolean isDefault(Long dsId)
   {
      if (hasDefault()) {
         try {
            String str = _config.getString(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID);
            Long defId = Long.valueOf(str);
            return defId.equals(dsId);
         }
         catch(NumberFormatException ex) {
            /* The default was not an ID */
         }
      }
      return false;
   }


   /**
    * Check to see if there is a default datastore.
    * @return true if a default datastore is present; otherwise, return false.
    */
   public boolean hasDefault()
   {
      String idStr = _config.getString(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID);
      return StringUtils.isNotEmpty(idStr);
   }


   /**
    * Remove all entries from the cache.
    * All subsequent queries will hit CWS again, and be readded to the cache.
    */
   public void clearCache()
   {
      _dsCache.clear();
   }


   /**
    * Check whether the given dsName is in use or not.
    *
    * @return true if the dsName exists is in use; otherwise, return false.
    * @throws DsException
    */
   public boolean isNameInUse(String dsName)
      throws DsException
   {
      for (DsDatastore ds : getAllDatastores()) {
         if (ds.getName().equals(dsName)) {
            return true;
         }
      }

      return false;
   }


   /**
    * Set a given datastore as default if the name is 'internal'
    * @param ds
    */
   private void setInternalDatastoreAsDefaultIfNotExist(DsDatastore ds)
   {
      if (!hasDefault() && DsUtil.INTERNAL.equals(ds.getName())) {
         _config.setValue(ConfigRegistryConstants.DATASTORE_DEFAULT_OUTPUT_ID, ds.getId().toString());
      }
   }

}
