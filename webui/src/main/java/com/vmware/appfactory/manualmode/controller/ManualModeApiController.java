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

package com.vmware.appfactory.manualmode.controller;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.manualmode.IManualModeService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.converter.dto.Status;

/**
 * This controller handles all the feed-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@SuppressWarnings("MissortedModifiers")
@Controller
public class ManualModeApiController extends AbstractApiController {

   @Resource
   @Qualifier("manualModeServiceImpl")
   protected IManualModeService _manualModeService;

   /**
    * Request manual mode ThinApp conversion with specific build values.
    *
    * @param captureRequest - all components needed for building an app.
    * @return The ticket ID for the manual mode request.
    */
   @RequestMapping(
         value="/manualMode",
         method=RequestMethod.POST)
   public @ResponseBody String requestManualModeConversion(
         @RequestBody CaptureRequestImpl captureRequest) throws AfServerErrorException
   {
      Long appId = captureRequest.getApplicationId();
      _log.debug("Manual Mode request for appId={}", appId);
      try {
         final Application app = _daoFactory.getApplicationDao().find(appId);
         if (app == null) {
            throw new AfNotFoundException("Invalid application id " + appId);
         }

         String ticketIdStr = _manualModeService.createRequest(captureRequest);
         _log.debug("Created a new ticket with id {}", ticketIdStr);
         Long ticketId = Long.valueOf(ticketIdStr);

         // Set the suggested build name, display name, and icons list.
         if (StringUtils.isEmpty(captureRequest.getBuildName())) {
            captureRequest.setBuildName(app.getSuggestedBuildName());
         }
         if (StringUtils.isEmpty(captureRequest.getDisplayName())) {
            captureRequest.setDisplayName(app.getDisplayName());
         }
         captureRequest.setIcons(app.getIcons());

         // Disable horizon flag.
         captureRequest.setAddHorizonIntegration(false);

         final AppFactoryTask task = _taskFactory.newManualModeTask(
               captureRequest,
               ticketId,
               _manualModeService,
               _conversionsQueue);
         _conversionsQueue.addTask(task);
         return ticketIdStr;
      } catch (Exception e) {
         _log.error("Failed to create new ticket for app id {}", appId);
         throw new AfServerErrorException(e);
      }
   }


   /**
    * Check the status of the ticket.
    *
    * @param ticketId - a ticket id.
    * @param appId - an application id.
    */
   @RequestMapping(
         value="/manualMode/{ticketId}",
         method=RequestMethod.GET)
   public @ResponseBody Status checkStatus(
         @PathVariable Long ticketId,
         @RequestParam(required=true) Long appId) throws AfServerErrorException {
      try {
         final Status status = _manualModeService.checkStatus(ticketId);
         _log.debug("The given request is in =" + status.getCurrentState().toString());
         return status;
      } catch (Exception e) {
         _log.error("Failed to check MM request status for for app id {}", appId);
         throw new AfServerErrorException(e);
      }

   }


   /**
    * Move on to the next step in the manual mode process.
    * @param ticketId - a ticket id.
    * @param stage - a next stage name.
    */
   @RequestMapping(
         value="/manualMode/{ticketId}/next/{stage}",
         method=RequestMethod.POST)
   public @ResponseBody void next(
         @PathVariable Long ticketId,
         @PathVariable String stage) throws AfServerErrorException {
      _log.debug("Manual Mode request NEXT stage [{}] for ticketId={}", stage, ticketId);

      try {
         _manualModeService.next(ticketId, stage);
         _log.debug("Next step passed.");
      } catch (Exception e) {
         _log.error("Failed to process NEXT request for ticket id {}", ticketId);
         throw new AfServerErrorException(e);
      }
   }

   /**
    * Cancel manual mode build of the given ticket id.
    * @param ticketId - a ticket id.
    */
   @RequestMapping(
         value="/manualMode/cancel",
         method=RequestMethod.PUT)
   public @ResponseBody void cancel(
         @RequestParam(required=true) Long ticketId) throws AfServerErrorException {
      _log.debug("Canceling Manual Mode build for ticketId={}", ticketId);

      try {
         _manualModeService.cancel(ticketId);
         _log.debug("Canceled manual build for the ticket id {}",  ticketId);
      } catch (Exception e) {
         _log.error("Failed to cancel MM build for the ticket id {}", ticketId);
         throw new AfServerErrorException(e);
      }
   }
}
