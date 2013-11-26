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

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.TaskStateBuilder;


/**
 * A task associated with a particular feed.
 * This does nothing in particular: see subclasses such as AfFeedScanTask
 * and AfFeedConvertTask.
 */
abstract class FeedTask
      <T extends TaskState<T,S,E>, S extends TaskStateBuilder<T,S,E>, E extends Enum<E>>
   extends AbstractTask<T,S,E>
{
   protected final TaskQueue _conversionsQueue;


   /**
    * Create a new instance for working with the specified feed.
    * Nothing will be done until this task is added to the task queue.
    *
    * @param taskHelperFactory   Accessor for global objects.
    * @param initialState  Initial state of the feed task.
    */
   FeedTask(TaskQueue conversionsQueue,
            TaskHelperFactory taskHelperFactory,
            T initialState)
   {
      super(taskHelperFactory, initialState);
      _conversionsQueue = Preconditions.checkNotNull(conversionsQueue);
   }


   /**
    * Implement run() from AbstractTask by just passing the feed DAO and
    * feed object to runFeedTask(). Removes the need for duplicate code in
    * any subclass.
    *
    */
   @Override
   protected void doRun()
      throws TaskException
   {
      FeedDao feedDao = getTaskHelperFactory().getDaoFactory().getFeedDao();

      /*
       * Get the feed. If not found, pass null to runFeedTask since some
       * tasks might be OK with a missing feed.
       */
      long feedId = getCurrentTaskState().getRecordId();
      Feed feed = feedDao.find(feedId);

      /* Feed is required for this task */
      if (feed == null) {
         _log.error("No feed entity for feedId: {}", feedId);
         // When no feed, remove this task completely.
         throw new TaskException(this, "Feed no longer exists");
      }

      runFeedTask(feedDao, feed);
   }


   /**
    * Since all feed tasks are likely to need the feed DAO, require a
    * run() method that supplies it.
    *
    * @param feedDao
    * @param feed
    * @throws TaskException
    */
   protected abstract void runFeedTask(
         @Nonnull FeedDao feedDao,
         @Nonnull Feed feed)
      throws TaskException;
}
