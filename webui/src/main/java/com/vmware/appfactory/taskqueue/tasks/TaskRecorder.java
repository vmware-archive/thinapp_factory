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

package com.vmware.appfactory.taskqueue.tasks;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.FeedConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.FeedScanState;
import com.vmware.appfactory.taskqueue.tasks.state.ManualModeState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.TaskFactory;

/**
 * Strictly responsible for storing information about running tasks
 * and responding to various queries.
 *
 * Main tasks are:
 *  - record a new task, including:
 *      - the task itself
 *        (which has an ID and an "item handle", which is a reference
 *        to the TAF object being worked on by the task, such as App-123
 *        or Feed-99)
 *
 *  - remove a task by id
 *
 *  - list all tasks
 *
 *  - find tasks by:
 *     - id
 *     - MetaStatus of the task
 *     - type of the task
 *
 * This class is not concerned with the operations involving running the task
 * or changing the task state.
 */
@SuppressWarnings("AccessToStaticFieldLockedOnInstance")
class TaskRecorder {

   @Nonnull
   private final Map<Long, AppFactoryTask> mapTaskIdToTask;

   @Nonnull
   private final Multimap<String, AppFactoryTask> mapTaskHandleToTasks;

   @Nonnull
   public static final Function<AppFactoryTask, TaskState> fnGetCurrentState =
         new Function<AppFactoryTask, TaskState>() {
            @Override
            public TaskState apply(AppFactoryTask input) {
               return input.getCurrentTaskState();
            }
         };


   TaskRecorder(int expectedSize, int maxFinishedSize, int expectedTasksPerId) {

      mapTaskIdToTask = Maps.newHashMapWithExpectedSize(expectedSize);
      mapTaskHandleToTasks = LinkedHashMultimap.create(expectedSize + maxFinishedSize,
                                                       expectedTasksPerId);

   }

   /**
    * Records a new task.
    *
    * @param task    The task itself.  It must have an ID field set.
    */
   public synchronized void recordTask(AppFactoryTask task) {
      long taskId = task.getCurrentTaskState().getId();
      mapTaskIdToTask.put(taskId, task);

      String taskHandle = task.getTaskHandle();
      mapTaskHandleToTasks.put(taskHandle, task);
   }

   /**
    * Deletes a task from the recorder.
    *
    * @param taskId
    * id of the task to delete
    *
    * @return the AppFactoryTask which was removed from the recorder,
    * or null if no task by the given id was found.
    */
   @Nullable
   public synchronized AppFactoryTask eraseTask(long taskId) {
      AppFactoryTask removedTask = mapTaskIdToTask.remove(taskId);
      if (null == removedTask) {
         // nothing more to do
         return null;
      }

      String taskHandle = removedTask.getTaskHandle();
      mapTaskHandleToTasks.remove(taskHandle, removedTask);

      return removedTask;
   }

   /**
    * @return a list of all tasks currently recorded, completed or not,
    *         in no particular order.
    */
   @Nonnull
   public synchronized List<AppFactoryTask> getAllTasks() {
      return ImmutableList.copyOf(mapTaskIdToTask.values());
   }

   @Nullable
   public synchronized TaskState findTaskById(long id) {
      AppFactoryTask task = mapTaskIdToTask.get(id);
      if (null == task) {
         return null;
      }
      return task.getCurrentTaskState();
   }

   @Nonnull
   public synchronized Iterable<? extends AbstractCaptureState> findActiveTasksForApp(
         final long appId) {
      return Iterables.filter(
         Iterables.concat(
            findTasksForRecord(AppConvertState.TYPE, appId, AppConvertState.class),
            findTasksForRecord(ManualModeState.TYPE, appId, ManualModeState.class)
         ),
         MetaStatusPredicate.NOT_FINISHED
      );
   }

   synchronized AppFactoryTask getTaskFromId(long taskId) {
      return mapTaskIdToTask.get(taskId);
   }

   @Nonnull
   synchronized Iterable<? extends TaskState> findActiveTasksForFeed(
         final long feedId) {
      return Iterables.filter(
            Iterables.concat(
               findTasksForRecord(FeedScanState.TYPE, feedId, FeedScanState.class),
               findTasksForRecord(FeedConvertState.TYPE, feedId, FeedConvertState.class)
            ),
            MetaStatusPredicate.NOT_FINISHED
      );
   }

   @SuppressWarnings("unchecked")
   private <T extends TaskState> Iterable<T> findTasksForRecord(
         String taskType, long recordId, @Nonnull Class<T> klass) {
      String recordHandle = TaskFactory.makeRecordId(taskType, recordId);
      return ImmutableList.copyOf(
            (Iterable<T>)Iterables.filter(
                  Collections.unmodifiableCollection(
                        Collections2.transform(
                              mapTaskHandleToTasks.get(recordHandle),
                              fnGetCurrentState
                        )
                  ),
                  Predicates.instanceOf(klass))
      );
   }
}