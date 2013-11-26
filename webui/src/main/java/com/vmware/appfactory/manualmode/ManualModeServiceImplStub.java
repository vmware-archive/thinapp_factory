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

package com.vmware.appfactory.manualmode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.thinapp.common.converter.dto.Status;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.common.workpool.dto.VCConfig;

/**
 * This class simulates manual mode conversion workflow, but hides
 * some of the manual mode states to fit with current UI flow.
 *
 * @author saung
 * @since v1.0 5/18/2011
 */
@Service("manualModeServiceImplStub")
public class ManualModeServiceImplStub implements IManualModeService {
   /**
    * get the logger
    */
   private final Logger _log = LoggerFactory.getLogger(ManualModeServiceImplStub.class);
   /**
    * Manual mode requests map to keep track of all MM requests.
    */
   private static final ConcurrentHashMap<String, MMRequest> _requestMap = new ConcurrentHashMap<String, MMRequest>();

   @Resource
   private AfDaoFactory _daoFactory;

   /** Spring IoC injected variables from xxxx-config.xml */
   private String _dcName;
   private String _dcMoid;
   private String _vcHost;
   private String _vcUsername;
   private String _vcPassword;
   private String _vmGuestUsername;
   private String _vmGuestPassword;
   private String _vmVmxPath;

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#createRequest(com.vmware.appfactory.taskqueue.dto.CaptureRequest)
    */
   @Override
   public String createRequest(CaptureRequest captureRequest) {
      Long appId = captureRequest.getApplicationId();
      _log.debug("createRequest started. appId={}", appId);
      if (appId == null) {
         throw new IllegalArgumentException("appId must NOT be null!");
      }

      /* Load app from DB */
      Application app = _daoFactory.getApplicationDao().find(appId);
      /* Check required fields info */
      if (app == null) {
         throw new IllegalArgumentException("Invalid appId = " + appId);
      }
      if (app.getDownload() == null) {
         throw new IllegalArgumentException("No source path or download url for app id " + appId);
      }
      AppDownload download = app.getDownload();
      String sourceUrl = download.getURI().toString();

      String commandLine = "";
      /**
       * Add install command if available.
       */
      if (!CollectionUtils.isEmpty(app.getInstalls())) {
         final AppInstall install = app.getInstalls().get(0);
         commandLine = install.getCommand();
      }

      Long defaultOutputDatastoreId = captureRequest.getDatastoreId();
      _log.info("Sending a ticket request to ManualMode - [sourceUrl:" + sourceUrl +
            ",defaultOutputDatastore:"+ defaultOutputDatastoreId + ",commandLine:" + commandLine + "]");
      final Ticket t = new Ticket();
      /* Use current system time as unique ticket id */
      t.setId(String.valueOf(System.currentTimeMillis()));
      /* Create a fake status */
      final Status status = new Status();
      status.setLease(createLease());
      status.setProjectId(Long.valueOf(t.getId()));
      status.setStates(createHappenedSet());
      final MMRequest request = new MMRequest(status, generateStatesToSimulate().iterator());
      _requestMap.putIfAbsent(t.getId(), request);
      return t.getId();
   }

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#checkStatus(java.lang.Long)
    */
   @Override
   public Status checkStatus(Long ticketId) {
      _log.debug("Checking status for ticket id = {}", ticketId);
      Status status = null;
      String ticketIdStr = String.valueOf(ticketId);
      if(_requestMap.containsKey(ticketIdStr)) {
         final MMRequest req = _requestMap.get(ticketIdStr);
         status =  req.status;
      } else {
         _log.error(ticketId + " not found in the simulator request map.");
      }
      return status;
   }

