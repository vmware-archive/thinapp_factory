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

package com.vmware.appfactory.common.runner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.appfactory.common.dto.WorkpoolAndImageFailCount;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This Runnable task is scheduled via ScheduledExecutorTask. The purpose
 * of this task is the keep track of workpool failures if any exists.
 * To aid in a performant solution, this class uses a flag that indicates
 * whether there is any active asynchronous processing going on with workpools.
 */
public class WorkpoolTracker implements Runnable
{
   private final Logger _log = LoggerFactory.getLogger(WorkpoolTracker.class);

   /** Flag to decide if the refresh needs to happen */
   private boolean _activeProcessing = true;

   /** Object to store workpool, image fail counts & recent timestamp of that count */
   private WorkpoolAndImageFailCount _wpImgFailCount = new WorkpoolAndImageFailCount(0, 0, 0L);

   @Resource
   private WorkpoolClientService _wpClient;

   @Override
   public void run()
   {
      // If no async, dont do anything.
      if (isActiveProcessing()) {
         _log.trace("Starting time: " + System.currentTimeMillis());
         try {
            List<Workpool> wpList = _wpClient.getAllWorkpools();
            // Flag to compute if processing is done.
            boolean stillProcessing = false;

            // Flag to be set if we know for sure the workpools are not LinkedWorkpools
            boolean isLinkedWorkpool = true;
            int wpFail = 0;
            for (int i = 0; wpList != null && i < wpList.size(); i++) {
               Workpool w = wpList.get(i);

               if (w.getState() == Workpool.State.unavailable) {
                  wpFail++;
               }

               // Set processing even if one of the workpool is currently processing.
               stillProcessing = stillProcessing || w.isProcessing();

               // If not a linked workpool, then the rest are the same.
               isLinkedWorkpool = (w instanceof LinkedWorkpool);
            }

            // Only check if its not Linked workpool.
            Set<Long> imageSet = new HashSet<Long>();

            if (isLinkedWorkpool) {
               // Now compute the vmImages that may have been in error state.
               stillProcessing = isVmImageFailOrProcessing(imageSet) || stillProcessing;
            }

            // Set the processing flag, wpFailCount, vmImageFailCount.
            setProcessingAndFailCount(stillProcessing, wpFail, imageSet.size());
         }
         catch (WpException e) {
            _log.error("Workpool fetch error: {}, retry next run.", e.getMessage());
         }
      }
   }

   /**
    * Go through the list of all available VmImages to see if there are any that are in fail state.
    * This check cannot be part of above check as it only checks against images that are linked to workpools.
    *
    * @param imageSet
    * @return
    */
   private boolean isVmImageFailOrProcessing(Set<Long> imageSet) {
      boolean stillProcessing = false;
      try {
         final List<VmImage> images = _wpClient.getAllImages();
         for (VmImage image : images) {
            if (image.isFailState()) {
               // An image could be used on multiple workpools.
               imageSet.add(image.getId());
            }
            // Set processing even if one of the images is currently processing.
            stillProcessing = stillProcessing || image.isProcessing();
         }
      } catch (Exception e) {
         _log.warn("Image fetch error: {}, retry next run.", e.getMessage());
         stillProcessing = true;
      }
      return stillProcessing;
   }

   /**
    * Exposing a method that marks the flag indicating workpool async
    * process is in play.
    *
    * This indicates that there is a possibility of a failure, and hence this
    * runnable can watch for failure state.
    */
   public void setWorkpoolAsyncProcessing()
   {
      setActiveProcessing(true);
   }


   /**
    * @return the wpImgFailCount
    */
   public WorkpoolAndImageFailCount getWpImgFailCount()
   {
      return _wpImgFailCount;
   }



   /**
    * @param activeProcessing the activeProcessing to set
    */
   private synchronized void setActiveProcessing(boolean activeProcessing)
   {
      _activeProcessing = activeProcessing;
   }


   /**
    * @return the activeProcessing
    */
   private synchronized boolean isActiveProcessing()
   {
      return _activeProcessing;
   }


   /**
    * Set the flags and fail count values.
    *
    * @param activeProcessing
    * @param wpFailCount
    * @param vmImageFailCount
    */
   private synchronized void setProcessingAndFailCount(
         boolean activeProcessing,
         int wpFailCount,
         int vmImageFailCount)
   {
      this._activeProcessing = activeProcessing;
      if (this._wpImgFailCount.getWpFailCount() != wpFailCount
            || this._wpImgFailCount.getVmImageFailCount() != vmImageFailCount) {
         this._wpImgFailCount = new WorkpoolAndImageFailCount(
               wpFailCount,
               vmImageFailCount,
               System.currentTimeMillis());
      }
   }
}
