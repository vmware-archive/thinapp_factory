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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vmware.appfactory.config.model.ConfigChangeEvent;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.util.Closure;
import com.vmware.thinapp.common.util.concurrent.BoundedBlockingQueueWithCallback;
import com.vmware.thinapp.common.util.concurrent.FutureStoringThreadPoolExecutor;
import com.vmware.thinapp.common.util.concurrent.OrderableBlockingQueue;
import com.vmware.thinapp.common.util.concurrent.OrderableLinkedBlockingQueue;


class TaskQueueImpl implements
      TaskQueue,
      ApplicationListener,
      ApplicationEventPublisherAware,
      DisposableBean {

   private final Logger log = LoggerFactory.getLogger(TaskQueueImpl.class);

   private static final int CORE_POOL_SIZE = 1;
   private static final int MAX_POOL_SIZE = 100;
   private static final long KEEP_ALIVE_TIME_MINUTES = 30L;
   private static final int MAXIMUM_FINISHED_SIZE = 1000;

   @Nonnull
   final OrderableBlockingQueue<Runnable> activeTaskQueue;

   @Nonnull
   final FutureStoringThreadPoolExecutor<Void,AppFactoryTask> executorService;

   @Nonnull
   private final BlockingQueue<Future<Void>> completedTasksQueue;

   @Nonnull
   private final String name;

   @Nonnull
   private final Supplier<Integer> maxPoolSizeSupplier;

   @Nonnull
   private final TaskRecorder taskRecorder;

   @Nullable
   private ApplicationEventPublisher applicationEventPublisher;


   @Nonnull
   private static final Ordering<AppFactoryTask> metaStatusOrdering =
         Ordering.natural().onResultOf(new Function<AppFactoryTask, Comparable>() {
            @SuppressWarnings("ReturnOfNull")
            @Override
            public Comparable apply(@Nullable AppFactoryTask input) {
               return (null == input) ? null
                                      : input.getCurrentTaskState().getQueueStatus();
            }
         }).reverse();

   @Nonnull
   private static final
   Ordering<AppFactoryTask>
         taskIdOrdering =
         Ordering.natural().onResultOf(new Function<AppFactoryTask, Comparable>() {
            @SuppressWarnings("ReturnOfNull")
            @Override
            public Comparable apply(@Nullable AppFactoryTask input) {
               return (null == input) ? null
                                      : input.getCurrentTaskState().getId();
            }
         });

   /**
    * @param name
    * Name of the task queue, to be used in log messages.
    *
    * @param expectedTasksPerId
    * Number of conversions of a given single app that we expect to have in
    * the queue at the same time.
    *
    * For feed-related tasks, this would be the number of tasks related to a single
    * feed that we expect to have in the queue at the same time.
    *
    * Note that this includes tasks which have completed but have not yet
    * been cleaned up.
    *
    * @param maxPoolSizeSupplier
    * Get the maximum number of tasks that are allowed to run at any one
    * time. We need to limit this, since each AppConvertTask actually runs
    * in a thread, so until we can improve that, we need to be careful about
    * resources.
    *
    * See bugs 767721, 774926, 723167.
    *
    * When called, it returns a limit on number of running tasks, between
    * zero for none and the constant MAX_RUNNING_TASKS_LIMIT.
    *
    * @param maxFinishedSizeSupplier
    * Supplies the number of finished tasks to keep a record of.  This may be
    * zero.  When more than this number of tasks are finished, the oldest are
    * discarded.  The list of finished tasks may also be flushed manually by
    * calling cleanup().
    *
    */
   TaskQueueImpl(String name,
                 int expectedTasksPerId,
                 Supplier<Integer> maxPoolSizeSupplier,
                 Supplier<Integer> maxFinishedSizeSupplier) {
      this.name = name;

      this.maxPoolSizeSupplier = maxPoolSizeSupplier;

      activeTaskQueue = new OrderableLinkedBlockingQueue<Runnable>();

      int maxFinishedSize = maxFinishedSizeSupplier.get();
      if (maxFinishedSize < 1) {
         // unbounded, but in practice make a large limit so that we don't crash
         // under stress
         maxFinishedSize = MAXIMUM_FINISHED_SIZE;
      }

      taskRecorder = new TaskRecorder(MAX_POOL_SIZE,
                                      maxFinishedSize,
                                      expectedTasksPerId);

      @SuppressWarnings("StringConcatenation")
      ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(name + "-thread-%1$s")
            .build();

      /**
       * We will use a completion service to take completed tasks and add them to the
       * completedTasksQueue.  When this queue is full, take the oldest task out of
       * the queue and call eraseTask() on it.
       */
      completedTasksQueue = new BoundedBlockingQueueWithCallback<Future<Void>>(
            maxFinishedSize,
            new Closure<Future<Void>>() {
               @Override
               public void apply(@Nonnull Future<Void> removedElement) {
                  // find the task associated with it
                  AppFactoryTask expiredTask = FutureStoringThreadPoolExecutor.getOriginal(removedElement);
                  if (null != expiredTask) {
                     removeTask(expiredTask);
                  } else {
                     logErrorWithQueueName(
                           "error: future expired but could not find associated task: {}",
                           removedElement);
                  }
               }
            }
      );

      executorService = new FutureStoringThreadPoolExecutor<Void,AppFactoryTask>(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME_MINUTES,
            TimeUnit.MINUTES,
            activeTaskQueue,
            threadFactory,
            AppFactoryTask.class,
            completedTasksQueue
      );

      // In the simulator, as both the API and WebUI are running in the same servlet.
      // Spring needs to create a TaskQueueImpl before it will handle requests.
      // In the case of a conversions queue, the maxPoolSizeSupplier will make a
      // HTTP request to localhost (in certain cases) to determine the number of
      // simulated worker VMs available.  This causes us to make an HTTP request to
      // ourselves, which will deadlock.
      //
      // As a workaround, fire this change off on a background thread so that
      // it can complete at leisure.
      Executors.newSingleThreadExecutor().execute(new Runnable() {
         @Override
         public void run() {
            changePoolSize();
         }
      });

      applicationEventPublisher = null;

      logInfoWithQueueName("created with max finished size {}", maxFinishedSize);
   }

   private void removeTask(AppFactoryTask expiredTask) {
      try {
         expiredTask.cleanup();
      } catch (TaskException e) {
         logExceptionWithQueueName("When removing task, failure in task cleanup", e);
      }
      // remove the task anyway

      AppFactoryTask removedTask = taskRecorder.eraseTask(expiredTask.getCurrentTaskState().getId());
      if (null != removedTask) {
         fireTaskEvent(TaskEvent.newTaskRemoved(removedTask.getCurrentTaskState()));
      } else {
         logErrorWithQueueName("error: could not erase expired task: {}", expiredTask);
      }
   }

   @Override
   public synchronized void addTask(@Nonnull AppFactoryTask task)
   {
      if (!MetaStatus.INIT.equals(
            (Preconditions.checkNotNull(task).getCurrentTaskState().getQueueStatus()))
            || (null != taskRecorder.findTaskById(task.getCurrentTaskState().getId()))
         ) {
         // task is already running
         throw new IllegalArgumentException("Task is already queued");
      }
      executorService.submit(task);
      taskRecorder.recordTask(task);

      fireTaskEvent(TaskEvent.newTaskAdded(task.getCurrentTaskState()));
   }

   @Nonnull
   @Override
   public synchronized Iterable<TaskState> getAllTasks()
   {
      List<AppFactoryTask> tasks = taskRecorder.getAllTasks();
      List<AppFactoryTask> queuedOrder = getTasksInQueuedOrder();

      List<AppFactoryTask> orderedTasks = metaStatusOrdering
            .compound(GracefulExplicitOrdering.of(queuedOrder))
            .compound(taskIdOrdering)
            .nullsLast()
            .sortedCopy(tasks);

      return Iterables.transform(orderedTasks,TaskRecorder.fnGetCurrentState);
   }

   @Nonnull
   @Override
   public Iterable<TaskState> getTasks(Predicate<? super TaskState> predicate) {
      return Iterables.filter(getAllTasks(),predicate);
   }

   @Override
   public int countActiveTasksByDatastoreId(Long datastoreId) {
      Iterable<TaskState> result = Iterables.filter(getAllTasks(),MetaStatusPredicate.withDatastoreFilter(datastoreId));
      result = Iterables.filter(result, MetaStatusPredicate.NOT_FINISHED);
      return Iterables.size(result);
   }

   @Override
   @Nullable
   public synchronized TaskState findTaskById(long id)
   {
      return taskRecorder.findTaskById(id);
   }


   @Nonnull
   @Override
   public synchronized Iterable<? extends AbstractCaptureState> findActiveTasksForApp(
         final long appId)
   {
      return taskRecorder.findActiveTasksForApp(appId);
   }

   @Nonnull
   @Override
   public synchronized Iterable<? extends TaskState> findActiveTasksForFeed(
         final long feedId) {
      return taskRecorder.findActiveTasksForFeed(feedId);
   }

   @Nullable
   @Override
   public synchronized TaskState abortTask(long taskId)
   {
      AppFactoryTask task = taskRecorder.getTaskFromId(taskId);
      if (null != task) {
         task.abort();

         Future<?> future = task.getFuture();
         if (null != future) {
            future.cancel(true);
         } else {
            log.warn("Could not cancel future for task with id {}", taskId);
         }
      } else {
         log.warn("Could not abort task with id {}", taskId);
      }


      return (null != task) ? task.getCurrentTaskState() : null;
   }

   @Override
   public synchronized boolean moveToHead(long taskIdToMove) {
      AppFactoryTask task = taskRecorder.getTaskFromId(taskIdToMove);
      if (null == task) {
         return false;
      }
      Future<?> futureToMove = task.getFuture();

      // if futureToMove is null, either this was never in the queue, or it has
      // been already removed for processing, or it has been cancelled.
      // Either way, nothing to do.
      boolean moved = null != futureToMove
                      && activeTaskQueue.moveToHead((Runnable) futureToMove);
      if (moved) {
         fireTaskEvent(TaskEvent.newMovedToHead(task.getCurrentTaskState()));
      }
      return moved;
   }

   @Override
   public synchronized boolean moveToTail(long taskIdToMove) {
      AppFactoryTask task = taskRecorder.getTaskFromId(taskIdToMove);
      if (null == task) {
         return false;
      }
      Future<?> futureToMove = task.getFuture();

      // if futureToMove is null, either this was never in the queue, or it has
      // been already removed for processing, or it has been cancelled.
      // Either way, nothing to do.
      boolean moved = null != futureToMove
                      && activeTaskQueue.moveToTail((Runnable) futureToMove);
      if (moved) {
         fireTaskEvent(TaskEvent.newMovedToTail(task.getCurrentTaskState()));
      }
      return moved;
   }

   @Override
   public synchronized boolean moveBefore(long taskIdToMove, long taskIdToFollow) {
      AppFactoryTask taskToMove = taskRecorder.getTaskFromId(taskIdToMove);
      AppFactoryTask taskToFollow = taskRecorder.getTaskFromId(taskIdToFollow);
      if (null == taskToMove || null == taskToFollow) {
         return false;
      }
      Future<?> futureToMove = taskToMove.getFuture();
      Future<?> futureToFollow = taskToFollow.getFuture();

      // if either future is null, there's nothing we can do
      boolean moved =
             null != futureToMove
             && null != futureToFollow
             && activeTaskQueue.moveAfter((Runnable) futureToMove,
                                          (Runnable) futureToFollow);
      if (moved) {
         fireTaskEvent(TaskEvent.newMovedAfter(taskToMove.getCurrentTaskState(),
                                               taskIdToFollow));
      }
      return moved;
   }

   @Override
   public synchronized void cleanup()
   {
      logInfoWithQueueName("purging completed tasks");

      // clear all tasks from the finished queue
      final List<Future<Void>> completedTasks =
            Lists.newArrayListWithExpectedSize(this.completedTasksQueue.size());
      this.completedTasksQueue.drainTo(completedTasks);
      for (Future<Void> future: completedTasks) {
         AppFactoryTask task = FutureStoringThreadPoolExecutor.getOriginal(future);
         if (null != task) {
            removeTask(task);
         } else {
            logErrorWithQueueName("Could not find taskId for future {}", future);
         }
      }
   }

   @Override
   public synchronized boolean cleanup(long taskId)
   {
      logInfoWithQueueName("purging task id={}", taskId);

      // clear all tasks from the finished queue
      AppFactoryTask task = taskRecorder.getTaskFromId(taskId);
      if (null != task && MetaStatus.FINISHED.equals(task.getCurrentTaskState().getQueueStatus())) {
         boolean removed = this.completedTasksQueue.remove(task.getFuture());
         if (removed) {
            // remove from taskRecorder and fire task removal event
            removeTask(task);
            return true;
         }
      }
      return false;
   }

   // TODO: this is a leak in our abstraction!
   @Nullable
   @Override
   public synchronized TaskState unstallConversion(long taskId) {
      AppFactoryTask task = taskRecorder.getTaskFromId(taskId);
      if (task.getCurrentTaskState().getType().equals(AppConvertState.TYPE)) {
         task.unstall();
         return task.getCurrentTaskState();
      }
      return null;
   }

   @Override
   @Nonnull
   @VisibleForTesting
   public List<Runnable> gracefulShutdown(boolean abortUnfinishedTasks) {
      logInfoWithQueueName("Gracefully shutting down task queue.  May take up to 20 seconds.");
      executorService.shutdown(); // Disable new tasks from being submitted
      try {
         // try to abort all existing tasks
         if (abortUnfinishedTasks) {
            for (TaskState taskState: getTasks(MetaStatusPredicate.NOT_FINISHED)) {
               abortTask(taskState.getId());
            }
         }

         // Wait a while for existing tasks to terminate
         if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            executorService.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
               logErrorWithQueueName("Pool did not terminate");
         }
      } catch (InterruptedException ie) {
         // (Re-)Cancel if current thread also interrupted
         List<Runnable> result = executorService.shutdownNow();
         // Preserve interrupt status
         Thread.currentThread().interrupt();
         logErrorWithQueueName(
               "When shutting down, {} tasks did not complete gracefully.",
               result.size());
         return result;
      }
      logInfoWithQueueName("Graceful shutdown completed gracefully.");
      return Collections.emptyList();
   }

   @Override
   public synchronized void abortAllTasks() {
      // abort the waiting tasks first, so that they don't start running
      // as we cancel the running ones
      //
      for (TaskState taskState: getTasks(MetaStatusPredicate.WAITING)) {
         abortTask(taskState.getId());
      }
      for (TaskState taskState: getTasks(MetaStatusPredicate.RUNNING)) {
         abortTask(taskState.getId());
      }
   }

   /**
    * When spring shuts us down, try to cleanly abort any running tasks.
    *
    * @see DisposableBean#destroy()
    */
   @Override
   public void destroy() {
      gracefulShutdown(true);
   }

   @Override
   public void onApplicationEvent(ApplicationEvent applicationEvent) {
      if (applicationEvent instanceof ConfigChangeEvent) {
         changePoolSize();
      }
   }

   private void changePoolSize() {
      int maxRunning = maxPoolSizeSupplier.get();
      if (maxRunning <= 0) {
         // we can't stop the queue entirely, because maximumPoolSize will throw
         // an error.
         //
         maxRunning = 1;
      }
      logInfoWithQueueName("Changing queue size to {}", maxRunning);
      executorService.setCorePoolSize(maxRunning);
      executorService.setMaximumPoolSize(maxRunning);
   }

   private void logInfoWithQueueName(String msg, Object... args) {
      if (log.isInfoEnabled()) {
         log.info(prependQueueName(msg), args);
      }
   }

   private void logErrorWithQueueName(String msg, Object... args) {
      if (log.isErrorEnabled()) {
         log.error(prependQueueName(msg), args);
      }
   }

   private void logExceptionWithQueueName(String msg, Throwable t) {
      if (log.isErrorEnabled()) {
         log.error(prependQueueName(msg), t);
      }
   }

   private String prependQueueName(String msg) {
      return MessageFormat.format("{0} queue: {1}", name, msg);
   }

   @Override
   public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher applicationEventPublisher) {
      this.applicationEventPublisher = applicationEventPublisher;
   }

   private void fireTaskEvent(@Nonnull TaskEvent taskEvent) {
      if (null != applicationEventPublisher) {
         applicationEventPublisher.publishEvent(taskEvent);
      }
   }

   private List<AppFactoryTask> getTasksInQueuedOrder() {
      return Lists.newArrayList(
            Iterables.filter(
                  Iterables.transform(
                        activeTaskQueue,
                        new Function<Runnable, AppFactoryTask>() {
                           @Override
                           public AppFactoryTask apply(@Nullable Runnable future) {
                              return FutureStoringThreadPoolExecutor.getOriginal(future);
                           }
                        }
                  ),
                  Predicates.notNull()
            )
      );
   }
   /**
    * suppresses Ordering$IncomparableValueException and returns 0 instead
    * @param <T> type of elements to compare
    */
   private static class GracefulExplicitOrdering<T>
      extends Ordering<T> {
      private final Ordering<T> delegate;

      GracefulExplicitOrdering(Ordering<T> delegate) {
         this.delegate = delegate;
      }
      public static <T> Ordering<T> of(List<T> valuesInOrder) {
         return new GracefulExplicitOrdering<T>(
               Ordering.explicit(valuesInOrder)
         );
      }

      @Override
      public int compare(@Nullable T left, @Nullable T right) {
         try {
            return delegate.compare(left, right);
         } catch (ClassCastException e) {
            // to catch Ordering#IncomparableValueException
            return 0;
         }
      }
   }
}