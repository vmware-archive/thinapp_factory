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

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.vmware.appfactory.common.base.AbstractRestClient;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.config.ConfigRegistry.viTypes.VIType;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.thinapp.common.workpool.dto.VCConfig.ApiType;
import com.vmware.thinapp.common.workpool.dto.VCConfig.CloneSupport;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This is the interface to the virtual infrastructure service.
 * This consolidates all the VI REST API calls and channels them through here.
 */
@Service("viClient")
public class VIClientService
   extends AbstractRestClient
{
   private VCConfig _vcConfigCache;

   /**
    * Create a new instance.
    * Used to instantiate the service bean.
    */
   public VIClientService()
   {
      super(ConfigRegistryConstants.WORKPOOL_SERVICE_URL);
   }


   /**
    * Get the VCConfig via GET /config
    *
    * This VCConfig contains info regarding the type of virtual infrastructure: VC / WS.
    * and whether clone is supported in the VC or not. (WS does not support clones)
    *
    * @return VCConfig object
    * @throws WpException
    */
   public VCConfig getVCConfiguration(boolean useCache)
      throws WpException
   {
      if(useCache && getVcConfigCache() != null) {
         return getVcConfigCache();
      }
      try {
         VCConfig config = _rest.getForObject(
               baseUrl() + "/config",
               VCConfig.class);
         // Set the cache with the latest vcConfig object.
         setVcConfigCache(config);
         return config;
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Check if the virtual infrastructure supports clone.
    *
    * NOTE: Workstations do not support clone /cloning can be disabled in VC.
    *
    * @return boolean
    * @AfServerErrorException
    * @throws AfServerErrorException
    */
   public boolean checkCloneSupport(boolean useCache)
      throws AfServerErrorException
   {
      // During dev mode, cloneSupport is set as part of VI type.
      if (_appFactory.isDevModeDeploy()) {
         // Check configuration for viType : VC or WS and their clone support.
         String viType = _config.getString(ConfigRegistryConstants.WORKPOOL_VI_TYPE);
         // Return true only if VIType is vc which has clone support.
         return VIType.vc.name().equalsIgnoreCase(viType);
      }

      try {
         CloneSupport clonesupport = getVCConfiguration(useCache).getCloneSupport();

         // If the system is in an unrecoverable state, throw server error.
         if (CloneSupport.indeterminable == clonesupport) {
            throw new AfServerErrorException("System Error: vi cloneSupport="
                  + clonesupport);
         }
         // Return true if cloneSupport is available
         return (CloneSupport.available == clonesupport);
      }
      catch (WpException ex) {
         _log.warn("Failed to get VC configuration: {}", ex.getMessage());
         return false;
      }
   }


   /**
    * Check if the virtual infrastructure type is Workstation.
    *
    * @return boolean
    * @AfServerErrorException
    * @throws WpException
    */
   public boolean isVITypeWorkstation(boolean useCache)
      throws AfServerErrorException, WpException
   {
      // During dev mode, use the config page VI type setting.
      if (_appFactory.isDevModeDeploy()) {
         // Return true if VIType is workstation.
         String viType = _config.getString(ConfigRegistryConstants.WORKPOOL_VI_TYPE);
         return VIType.wsNoClone.name().equalsIgnoreCase(viType);
      }

      ApiType type = getVCConfiguration(useCache).getApiType();

      // If the system is in an unrecoverable state, throw server error.
      if (ApiType.indeterminable == type) {
         throw new AfServerErrorException("VI type cannot be determined");
      }

      // Return true if vi type is workstation
      return (ApiType.hostAgent == type);
   }


   /**
    * This method gets the network, datastores, or virtual machines based on
    * VINode.Type string value.
    *
    * @param viType VINode.Type.Network - to load network tree from compute resource
    *       VINode.Type.VirtualMachine to load vm and resource pool tree
    *       VINode.Type.Datastore to load datasource tree
    * @return - root node containing the child datastore/network nodes or
    *    virtualMachine / resource pools tree.
    * @throws WpException
    */
   public VINode getVIInventoryByType(VINode.Type viType)
      throws WpException, AfBadRequestException
   {
      try {
         VINode node = _rest.getForObject(
               baseUrl() + "/vi/inventory/{viType}",
               VINode.class,
               viType);
         return node;
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            throw new AfBadRequestException(ex);
         }
         throw new WpException(ex);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * This method gets the iso files and folder for the VINode that contains the
    * datastore and the path to the current folder within the datastore.
    *
    * @param node - FileNode
    * @return - Children files(iso) and folders.
    *
    * @throws WpException
    */
   public FileNode[] browseVIDatastoreForISO(VINode node)
      throws WpException, AfBadRequestException
   {
      try {
         FileNode[] nodeArray = _rest.postForObject(
               baseUrl() + "/vi/inventory/datastore/browseIso",
               node,
               FileNode[].class);
         return nodeArray;
      }
      catch (HttpStatusCodeException ex) {
         if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            throw new AfBadRequestException(ex);
         }
         throw new WpException(ex);
      }
      catch (RestClientException e) {
         throw new WpException(e);
      }
   }


   /**
    * Reset the VCConfig cache every hour.
    */
   @Scheduled(fixedDelay=360000)
   public void clearCache() {
      setVcConfigCache(null);
   }


   /**
    * @return the _vcConfigCache
    */
   public VCConfig getVcConfigCache()
   {
      return _vcConfigCache;
   }


   /**
    * @param vcConfig
    */
   public void setVcConfigCache(VCConfig vcConfig)
   {
      _vcConfigCache = vcConfig;
   }

}
