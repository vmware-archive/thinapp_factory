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

import java.util.Collection;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import static org.junit.Assert.*;

/**
 * A test for OrderableLinkedBlockingQueue.
 */
@SuppressWarnings({"NestedAssignment"})
public class OrderableLinkedBlockingQueueTest {

   private static final Logger log = LoggerFactory.getLogger(OrderableLinkedBlockingQueueTest.class);

   @Before
   public void setUp() throws Exception {
      /* Empty */
   }

   @After
   public void tearDown() throws Exception {
      /* Empty */
   }

   @Test
   public void myTest() {
      int NUM_ELEMENTS = 12;
      boolean result;
      OrderableLinkedBlockingQueue<Integer> queue = new OrderableLinkedBlockingQueue<Integer>();
      fillQueueWithElements(queue, NUM_ELEMENTS);

      assertEquals(NUM_ELEMENTS, queue.size());
      assertQueueEquals(queue,
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

      assertEquals(0, (int)queue.peek());
      int removed = queue.poll();
      assertEquals(0, removed);
      assertQueueEquals(queue,
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

      assertEquals(NUM_ELEMENTS - 1, queue.size());
      assertEquals(1, (int)queue.peek());

      queue.moveToHead(5);
      assertQueueEquals(queue,
                        5, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11);

      removed = queue.poll();
      assertEquals(5, removed);

      assertQueueEquals(queue,
                        1, 2, 3, 4, 6, 7, 8, 9, 10, 11);

      // 1 should still be the next element in the queue
      assertEquals(1, (int)queue.peek());

      result = queue.moveAfter(3, 1);
      assertTrue(result);

      assertQueueEquals(queue,
                        1, 3, 2, 4, 6, 7, 8, 9, 10, 11);

      // 1 should still be the next element in the queue
      removed = queue.poll();
      assertEquals(1, removed);

      assertQueueEquals(queue,
                        3, 2, 4, 6, 7, 8, 9, 10, 11);

      // now 3 should be the next element of in the queue
      assertEquals(3, (int)queue.peek());
      removed = queue.poll();
      assertEquals(3, removed);

      assertQueueEquals(queue,
                        2, 4, 6, 7, 8, 9, 10, 11);

      // now 2 should be the next element of in the queue
      assertEquals(2, (int)queue.peek());

      result = queue.moveAfter(2, 4);
      assertTrue(result);

      assertQueueEquals(queue,
                        4, 2, 6, 7, 8, 9, 10, 11);

      // now 2 should now be the second element of in the queue
      assertEquals(2, (int) Iterators.get(queue.iterator(), 1));
      removed = queue.poll();
      assertEquals(4, removed);

      assertQueueEquals(queue,
                        2, 6, 7, 8, 9, 10, 11);


      removed = queue.poll();
      assertEquals(2, removed);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 10, 11);

      // try to move an element in front of itself
      result = queue.moveAfter(6, 6);
      assertFalse(result);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 10, 11);

      // remember that 5 has been removed
      result = queue.moveAfter(7, 5);
      assertFalse(result);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 10, 11);

      result = queue.moveAfter(5, 7);
      assertFalse(result);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 10, 11);

      result = queue.moveAfter(33, 32);
      assertFalse(result);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 10, 11);

      // move 10 to the end of the queue
      queue.moveToTail(10);

      assertQueueEquals(queue,
                        6, 7, 8, 9, 11, 10);

      // drain the rest of the queue, and check the last element
      int last = -1;
      Integer val;
      while (null != (val = queue.poll())) {
         last = val;
      }
      assertEquals(10, last);
   }

   private static void assertQueueEquals(Collection<Integer> queue,
                                         Integer... vals) {
      log.debug("Queue contents:");
      log.debug(Joiner.on("\n\t").join(queue));

      assertArrayEquals(
            "Queue in unexpected order",
            Lists.newArrayList(vals).toArray(),
            queue.toArray()
      );
   }

   private static void fillQueueWithElements(Queue<Integer> queue, int elementCount) {
      for (int i = 0; i < elementCount; ++i) {
         queue.offer(i);
      }
   }
}
