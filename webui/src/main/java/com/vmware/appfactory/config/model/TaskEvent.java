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

package com.vmware.appfactory.config.model;

import java.util.Set;

import javax.annotation.Nonnull;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.context.ApplicationEvent;

import com.vmware.appfactory.taskqueue.tasks.state.HasBuilderChanges;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

/**
 * Fired whenever a task changes.  Changes include:
 *  - being added
 *  - completing successfully
 *  - failing due to an error
 *  - being cancelled
 *  - making progress while running (% completion, internal state change)
 *  - being moved to a new position in the pending tasks queue
 */
public abstract class TaskEvent extends ApplicationEvent {

   @Nonnull
   public static TaskEvent newTaskAdded(@Nonnull TaskState source) {
      return new TaskAddedEvent(source);
   }

   @Nonnull
   public static TaskEvent newTaskUpdated(@Nonnull TaskState state) {
      return new TaskUpdatedEvent(state);
   }

   @Nonnull
   public static TaskEvent newMovedToHead(@Nonnull TaskState state) {
      return new TaskMoveEvent(state, Type.moved_to_top, -1);
   }

   @Nonnull
   public static TaskEvent newMovedToTail(@Nonnull TaskState state) {
      return new TaskMoveEvent(state, Type.moved_to_bottom, Long.MAX_VALUE);
   }

   @Nonnull
   public static TaskEvent newMovedAfter(@Nonnull TaskState state,
                                         long followingTaskId) {
      return new TaskMoveEvent(state, Type.reordered, followingTaskId);
   }

   @Nonnull
   public static TaskEvent newTaskRemoved(@Nonnull TaskState state) {
      return new TaskRemoveEvent(state.getType(), state.getId());
   }

   private final String taskType;


   protected TaskEvent(@Nonnull String taskType, @Nonnull Object source) {
      super(source);
      this.taskType = taskType;
   }

   @Nonnull
   public abstract Type getType();

   @Nonnull
   @JsonIgnore
   public String getTaskType() {
      return taskType;
   }

   public enum Type {
      added,
      updated,
      moved_to_top,
      moved_to_bottom,
      reordered,
      removed
   }

   /**
    * Fired when a task is added to the queue.
    */
   public static class TaskAddedEvent extends TaskEvent {
      protected TaskAddedEvent(TaskState source) {
         super(source.getType(), source);
      }

      @Override
      @Nonnull
      public Type getType() {
         return Type.added;
      }
   }

   /**
    * Fired when a TaskState changes, such as when progress is
    * updated or when the task moves from WAITING to RUNNING.
    * For efficiency, only the names of the changed properties
    * and their new values are included.
    */
   public static class TaskUpdatedEvent extends TaskEvent {

      private final Set<HasBuilderChanges.BuilderChange> builderChanges;

      @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
      public TaskUpdatedEvent(TaskState state) {
         super(state.getType(), state.getId());
         this.builderChanges = state.getChanges();
      }

      public Set<HasBuilderChanges.BuilderChange> getBuilderChanges() {
         return builderChanges;
      }

      @Nonnull
      @Override
      public TaskEvent.Type getType() {
         return Type.updated;
      }
   }

   /**
    * Fired when a pending task has changed position in the pending queue
    * as the result of a user request.
    *
    * This subclasses TaskEvent in order to describe where the task moved to.
    * (TaskEvent only deals with the internal state of the task itself, not
    * with the task's position in the list.)
    */
   public static class TaskMoveEvent extends TaskEvent {
      private final long followingTaskId;
      private final Type type;

      public TaskMoveEvent(@Nonnull TaskState source,
                           @Nonnull Type type,
                           long followingTaskId) {
         super(source.getType(), source.getId());
         this.type = type;
         this.followingTaskId = followingTaskId;
      }

      public long getFollowingTaskId() {
         return followingTaskId;
      }

      @Override
      @Nonnull
      public Type getType() {
         return type;
      }
   }

   public static class TaskRemoveEvent extends TaskEvent {
      public TaskRemoveEvent(@Nonnull String queueName, @Nonnull Long taskId) {
         super(queueName, taskId);
      }

      @Nonnull
      @Override
      public Type getType() {
         return Type.removed;
      }
   }
}
