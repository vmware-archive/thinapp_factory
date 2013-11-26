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

import javax.annotation.Nullable;

/**
 * A {@see BlockingQueue} which allows the elements within the queue to be re-ordered.
 *
 * @param <T> the type of elements held in the queue
 */
public interface OrderableBlockingQueue<T> extends BlockingQueue<T> {
   /**
    * Move the specified element to the head (first position) of the queue.
    *
    * @param elToMove - element to move.  This element must already be
    *                 present in the queue.
    *
    * @return
    * true - if elToMove was successfully moved to the head
    * false - if elToMove was not found in the queue, or if elToMove is null
    */
   boolean moveToHead(@Nullable T elToMove);

   /**
    * Move the specified element to the tail (last position) of the queue.
    *
    * @param elToMove - element to move.  This element must already be
    *                 present in the queue.
    *
    * @return
    * true - if elToMove was successfully moved to the tail
    * false - if elToMove was not found in the queue, or if
    *         elTMove is null
    */
   boolean moveToTail(@Nullable T elToMove);

   /**
    * Move the queue element specified by elToMove from its current position
    * to one immediately after elToFollow in the queue.
    *
    * @param elToMove - element to move.  This element must already be
    *                 present in the queue.
    *
    * @param elToFollow - element immediately before the desired location of
    *                 elToMove.
    *                 This element must already be present in the queue.
    *
    * Precondition:
    * both elToMove and elToFollow are elements in the queue
    *
    * Post-condition:
    * elToMove appears in the queue immediately after the element elToFollow
    *
    * @return
    * false if no move occurred.  This could be because:
    *  - one of elToMove or elToFollow were no longer in the queue
    *  - elToMove or elToFollow were either or both null
    *  - elToMove and elToFollow refer to the same element
    */
   boolean moveAfter(@Nullable  T elToMove, @Nullable T elToFollow);
}
