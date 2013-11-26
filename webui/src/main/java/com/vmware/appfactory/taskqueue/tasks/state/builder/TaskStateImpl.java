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

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

/**
 * Every task state is a subclass of this abstract class.
 * This class provides all the properties common to all tasks.
 */
public abstract class TaskStateImpl
      <Api extends TaskState<Api, Builder, StatusEnum>,
            Builder extends TaskStateBuilder<Api, Builder, StatusEnum>,
            StatusEnum extends Enum<StatusEnum>>
      implements TaskState<Api,Builder,StatusEnum>
{
   private long _id;

   private long _queued = -1;

   private long _started = -1;

   private long _finished = -1;

   private int _progress = -1;

   private volatile boolean _aborted = false;

   private String _statusLink;

   private String _description;

   private StatusEnum _status;

   private TaskQueue.MetaStatus _queueStatus = TaskQueue.MetaStatus.INIT;

   private long _recordId;

   private String _type;

   private Set<BuilderChange> _changes = Collections.emptySet();


   /**
    * Get a description of this task.
    * This should be a short text description of what the task's purpose it.
    * This should not change during the task's lifetime (although if it does,
    * nothing bad will happen except a slight confusion of the user).
    *
    * @return
    */
   @Nonnull
   @Override
   public String getDescription() {
      return _description;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setDescription(String description) {
      this._description = description;
   }

   @Nonnull
   @Override public Long getId()
   {
      return _id;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setId(Long taskId) {
      _id = taskId;
   }

   @Override public String getStatusLink()
   {
      return _statusLink;
   }

   /**
    * Set the relative path (e.g. "/builds/edit/123") that the task "queueStatus"
    * should link to. If tooltip is not null, it will also be set.
    *
    * @param path the path to set
    */
   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setStatusLink(String path)
   {
      _statusLink = path;
   }


   /**
    * Set the time when this task was added to the queue.
    * @param queued
    */
   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setQueued(long queued)
   {
      _queued = queued;
   }


   @Override
   public long getQueued()
   {
      return _queued;
   }


   /**
    * Used by builder.  Do not call directly.
    *
    * Set the time when this task started.
    * @param started
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setStarted(long started)
   {
      _started = started;
   }


   @Override
   public long getStarted()
   {
      return _started;
   }


   /**
    * Set the time when this task finished.
    * @param finished
    */
   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setFinished(long finished)
   {
      _finished = finished;
   }


   @Override
   public long getFinished()
   {
      return _finished;
   }


   /**
    * Set the percent progress of this task.
    * If this cannot be measured, set to -1.
    * @param progress
    */
   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setProgress(int progress)
   {
      _progress = progress;
   }

   @Override
   public void setProgressInternal(int progress) {
      setProgress(progress);
   }

   @Override public int getProgress()
   {
      return _progress;
   }

   /**
    * Set a flag to indicate this task was asked to abort.
    * @param aborted
    */
   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setAborted(boolean aborted)
   {
      _aborted = aborted;
   }

   @Override public boolean isAborted()
   {
      return _aborted;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setQueueStatus(TaskQueue.MetaStatus queueStatus) {
      this._queueStatus = queueStatus;
   }

   @Nonnull
   @Override
   public TaskQueue.MetaStatus getQueueStatus() {
      return _queueStatus;
   }

   @Nonnull
   @Override
   public StatusEnum getStatus() {
      return _status;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setStatus(StatusEnum status) {
      this._status = status;
   }

   @Override
   public long getRecordId() {
      return _recordId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setRecordId(long record) {
      this._recordId = record;
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }

   @Nonnull
   @Override
   public String getType() {
      return _type;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setType(@Nonnull String type) {
      this._type = type;
   }

   @Nonnull
   @Override
   public Set<BuilderChange> getChanges() {
      return _changes;
   }

   @SuppressWarnings({"UnusedDeclaration",
                      "PublicMethodNotExposedInInterface",
                      "TypeMayBeWeakened",
                      "AssignmentToCollectionOrArrayFieldFromParameter"})
   public void setChanges(Set<BuilderChange> changes) {
      this._changes = changes;
   }
}
