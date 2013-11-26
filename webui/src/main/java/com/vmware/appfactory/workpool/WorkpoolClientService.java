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

package com.vmware.appfactory.workpool;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.vmware.appfactory.common.base.AbstractRestClient;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This is the main interface to the workpool service on the appliance.
 * Instead of using Workpool REST API at random locations throughout the webui,
 * they are all wrapped in methods inside this class.
 */
@Service("workpoolClient")
public class WorkpoolClientService
   extends AbstractRestClient
{
   /**
    * Create a new instance.
    * Used to instantiate the service bean.
    */
   public WorkpoolClientService()
   {
      super(ConfigRegistryConstants.WORKPOOL_SERVICE_URL);
   }


   /**
    * When using the bean instance is not possible, this constructor
    * will create a new instance.
    *
    * @param config
    */
   public WorkpoolClientService(ConfigRegistry config)
   {
      this();
      _config = config;
   }


   /**
    * Get all known workpools.
    * @return
    * @throws WpException
    */
   public List<Workpool> getAllWorkpools()
      throws WpException
   {
      try {
         Workpool[] objects = _rest.getForObject(
               baseUrl() + "/workpools",
               Workpool[].class);

         return Arrays.asList(objects);
      }
      catch (RestClientException ex) {
         _log.error("Failed to load workpools from workpool service", ex);
         throw new WpException(ex);
      }
      catch (Exception ex) {
         _log.error("Failed to get workpools from workpool service", ex);
         throw new WpException(ex);
      }
   }


   /**
    * Returns the workpool identified by the id passed.
    *
    * @param id
    * @return
    * @throws WpException
    */
   public Workpool getWorkpoolById(Long id)
      throws WpException, AfNotFoundException
   {
      if (id == null) {
         throw new WpException("Invalid input");
      }
      try {
         return _rest.getForObject(
               baseUrl() + "/workpools/{id}",
               Workpool.class,
               id);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException("NOT_FOUND: " + ex.getMessage());
         }
         throw new WpException(ex);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Returns the workpool identified by the parameter name.
    *
    * @TODO Have a new API that gets workpool by name. For now, getting a list
    * and matching names.
    *
    * @param name
    * @return
    * @throws WpException
    */
   public Workpool getWorkpoolByName(String name)
      throws WpException, AfNotFoundException
   {
      // Validate input.
      if (StringUtils.isEmpty(name)) {
         throw new WpException("Invalid input");
      }
      List<Workpool> wpList = getAllWorkpools();
      if (!CollectionUtils.isEmpty(wpList)) {
         for (Workpool w : wpList)
            if (name.equals(w.getName())) {
               return w;
            }
      }
      throw new AfNotFoundException("NOT_FOUND: " + "Workpool '" + name + " is not found");
   }


   /**
    * Deletes the workpool by id
    *
    * @param workpoolId
    * @param deleteMethod
    */
   public void deleteWorkpool(Long workpoolId, DeleteMethod deleteMethod)
      throws WpException
   {
      try {
         _rest.delete(
               baseUrl() + "/workpools/{workpoolId}?deleteMethod={deleteMethod}",
               workpoolId,
               deleteMethod);
      }
      catch (RestClientException e) {
         _log.error("Cannot delete workpool id: " + workpoolId
               + "due to: " + e.getMessage());
         throw new WpException(e);
      }
   }


   /**
    * Acquires the lease of a workpool instance.
    *
    * @TODO Write simulator if planning on using this.
    *
    * @param workpoolId
    * @return
    * @throws WpException
    */
   public Lease acquire(Long workpoolId)
      throws WpException
   {
      try {
         return _rest.postForObject(
               baseUrl() + "/workpools/leases/acquire/{workpoolId}",
               "",
               Lease.class,
               workpoolId);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Releases a previously acquired lease of a workpool instance.
    *
    * @TODO Write simulator if planning on using this.
    * @TODO Should this really be a POST or PUT? Verify if plan to use this.
    *
    * @param workpoolId
    * @param leaseId
    * @throws WpException
    */
   public void release(Long workpoolId, Long leaseId)
      throws WpException
   {
      try {
         _rest.postForObject(
               baseUrl() + "/workpools/{workpoolId}/leases/{id}/release",
               null,
               null,
               workpoolId,
               leaseId);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Used to create a workpool. This call can create workpools of Full,
    * linked, Custom types.
    *
    * NOTE: The created workpool or the Id is not returned as its not needed
    * at this point in time. If needed @see VmImage.createVmImage()
    *
    * @param workpool
    * @return
    * @throws WpException, AfConflictException
    */
   public Long createWorkpool(Workpool workpool)
      throws WpException, AfConflictException
   {
      try {
         // This method does not return the
         URI uri = _rest.postForLocation(
               baseUrl() + "/workpools",
               workpool);

         return Long.valueOf(AfUtil.extractLastURIToken(uri));
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            throw new AfConflictException("CONFLICT_NAME: " + ex.getMessage());
         }
         throw new WpException("Workpool cannot be created: " + ex.getMessage());
      }
      catch (RestClientException e) {
         throw new WpException("Workpool cannot be created: " + e.getMessage());
      }
   }


   /**
    * Used to update a VM Image. Name field can be updated.
    *
    * @param workpool
    * @throws WpException
    */
   public void updateWorkpool(Workpool workpool)
      throws WpException, AfNotFoundException, AfConflictException
   {
      Long id = workpool.getId();
      try {
         _rest.put(baseUrl() + "/workpools/{id}", workpool, id);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException("NOT_FOUND: " + ex.getMessage());
         }
         else if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            throw new AfConflictException("CONFLICT_NAME: " + ex.getMessage());
         }
         throw new WpException("Workpool cannot be updated: " + ex.getMessage());
      }
      catch (RestClientException e) {
         throw new WpException("Workpool cannot be updated: " + e.getMessage());
      }
   }


   /*****
    * The following methods constitute for the management of VM Image.
    * These should be abstracted in the backend, so the transactions across creation
    * of Image and workpool happen together.
    *****/

   /**
    * Get all known VM images.
    * @return
    * @throws WpException
    */
   public List<VmImage> getAllImages()
      throws WpException
   {
      try {
         VmImage[] objects = _rest.getForObject(
               baseUrl() + "/vmimages",
               VmImage[].class);

         return Arrays.asList(objects);
      }
      catch (RestClientException ex) {
         _log.error("Failed to get images from VmImage service", ex);
         throw new WpException(ex);
      }
      catch (Exception ex) {
         _log.error("Failed to get images from VmImage service", ex);
         throw new WpException(ex);
      }

   }


   /**
    * Returns the VmImage identified by the id passed.
    *
    * @param id
    * @return
    * @throws WpException
    */
   public VmImage getVmImageById(Long id)
      throws WpException, AfNotFoundException
   {
      if (id == null) {
         throw new WpException("Empty input");
      }
      try {
         return _rest.getForObject(
               baseUrl() + "/vmimages/{id}",
               VmImage.class,
               id);
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new AfNotFoundException("NOT_FOUND: " + ex.getMessage());
         }
         throw new WpException(ex);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Returns the VmImage identified by the parameter name.
    *
    * @TODO Eventually have a REST API to do this look up. For now using a less
    * performant way. Leaving the simulator as is to support lookup by name.
    *
    * @param name
    * @return
    * @throws WpException
    */
   public VmImage getVmImageByName(String name)
      throws WpException, AfNotFoundException
   {
      // Validate input.
      if (name == null) {
         throw new WpException("Empty input");
      }
      List<VmImage> imageList = getAllImages();
      if (!CollectionUtils.isEmpty(imageList)) {
         for (VmImage i : imageList)
            if (name.equals(i.getName())) {
               return i;
            }
      }
      throw new AfNotFoundException("NOT_FOUND: " + "VM Image '" + name + "' is not found");
   }



   /**
    * Deletes the VmImage by id
    *
    * @param imageId
    * @param deleteMethod
    */
   public void deleteVmImage(Long imageId, DeleteMethod deleteMethod)
      throws WpException
   {
      try {
         _rest.delete(
               baseUrl() + "/vmimages/{imageId}?deleteMethod={deleteMethod}",
               imageId,
               deleteMethod);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Used to create a VM Image.
    *
    * @param image
    * @return
    * @throws WpException
    */
   public Long createVmImage(VmImage image)
      throws WpException
   {
      try {
         URI uri = _rest.postForLocation(
               baseUrl() + "/vmimages",
               image);

         if (uri == null) {
            _log.debug("uri is null");
            throw new WpException("uri is null after creating vmimage");
         }
         return Long.valueOf(AfUtil.extractLastURIToken(uri));
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Used to update a VM Image. Name field can be updated.
    *
    * @param image
    * @throws WpException
    */
   public void updateVmImage(VmImage image)
      throws WpException
   {
      Long id = image.getId();
      try {
         _rest.put(baseUrl() + "/vmimages/{id}", image, id);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * A helper method that checks if the default is not set, and if so,
    * sets the default to the workpoolId passed.
    *
    * @param workpoolId
    */
   public void setIfNoDefaultExists(Long workpoolId)
   {
      if(!hasDefault()) {
         _config.setValue(
               ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL,
               workpoolId.toString());
      }
   }


   /**
    * Helper method that fetches the default workpool if one exists.
    *
    * @return
    * @throws WpException
    * @throws AfNotFoundException - When no default workpool is set.
    */
   @Nonnull
   public Workpool getDefault()
      throws WpException, AfNotFoundException
   {
      if (hasDefault()) {
         Long defaultWp = Long.valueOf(_config.getLong(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL));
         return getWorkpoolById(defaultWp);
      }
      throw new AfNotFoundException("Default workpool does not exist");
   }


   /**
    * Check if the param is the default.
    * @param workpool
    * @return True if a default has been selected, and is the one passed.
    */
   public boolean isDefault(Workpool workpool)
   {
      if (workpool == null) {
         return false;
      }
      String configKey = _config.getString(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL);
      return String.valueOf(workpool.getId()).equals(configKey);
   }


   /**
    * Check whether a default workpool exists or not.
    * @return true if the default workpool is not empty
    */
   public boolean hasDefault()
   {
      /* Set the first workpool as default */
      String defaultWp = _config.getString(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL);
      return !StringUtils.isEmpty(defaultWp);
   }

}
