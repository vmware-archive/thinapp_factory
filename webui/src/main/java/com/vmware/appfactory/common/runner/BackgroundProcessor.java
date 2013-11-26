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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.common.dao.AfDaoFactory;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This is a scheduled background task which will execute periodically.
 * It updates everything in the AppFactory database. Any client UI needs to
 * update itself.
 */
public class BackgroundProcessor
   extends TimerTask
{
   /** If we fail to get the actual refresh period, use this as a back-up */
   private static final long FALLBACK_REFRESH_PERIOD_SECS = 10;

   /**
    * Default frequency (in mins) to scan feeds if no valid value exists.
    */
   public static final long DEFAULT_SCAN_FREQUENCY = 30L;

   private final Logger _log = LoggerFactory.getLogger(BackgroundProcessor.class);

   @Resource(name = "conversionsQueue")
   private TaskQueue _conversionsQueue;

   @Resource(name = "scanningQueue")
   private TaskQueue _scanningQueue;

   @Resource
   private AfDaoFactory _daoFactory;

   @Resource
   private ConfigRegistry _config;

   @Resource
   private TaskFactory _taskFactory;

   /**
    * Constructor for the initial bean which is created by the container
    * when the application starts up.
    */
   public BackgroundProcessor()
   {
      /* Nothing to do */
   }


   /**
    * Constructor for second and all subsequent invocations.
    * Each instance of this class schedules the next, as appropriate.
    *
    * @param processor  to copy from
    */
   public BackgroundProcessor(BackgroundProcessor processor)
   {
      _conversionsQueue = processor._conversionsQueue;
      _scanningQueue = processor._scanningQueue;
      _daoFactory = processor._daoFactory;
      _config = processor._config;
      _taskFactory = processor._taskFactory;
   }


   @Override
   public void run()
   {
      long time = System.currentTimeMillis();
      _log.trace("Background processor: Start");
      try {
         /*
         * Job #1:
         * Examine all the feeds and kick of scan/convert tasks as needed
         */
         try {
            processFeeds();
         } catch (DsException e) {
            _log.error("Could not get default datastore", e);
         } catch (AfNotFoundException e) {
            _log.error("Could not find default datastore or workpool", e);
         } catch (WpException e) {
            _log.error("Could not get default workpool", e);
         }

         /* How long did that take? */
         time = System.currentTimeMillis() - time;
         _log.trace("Background processor: End (" + time + " ms)");

      } finally {
         /*
         * Schedule the next update.
         * Be careful that we always schedule something (assuming the configuration
         * value is non-zero) else task and feed processing will stop.
         */
         long delayS = _config.getLong(
               ConfigRegistryConstants.GEN_REFRESH_PERIOD_SECS,
               FALLBACK_REFRESH_PERIOD_SECS);

         // TODO: could we just use a spring timer and be done with it?
         if (delayS > 0) {
            _log.trace("Next background processor in " + delayS + " secs");
            Timer t = new Timer();
            t.schedule(
                  new BackgroundProcessor(this),
                  delayS * 1000);
         }
      }
   }


   private void processFeeds() throws DsException, AfNotFoundException, WpException {
      _log.trace("Scanning feeds:");
      FeedDao feedDao = _daoFactory.getFeedDao();
      List<Feed> feeds = feedDao.findAll();

      long rescanPeriodMs = getFeedRescanFrequency();
      boolean retryFailedScan = _config.getBool(ConfigRegistryConstants.FEED_RETRY_FAILED_SCAN);

      for (final Feed feed : feeds) {
         /* Skip if scanning disabled */
         if (!feed.isOkToScan()) {
            continue;
         }

         /* Skip if recently scanned */
         if ((AfCalendar.Now() - feed.getLastScan()) < rescanPeriodMs) {
            continue;
         }

         /* Skip if failed */
         if ((null != feed.getFailure()) && !retryFailedScan) {
            continue;
         }

         /* See what active tasks we have for this feed */

         // then conversion tasks
         Iterable<? extends TaskState> activeTasks = _scanningQueue.findActiveTasksForFeed(feed.getId());
         /* Skip if currently active */
         if (!Iterables.isEmpty(activeTasks)) {
            continue;
         }

         /* Feed needs to be scanned */

         AppFactoryTask feedTask = _taskFactory.newFeedScanTask(
               feed,
               _conversionsQueue
         );
         _scanningQueue.addTask(feedTask);
      }
   }


   /**
    * Helper method to get the feed rescan frequency in milliseconds
    * based on the configuration value, or the default.
    *
    * NOTE: The feed scan frequency is at least once every minute.
    *
    * @return
    */
   private long getFeedRescanFrequency()
   {
      long rescanPeriodMins = _config.getLong(
            ConfigRegistryConstants.FEED_RESCAN_PERIOD_MINS,
            DEFAULT_SCAN_FREQUENCY);

      return 60000 * (rescanPeriodMins == 0L? DEFAULT_SCAN_FREQUENCY
            : rescanPeriodMins);
   }
}
