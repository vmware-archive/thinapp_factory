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

package com.vmware.appfactory.feed.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.common.exceptions.AfForbiddenException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.common.exceptions.AfServerErrorException;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.appfactory.config.model.FeedAddRemoveEvent;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.dto.FeedRequest;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles all the feed-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class FeedApiController
   extends AbstractApiController
{
   /**
    * Get a list of all the known feeds.
    *
    *
    * @param request - Servlet request.  Set by spring.
    * @param response - Servlet response.  Set by spring.
    * @param sort - Sort the data (defaults to false)
    * @return List of all feeds.
    * @throws IOException - if the etag headers could not be written
    * @throws AfServerErrorException
    */
   @ResponseBody
   @Nullable
   @RequestMapping(value = "/feeds", method = RequestMethod.GET)
   public List<Feed> getAllFeeds(
         @Nonnull HttpServletRequest request,
         @Nonnull HttpServletResponse response,
         @RequestParam(required = false) boolean sort)
         throws AfServerErrorException, IOException {

      // the e-tag for this list must be based on BOTH the feeds table
      // and the applications table.  This is because the Feed object contains
      // a count of ignored apps, and an app can be set to "ignored" without
      // touching the Feed object directly.
      if (checkModified(request,
                        response,
                        null,
                        _daoFactory.getFeedDao(),
                        _daoFactory.getApplicationDao())) {
         // shortcut exit - no further processing necessary
         return null;
      }

      try {
         final List<Feed> feeds = super.getFeedsList();

         if (sort) {
            Collections.sort(feeds);
            for (Feed feed : feeds) {
               Collections.sort(feed.getApplications());
            }
         }

         return feeds;
      }
      catch(Exception ex) {
         throw new AfServerErrorException(ex);
      }
   }


   /**
    * Get a single feed, including all its applications.
    * The path variable can either be a feed ID or a feed name.
    * @param idOrName Feed ID or name.
    * @param sort
    * @return The requested feed.
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds/{idOrName}",
         method = RequestMethod.GET)
   public Feed getFeed(
         @PathVariable String idOrName,
         @RequestParam(required=false) boolean sort)
      throws AfNotFoundException
   {
      Feed feed = null;
      FeedDao feedDao = _daoFactory.getFeedDao();

      try {
         /* First see if we were given an ID */
         Long id = Long.valueOf(idOrName);
         feed = feedDao.find(id);
      }
      catch(NumberFormatException ex) {
         /* Ignore */
      }

      if (feed == null) {
         /* Secondly, see if we were given a name */
         feed = feedDao.findByName(idOrName);
      }

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed ID or name " + idOrName);
      }

      if (sort) {
         Collections.sort(feed.getApplications());
      }

      return feed;
   }


   /**
    * Delete a feed.
    * @param id
    * @throws AfNotFoundException
    * @throws AfForbiddenException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds/{id}",
         method = RequestMethod.DELETE)
   public void deleteFeed(@PathVariable Long id)
      throws AfNotFoundException, AfForbiddenException
   {
      FeedDao feedDao = _daoFactory.getFeedDao();
      Feed feed = feedDao.find(id);

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed ID " + id);
      }

      // Cleanup feed scans & abort any applications that are being captured.
      deleteFeedAndRelated(feed);

      publishEvent(new FeedAddRemoveEvent(feed));
   }


   /**
    * Deletes a feed and all the other stuff that points back to it.
    *
    * This will delete related jobs and job batches.
    */
   private void deleteFeedAndRelated(Feed feed) {
      // Abort the currently scanning task if any.
      Iterable<? extends TaskState> activeTasks = _scanningQueue.findActiveTasksForFeed(feed.getId());

      if (!Iterables.isEmpty(activeTasks)) {
         Iterator i = activeTasks.iterator();
         while(i.hasNext()) {
            TaskState s = (TaskState)i.next();
            _log.info("Aborting active feed task({}) with status: {}", s.getId(), s.getStatus());
            // Abort running feed scan task.
            _scanningQueue.abortTask(s.getId());
         }
      }

      // Abort any running conversions pertaining to this feed.
      deleteAppSourceRelated(feed);

      // Now we can safely delete the feed.
      _daoFactory.getFeedDao().delete(feed);
   }


   /**
    * Update a feed.
    * @param id
    * @param spec
    * @throws AfNotFoundException
    * @throws AfConflictException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds/{id}",
         method = RequestMethod.PUT)
   public void updateFeed(
         @PathVariable Long id,
         @RequestBody FeedRequest spec)
         throws AfNotFoundException, AfConflictException, AfBadRequestException, DsException, WpException {
      FeedDao feedDao = _daoFactory.getFeedDao();
      Feed feed = feedDao.find(id);

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed ID " + id);
      }

      try {
         /* Basic spec validation */
         spec.validate();
      }
      catch(InvalidDataException ex) {
         throw new AfBadRequestException(ex.getMessage());
      }

      /* Make sure the name is not used by another feed */
      Feed namedFeed = feedDao.findByName(spec.name);
      if (namedFeed != null && !namedFeed.getId().equals(id)) {
         throw new AfConflictException("Feed name already in use.");
      }

      /* Update the feed: basics */
      feed.setName(spec.name);
      feed.setDescription(AfText.plainTextInstance(spec.description));
      feed.setUrl(spec.realUrl);

      /* Update the feed: Options */
      feed.setOkToScan(spec.okToScan);
      feed.setOkToConvert(spec.okToConvert);

      /* Update the feed: HTTP authentication. */
      if (spec.authRequired) {
         feed.setUsername(spec.authUsername);
         feed.setPassword(spec.authPassword);
      }
      else {
         feed.setUsername("");
         feed.setPassword("");
      }

      /* Update the feed: applications to include/skip */
      if (spec.appIncludes != null) {
         for (Long appId : spec.appIncludes.keySet()) {
            boolean include = spec.appIncludes.get(appId).booleanValue();

            Application app = feed.findApplication(appId);
            if (app == null) {
               throw new AfBadRequestException("Invalid application " + appId + " for feed " + id);
            }

            // Reset the application's fail counter for apps that are made available.
            if (include && app.isSkipped()) {
               app.setFailCount(0);
            }
            app.setSkipped(!include);

            _daoFactory.getApplicationDao().update(app);
         }
      }

      /* Save the feed */
      feedDao.update(feed);

      /* Force a rescan now */
      scanFeedIfNotBusy(feed);
   }


   /**
    * Reset a feed.
    * This will set a feed back to the state it was in when it was first
    * created. This means any error is removed, and the previous scan and
    * convert times are set to "never".
    *
    * @param id Feed to reset
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds/{id}/reset",
         method = RequestMethod.PUT)
   public void resetFeed(
         @PathVariable Long id)
      throws AfNotFoundException
   {
      FeedDao feedDao = _daoFactory.getFeedDao();
      Feed feed = feedDao.find(id);

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed ID " + id);
      }

      feed.setFailure(null);
      feed.setLastConversion(AfCalendar.NEVER);
      feed.setLastScan(AfCalendar.NEVER);

      feedDao.update(feed);
   }


   /**
    * Scan a feed.
    * This will initiate a scan of the given feed, unless there is already a
    * scan of this feed scheduled or running. This occurs even if the feed
    * is marked for "no scan".
    *
    * @param id Feed to reset
    * @throws AfNotFoundException
    * @throws AfConflictException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds/{id}/scan",
         method = RequestMethod.PUT)
   public void forceFeedScan(
         @PathVariable Long id)
         throws AfNotFoundException, AfConflictException, DsException, WpException {
      FeedDao feedDao = _daoFactory.getFeedDao();
      final Feed feed = feedDao.find(id);

      if (feed == null) {
         throw new AfNotFoundException("Invalid feed ID " + id);
      }

      boolean queued = scanFeedIfNotBusy(feed);
      if (!queued) {
         throw new AfConflictException("FEED_TASK_EXISTS");
      }
   }


   /**
    * Create a new feed.
    * @param spec
    * @throws AfBadRequestException
    * @throws AfConflictException
    */
   @ResponseBody
   @RequestMapping(
         value = "/feeds",
         method = RequestMethod.POST)
   public void createFeed(@RequestBody FeedRequest spec)
         throws AfBadRequestException,
         AfConflictException, DsException, AfNotFoundException, WpException {
      FeedDao feedDao = _daoFactory.getFeedDao();

      try {
         /* Basic spec validation */
         spec.validate();
      }
      catch(InvalidDataException ex) {
         throw new AfBadRequestException(ex);
      }

      /* Name must be unique */
      if (feedDao.findByName(spec.name) != null) {
         throw new AfConflictException("Feed name already in use.");
      }

      /* Create the feed */
      Feed feed = new Feed();
      feed.setName(spec.name);
      feed.setOkToScan(spec.okToScan);
      feed.setOkToConvert(spec.okToConvert);
      if (spec.authRequired) {
         feed.setUsername(spec.authUsername);
         feed.setPassword(spec.authPassword);
      }

      feed.setUrl(spec.realUrl);
      feedDao.create(feed);

      publishEvent(new FeedAddRemoveEvent(feed));

      // scan the feed immediately; no need to wait for the background
      // thread to pick it up, as the background feed should really only
      // run occasionally
      if (spec.okToScan) {
         scanFeedIfNotBusy(feed);
      }
   }


   /**
    * If there are no scan or conversion tasks currently running or waiting for
    * the specified feed, then schedule a scan task now. Note: a successful
    * scan task triggers a convert task when complete.
    *
    * @param feed
    * @return True if a task was scheduled.
    */
   private boolean scanFeedIfNotBusy(Feed feed) throws AfNotFoundException, WpException, DsException {
      /* Find tasks that already scan this feed */
      Iterable<? extends TaskState> matches = _scanningQueue.findActiveTasksForFeed(feed.getId());

      if (Iterables.isEmpty(matches)) {
         _log.debug("Adding forced scan task of " + feed);
         AppFactoryTask task = _taskFactory.newFeedScanTask(
               feed,
               _conversionsQueue);
         _scanningQueue.addTask(task);
         return true;
      }

      _log.debug("Ignore forced scan task of " + feed + ": tasks in queue");
      return false;
   }
}
