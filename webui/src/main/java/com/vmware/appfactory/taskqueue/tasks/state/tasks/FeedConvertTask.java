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

package com.vmware.appfactory.taskqueue.tasks.state.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.FeedConvertState;
import com.vmware.thinapp.common.base.HasId;


/**
 * A task that will look at all the applications in a feed, find ones that have
 * no matching build yet, and kick of more tasks to create them.
 */
class FeedConvertTask extends FeedTask<
      FeedConvertState,
      FeedConvertState.Builder,
      FeedConvertState.FeedConvertStatus>
{
   /**
    * Create a new instance to queue conversions for the specified feed.
    * Nothing will be done until this task is added to the task queue.
    *
    * @param feed Feed to convert.
    * @param taskHelperFactory   Accessor for global objects.
    * @param conversionsQueue    Queue on which to place new conversion tasks.
    * @param maxConversionAttempts Number of times to try building each app.
    *                              Must be at least 1.
    * @param horizonUrl           URL to Horizon organization.
    */
   FeedConvertTask(Feed feed,
                   TaskHelperFactory taskHelperFactory,
                   TaskQueue conversionsQueue,
                   int maxConversionAttempts,
                   long conversionWorkpoolId,
                   long conversionDatastoreId,
                   long conversionRuntimeId)
   {
      super(conversionsQueue,
            taskHelperFactory,
            new FeedConvertState.Builder()
            .withNewId(taskHelperFactory.getTaskIdSupplier())
            .withRecordId(feed.getId())
            .withDescription("Queueing conversions for feed \"" + feed.getName() + '"')
            .withMaxConverisonAttempts(maxConversionAttempts)
            .withConverisonWorkpoolId(conversionWorkpoolId)
            .withConversionDatastoreId(conversionDatastoreId)
            .withConversionRuntimeId(conversionRuntimeId)
            .withStatus(FeedConvertState.FeedConvertStatus.waiting)
            .build()
      );
   }

   @Override
   protected void runFeedTask(@Nonnull FeedDao feedDao, @Nonnull Feed feed)
         throws TaskException {

      updateStatus(FeedConvertState.FeedConvertStatus.comparing);
      _log.debug("Attempting to create builds from " + feed);

      List<Application> appsToBuild = new ArrayList<Application>();

      /*
       * First, add all the apps from the feed that are marked for
       * conversion.
       */
      appsToBuild.addAll(feed.getIncludedApplications());
      _log.debug("Apps in feed: " + appsToBuild.size());

      /*
       * Second, remove apps that have builds or running/waiting tasks
       * or have failed more then the allowed failure count.
       */
      BuildDao buildDao = getTaskHelperFactory().getDaoFactory().getBuildDao();
      Iterator<Application> it = appsToBuild.iterator();
      while (it.hasNext()) {
         Application app = it.next();
         if (appHasMaxedOutFailAttempts(app)
               || appHasBuilds(buildDao, app)
               || appHasTasks(_conversionsQueue, app)) {
            it.remove();
         }
      }
      _log.debug("Apps without builds/tasks/belowMaxFailLimit: "
            + appsToBuild.size());

      /* Create a job for each application */
      updateStatus(FeedConvertState.FeedConvertStatus.queueing);
      for (Application app : appsToBuild) {
         CaptureRequestImpl cr = new CaptureRequestImpl(
               app.getId(),
               app.getIcons(),
               app.getDisplayName(),
               feed.getName() + '-' + app.getSuggestedBuildName(),
               getCurrentTaskState().getConversionWorkpoolId(),
               getCurrentTaskState().getConversionDatastoreId(),
               getCurrentTaskState().getConversionRuntimeId(),
               false,
               getTaskHelperFactory()
         );
         _conversionsQueue.addTask(
               getTaskHelperFactory().getTaskFactory().newAppConvertTask(cr, _conversionsQueue)
         );
      }

      // todo: move to proper completion method?
      /* Set feed to QUEUED */
      updateStatus(FeedConvertState.FeedConvertStatus.complete);
      NotificationService.INSTANCE.newInfoEvent(
            appsToBuild.size() + " applications from " + feed.getName() + " are now waiting for auto capture.",
            Component.autoCapture);
   }

   private static boolean appHasBuilds(
         BuildDao buildDao,
         final Application app)
   {
      return !buildDao.findForApp(app).isEmpty();
   }


   /**
    * Helper method to check if the app has already failed
    * AfConfigRegistry.APPS_MAX_CONVERT_ATTEMPT times.
    *
    * @param app
    * @return
    */
   protected boolean appHasMaxedOutFailAttempts(
         final Application app)
   {
      return (app.getFailCount() >= getCurrentTaskState().getMaxConversionAttempts());
   }


   private static boolean appHasTasks(
         TaskQueue taskQueue,
         final HasId app)
   {
      return !Iterables.isEmpty(taskQueue.findActiveTasksForApp(app.getId()));
   }

   @Override
   protected void doCleanup() throws TaskException {
      // nothing to do
   }
}
