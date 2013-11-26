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

package com.vmware.thinapp.manualmode.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.thinapp.common.converter.dto.Status;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.converter.dto.TicketRequest;
import com.vmware.thinapp.manualmode.Config;
import com.vmware.thinapp.manualmode.dao.ConversionJobRepository;
import com.vmware.thinapp.manualmode.model.ConversionJobModel;
import com.vmware.thinapp.manualmode.server.ManualMode;
import com.vmware.thinapp.manualmode.server.RequestInProgressException;
import com.vmware.thinapp.manualmode.server.UnknownTicketException;
import com.vmware.thinapp.manualmode.web.converters.StatusConverter;

@Controller
@RequestMapping("/manual")
public class ManualController {
   @Autowired
   private ManualMode mmode;

   @Autowired
   private Config config;

   @Autowired
   private ConversionJobRepository jobDao;

   @RequestMapping(method = RequestMethod.GET)
   public ModelAndView index() {
      ModelAndView mv = new ModelAndView("requests/index");
      mv.getModel().put("proxyHost", config.getHttpProxyHost());
      mv.getModel().put("proxyPort", config.getHttpProxyPort());
      return mv;
   }

   /**
    * Creates a new manual mode request
    * <p>
    * When the client wishes to start a manual mode, it makes a request to
    * acquire a lease on a specific template type from the workpool. Given this
    * may take a non-trivial amount of time (all VMs may be in use, template
    * creation may still be in progress, etc.) the HTTP connection may timeout
    * before then.
    * <p>
    * Therefore, after receiving the request we immediately issue the client a
    * ticket which they can later use to query the status of their request and
    * to finally claim their VM when it becomes available.
    * <p>
    * XXX: If the client doesn't turn in their ticket after a certain period of
    * time the acquired VM should be returned to the workpool.
    * <p>
    * TODO: Higher priority would be nice for manual requests.
    * <p>
    *
    * @return a ticket the client can use to check the status of their request
    *         and receive their lease from
    */
   @RequestMapping(method = RequestMethod.POST)
   @ResponseBody
   public Ticket create(@RequestBody TicketRequest request) {
      ConversionJobModel job = jobDao.createNewConversionJob();
      return mmode.create(request, job);
   }

   /**
    * Redeems
    * @param ticket
    * @param resp
    * @param request
    * @return
    * @throws Exception
    */
   @RequestMapping(method = RequestMethod.POST, value = "/redeem")
   @ResponseBody
   public Status redeem(
         @RequestBody Ticket ticket,
         HttpServletResponse resp,
         HttpServletRequest request) throws Exception {
      try {
         com.vmware.thinapp.manualmode.server.Status s = mmode.redeem(ticket);
         return StatusConverter.toDto(s);
      } catch (UnknownTicketException e) {
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
         return null;
      }
   }

   /**
    * Transition to the next state.
    * @param ticket
    * @param currentState
    * @param resp
    * @return an empty String so that a Content-Type will be set for clients
    */
   @RequestMapping(method = RequestMethod.POST, value = "/next/{state}")
   @ResponseBody
   public String next(
         @RequestBody Ticket ticket,
         @PathVariable State state,
         HttpServletResponse resp) {
      try {
         mmode.next(ticket, state);
      } catch (IllegalArgumentException e){
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (UnknownTicketException e) {
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } catch (RequestInProgressException e) {
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }

      return "";
   }

   /**
    * Cancels the given request.
    * @param ticket
    * @param resp
    * @return an empty String so that a Content-Type will be set for clients
    */
   @RequestMapping(method = RequestMethod.POST, value = "/cancel")
   @ResponseBody
   public String cancel(
         @RequestBody Ticket ticket,
         HttpServletResponse resp) {
      try {
         mmode.cancel(ticket);
      } catch (UnknownTicketException e) {
         resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (RequestInProgressException e) {
         resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }

      return "";
   }
}
