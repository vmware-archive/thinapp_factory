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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.vmware.thinapp.common.converter.dto.ConversionJobStatus;
import com.vmware.thinapp.common.converter.dto.ConversionRequest;
import com.vmware.thinapp.common.converter.dto.ConversionResponse;
import com.vmware.thinapp.common.converter.dto.ConversionResult;
import com.vmware.thinapp.common.converter.dto.ConversionResult.Disposition;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.converter.dto.TicketRequest;
import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.thinapp.manualmode.dao.ConversionJobRepository;
import com.vmware.thinapp.manualmode.model.ConversionJobModel;
import com.vmware.thinapp.manualmode.server.ManualMode;
import com.vmware.thinapp.manualmode.server.Status;

@Controller
public class ConversionsController {
   private final Logger _log = LoggerFactory.getLogger(ConversionsController.class);

   @Autowired
   private ManualMode mmode;

   @Autowired
   private ConversionJobRepository jobDao;

   @RequestMapping(
         value="/conversions",
         method = RequestMethod.GET)
   @ResponseBody
   public List<ConversionJobStatus> list(
         @SuppressWarnings("unused") @RequestBody ConversionRequest request) {
      _log.debug("Received GET request to list all conversion job status");

      // todo: scope this somehow?  What about really old jobs?
      List<ConversionJobModel> jobModels = jobDao.findAll();

      List<ConversionJobStatus> statusList = Lists.transform(jobModels,
            new Function<ConversionJobModel, ConversionJobStatus>() {
               @Override
               public ConversionJobStatus apply(ConversionJobModel jobModel) {
                  return getJobStatus(jobModel);
               }
            });

      return statusList;
   }

   @RequestMapping(
         value="/conversions",
         method = RequestMethod.POST)
   @ResponseBody
   public ConversionResponse createConversion(
         @RequestBody ConversionRequest request) {
      _log.debug("Received POST request to create a new conversion job");

      // input files are either a) download URL or b) datastore://path/to/installer
      // output urls are just a datastore name
      // installation commands are full commands to run at each phase

      TicketRequest ticketRequest = new TicketRequest(
            request.getFiles(),
            request.getOutput().getUrl(),
            request.getSteps(),
            true,
            request.getWorkpool(),
            request.getRuntimeId());

      ConversionJobModel job = jobDao.createNewConversionJob();
      Ticket ticket = mmode.create(ticketRequest, job);
      mmode.redeem(ticket);
      _log.debug("Returning from POST request to create a new conversion job");

      ConversionResponse response = new ConversionResponse();
      response.setJobId(job.getId());
      return response;
   }

   @RequestMapping(
         value="/conversions/{jobId}",
         method=RequestMethod.GET)
   @ResponseBody
   public ConversionJobStatus getConversionSatus(
         @PathVariable Long jobId) {
      ConversionJobModel job = jobDao.getJobById(jobId);
      if (job == null) {
         throw new ConverterException(String.format("Invalid job ID: %s", jobId));
      }

      return getJobStatus(job);
   }

   private ConversionJobStatus getJobStatus(ConversionJobModel job) {
      Status status = mmode.redeem(job.createTicket());
      ConversionJobStatus jobStatus = new ConversionJobStatus();
      jobStatus.setJobId(job.getId());
      jobStatus.setProjectId(status.getProjectId());
      jobStatus.setPercent(status.getPercent());
      jobStatus.setPerformanceData(status.getPerfData());
      ConversionResult result = null;

      jobStatus.setState(ConversionJobStatus.toJobState(
            status.getCurrentState(), status.isRequestCancelled()));

      if (status.getCurrentState() == State.finished) {
         Disposition disposition;
         if (status.isRequestSuccess()){
            disposition = Disposition.succeeded;
         } else {
            disposition = Disposition.failed;
         }
         result = new ConversionResult(
                  disposition,
                  status.getLastRunningState(),
                  status.getLastCommand(),
                  status.getLastError());
      }
      jobStatus.setResult(result);

      return jobStatus;
   }

   @RequestMapping(
         value="/conversions/{jobId}/cancel",
         method=RequestMethod.POST)
   @ResponseBody
   public void cancelConversion(@PathVariable Long jobId) {
      _log.debug("Received POST request to cancel status of job " +
            jobId.toString());
      ConversionJobModel job = jobDao.getJobById(jobId);
      if (job == null) {
         throw new ConverterException("Invalid job ID");
      }

      Ticket ticket = job.createTicket();
      mmode.cancel(ticket);
      Status status = mmode.redeem(ticket);
      status.setCurrentState(State.cancelling);
   }
}
