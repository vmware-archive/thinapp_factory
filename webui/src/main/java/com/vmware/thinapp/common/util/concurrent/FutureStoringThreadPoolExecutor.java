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

package com.vmware.thinapp.common.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.tasks.state.tasks.StoresFuture;

/**
 * A version of ThreadPoolExecutor which affords
 * getting the original submitted callable from a Future.
 */
@SuppressWarnings({"unchecked", "AssignmentToCollectionOrArrayFieldFromParameter"})
public class FutureStoringThreadPoolExecutor<T,K extends StoresFuture>
      extends ThreadPoolExecutor {

   private final Class<K> klass;
   private final BlockingQueue<Future<T>> completionQueue;

   public FutureStoringThreadPoolExecutor(int corePoolSize,
                                          int maximumPoolSize,
                                          long keepAliveTime,
                                          TimeUnit unit,
                                          BlockingQueue<Runnable> workQueue,
                                          ThreadFactory threadFactory,
                                          Class<K> klass,
                                          BlockingQueue<Future<T>> completionQueue) {
      super(corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue,
            threadFactory);
      this.klass = klass;
      this.completionQueue = completionQueue;
   }

   @SuppressWarnings("hiding")
   @Override
   protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
      if (klass.isAssignableFrom(callable.getClass())) {
         RunnableFuture result = new OriginalSupplyingQueueingFuture(callable, completionQueue);
         ((StoresFuture)callable).setFuture(result);
         return result;
      }
      return new FutureTask<T>(callable);
   }

   @Nullable
   public static <K> K getOriginal(Object future) {
      if (future instanceof SuppliesOriginal<?>) {
         return ((SuppliesOriginal<K>)future).getOriginal();
      }
      return null;
   }

   // note: we can't use Supplier, because FutureTask defines
   // an incompatible method also named get()!
   public interface SuppliesOriginal<K> {
      K getOriginal();
   }

   /**
    * A wrapper for a {@link Callable<T>} which sends the item to a completion
    * queue when it is finished.
    *
    * This is needed so that we can fire a callback when the task completes.
    * This QueueingFuture is what will be placed in the workQueue.
    *
    * @param <T>
    * Return type of the Callable passed to {@link ExecutorService#submit(Callable)}.
    */
   abstract static class QueueingFuture<T>
      extends FutureTask<T>
      implements Future<T> {

      private final BlockingQueue<Future<T>> completionQueue;

      QueueingFuture(Callable<T> tCallable,
                     BlockingQueue<Future<T>> completionQueue) {
         super(tCallable);
         this.completionQueue = completionQueue;
      }

      @Override
      protected void done() {
         completionQueue.add(this);
      }
   }

   /**
    * A QueueingFuture which supplies the original task submitted to
    * the executor.
    *
    * This class provides a way of obtaining object which was originally
    * passed to {@link ExecutorService#submit(Callable)} from the item
    * added to the workQueue.
    *
    * @param <T>
    * Return type of the Callable passed to {@link ExecutorService#submit(Callable)}.
    *
    * @param <K>
    * Specific sub-class of Callable<T> that was passed to
    * {@link ExecutorService#submit(Callable)}.  In our case, this is probably
    * an AppFactoryTask.
    */
   @SuppressWarnings("hiding")
   public class OriginalSupplyingQueueingFuture<T,K extends Callable<T>>
      extends QueueingFuture<T>
      implements SuppliesOriginal<K>
   {

      @Nullable
      private final K callable;

      OriginalSupplyingQueueingFuture(K callable,
                                      BlockingQueue<Future<T>> completionQueue) {
         super(callable, completionQueue);
         this.callable = callable;
      }

      @Override
      public K getOriginal() {
         return callable;
      }
   }
}
