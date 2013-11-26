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

package com.vmware.appfactory.taskqueue.tasks.state.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

@SuppressWarnings("unchecked")
public abstract class TaskStateBuilder
      <Api extends TaskState<Api, Builder, StatusEnum>,
       Builder extends TaskStateBuilder<Api, Builder, StatusEnum>,
       StatusEnum extends Enum<StatusEnum>>
      extends TafBuilder<Api> {

   protected TaskStateBuilder(@Nonnull Class<? extends Api> resultClass,
                              @Nonnull String type) {
      super(resultClass);
      addChange("type",type);
   }

   /**
    * Sets the description of this task.
    * This should be a short text description of what the task's purpose it.
    * This should not change during the task's lifetime (although if it does,
    * nothing bad will happen except a slight confusion of the user).
    *
    * @return this, for chaining
    */
   public Builder withDescription(@Nonnull String description) {
      addChange("description", description);
      return (Builder)this;
   }

   /**
    * Set the relative path (e.g. "/builds/edit/123") that the task "status"
    * should link to.
    *
    * @return the action link.
    */
   public Builder withStatusLink(@Nullable String statusLink) {
      addChange("statusLink", statusLink);
      return (Builder)this;
   }

   public Builder withQueueStatus(TaskQueue.MetaStatus queueStatus) {
      addChange("queueStatus", queueStatus);
      return (Builder)this;
   }

   public Builder withStatus(@Nonnull StatusEnum status) {
      addChange("status", status);
      return (Builder)this;
   }

   public Builder withNewId(@Nonnull Supplier<Long> newIdSupplier) {
      addChange("id", newIdSupplier.get());
      return (Builder)this;
   }

   public Builder withRecordId(long recordId) {
      addChange("recordId", recordId);
      return (Builder)this;
   }

   /**
    * Set the time when this task was added to the queue.
    * @return
    */
   public Builder withQueued(long timestamp) {
      addChange("queued", timestamp);
      return (Builder)this;
   }

   /**
    * Get the time when this task started.
    * @return The time when this task started.
    */
   public Builder withStarted(long timestamp) {
      addChange("started", timestamp);
      return (Builder)this;
   }

   /**
    * Set the time when this task finished.
    * @return
    */
   public Builder withFinished(long timestamp) {
      addChange("finished", timestamp);
      return (Builder)this;
   }

   /**
    * Get the percent progress of this task.
    * If this cannot be measured, return -1.
    * @return -1, or a percentage from 0 to 100.
    */
   public Builder withProgress(int percent) {
      addChange("progress", percent);
      return (Builder)this;
   }

   /**
    * Set the 'abort' flag for this task.
    * Used by subclasses while running to see if they should abort.
    * @return
    */
   public Builder withAborted(boolean aborted) {
      addChange("aborted", aborted);
      return (Builder)this;
   }

   @Override
   public Builder withOriginal(@Nonnull Api original) {
      addChanges(original);
      return (Builder)this;
   }

   // note: there is deliberately no "withErrorLog" method


   // note: some of the checks which appear to be "always true" are not,
   //  because this is the class that enforces the @Nonnull conditions
   @SuppressWarnings("ConstantConditions")
   @Override
   public Api build() {
      Api result = super.build();
      Preconditions.checkArgument(result.getId() != 0,
            "TaskState constraint violation: id must be supplied");
      Preconditions.checkArgument(result.getRecordId() > 0,
            "Record must have an ID > 0");
      switch(result.getQueueStatus()) {
         case FINISHED:
            Preconditions.checkArgument(result.getFinished() > 0);
            Preconditions.checkArgument(result.getStarted() > 0 || result.isAborted());
            Preconditions.checkArgument(result.getQueued() > 0);
            break;
         case RUNNING:
            Preconditions.checkArgument(result.getFinished() == -1);
            Preconditions.checkArgument(result.getStarted() > 0 || result.isAborted());
            Preconditions.checkArgument(result.getQueued() > 0);
            break;
         case WAITING:
            Preconditions.checkArgument(result.getFinished() == -1);
            Preconditions.checkArgument(result.getStarted() == -1);
            Preconditions.checkArgument(result.getQueued() > 0);
            break;
         case INIT:
            // in no state, hasn't yet been queued
            Preconditions.checkArgument(result.getFinished() == -1);
            Preconditions.checkArgument(result.getStarted() == -1);
            Preconditions.checkArgument(result.getQueued() == -1);
      }

      Preconditions.checkArgument(result.getProgress() >= -1);
      Preconditions.checkArgument(result.getProgress() <= 100);
      Preconditions.checkArgument(!StringUtils.isEmpty(result.getDescription()));

      return result;
   }
}
