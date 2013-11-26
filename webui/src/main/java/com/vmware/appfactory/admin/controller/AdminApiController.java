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

package com.vmware.appfactory.admin.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.auth.dto.PasswordResetRequest;
import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.cws.CwsServerInfo;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.file.FileData;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.taskqueue.tasks.MetaStatusPredicate;
import com.vmware.thinapp.common.util.AfUtil;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles all the Appliance administration-related API calls.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class AdminApiController
   extends AbstractApiController
{
   @Autowired
   private UserDetailsManager _userDetailsManager;

   /**
    * Reboot the appliance.
    * @throws CwsException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/reboot",
         method = RequestMethod.POST)
   public void reboot()
      throws CwsException
   {
      _log.debug("Received /admin/reboot POST request");
      _cwsClient.reboot();
   }


   /**
    * Obtain the byte array for a ZIP file of the appliance system logs.
    * @param response
    * @return
    * @throws CwsException
    */
   @RequestMapping(
         value="/admin/logs",
         method=RequestMethod.GET)
   public void getLogs(HttpServletResponse response)
      throws CwsException
   {
      FileData zipFileData = _cwsClient.getLogs();
      getLogsHelper(zipFileData, response);
   }


   /**
    * Obtain the byte array for a ZIP file of the appliance system logs.
    *
    * @param id ID of the project whose logs are being requested
    * @param response
    * @return
    * @throws CwsException
    */
   @RequestMapping(
         value="/admin/logs/projects/{id}",
         method=RequestMethod.GET)
   public void getProjectLogs(
         @PathVariable Long id,
         HttpServletResponse response)
      throws CwsException
   {
      FileData zipFileData = _cwsClient.getProjectLogs(id);
      getLogsHelper(zipFileData, response);
   }


   private void getLogsHelper(
         FileData zipFileData,
         HttpServletResponse response)
      throws CwsException
   {
      _log.debug("Received ZIP file data, length={}", zipFileData.getContentLength());

      response.setHeader(AfUtil.CONTENT_DISPOSITION, zipFileData.getContentDisposition());
      response.setContentType(zipFileData.getContentType());
      response.setContentLength(zipFileData.getContentLength());

      try {
         FileCopyUtils.copy(zipFileData.getInputStream(), response.getOutputStream());
      }
      catch (IOException ex) {
         throw new CwsException(ex);
      }
   }


   /**
    * Get various info about the server.
    * @return
    * @throws CwsException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/info",
         method = RequestMethod.GET)
   public CwsServerInfo getServerInfo()
      throws CwsException
   {
      _log.debug("Received /admin/info GET request");
      CwsServerInfo info = _cwsClient.getServerInfo();
      _log.debug("Received date/time " + info.date + " and uptime (seconds) " + info.uptime);
      return info;
   }


   /**
    * Get the host/guest time synchronization status.
    * @return
    * @throws CwsException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/timesync",
         method = RequestMethod.GET)
   public boolean getTimeSync()
      throws CwsException
   {
      _log.debug("Received /admin/timesync GET request");

      boolean state = _cwsClient.getTimeSync().booleanValue();
      _log.debug("Received timesync state: " + state);
      return state;
   }


   /**
    * Enable host/guest time synchronization.
    * @throws CwsException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/timesync/enable",
         method = RequestMethod.POST)
   public void enableTimeSync()
      throws CwsException
   {
      _log.debug("Received /admin/timesync/enable POST request");
      _cwsClient.setTimeSync(true);
   }


   /**
    * Disable host/guest time synchronization.
    * @throws CwsException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/timesync/disable",
         method = RequestMethod.POST)
   public void disableTimeSync()
      throws CwsException
   {
      _log.debug("Received /admin/timesync/disable POST request");
      _cwsClient.setTimeSync(false);
   }

   /**
    * Return a list of system statistics.
    *
    * @return A map of name/value pairs of system information.
    * @throws AfServerErrorException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/stats",
         method = RequestMethod.GET)
   public Map<String, Object> stats()
      throws AfServerErrorException
   {
      final Map<String, Object> stats = new HashMap<String, Object>();

      /* Feeds */
      final FeedDao feedDao = _daoFactory.getFeedDao();
      stats.put("numFeedsTotal", Long.valueOf(feedDao.countAll()));

      /* File Shares */
      final FileShareDao fsDao = _daoFactory.getFileShareDao();
      stats.put("numFileSharesTotal", Long.valueOf(fsDao.countAll()));

      /* Applications */
      final ApplicationDao appDao = _daoFactory.getApplicationDao();
      stats.put("numAppsTotal", Long.valueOf(appDao.countAllIncluded()));

      /* Tasks */
      stats.put("numTasksRunning", Long.valueOf( Iterables.size(_conversionsQueue.getTasks(MetaStatusPredicate.RUNNING))));

      /* Builds */
      final BuildDao buildDao = _daoFactory.getBuildDao();
      stats.put("numBuildsTotal", Long.valueOf(buildDao.countAll()));
      stats.put("numBuildsPublished", Long.valueOf(buildDao.findForStatus(Build.Status.PUBLISHED).size()));

      /* Workpools status */
      int totalWp = 0;
      int numAvailable = 0;
      try {
         final List<Workpool> wpList = _wpClient.getAllWorkpools();
         if (wpList != null) {
            totalWp = wpList.size();
            for (Workpool wp : wpList) {
               if (wp.getState() == Workpool.State.available) {
                  numAvailable++;
               }
            }
         }
      }
      catch (WpException e) {
         /* Workpools not available? */
      }
      stats.put("totalWorkpools", Integer.valueOf(totalWp));
      stats.put("numAvailableWorkpools", Integer.valueOf(numAvailable));

      return stats;
   }

   /**
    * Reset 'admin' password.
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/admin/resetpwd",
         method = RequestMethod.PUT)
   public void changePassword(
         @RequestBody PasswordResetRequest request)
      throws AfBadRequestException
   {
      _log.debug("Received Password reset to the current logged-in user");
      request.validate();
      _userDetailsManager.changePassword(request.oldPassword, request.newPassword);
      SecurityContextHolder.clearContext();
   }

   @ResponseBody
   @RequestMapping(value = "/server-health", method = RequestMethod.GET)
   public String serverHealthCheck() {
      boolean healthy;
      try {
         _cwsClient.getTimeSync();

         _daoFactory.getConfigDao().lastModified();

         healthy = true;
      } catch (CwsException e) {
         healthy = false;
      } catch (HibernateException e) {
         healthy = false;
      }

      return Boolean.toString(healthy);
   }
}
