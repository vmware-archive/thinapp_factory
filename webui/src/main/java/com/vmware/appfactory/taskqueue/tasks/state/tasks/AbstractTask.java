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

import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.cws.exception.CwsException;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.TaskStateBuilder;

abstract class AbstractTask
      <T extends TaskState<T,S,E>,
       S extends TaskStateBuilder<T,S,E>,
       E extends Enum<E>>
   implements AppFactoryTask<T,S,E> {

   protected static final int POLL_FREQ_SECS = 1; // Poll for status every second

   protected final Logger _log = LoggerFactory.getLogger(getClass());

   private final TaskHelperFactory _taskHelperFactory;

   @GuardedBy(value = "this")
   private T _currentState;

   @GuardedBy(value = "this")
   @Nullable
   private Future<Void> future;

   AbstractTask(@Nonnull TaskHelperFactory taskHelperFactory,
                @Nonnull T initialState) {
      _taskHelperFactory = Preconditions.checkNotNull(taskHelperFactory);
      _currentState = initialState;
      future = null;
   }

   // ********* For our derived classes to implement ********* //

   /**
    * Do the work.
    * This is the reason for the task; use this method to perform the entire
    * operation. Tasks are run in threads, so this will not be called again.
    *
    * When this is called, we are moved to the RUNNING queue.
    *
    * @throws TaskException   if the task fails
    */
   protected abstract void doRun()
         throws TaskException;

   protected abstract void doCleanup()
         throws TaskException;

   protected void doCleanupConversionTask(E state, E completeState, Long projectId, String displayName) {
      // Only delete project directories for conversions that were not successful
      if (state != completeState) {
         boolean failed = false;
         Exception ex = null;

         _log.info("Cleaning up conversion task for {}", displayName);

         try {
            if (null != projectId) {
               getTaskHelperFactory().getCws().deleteProject(projectId);
            }
         } catch (AfNotFoundException e) {
            failed = true;
            ex = e;
         } catch (RestClientException e) {
            failed = true;
            ex = e;
         } catch (CwsException e) {
            failed = true;
            ex = e;
         }

         if (failed) {
            _log.debug(
                  String.format("Unable to clean up conversion task for %s",
                        displayName),
                  ex);
         }
      }
   }

   // ********* public methods ********* //

   @Override
   public final String getTaskHandle() {
      return TaskFactory.makeRecordId(
            getCurrentTaskState().getType(),
            getCurrentTaskState().getRecordId()
      );
   }

   @Override
   @Nonnull
   public final synchronized T getCurrentTaskState() {
      return _currentState;
   }

   /**
    * Called when the queue wants us to abort processing.
    */
   @Override
   public synchronized void abort() {
      _currentState = getBuilderForThis().withAborted(true).build();
      _taskHelperFactory.fireTaskEvent(TaskEvent.newTaskUpdated(_currentState));
      if (!TaskQueue.MetaStatus.RUNNING.equals(_currentState.getQueueStatus())) {
         // we can get here if the task never started.  In this
         // case, the MetaStatus will still be WAITING.
         // For the UI to be nice, we want to make it FINISHED.
         setMetaStatus(TaskQueue.MetaStatus.FINISHED);
      }
   }

   /**
    * Called when the queue wants us to abort processing.
    */
   @Override
   public synchronized void cleanup() throws TaskException {
      if (!TaskQueue.MetaStatus.FINISHED.equals(_currentState.getQueueStatus())) {
         throw new TaskException(this, "Can not cleanup task when it is not finished");
      }
      doCleanup();
   }

   @SuppressWarnings({"CallToNativeMethodWhileLocked"})
   private synchronized void setMetaStatus(@Nonnull TaskQueue.MetaStatus metaStatus) {

      S builder = _currentState.newBuilderForThis();
      builder.withQueueStatus(metaStatus);
      switch (metaStatus) {
         case INIT:
            // Ignore the INIT state
            break;
         case WAITING:
            builder.withQueued(System.currentTimeMillis());
            break;
         case RUNNING:
            builder.withStarted(System.currentTimeMillis());
            break;
         case FINISHED:
            builder.withFinished(System.currentTimeMillis());
            break;
      }
      _currentState = builder.build();
      _taskHelperFactory.fireTaskEvent(TaskEvent.newTaskUpdated(_currentState));
   }

   @Override
   public void unstall() {
      throw new IllegalStateException("Do not expect call to unstall on task of this type!");
   }

   @Override
   public synchronized void setFuture(@Nonnull Future<Void> future) {
      this.future = future;
      setMetaStatus(TaskQueue.MetaStatus.WAITING);
   }

   @Override
   @Nullable
   public synchronized Future<Void> getFuture() {
      return future;
   }

   @Override
   @Nullable
   public Void call() throws TaskException {
      synchronized (this) {
         if (getCurrentTaskState().isAborted()) {
            getCurrentTaskState();
            return null;
         }
         setMetaStatus(TaskQueue.MetaStatus.RUNNING);
      }
      try {
         doRun();
         return null;
      } finally {
         setMetaStatus(TaskQueue.MetaStatus.FINISHED);
      }
   }

   // ********* Utility functions for our derived classes ********* //

   @Nonnull
   protected final TaskHelperFactory getTaskHelperFactory() {
      return _taskHelperFactory;
   }

   protected static void sleepUntilNextUpdate() throws InterruptedException {
      Thread.sleep(POLL_FREQ_SECS * 1000);
   }

   protected final synchronized void updateStatus(final E newStatus) {
      updateState(new Function<S, T>() {
         @Override
         public T apply(S builder) {
            return builder.withStatus(newStatus).build();
         }
      });
   }

   protected final synchronized void updateStatusWithProgress(final E newStatus, final int percent) {
      updateState(new Function<S, T>() {
         @Override
         public T apply(S builder) {
            return builder.withStatus(newStatus).withProgress(percent).build();
         }
      });
   }

   protected final synchronized void updateState(@Nonnull Function<S,T> modifier) {

      T newState = modifier.apply(getCurrentTaskState().newBuilderForThis());
      if (!newState.equals(_currentState)) {
         _currentState = newState;
         _taskHelperFactory.fireTaskEvent(TaskEvent.newTaskUpdated(_currentState));
      }
   }

   protected final synchronized void updateProgress(final int percent) {
      updateState(new Function<S, T>() {
         @Override
         public T apply(S builder) {
            return builder.withProgress(percent).build();
         }
      });
   }

   protected final synchronized void incrementProgressBy(int progressStep) {
      updateProgress(_currentState.getProgress() + progressStep);
   }

   // ** Implementation methods ** //

   private synchronized S getBuilderForThis() {
      return _currentState.newBuilderForThis();
   }

   protected static String buildIconUrl(String apiName, long entityId, int iconPos, String iconHash) {
      return String.format("/api/%s/%d/icon/%d/%s", apiName, entityId, iconPos, iconHash);
   }
}
