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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;

public interface TaskQueue {

   /**
    * Add a new task to the queue.
    * Creating a new task is not sufficient for it to actually do anything:
    * you must add it to a queue. It goes immediately into the "WAITING"
    * bucket until the queue updates, at which point it might move it to the
    * "RUNNING" bucket, if there is room.
    *
    * @param task   Task to add.
    *
    * Precondition: task is not already added to the queue
    */
   void addTask(AppFactoryTask task);

   /**
    * @return all tasks in this queue.
    *
    * The returned list is in the following order:
    *  1. Completed / Cancelled tasks
    *     - as indicated by MetaStatus.FINISHED
    *     - sorted by increasing "finish" timestamp
    *       - in the event of a tie, sorted by order of increasing ID
    *  2. Running / Cancelling tasks
    *     - as indicated by MetaStatus.RUNNING
    *     - sorted by increasing "started" timestamp
    *       - in the event of a tie, sorted by order of increasing ID
    *  3. Waiting (queued) tasks
    *     - as indicated by MetaStatus.WAITING
    *     - sorted in queued precedence order
    *       - by default, new tasks will appear at the end
    *       - this order may be changed by calling
    *         {@link TaskQueue#moveToHead(long)},
    *         {@link TaskQueue#moveToTail(long)},
    *         or {@link TaskQueue#moveBefore(long, long)}.
    */
   @Nonnull
   Iterable<TaskState> getAllTasks();

   /**
    * Find a task of a given meta-status that match a predicate
    * function.
    *
    * The returned list is in no particular order.
    *
    * @return The matching tasks, or an empty array. Never null.
    */
   @Nonnull
   Iterable<TaskState> getTasks(Predicate<? super TaskState> predicate);

   /**
    * Find the task with the given ID.
    * If not found, returns null.
    *
    * @param id
    * @return The matching task or null if no such ID.
    */
   @Nullable
   TaskState findTaskById(long id);

   /**
    * Find all running or queued tasks that are in some way related to the specified
    * application.
    *
    * @param appId
    * @return
    */
   @Nonnull
   Iterable<? extends AbstractCaptureState> findActiveTasksForApp(long appId);

   /**
    * Find all tasks that are in some way related to the specified feed.
    *
    * NOTE: All derived classes of AfFeedTasks are matched.
    *
    * @param feedId
    * @return
    */
   @Nonnull
   Iterable<? extends TaskState> findActiveTasksForFeed(long feedId);

   /**
    * Abort a task.
    *
    * If the task is waiting or finished, it is simply discarded.
    * If it is running, it is marked for abort, but it's up to the specific
    * Task implemented to decide whether or not to do anything about it.
    *
    * @param taskId  ID of the task to abort, as returned from {@see TaskState}.getId()
    *
    * @return current state of the task, if cancelled
    *         or null if no task was found
    */
   @Nullable
   TaskState abortTask(long taskId);

   /**
    * Moves the task specified by taskIdToMove to the head of the
    * WAITING queue.
    *
    * Preconditions:
    *  - taskIdToMove is the ID of a task already in the queue
    *  - the task referenced by taskIdToMove is not yet running or
    *    cancelled (i.e. has MetaStatus.WAITING)
    *
    * Postconditions:
    *  - true is returned => the task is at the head of the queue
    *  - false is returned => the queue order has not been changed
    *
    * @param taskIdToMove
    * The id of the WAITING task, as returned by {@link TaskState#getId()}.
    *
    * @return
    * true if the task was moved, or if the task was already at the head
    * of the queue.
    *
    * false if the task was not moved.  This can happen if taskIdToMove
    * is no longer in the queue.
    */
   boolean moveToHead(long taskIdToMove);

   /**
    * Moves the task specified by taskIdToMove to the tail of the
    * WAITING queue.
    *
    * Preconditions:
    *  - taskIdToMove is the ID of a task already in the queue
    *  - the task referenced by taskIdToMove is not yet running or
    *    cancelled (i.e. has MetaStatus.WAITING)
    *
    * Postconditions:
    *  - true is returned => the task is at the tail of the queue
    *  - false is returned => the queue order has not been changed
    *
    * @param taskIdToMove
    * The id of the WAITING task, as returned by {@link TaskState#getId()}.
    *
    * @return
    * true if the task was moved, or if the task was already at the tail
    * of the queue.
    *
    * false if the task was not moved.  This can happen if taskIdToMove
    * is no longer in the queue.
    */
   boolean moveToTail(long taskIdToMove);

   /**
    * Changes the priority of the given task so that runs immediately after another
    * specific task already in the queue.
    *
    * @param taskIdToMove
    * task ID whose priority you want to change
    *
    * @param taskIdToFollow
    * task ID that this task will be moved immediately after.
    *
    * This task will be assigned a new priority so that it will run immediately
    * before the task specified by taskIdToPrecede.
    *
    * @return  true if the priority was changed
    *          false on failure (e.g. the task specified by taskIdToMove
    *          or taskIdToFollow is no longer in the queue, or
    *          taskIdToMove and taskIdToFollow are equal to each other).
    */
   boolean moveBefore(long taskIdToMove, long taskIdToFollow);

   /**
    * Remove all tasks that are finished.
    */
   void cleanup();

   /**
    * Remove a single finished task.
    *
    * @param taskId
    * id of the task to cleanup.  This task must be in the MetaStatus.FINISHED state.
    *
    * @return
    * true if a finished task of the given ID was found and removed.
    * false otherwise (e.g. the task was not found, or the task was not finished).
    */
   boolean cleanup(long taskId);

   /**
    * Abort every running or waiting task in the queue, starting with
    * the waiting ones.
    */
   void abortAllTasks();

   @Nullable
   TaskState unstallConversion(long taskId);

   /**
    * Stops all queue tasks and waits for them to exit before returning.
    *
    * This will wait gracefully for up to two minutes for the tasks
    * to finish.  After that point, it will abort all tasks, and return
    * a list of the tasks which did not complete.
    *
    * note: this is used only for testing.  It hoses the task queue such
    * that it can no longer be used.
    *
    * @param abortUnfinishedTasks
    * When true, calls abort() on every running or waiting task still in
    * the queue.  When false, it will attempt to wait for the tasks to complete
    * on their own.
    */
   @VisibleForTesting
   @Nonnull
   List<Runnable> gracefulShutdown(boolean abortUnfinishedTasks);

   /**
    * All tasks, no matter what they do, will be in one of these
    * states.
    */
   enum MetaStatus {
      /** Task has been created but not added to queue */
      INIT,
      /** Task is in the queue waiting to run */
      WAITING,
      /** Task is running */
      RUNNING,
      /** Task has finished (successfully or otherwise) */
      FINISHED
   }

   /**
    * Count the number of active (waiting or running) tasks in the conversion queue.
    * @param datastoreId a datastore id
    * @return the number of active tasks.
    */
   int countActiveTasksByDatastoreId(Long datastoreId);
}
