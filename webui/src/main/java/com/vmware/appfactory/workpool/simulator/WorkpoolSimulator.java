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

package com.vmware.appfactory.workpool.simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

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
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistry.viTypes.VIType;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.FolderNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.workpool.dto.CustomWorkpool;
import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.common.workpool.dto.ExistingVm;
import com.vmware.thinapp.common.workpool.dto.FullWorkpool;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.OsRegistration;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.VmPattern;
import com.vmware.thinapp.common.workpool.dto.VmSource;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This simulator pretends to be a Workpool service, and implements the REST
 * API defined by the actual Workpool service. It is useful for simple testing
 * and debugging.
 */
@Controller
@RequestMapping(value = {"/wp", "/webui/wp"})
public class WorkpoolSimulator extends AbstractApiController
{
   private static long NEXT_ID = 1L;

   private static synchronized Long nextId() {
      return Long.valueOf(NEXT_ID++);
   }

   private static final Random RANDOM = new Random();

   private static final Map<Long,Workpool> DUMMY_POOLS = new HashMap<Long,Workpool>();
   private static final Map<Long,VmImage> DUMMY_IMAGES = new HashMap<Long,VmImage>();

   /** Create dummy values and maintain this sequence of loading data. */
   private static final VCConfig DUMMY_CONFIG = new VCConfig();
   private static final VINode DUMMY_ROOT_VM_NODE = VISimulatorData.createDummyVMTree();
   private static final VINode DUMMY_ROOT_DS_NODE = VISimulatorData.createDummyDatastoresTree(DUMMY_ROOT_VM_NODE);
   private static final VINode DUMMY_ROOT_NET_NODE = VISimulatorData.createDummyNetworkTree(DUMMY_ROOT_DS_NODE);

   private static final String BASE_WP_URL = "/workpools";
   private static final String BASE_IMG_URL = "/vmimages";
   private static final String BASE_VI_URL = "/vi";

   static {
      // Crease a dummy config detail representing the virtual infrastructure
      // that hosts the taf appliance.
      DUMMY_CONFIG.setHost("appfactory.mycompany.com");
      DUMMY_CONFIG.setPassword("TAFPassword");
      DUMMY_CONFIG.setUsername("TAFUsername");
      DUMMY_CONFIG.setDatacenter("Datacenter 4 TAF");
      // cloneSupported is driven off of a config param.

      // Populate workpool and image details
      Workpool workpool;
      LinkedWorkpool pool;
      VmImage image;
      ExistingVm existingVm;
      OsRegistration osReg;
      VmPattern vmPattern;

      pool = new LinkedWorkpool();

      /** Create os reg entries */
      osReg = createDummyOsRegistration();

      /** Dummy osVariant for workpools */
      OsType osType = new Win7OsType(Win7OsType.Variant.enterprise);

      vmPattern = new VmPattern();
      vmPattern.setOsRegistration(osReg);
      vmPattern.setOsType(osType);

      image = new VmImage();
      image.setId(nextId());
      image.setName("VmImage #1");
      image.setState(VmImage.State.available);
      image.setVmSource(vmPattern);

      pool.setId(nextId());
      pool.setName("Workpool win 7 enterprise");
      pool.setState(Workpool.State.available);
      pool.setVmImage(image);
      DUMMY_IMAGES.put(image.getId(), image);
      pool.setMaximum(5);
      workpool = pool;
      DUMMY_POOLS.put(workpool.getId(), workpool);

      pool = new LinkedWorkpool();

      image = new VmImage();
      image.setId(nextId());
      image.setName("VmImage #2");
      image.setState(VmImage.State.available);

      existingVm = new ExistingVm();
      existingVm.setOsRegistration(osReg);

      /** Dummy osVariant for workpools */
      osType = new WinXPProOsType();
      existingVm.setOsType(osType);

      image.setVmSource(existingVm);
      existingVm.setMoid("vm-40");

      pool.setId(nextId());
      pool.setName("Workpool win xp");
      pool.setState(Workpool.State.available);
      pool.setVmImage(image);
      DUMMY_IMAGES.put(image.getId(), image);
      pool.setMaximum(6);
      workpool = pool;
      DUMMY_POOLS.put(workpool.getId(), workpool);
   }


