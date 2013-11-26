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

package com.vmware.appfactory.workpool.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.common.runner.WorkpoolTracker;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.config.model.ImageAddRemoveEvent;
import com.vmware.appfactory.config.model.WorkpoolAddRemoveEvent;
import com.vmware.appfactory.workpool.dto.JsonWorkpoolRequest;
import com.vmware.appfactory.workpool.dto.JsonWorkpoolRequest.LicenseType;
import com.vmware.thinapp.common.exception.BaseException;
import com.vmware.thinapp.common.workpool.dto.CustomWorkpool;
import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.common.workpool.dto.ExistingVm;
import com.vmware.thinapp.common.workpool.dto.FullWorkpool;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.OsRegistration;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.VmPattern;
import com.vmware.thinapp.common.workpool.dto.VmSource;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles all the workpool-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class WorkpoolApiController
   extends AbstractApiController
{
   private static Random randomGen = new Random((new Date()).getTime());

   @Resource
   WorkpoolTracker _wpTracker;

   /**
    * Get a JSON object containing all workpools, plus other related data.
    *
    * @return A map containing keys:
    *          "workpools" (array of workpools) and
    *          "default", the name of the default output workpool.
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools",
         method = RequestMethod.GET)
   public Map<String, Object> getAllWorkpools()
      throws WpException
   {
      /* Get all the workpools and sort them by name */
       List<Workpool> allWorkpools;
       try {
           allWorkpools = _wpClient.getAllWorkpools();
       } catch (WpException ignored) {
           // on failure, return an empty list of workpools from
           // the WorkpoolApiController, so that the config page
           // does not blow up when the workpool settings were wrong.
           //
           // Previously, if the workpool URL was incorrect, this would
           // cause the settings page to blow up so that there was no
           // way to correct the workpool URL!
           //
           // TODO: fix this more properly!
           allWorkpools = Collections.emptyList();
       }

      /* Put workpools into the results map */
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("workpools", allWorkpools);
      map.put("default", _config.getString(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL));

      return map;
   }


   /**
    * Get a JSON description of a single workpool by passing its id.
    * If there is no matching workpool, 404 error is returned.
    *
    * @param workpoolId
    * @return
    * @throws AfNotFoundException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/{workpoolId}",
         method = RequestMethod.GET)
   public Workpool getWorkpool(
         @PathVariable Long workpoolId)
      throws AfNotFoundException, WpException
   {
      Workpool wp = _wpClient.getWorkpoolById(workpoolId);
      return wp;
   }


   /**
    * Create a new workpool.
    *
    * The request body must contain a JSON object that serializes into an
    * instance of JsonWorkpoolRequest.
    *
    * There are 5 different ways of creating the workpool depending on whether
    * the virtual infrastructure (Workstation or VC) allows clone or not.
    *
    * A) If clone supported: (Linked)
    *    1. Create workpool & new image (db entry) by looking up an existing VM.
    *    2. Create workpool & new image by using iso and creating new VM.
    *    3. Create workpool & use existing image (db entry)
    * B) Clone not supported: (Full, Custom)
    *    4. Create workpool and associate existing VM(s) as workpool instance(s).
    *    5. Create workpool and create new workpool instance using ISO. (Full)
    *
    * Rules:
    * 1. We create an Image only if clone is permitted by virtual infrastructure.
    * 2. If no clones only then we use existing vm/create vm for the workpool.
    *
    * If the workpool name is already in use, an SC_CORFLICT error is returned.
    * @param request
    * @throws AfBadRequestException
    * @throws AfConflictException
    * @throws WpException
    *
    * @see JsonWorkpoolRequest
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools",
         method = RequestMethod.POST)
   public void createWorkpool(
         @RequestBody JsonWorkpoolRequest request)
      throws AfBadRequestException, AfConflictException, AfNotFoundException, WpException
   {
      _log.debug(request.toString());
      try {
         request.validateCreate();
      }
      catch(InvalidDataException ex) {
         throw new AfBadRequestException(ex);
      }

      // Check whether the new workpool name is already in use.
      checkUniqueWorkpoolName(request.getName());

      Long workpoolId = null;
      // Find the various ways of creating the workpool based on input flags.
      if (request.isCloneSupported()) {
         // Linked workpool creation. This type of workpools will have an image.
         workpoolId = createLinkedWorkpool(request);
      }
      else if (JsonWorkpoolRequest.SourceType.selectVM == request.getSourceType()) {
         workpoolId = createCustomWorkpool(request);
      }
      else if (JsonWorkpoolRequest.SourceType.selectISO == request.getSourceType()) {
         workpoolId = createFullWorkpool(request);
      }
      else {
         throw new AfBadRequestException("Invalid input sourceType");
      }

      // Indicate a new workpool on the workpoolTracker
      _wpTracker.setWorkpoolAsyncProcessing();

      // Set this as the default workpool if default is not set.
      _wpClient.setIfNoDefaultExists(workpoolId);

      publishEvent(new WorkpoolAddRemoveEvent(workpoolId));
   }


   /**
    * Create the Fullworkpool by using the iso and creating a new
    * workpool instance.
    *
    * @param request
    * @return workpoolId
    * @throws WpException
    * @throws AfConflictException
    */
   private Long createFullWorkpool(JsonWorkpoolRequest request)
      throws WpException, AfConflictException
   {
      FullWorkpool wp = new FullWorkpool();

      // Populate the FullWorkpool object and invoke create
      wp.setName(request.getName());
      wp.setMaximum(request.getMaximum()); // @TODO is this 1?

      VmPattern vmPattern = generateVmPattern(request);
      wp.setVmPattern(vmPattern);
      return _wpClient.createWorkpool(wp);
   }


   /**
    * Create the Custom workpool by assigning the selected vm(s) as the
    * workpool instance.
    *
    * @param request
    * @return workpoolId
    * @throws WpException
    * @throws AfConflictException
    */
   private Long createCustomWorkpool(JsonWorkpoolRequest request)
      throws WpException, AfConflictException
   {
      CustomWorkpool wp = new CustomWorkpool();

      // Populate the workpool object and invoke create
      wp.setName(request.getName());
      wp.setMaximum(request.getMaximum());

      ExistingVm existingVm = new ExistingVm();
      existingVm.setMoid(request.getVmSelected().getMoid());
      existingVm.setOsType(request.getOsType());
      existingVm.setOsRegistration(getOsRegWithDefaults(request));

      // No backend support yet: See bug 757170.
//      // @TODO create a single instance for now.
//      List<ExistingVm> list = Collections.singletonList(existingVm);
//      wp.setInstances(...);

      return _wpClient.createWorkpool(wp);
   }

   /**
    * Create the linked workpool by creating an
    * @param request
    * @return workpoolId
    * @throws WpException
    * @throws AfConflictException
    */
   private Long createLinkedWorkpool(JsonWorkpoolRequest request)
      throws WpException, AfNotFoundException, AfConflictException
   {
      VmImage newImage = null;
      LinkedWorkpool wp = new LinkedWorkpool();

      if (JsonWorkpoolRequest.SourceType.existingImage == request.getSourceType()) {
         // Use an existing vmImageId for workpool creation, validate by loading.
         VmImage image = _wpClient.getVmImageById(request.getVmImageId());
         wp.setVmImage(image);
      }
      else if (JsonWorkpoolRequest.SourceType.selectVM == request.getSourceType()) {
         // create a new image object and attach the details.
         // Associate the existing vm details to this image object.
         ExistingVm existingVm = new ExistingVm();
         existingVm.setMoid(request.getVmSelected().getMoid());
         existingVm.setOsType(request.getOsType());
         existingVm.setOsRegistration(getOsRegWithDefaults(request));

         newImage = createNewImage(request, existingVm);
         wp.setVmImage(newImage);
      }
      else if (JsonWorkpoolRequest.SourceType.selectISO == request.getSourceType()) {
         VmPattern vmPattern = generateVmPattern(request);

         newImage = createNewImage(request, vmPattern);
         wp.setVmImage(newImage);
      }

      // Populate the workpool object and invoke create
      wp.setName(request.getName());
      wp.setMaximum(request.getMaximum());

      try {
         return _wpClient.createWorkpool(wp);
      }
      catch (AfConflictException e) {
         if (newImage != null) {
            // In case there is a workpool with the same name already.
            rollbackNewlyCreatedImage(newImage.getId());
         }
         throw e;
      }
      catch (WpException e) {
         if (newImage != null) {
            rollbackNewlyCreatedImage(newImage.getId());
         }
         throw e;
      }
   }


   /**
    * Helper method that creates the VmPattern object based on the input
    *
    * @param request
    * @return
    */
   private VmPattern generateVmPattern(JsonWorkpoolRequest request)
   {
      VmPattern vmPattern = new VmPattern();
      vmPattern.setSourceIso(request.getSourceIso());
      vmPattern.setNetworkName(request.getNetworkName());
      vmPattern.setOsType(request.getOsType());
      vmPattern.setOsRegistration(getOsRegWithDefaults(request));
      return vmPattern;
   }


   /**
    * Helper method that extracts osRegistration from request, but also
    * set the KMS Activation key if the request.kmsServer is set for
    * non winXPPro osTypes.
    *
    * @param request
    * @return
    */
   private OsRegistration getOsRegWithDefaults(JsonWorkpoolRequest request)
   {
      OsRegistration osReg = request.getOsRegistration();
      String osTypeImplName = request.getOsType().getClass().getSimpleName();

      // if kmsServer is set for non winXPPro, pull up the generic KMS license key.
      if (!(request.getOsType() instanceof WinXPProOsType)
            && request.getLicType() == LicenseType.kmsServer) {
         osReg.setLicenseKey(_af.getKMSActivationKeyByOsType(osTypeImplName));
      }
      else {
         // Cleanup unwanted kmsServer data.
         osReg.setKmsServer(StringUtils.EMPTY);
      }
      return osReg;
   }


   /**
    * Helper function to create a new VmImage object and sending the create request.
    *
    * @param request
    * @param vmSource
    * @return
    * @throws WpException
    */
   private VmImage createNewImage(JsonWorkpoolRequest request, VmSource vmSource)
      throws WpException
   {
      // Create the new vmImage
      VmImage image = new VmImage();
      image.setName(String.format("%s Image %05x", request.getName(),
                                             randomGen.nextInt(0xfffff)));
      image.setVmSource(vmSource);

      Long newImageId = _wpClient.createVmImage(image);
      _log.debug("New vmImage created, id: " + newImageId);
      image.setId(newImageId);

      return image;
   }


   /**
    * Update an existing workpool.
    *
    * If the specified workpool is not found, a 404 error is returned.
    * Otherwise, that workpool is updated with the 'JsonWorkpoolRequest'
    *
    * NOTE: Only some fields can be changed. Any fields in the request that
    * can't be changed are ignored.
    *
    * @param workpoolId
    * @param request
    * @throws InvalidDataException
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/{workpoolId}",
         method = RequestMethod.PUT)
   public void updateWorkpool(
         @PathVariable Long workpoolId,
         @RequestBody JsonWorkpoolRequest request)
      throws InvalidDataException, AfNotFoundException, AfConflictException, WpException
   {
      // Perform server side validations.
      request.validateEdit();

      Workpool wp = _wpClient.getWorkpoolById(workpoolId);

      if (!wp.getName().equals(request.getName())) {
         // There is a new workpool name, validate if new name is available.
         checkUniqueWorkpoolName(request.getName());
      }
      /* Update the workpool with input details */
      wp.setName(request.getName());
      wp.setMaximum(request.getMaximum());

      /* Update the workpool */
      _wpClient.updateWorkpool(wp);

      // Indicate a change to workpool on the workpoolTracker
      _wpTracker.setWorkpoolAsyncProcessing();

      publishEvent(new ImageAddRemoveEvent(workpoolId));
   }


   /**
    * Change the status of a workpool, or make it the default. Valid values
    * for "statusStr" are: "default"
    *
    * If "default" status: Set this workpool as the default one.
    * If workpoolId does not map to a known workpool, SC_NOT_FOUND is returned.
    *
    * @TODO enable / disable a workpool
    *
    * @param workpoolId
    * @param statusStr
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/{workpoolId}/{statusStr}",
         method = RequestMethod.PUT)
   public void setWorkpoolStatus(
         @PathVariable Long workpoolId,
         @PathVariable String statusStr)
      throws AfNotFoundException, AfBadRequestException, WpException
   {
      Workpool wp = _wpClient.getWorkpoolById(workpoolId);

      if ("default".equals(statusStr)) {
         _config.setValue(
               ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL,
               wp.getId().toString());
      }
      else {
         throw new AfBadRequestException("Invalid status for workpool");
      }
   }


   /**
    * Deletes a workpool.
    *
    * Deletes a workpool whose id is passed. If the id is not recognized,
    * SC_NOT_FOUND error is returned. If its the default workpool, it cannot
    * be deleted, SC_BAD_REQUEST error is returned.
    *
    * deleteMethod defines whether the workpool should be removed from TAF
    * or additionally delete the vm from the virtual infrastructure as well.
    *
    * @param workpoolId
    * @param deleteMethod
    * @throws AfNotFoundException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/{workpoolId}/method/{deleteMethod}",
         method = RequestMethod.DELETE)
   public void deleteWorkpool(
         @PathVariable Long workpoolId,
         @PathVariable DeleteMethod deleteMethod)
      throws AfNotFoundException, WpException
   {
      Workpool wp = _wpClient.getWorkpoolById(workpoolId);

      _wpClient.deleteWorkpool(wp.getId(), deleteMethod);
      _log.debug("Deleted workpool: " + wp.getName());

      // Indicate a change to workpool on the workpoolTracker
      _wpTracker.setWorkpoolAsyncProcessing();

      // Remove as default (if set) only upon successful delete request.
      handleWorkpoolDefault(wp);

      publishEvent(new WorkpoolAddRemoveEvent(workpoolId));
   }


   /**
    * This is a customized list of Win OS Types that appfactory UI supports
    * and the list of applicable variants for the osTypes.
    *
    * Support Windows XP, 7 only.
    *
    * @return
    */
   @SuppressWarnings("rawtypes")
   @RequestMapping(
         value="/workpools/osTypes",
         method=RequestMethod.GET)
   public @ResponseBody Map<String, List> getSupportedOsTypes()
   {
      Map<String, List> osInfoMap = new HashMap<String, List>();
      osInfoMap.put(
            WinXPProOsType.class.getSimpleName(),
            Collections.EMPTY_LIST);
      osInfoMap.put(
            Win7OsType.class.getSimpleName(),
            Arrays.asList(Win7OsType.Variant.values()));
      return osInfoMap;
   }


   /**
    * Method to return the total number of workpools for a given Linkedworkpool image.
    * The image is only applicable for LinkedWorkpools.
    *
    * Various values and the inference:
    *  0 : workpool does not have an image.
    *  1 : This is the only workpool that is tied to this image.
    *  2+: There are other workpools that have the same image associated.
    *
    * @param workpoolId
    * @return Integer
    * @throws AfNotFoundException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/imageLinkCount/{workpoolId}",
         method = RequestMethod.GET)
   public Integer imageAssociatedToWorkpoolCount(
         @PathVariable Long workpoolId)
      throws AfNotFoundException, WpException
   {
      Workpool wp = _wpClient.getWorkpoolById(workpoolId);
      // If LinkedWorkpool, only then there are images.
      int count = 0;
      if (wp instanceof LinkedWorkpool) {
         Long imageId = ((LinkedWorkpool)wp).getVmImage().getId();
         List<Workpool> wpList = _wpClient.getAllWorkpools();

         // For each workpool, check if the image is used by other workpool
         for( Workpool w : wpList) {
            if (w instanceof LinkedWorkpool) {
               VmImage i = ((LinkedWorkpool)w).getVmImage();
               if (imageId.equals(i.getId())) {
                  count++;
               }
            }
         }
      }
      return Integer.valueOf(count);
   }


   /**
    * Get a JSON object containing all images.
    *
    * @return A map containing keys:
    *  "images" (List of images) &
    *  "imageWpCount" (List of workpool count that each image is associated to
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/image",
         method = RequestMethod.GET)
   public Map<String, Object> getAllImages()
      throws WpException
   {
      /* Get all the workpools and sort them by name */
      final List<VmImage> images = _wpClient.getAllImages();
      Map<Long, Integer> imageWpCount = getImageWorkpoolCount();
      /* Put images and associated workpool count into the results map */
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("images", images);
      map.put("imageWpCount", imageWpCount);

      return map;
   }


   /**
    * Deletes a workpool.
    *
    * Deletes a image whose id is passed. If the id is not recognized,
    * SC_NOT_FOUND error is returned. If the image is associated to other
    * workpools, it cannot be deleted (unless we force delete the workpools).
    * If workpools exists then SC_BAD_REQUEST error is returned.
    *
    * deleteMethod definles whether the image should be removed from TAF
    * or additionally delete the vm from the virtual infrastructure as well.
    *
    * @param imageId
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/workpools/image/{imageId}/method/{deleteMethod}",
         method = RequestMethod.DELETE)
   public void deleteImage(
         @PathVariable Long imageId,
         @PathVariable DeleteMethod deleteMethod)
      throws AfNotFoundException, AfBadRequestException, WpException
   {
      VmImage image = _wpClient.getVmImageById(imageId);

      // Validate if the image has any workpool references since last checked?
      Map<Long, Integer> imagesWpCountMap = getImageWorkpoolCount();
      Integer imageWpCount = imagesWpCountMap.get(image.getId());
      if (imageWpCount != null && imageWpCount > 0) {
         throw new AfBadRequestException("This image cannot be deleted while it is associated with a workpool.");
      }
      _wpClient.deleteVmImage(image.getId(), deleteMethod);
      _log.debug("Deleted workpool image: " + image.getName());

      // Indicate a change to workpool/image on the workpoolTracker
      _wpTracker.setWorkpoolAsyncProcessing();

      publishEvent(new ImageAddRemoveEvent(imageId));
   }


   /**
    * Helper method to get a map of imageId and count of associated workpools
    *
    * @return
    * @throws WpException
    */
   private Map<Long, Integer> getImageWorkpoolCount()
      throws WpException
   {
       final List<Workpool> workpools = _wpClient.getAllWorkpools();
      Map<Long, Integer> imageWpCount = new HashMap<Long, Integer>();

      // Compute the image - workpool count map.
      for (Workpool workpool: workpools) {
         if (workpool instanceof LinkedWorkpool) {
            Long imageId = ((LinkedWorkpool)workpool).getVmImage().getId();
            Integer count = imageWpCount.get(imageId);
            imageWpCount.put(imageId, Integer.valueOf(
                  (count == null? 0 : count.intValue()) + 1 ));
         }
      }
      return imageWpCount;
   }


   /**
    * Method to replace the default workpool with another existing workpool
    * and if no workpool exists, remove the default alltogether.
    *
    * @param workpool
    * @throws WpException
    */
   private void handleWorkpoolDefault(Workpool workpool)
      throws WpException
   {
      // If default, assign another workpool or reset default setting.
      if (_wpClient.isDefault(workpool)) {
         // Now get a list of workpools and pick one.
         String defaultIdStr = StringUtils.EMPTY;
         List<Workpool> wpList = _wpClient.getAllWorkpools();
         for (Workpool wp : wpList) {
            if (!(wp.equals(workpool)
                  || wp.getState() == Workpool.State.deleting
                  || wp.getState() == Workpool.State.deleted)) {
               defaultIdStr = wp.getId().toString();
               break;
            }
         }
         // Set the defaultIdStr as the default, its another workpool or null.
         _config.setValue(ConfigRegistryConstants.WORKPOOL_DEFAULT_WORKPOOL, defaultIdStr);
      }
   }


   /**
    * This methods searches a workpool by name, if found, it throws
    * AfConflictException
    *
    * @param name
    * @throws AfConflictException
    * @throws WpException
    */
   private void checkUniqueWorkpoolName(String name)
   throws AfConflictException, WpException
   {
      try {
         Workpool w = _wpClient.getWorkpoolByName(name);
         if (w != null) {
            throw new AfConflictException("CONFLICT_NAME: Workpool name \""
                  + name + "\" is already in use.");
         }
      }
      catch (AfNotFoundException e) {
         // This is good, coz there are no workpools with that name
      }
   }


   /**
    * Here if the image id is set, we attempt to rollback the new image creation
    *
    * This is needed until the workpool and image creation will be a single
    * transaction that can be moved to the backend.
    *
    * @param newImageId
    */
   private void rollbackNewlyCreatedImage(Long newImageId) {
      if (newImageId != null) {
         try {
            _wpClient.deleteVmImage(newImageId, DeleteMethod.deleteFromDisk);
         }
         catch (BaseException e) {
            // Exception occured during rollback. Nothing to do now, but log.
            _log.error(e.getMessage(), e);
         }
      }
   }

}