   @Override
   public void next(Long ticketId, String stage) {
      _log.debug("Done manual app conversion. ticket id = {}", ticketId);
      String ticketIdStr = String.valueOf(ticketId);
      if (_requestMap.containsKey(ticketIdStr)) {
         final MMRequest req = _requestMap.get(ticketIdStr);
         if (req.statesToSimulate.hasNext()) {
            final State nextState = req.statesToSimulate.next();
            final State currState = req.status.getCurrentState();
            req.status.getStates().add(currState);
            _log.debug("Moved from state {} to {}", currState, nextState);
            /* Check end state */
            if(!req.statesToSimulate.hasNext()) {
               req.status.getStates().add(nextState);
            }
         } else {
            _log.debug("Simulation for " + ticketIdStr + " has already ended.");
         }
      } else {
         _log.debug(ticketIdStr + " not found in the simulator request map.");
      }
   }

   private static final SortedSet<State> createHappenedSet() {
      final SortedSet<State> happenedSet = new TreeSet<State>();
      happenedSet.add(State.poweringOnVm);
      happenedSet.add(State.acquiringVm);
      happenedSet.add(State.vmAcquired);
      return happenedSet;
   }

   private static final List<State> generateStatesToSimulate() {
      final List<State> statesToSimulate = new ArrayList<State>();
      statesToSimulate.add(State.installationWait);
      statesToSimulate.add(State.refreshingProjectDone);
      return statesToSimulate;
   }

   /**
    * This inner class is to encapsulate MM request state.
    */
   private static final class MMRequest {
      Status status;
      Iterator<State> statesToSimulate;

      /**
       * @param _status
       * @param _statesToSimulate
       */
      public MMRequest(Status _status, Iterator<State> _statesToSimulate) {
         this.status = _status;
         this.statesToSimulate = _statesToSimulate;
      }

   }

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#cancel(java.lang.Long)
    */
   @Override
   public void cancel(Long ticketId) {
      _log.debug("Canceling manual mode process. ticket id = {}", ticketId);
      String ticketIdStr = String.valueOf(ticketId);
      if (_requestMap.containsKey(ticketIdStr)) {
         _requestMap.get(ticketIdStr);
         _log.debug("Canceled MM. ticket id = {}", ticketId);

      } else {
         _log.warn("TicketId {} not found in the request map!", ticketId);
      }

   }

   /**
    * Create a lease from a test VM.
    * @return a Lease instance.
    */
   private Lease createLease() {
      Lease lease = new Lease();
      VCConfig vcConfig = new VCConfig();
      vcConfig.setDatacenter(_dcName);
      vcConfig.setDatacenterMoid(_dcMoid);
      vcConfig.setHost(_vcHost);
      vcConfig.setUsername(_vcUsername);
      vcConfig.setPassword(_vcPassword);

      InstanceInfo vmInfo = new InstanceInfo();
      vmInfo.setGuestUsername(_vmGuestUsername);
      vmInfo.setGuestPassword(_vmGuestPassword);
      vmInfo.setVmxPath(_vmVmxPath);

      lease.setVc(vcConfig);
      lease.setVm(vmInfo);

      return lease;
   }

   /**
    * @param dcName the dcName to set
    */
   public void setDcName(String dcName) {
      _dcName = dcName;
   }

   /**
    * @param dcMoid the dcMoid to set
    */
   public void setDcMoid(String dcMoid) {
      _dcMoid = dcMoid;
   }

   /**
    * @param vcHost the vcHost to set
    */
   public void setVcHost(String vcHost) {
      _vcHost = vcHost;
   }

   /**
    * @param vcUsername the vcUsername to set
    */
   public void setVcUsername(String vcUsername) {
      _vcUsername = vcUsername;
   }

   /**
    * @param vcPassword the vcPassword to set
    */
   public void setVcPassword(String vcPassword) {
      _vcPassword = vcPassword;
   }

   /**
    * @param vmGuestUsername the vmGuestUsername to set
    */
   public void setVmGuestUsername(String vmGuestUsername) {
      _vmGuestUsername = vmGuestUsername;
   }

   /**
    * @param vmGuestPassword the vmGuestPassword to set
    */
   public void setVmGuestPassword(String vmGuestPassword) {
      _vmGuestPassword = vmGuestPassword;
   }

   /**
    * @param vmVmxPath the vmVmxPath to set
    */
   public void setVmVmxPath(String vmVmxPath) {
      _vmVmxPath = vmVmxPath;
   }
}