   /**
    * Handles POST /vmimage call for creating a new image
    *
    * @param response
    * @param image
    * @throws IOException
    * @throws AfConflictException, AfBadRequestException, IOException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_IMG_URL,
         method = RequestMethod.POST)
   public void saveImage(
         HttpServletResponse response,
         @RequestBody VmImage image)
   throws AfConflictException, AfBadRequestException, IOException
   {
      // Check whether the image name already exists, if so raise exception.
      for (VmImage i : DUMMY_IMAGES.values()) {
         // If image matches the Id, return the workpool.
         if(image.getName().equals(i.getName())) {
            throw new AfConflictException(image.getName() + " - image name already exists.");
         }
      }

      Long newId = nextId();
      image.setId(newId);
      image.setState(VmImage.State.available);
      VmSource source = image.getVmSource();

      // TODO Use exact validation as backend to be consistent.
      if (source == null
            || source.getOsType() == null
            || source.getOsRegistration() == null) {
         throw new AfBadRequestException("Invalid input");
      }
      DUMMY_IMAGES.put(newId, image);
      response.setHeader("Location", BASE_IMG_URL + "/" + newId);
   }


   /**
    * GET /vmimage
    * Get a VmImage[] of all images
    *
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_IMG_URL,
         method = RequestMethod.GET)
   public VmImage[] getAllImages()
   {
      Collection<VmImage> imageList = DUMMY_IMAGES.values();
      return imageList.toArray(new VmImage[imageList.size()]);
   }


   /**
    * Handles the GET /vmimage/{id} call
    *
    * This method looks for the VmImage with the passed param if its a number.
    * If the VmImage is not found / is param is not a number, then the same value
    * is compared against the name field to find by name.
    *
    * @TODO: Status to be found within the VmImage?
    *
    * @param idNameStr
    * @return VmImage matching the id/Name, else AfNotFoundException is thrown.
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_IMG_URL + "/{idNameStr}",
         method = RequestMethod.GET)
   public VmImage getImage(
         @PathVariable("idNameStr") String idNameStr)
   throws AfNotFoundException
   {
      VmImage image = null;
      try {
         Long id = Long.valueOf(idNameStr);
         image = DUMMY_IMAGES.get(id);
      }
      catch (NumberFormatException e) {
         /* Do nothing, we now know the lookup key is a string */
      }
      // Check if the image is not found, if so, lookup by name
      if (image != null) {
         return image;
      }

      for (VmImage i : DUMMY_IMAGES.values()) {
         // If image matches the Id, return the workpool.
         if (idNameStr.equals(i.getName())) {
            return i;
         }
      }

      throw new AfNotFoundException("Invalid image lookup key: " + idNameStr);
   }


   /**
    * Handles the DELETE /vmimage/{id} call
    *
    * This method looks for the VmImage using input param.
    * If found, deletes the image. If not, throws AfNotFoundException
    *
    * @param id
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_IMG_URL + "/{id}",
         method = RequestMethod.DELETE)
   public void deleteImage(
         @PathVariable Long id,
         @RequestParam DeleteMethod deleteMethod)
   throws AfNotFoundException, AfBadRequestException
   {
      if (deleteMethod == null) {
         throw new AfBadRequestException("Invalid deleteMethod");
      }
      // Validate the image id before deleting it.
      if (DUMMY_IMAGES.containsKey(id)) {
         DUMMY_IMAGES.remove(id);
      }
      else {
         throw new AfNotFoundException("Invalid image lookup key: " + id);
      }
   }


   /**
    * PUT /vmimage/{id}
    * Updates the image name. Ensure the name is unique.
    *
    * @param id
    * @throws AfNotFoundException - if no matching VmImage found
    * @throws AfConflictException - if new name exists for another VmImage.
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_IMG_URL + "/{id}",
         method = RequestMethod.PUT)
   public void updateImage(
         @PathVariable Long id,
         @RequestBody VmImage image)
   throws AfConflictException, AfNotFoundException
   {
      // Lookup image by id. If not found, throw AfNotFoundException
      VmImage existing = findImageById(id);

      // Check no duplicates exist before we save.
      for (VmImage i : DUMMY_IMAGES.values()) {

         // Skip comparing lookedup image.
         if (i.getId().equals(existing.getId())) {
            continue;
         }
         // If image matches the Id, return the workpool.
         if(image.getName().equals(i.getName())) {
            throw new AfConflictException("Image already exists with name: " + image.getName());
         }
      }
      // Update the old image object with new values
      existing.setName(image.getName());
   }


   /**
    * GET /workpools
    * Get a list of all workpools.
    * @return
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL,
         method = RequestMethod.GET)
   public Workpool[] getAllPools()
   {
      Collection<Workpool> wpList = DUMMY_POOLS.values();
      return wpList.toArray(new Workpool[wpList.size()]);
   }


   /**
    * Handles the GET /workpools/{id} call
    * This method looks for the workpool with the passed id, if its a number.
    * If a workpool is not found / is not a number, then the same value is
    * compated against the name field to find by name.
    *
    * @param idNameStr
    * @return workpool matching the id/Name, else AfNotFoundException is thrown.
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL + "/{idNameStr}",
         method = RequestMethod.GET)
   public Workpool getWorkpool(
         @PathVariable("idNameStr") String idNameStr)
   throws AfNotFoundException
   {
      Workpool wp = null;
      try {
         Long id = Long.valueOf(idNameStr);
         wp = DUMMY_POOLS.get(id);
      }
      catch (NumberFormatException e) {
         /* Do nothing, we now know the lookup key is a string */
      }
      // Check if the workpool was not found, lookup by name
      if (wp != null) {
         return wp;
      }

      for (Workpool w : DUMMY_POOLS.values()) {
         // If workpool matches the Id, return the workpool.
         if(idNameStr.equals(w.getName())) {
            return w;
         }
      }

      throw new AfNotFoundException("Invalid workpool lookup key: " + idNameStr);
   }


   /**
    * Handles the POST /workpools call for creating a new workpool
    *
    * @param workpool - to be saved.
    * @throws AfNotFoundException - if vmImageId is passed and does not match a VmImage
    * @throws AfConflictException - if another workpool has the same name already.
    * @throws AfBadRequestException - if vmPatternId / vmImageId is not passed.
    * @throws IOException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL,
         method = RequestMethod.POST)
   public void createWorkpool(
         HttpServletResponse response,
         @RequestBody Workpool workpool)
   throws AfConflictException, AfNotFoundException, AfBadRequestException, IOException
   {
      // Check if a workpool exists with this name
      Workpool w = getWorkpoolByName(workpool.getName());
      if (w != null) {
         throw new AfConflictException("Workpool already exists with name: " + workpool.getName());
      }
      _log.debug("Workkpool to create: " + workpool);
      // Look for the type of workpool thats getting created.
      if(workpool instanceof FullWorkpool) {
         // Ensure the pattern contains iso, etc.
      }
      else if(workpool instanceof CustomWorkpool) {
         // Ensure the vm exists and then save workpool.
      }
      else { // This is linked
         // Ensure the image exists before we save the workpool.
         LinkedWorkpool linked = (LinkedWorkpool)workpool;
         findImageById(linked.getVmImage().getId());
      }

      Long newId = nextId();
      workpool.setId(newId);
      workpool.setState(Workpool.State.available);

      DUMMY_POOLS.put(newId, workpool);
      response.setHeader("Location", BASE_WP_URL + "/" + newId);
   }


   /**
    * Change the status of a workpool.
    *
    * The value for 'status' must match one of "enable" or "disable".
    *
    * @param id
    * @param statusStr
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL + "/{id}/{status}",
         method = RequestMethod.POST)
   public void setWorkpoolStatus(
         @PathVariable("id") Long id,
         @PathVariable("status") String statusStr)
      throws AfNotFoundException, AfBadRequestException
   {
      if ("disable".equals(statusStr) ||
         "enable".equalsIgnoreCase(statusStr)) {
         // TODO implement later if backend supports this
      }
      throw new AfBadRequestException("Invalid workpool status: " + statusStr);
   }


   /**
    * Delete a workpool but not the image (as this is only a simulator).
    *
    * @param id
    * @throws AfNotFoundException - if no matching workpools exist.
    * @throws AfBadRequestException - if workpool is not disabled
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL + "/{id}",
         method = RequestMethod.DELETE)
   public void deleteWorkpool(
         @PathVariable Long id,
         @RequestParam DeleteMethod deleteMethod)
      throws AfNotFoundException, AfBadRequestException
   {
      if (deleteMethod == null) {
         throw new AfBadRequestException("Invalid deleteMethod");
      }
      findWorkpoolById(id);
      DUMMY_POOLS.remove(id);
   }


   /**
    * PUT /workpools/{id}
    * Updates the workpool name, min/max. Ensure new name is unique.
    *
    * @param id
    * @throws AfNotFoundException - if no matching workpool found
    * @throws AfConflictException - if new name already exists for other workpool
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_WP_URL + "/{id}",
         method = RequestMethod.PUT)
   public void updateWorkpool(
         @PathVariable Long id,
         @RequestBody Workpool workpool)
      throws AfConflictException, AfNotFoundException
   {
      Workpool existing = findWorkpoolById(id);

      // Load workpool by id and check no duplicate names exist before saving new name.
      for (Workpool w : DUMMY_POOLS.values()) {

         // Skip comparing lookedup workpool.
         if (id.equals(w.getId())) {
            continue;
         }
         // If workpool name matches another exisitng workpool, raise
         if(workpool.getName().equals(w.getName())) {
            throw new AfConflictException("Workpool already exists with name: " + workpool.getName());
         }
      }
      existing.setName(workpool.getName());
      existing.setMaximum(workpool.getMaximum());
      _log.debug("Update successful for workpool id: " + id);
   }



   // TODO: GET /workpools/{id}/instances


   // TODO: POST /workpools/{id}/leases/acquire


   // TODO: POST /workpools/{id}/leases/{lease}/release


   /**
    * Create a dummy VCconfig and return it likewise.
    * First time creation of this dummy VCConfig would switch between VC/WS like
    * behavior. Within VC, the switch b/w clone allowed and not is also randomly switched.
    */
   @ResponseBody
   @RequestMapping(
         value = "/config",
         method = RequestMethod.GET)
   public VCConfig getVCConfiguration()
   {
      // Check if configuration is set as VC or WS and their clone support.
      String viType = _config.getString(ConfigRegistryConstants.WORKPOOL_VI_TYPE);
      DUMMY_CONFIG.setCloneSupport(VIType.vc.name().equalsIgnoreCase(viType)?
            VCConfig.CloneSupport.available : VCConfig.CloneSupport.unavailable);

      DUMMY_CONFIG.setApiType(VIType.wsNoClone.name().equalsIgnoreCase(viType)?
            VCConfig.ApiType.hostAgent : VCConfig.ApiType.virtualCenter);

      return DUMMY_CONFIG;
   }


   /**
    * This method provides the virtual infrastructure's inventory by type.
    * Given a VINode.nodeType, it fetches all the corresponding inventory
    * and returns the root of a canonical form result.
    *
    * @param viType One of "Datastore", "Network", "VirtualMachine"
    * @return VINode
    * @throws WpException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_VI_URL + "/inventory/{viType}",
         method = RequestMethod.GET)
   public VINode getVIInventoryByType(
         @PathVariable VINode.Type viType)
      throws WpException, AfBadRequestException
   {
      if (VINode.Type.Datastore == viType) {
         return DUMMY_ROOT_DS_NODE;
      }
      else if (VINode.Type.Network == viType) {
         return DUMMY_ROOT_NET_NODE;
      }
      else if (VINode.Type.VirtualMachine == viType) {
         return DUMMY_ROOT_VM_NODE;
      }
      throw new AfBadRequestException("Invalid invType: " + viType);
   }


   /**
    * This method returns folders / iso files for the input datastore node
    * under the path specified in the node.
    *
    * @param node
    * @return
    * @throws WpException - If input is invalid
    */
   @ResponseBody
   @RequestMapping(
         value = BASE_VI_URL + "/inventory/datastore/browseIso",
         method = RequestMethod.POST)
   public FileNode[] browseVIDatastoreForISO(
         @RequestBody VINode node)
      throws WpException, AfBadRequestException
   {
      if (node.getNodeType() != VINode.Type.Datastore) {
         throw new WpException("Invalid input");
      }

      // Now browse through the datastore and find the children.
      for (VINode n : DUMMY_ROOT_DS_NODE.getChildren()) {
         if (n.getName().equals(node.getName())) {
            // Found a matching datstore name here, so create dummy data.
            List<FileNode> nodeList = createDummyFolderAndFile(node);
            return nodeList.toArray(new FileNode[nodeList.size()]);
         }
      }
      throw new AfBadRequestException("Input datastore/path does not exist.");
   }


   /**
    * Create dummy folder and files for the passed datastore node for the
    * specified path.
    *
    * @param node
    * @return
    */
   private List<FileNode> createDummyFolderAndFile(VINode node)
   {
      List<FileNode> nodeList = new ArrayList<FileNode>();
      String folderArray[] = node.getPath().split("/");
      boolean createFolder = folderArray.length < 3 || RANDOM.nextBoolean();
      int folderSize = RANDOM.nextInt(3);
      int fileSize = RANDOM.nextInt(5);
      String pathSeparator = node.getPath().equals("[" + node.getName() +"]")? " " : "/";

      // Create dummy 'folderSize' number of folders.
      for(int i = 0; createFolder && i<folderSize; i++) {
         String name = "folder-" + String.valueOf(RANDOM.nextInt(999));
         FileNode newNode = new FolderNode(
               name,
               node.getPath() + pathSeparator + name,
               Long.valueOf(2010L),
               true);
         nodeList.add(newNode);
      }

      // Create dummy 'fileSize' number of iso files.
      for(int i = 0; i< fileSize; i++) {
         String name = "iso-file-name-" + String.valueOf(RANDOM.nextInt(99999)) + ".iso";
         FileNode newNode = new FileNode(
               name,
               node.getPath() + pathSeparator + name,
               Long.valueOf(RANDOM.nextLong()));
         nodeList.add(newNode);
      }
      return nodeList;
   }


   /**
    * Helper method to find an image by id.
    *
    * @param id
    * @return
    * @throws AfNotFoundException - if no image found
    */
   private VmImage findImageById(Long id) throws AfNotFoundException
   {
      VmImage existing = DUMMY_IMAGES.get(id);
      if(existing == null) {
         throw new AfNotFoundException("No image exists with id: " + id);
      }
      return existing;
   }


   /**
    * Helper method to find a workpool by id.
    *
    * @param id
    * @return
    * @throws AfNotFoundException - if no workpool found
    */
   private Workpool findWorkpoolById(Long id) throws AfNotFoundException
   {
      Workpool w = DUMMY_POOLS.get(id);
      if(w == null) {
         throw new AfNotFoundException("No workpool exists with id: " + id);
      }
      return w;
   }


   /**
    * Helper method to get workpool by name.
    * @param name
    * @return
    */
   private Workpool getWorkpoolByName(String name)
   {
      Workpool workpool = null;
      for(Workpool w : DUMMY_POOLS.values()) {
         if (name.equalsIgnoreCase(w.getName())) {
            workpool = w;
            break;
         }
      }
      return workpool;
   }


   /**
    * Creates a dummy OsRegistration data.
    * NOTE: Load this only once in the static block to reduce overhead.
    * @return
    */
   private static OsRegistration createDummyOsRegistration()
   {
      OsRegistration osReg;
      osReg = new OsRegistration();
      osReg.setKmsServer("kms-server-host.domain.com");
      osReg.setLicenseKey("Win80-XXXXX-XXXXX-XXXXX-XXXXX");
      osReg.setOrganization("VMware Inc.");
      osReg.setUserName("vmware");
      return osReg;
   }

}
