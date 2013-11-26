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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.cws.CwsHelper;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.CommandList;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.converter.dto.Status;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.converter.dto.TicketRequest;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This class implements manual mode's ticket creation to request a VM,
 * checking VM status and submitting commands to the VM.
 *
 * @author saung
 * @since v1.0 5/10/2011
 */
@Service("manualModeServiceImpl")
public class ManualModeServiceImpl implements IManualModeService {
   /**
    * get the logger
    */
   private final Logger _log = LoggerFactory.getLogger(ManualModeServiceImpl.class);

   @Resource
   private AfDaoFactory _daoFactory;
//
//   @Resource
//   protected CwsClientService _cwsClient;

   @Resource
   private ManualModeClientService _mmodeClient;

   @Resource
   private WorkpoolClientService _wpClient;

   @Nonnull
   @Resource
   private ConfigRegistry config;

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#createRequest(com.vmware.appfactory.taskqueue.dto.CaptureRequest)
    */
   @Override
   public String createRequest(CaptureRequest captureRequest) throws DsException, WpException, AfNotFoundException {
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

      final AppDownload download = app.getDownload();
      String sourceUrl = download.getURI().toString();

      String commandLine = "";
      /**
       * Add install command if available.
       */
      if (!CollectionUtils.isEmpty(app.getInstalls())) {
         final AppInstall install = app.getInstalls().get(0);
         commandLine = install.getCommand();
      }

      _log.info("Request ManualMode convert ticket - [sourceUrl: " + sourceUrl
            + ", outputDatastoreId: "+ captureRequest.getDatastoreId()
            + ",commandLine: " + commandLine + "]");

      // Create a list of input files for the manual conversion
      List<ProjectFile> inputFiles = new ArrayList<ProjectFile>();
      inputFiles.add(new ProjectFile(null, sourceUrl));

      // Create a list of commands for various phases of the manual conversion
      Map<ConversionPhase, CommandList> commands = new HashMap<ConversionPhase, CommandList>();
      List<Command> installCommands = new ArrayList<Command>();
      installCommands.add(new Command(null, commandLine));
      commands.put(ConversionPhase.install, new CommandList(installCommands));

      // Enable QR on the ThinApp package if necessary
      boolean enableQR = config.getBool(ConfigRegistryConstants.CWS_ENABLE_QUALITY_REPORTING);
      if (enableQR) {
         String tagQR = config.getString(ConfigRegistryConstants.CWS_QUALITY_REPORTING_TAG);
         List<Command> prebuildCommands = new ArrayList<Command>();
         CwsHelper.enableQR(prebuildCommands, tagQR);
         commands.put(ConversionPhase.prebuild, new CommandList(prebuildCommands));
      }

      final TicketRequest ticketRequest = new TicketRequest(
            inputFiles,
            captureRequest.getDatastoreId().toString(),
            commands,
            false,
            _wpClient.getWorkpoolById(captureRequest.getWorkpoolId()),
            captureRequest.getRuntimeId());
      final Ticket t = _mmodeClient.create(ticketRequest);

      return t.getId();
   }

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#checkStatus(java.lang.Long)
    */
   @Override
   public Status checkStatus(Long ticketId) {
      final Ticket ticket = new Ticket();
      ticket.setId(String.valueOf(ticketId));
      final Status status = _mmodeClient.redeem(ticket);

      return status;
   }

   @Override
   public void next(Long ticketId, String stage) {
      _log.debug("Done manual app conversion. ticket id = {}", ticketId);
      final Ticket ticket = new Ticket();
      ticket.setId(String.valueOf(ticketId));
      _mmodeClient.next(ticket, stage);
   }

   /**
    * @see com.vmware.appfactory.manualmode.IManualModeService#cancel(java.lang.Long)
    */
   @Override
   public void cancel(Long ticketId) {
      _log.debug("Canceling manual mode process. ticket id = {}", ticketId);
      final Ticket ticket = new Ticket();
      ticket.setId(String.valueOf(ticketId));
      _mmodeClient.cancel(ticket);
   }
}
