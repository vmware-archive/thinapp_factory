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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingBlockingQueue;
import com.vmware.thinapp.common.util.Closure;

/**
 * A bounded blocking queue which fires a callback when items are dropped from
 * the queue:
 *  - when an item is added to the queue, and the queue is full, instead of
 *    prohibiting the add or blocking the thread attempting to add the element,
 *  - this class takes the oldest element in the queue and removes it
 *  - it will then fire the provided callback, passing in the removed element
 *  - and then add the new element to the head of the queue
 *
 * We use this in TAF to keep a bounded list of completed tasks, and then
 * remove all reference to the oldest as newer ones complete.
 *
 * @param <T> the type of elements held in this collection
 */
public class BoundedBlockingQueueWithCallback<T>
      extends ForwardingBlockingQueue<T> {

   private final BlockingQueue<T> delegate;
   private final Closure<T> fnDroppedCallback;

   /**
    * Creates a new bounded queue with the specified max size.
    *
    * When this queue fills and the oldest items are dropped, the function
    * fnDroppedCallback is called, passing in each dropped element.
    *
    * @param maxFinishedSize
    * Bounded size of the queue.  Must be at least 1.
    *
    * @param fnDroppedCallback
    * Function to call as the oldest items are dropped from the queue.
    * This function is passed the item which is about to be removed.
    */
   public BoundedBlockingQueueWithCallback(
         int maxFinishedSize,
         @Nonnull Closure<T> fnDroppedCallback) {
      this.fnDroppedCallback = Preconditions.checkNotNull(fnDroppedCallback);
      this.delegate = new ArrayBlockingQueue<T>(maxFinishedSize, true);
   }

   @Override
   public synchronized boolean add(T completedTask) {
      makeSpace();
      return delegate.add(completedTask);
   }

   @Override public void put(T e) throws InterruptedException {
      makeSpace();
      delegate.put(e);
   }

   @Override
   protected BlockingQueue<T> delegate() {
      return delegate;
   }

   private void makeSpace() {
      if (0 == delegate.remainingCapacity()) {
         // we need to make space from the element.
         // Drop the oldest element.
         T expungedElement = delegate.remove();
         if (null != expungedElement) {
            // fire the callback
            fnDroppedCallback.apply(expungedElement);
         }
      }
   }

}
