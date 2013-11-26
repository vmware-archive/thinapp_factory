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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.thinapp.common.util.Closure;

/**
 * An implementation of {@see OrderableBlockingQueue} based on our
 * {@see CustomLinkedBlockingQueue}, which is a private version of Java's
 * standard {@see LinkedBlockingQueue}
 *
 * @param <T> the type of elements held in the queue
 */
@SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
public class OrderableLinkedBlockingQueue<T>
   extends CustomLinkedBlockingQueue<T>
      implements OrderableBlockingQueue<T> {
   @SuppressWarnings("ObjectEquality")
   @Override
   public boolean moveToHead(@Nullable T elToMove) {
      return unlinkAndCall(elToMove, new Closure<Node<T>>() {
         @Override
         public void apply(@Nonnull Node<T> nodeCurrent) {
            // add it to the head
            linkQuietly(nodeCurrent, head);
         }
      });
   }


   @Override
   public boolean moveToTail(@Nullable T elToMove) {
      return unlinkAndCall(elToMove, new Closure<Node<T>>() {
         @Override
         public void apply(@Nonnull Node<T> nodeCurrent) {
            // add it to the tail
            linkQuietly(nodeCurrent, last);
         }
      });
   }

   @Override
   public boolean moveAfter(@Nullable T elToMove, @Nullable T elToFollow) {

      if (null == elToMove || null == elToFollow) {
         return false;
      }
      if (elToMove.equals(elToFollow)) {
         // cannot move an element after itself
         return false;
      }

      fullyLock();
      try {
         // find nodes of both elToMove and elToFollow
         Node<T> nodeToMove = null;
         Node<T> nodeToMoveParent = null;
         Node<T> nodeToFollow = null;
         for (Node<T> nodeLast = head, nodeCurrent = nodeLast.next;
              nodeCurrent != null && (null == nodeToMove || null == nodeToFollow );
              nodeLast = nodeCurrent, nodeCurrent = nodeCurrent.next) {

            if (elToMove.equals(nodeCurrent.item)) {
               nodeToMove = nodeCurrent;
               nodeToMoveParent = nodeLast;
            } else if (elToFollow.equals(nodeCurrent.item)) {
               nodeToFollow = nodeCurrent;
            }
         }
         if (null != nodeToMove && null != nodeToFollow) {
            unlinkQuietly(nodeToMove, nodeToMoveParent);
            linkQuietly(nodeToMove, nodeToFollow);
            return true;
         }
         return false;
      } finally {
         fullyUnlock();
      }
   }


   /**
    * Removes the node representing the given element from the list.
    * If it is removed, call insertProc to re-insert it somewhere else in the list.
    *
    * note: this will NOT signal any callbacks to signal that space is available
    * in the list, as the item will be immediately re-added in insertProc
    *
    * invariant:
    *  - the overall list size does not change
    *
    * @param itemToMove
    * Value of the node to move
    *
    * @param insertProc
    * A method which takes the Node whose node.item is equal to itemToMove
    * and inserts it somewhere else in the list.
    *
    * @return
    * true - if the list was modified
    * false - if no Node whose value matched itemToMove was found
    */
   private boolean unlinkAndCall(T itemToMove, Closure<Node<T>> insertProc) {
      fullyLock();
      try {
         // find the nodeToLink in the list
         for (Node<T> nodeLast = head, nodeCurrent = nodeLast.next;
              nodeCurrent != null;
              nodeLast = nodeCurrent, nodeCurrent = nodeCurrent.next) {
            if (itemToMove.equals(nodeCurrent.item)) {

               // remove it from its current location
               unlinkQuietly(nodeCurrent, nodeLast);

               // add it where the caller wants
               insertProc.apply(nodeCurrent);

               return true;
            }
         }
         return false;
      } finally {
         fullyUnlock();
      }
   }

   /**
    * Different from unlink in that it doesn't signal anything
    * and doesn't clear nodeToUnlink.item.
    *
    * precondition:
    *  - nodeParent.next == nodeToUnlink
    *
    * invariant:
    *  - the list must be fully locked when this is called
    *
    * @param nodeToUnlink
    * the node which will be removed
    *
    * @param nodeParent
    * the item before nodeToUnlink in the linked list
    */
   @SuppressWarnings("ObjectEquality")
   private void unlinkQuietly(@Nonnull Node<T> nodeToUnlink, @Nonnull Node<T> nodeParent) {
      if (nodeParent.next != nodeToUnlink) {
         throw new IllegalStateException("unlinkQuietly precondition violated");
      }
      nodeParent.next = nodeToUnlink.next;
      if (last == nodeToUnlink) {
         last = nodeParent;
      }
   }

   /**
    * Links nodeToLink immediately before nodeParent.
    *
    * preconditions:
    *  - nodeToLink and nodeParent are different nodes
    *  - nodeParent is already in the list
    *
    * invariant:
    *  - the list must be fully locked when this is called
    *
    * @param nodeToLink
    * the node to insert
    *
    * @param nodeParent
    * the node, currently in the list, immediately after
    * which nodeToLink will be inserted
    */
   @SuppressWarnings("ObjectEquality")
   private void linkQuietly(@Nonnull Node<T> nodeToLink, @Nonnull Node<T> nodeParent) {
      if (nodeParent == nodeToLink) {
         throw new IllegalStateException("linkQuietly precondition violated");
      }
      nodeToLink.next = nodeParent.next;
      nodeParent.next = nodeToLink;
      if (last == nodeParent) {
         last = nodeToLink;
      }
   }
}
