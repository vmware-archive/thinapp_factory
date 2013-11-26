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

package com.vmware.appfactory.taskqueue.tasks.state;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.builder.TaskStateBuilder;

public interface TaskState
      <Api extends TaskState<Api, Builder, StatusEnum>,
       Builder extends TaskStateBuilder<Api, Builder, StatusEnum>,
       StatusEnum extends Enum<StatusEnum>>
      extends HasBuilderChanges {
   /**
    * @return the task type.
    * This is like a "class" for tasks, so each subclass should return the same
    * value for every instance. This value may be used in the UI to highlight
    * the different types of tasks currently running or waiting.
    */
   @Nonnull
   String getType();

   /**
    * @return a description of this task.
    *
    * This should be a short text description of what the task's purpose it.
    * This should not change during the task's lifetime (although if it does,
    * nothing bad will happen except a slight confusion of the user).
    */
   @Nonnull
   String getDescription();

   /**
    * Get the unique ID number assigned to this task.
    * @return The unique ID number assigned to this task.
    */
   @Nonnull
   Long getId();

   /**
    * Get the relative path (e.g. "/builds/edit/123") that the task "status"
    * should link to.
    *
    * @return the action link.
    */
   @Nullable
   String getStatusLink();

   /**
    * @return the time when this task was added to the queue, or -1
    * if it has not been added to the queue.
    */
   long getQueued();

   /**
    * @return the time when this task started, or -1 if it has not
    * been started.
    *
    * Note: for tasks which were queued but then cancelled before they
    * started running, this will never be set, remaining always at -1.
    */
   long getStarted();

   /**
    * @return the time when this task finished (or was cancelled), or
    * -1 if it has not yet finished or been cancelled.
    */
   long getFinished();

   /**
    * Get the percent progress of this task.
    * If this cannot be measured, return -1.
    * @return -1, or a percentage from 0 to 100.
    */
   int getProgress();

   /**
    * @return true if the 'abort' flag is set for this task.
    * Used by subclasses while running to see if they should abort.
    *
    * This only represents that the task was asked to abort before it
    * was completed.  This does NOT mean that the task has noticed that
    * the abort was requested, or responded to it in a meaningful way.
    */
   boolean isAborted();

   /**
    * @return whether we are WAITING, RUNNING, or FINISHED
    *
    * note: if we have not yet been added to the task queue, this might
    * also return INIT.
    */
   @Nonnull
   TaskQueue.MetaStatus getQueueStatus();

   /**
    * Get the current "action".
    *
    * This is a word the describes what the task is doing (DOWNLOADING,
    * CHURNING, WAITING, etc). This can (and usually does) change during the
    * task's lifetime. It is usually a direct string representation of an
    * enum value.
    *
    * @return Our current task-specific status
    */
   @Nonnull
   StatusEnum getStatus();

   /**
    * @return the database id of the resource which is being
    * modified in this task (e.g. Application, Build, Feed).
    */
   long getRecordId();

   /**
    * Note: this is the ONE field which we are lax about,
    * because we expect it to change all of the time.
    *
    * Please DO NOT call this method from anywhere but AbstractTask.
    *
    * @param progress   Value, in the range [-1..100].
    *                   When -1, the status will be set to "unknown".
    *                   When in the range [0..100], represents a rough
    *                   estimate of how much of the task has completed,
    *                   relative to what is left.
    */
   void setProgressInternal(int progress);

   /**
    * @return
    * Creates a Builder object with all fields initialized to the
    * object's current state.
    */
   @Nonnull
   Builder newBuilderForThis();
}
