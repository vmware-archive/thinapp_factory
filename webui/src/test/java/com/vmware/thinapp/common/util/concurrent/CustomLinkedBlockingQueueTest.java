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

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.*;

@SuppressWarnings({"ALL","unchecked"})
public class CustomLinkedBlockingQueueTest extends JSR166TestCase {

   @SuppressWarnings("override")
   public static class Unbounded extends BlockingQueueTest {
      @Override
      protected BlockingQueue emptyCollection() {
         return new CustomLinkedBlockingQueue();
      }
   }

   @SuppressWarnings("override")
   public static class Bounded extends BlockingQueueTest {
      @Override
      protected BlockingQueue emptyCollection() {
         return new CustomLinkedBlockingQueue(SIZE);
      }
   }

   /**
    * Create a queue of given size containing consecutive
    * Integers 0 ... n.
    */
   private CustomLinkedBlockingQueue<Integer> populatedQueue(int n) {
      CustomLinkedBlockingQueue<Integer> q =
            new CustomLinkedBlockingQueue<Integer>(n);
      assertTrue(q.isEmpty());
      for (int i = 0; i < n; i++)
         assertTrue(q.offer(new Integer(i)));
      assertFalse(q.isEmpty());
      assertEquals(0, q.remainingCapacity());
      assertEquals(n, q.size());
      return q;
   }

   /**
    * A new queue has the indicated capacity, or Integer.MAX_VALUE if
    * none given
    */
   public void testConstructor1() {
      assertEquals(SIZE, new CustomLinkedBlockingQueue(SIZE).remainingCapacity());
      assertEquals(Integer.MAX_VALUE, new CustomLinkedBlockingQueue().remainingCapacity());
   }

   /**
    * Constructor throws IllegalArgumentException if capacity argument nonpositive
    */
   public void testConstructor2() {
      try {
         new CustomLinkedBlockingQueue(0);
         shouldThrow();
      } catch (IllegalArgumentException success) {/* Ignore */}
   }

   /**
    * Initializing from null Collection throws NullPointerException
    */
   public void testConstructor3() {
      try {
         new CustomLinkedBlockingQueue(null);
         shouldThrow();
      } catch (NullPointerException success) {/* Ignore */}
   }

   /**
    * Initializing from Collection of null elements throws NullPointerException
    */
   public void testConstructor4() {
      Collection<Integer> elements = Arrays.asList(new Integer[SIZE]);
      try {
         new CustomLinkedBlockingQueue(elements);
         shouldThrow();
      } catch (NullPointerException success) {/* Ignore */}
   }

   /**
    * Initializing from Collection with some null elements throws
    * NullPointerException
    */
   public void testConstructor5() {
      Integer[] ints = new Integer[SIZE];
      for (int i = 0; i < SIZE-1; ++i)
         ints[i] = new Integer(i);
      Collection<Integer> elements = Arrays.asList(ints);
      try {
         new CustomLinkedBlockingQueue(elements);
         shouldThrow();
      } catch (NullPointerException success) {/* Ignore */}
   }

   /**
    * Queue contains all elements of collection used to initialize
    */
   public void testConstructor6() {
      Integer[] ints = new Integer[SIZE];
      for (int i = 0; i < SIZE; ++i)
         ints[i] = new Integer(i);
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(Arrays.asList(ints));
      for (int i = 0; i < SIZE; ++i)
         assertEquals(ints[i], q.poll());
   }

   /**
    * Queue transitions from empty to full when elements added
    */
   public void testEmptyFull() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(2);
      assertTrue(q.isEmpty());
      assertEquals("should have room for 2", 2, q.remainingCapacity());
      q.add(one);
      assertFalse(q.isEmpty());
      q.add(two);
      assertFalse(q.isEmpty());
      assertEquals(0, q.remainingCapacity());
      assertFalse(q.offer(three));
   }

   /**
    * remainingCapacity decreases on add, increases on remove
    */
   public void testRemainingCapacity() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.remainingCapacity());
         assertEquals(SIZE-i, q.size());
         q.remove();
      }
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(SIZE-i, q.remainingCapacity());
         assertEquals(i, q.size());
         q.add(new Integer(i));
      }
   }

   /**
    * Offer succeeds if not full; fails if full
    */
   public void testOffer() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(1);
      assertTrue(q.offer(zero));
      assertFalse(q.offer(one));
   }

   /**
    * add succeeds if not full; throws IllegalStateException if full
    */
   public void testAdd() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE);
      for (int i = 0; i < SIZE; ++i)
         assertTrue(q.add(new Integer(i)));
      assertEquals(0, q.remainingCapacity());
      try {
         q.add(new Integer(SIZE));
         shouldThrow();
      } catch (IllegalStateException success) {/* Ignore */}
   }

   /**
    * addAll(this) throws IllegalArgumentException
    */
   public void testAddAllSelf() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      try {
         q.addAll(q);
         shouldThrow();
      } catch (IllegalArgumentException success) {/* Ignore */}
   }

   /**
    * addAll of a collection with any null elements throws NPE after
    * possibly adding some elements
    */
   public void testAddAll3() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE);
      Integer[] ints = new Integer[SIZE];
      for (int i = 0; i < SIZE-1; ++i)
         ints[i] = new Integer(i);
      Collection<Integer> elements = Arrays.asList(ints);
      try {
         q.addAll(elements);
         shouldThrow();
      } catch (NullPointerException success) {/* Ignore */}
   }

   /**
    * addAll throws IllegalStateException if not enough room
    */
   public void testAddAll4() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE - 1);
      Integer[] ints = new Integer[SIZE];
      for (int i = 0; i < SIZE; ++i)
         ints[i] = new Integer(i);
      Collection<Integer> elements = Arrays.asList(ints);
      try {
         q.addAll(elements);
         shouldThrow();
      } catch (IllegalStateException success) {/* Ignore */}
   }

   /**
    * Queue contains all elements, in traversal order, of successful addAll
    */
   public void testAddAll5() {
      Integer[] empty = new Integer[0];
      Integer[] ints = new Integer[SIZE];
      for (int i = 0; i < SIZE; ++i)
         ints[i] = new Integer(i);
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE);
      assertFalse(q.addAll(Arrays.asList(empty)));
      assertTrue(q.addAll(Arrays.asList(ints)));
      for (int i = 0; i < SIZE; ++i)
         assertEquals(ints[i], q.poll());
   }

   /**
    * all elements successfully put are contained
    */
   public void testPut() throws InterruptedException {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         Integer I = new Integer(i);
         q.put(I);
         assertTrue(q.contains(I));
      }
      assertEquals(0, q.remainingCapacity());
   }

   /**
    * put blocks interruptibly if full
    */
   public void testBlockingPut() throws InterruptedException {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(SIZE);
      final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
      Thread t = newStartedThread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            for (int i = 0; i < SIZE; ++i)
               q.put(i);
            assertEquals(SIZE, q.size());
            assertEquals(0, q.remainingCapacity());

            Thread.currentThread().interrupt();
            try {
               q.put(99);
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
            assertFalse(Thread.interrupted());

            pleaseInterrupt.countDown();
            try {
               q.put(99);
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
            assertFalse(Thread.interrupted());
         }});

      await(pleaseInterrupt);
      assertThreadStaysAlive(t);
      t.interrupt();
      awaitTermination(t);
      assertEquals(SIZE, q.size());
      assertEquals(0, q.remainingCapacity());
   }

   /**
    * put blocks interruptibly waiting for take when full
    */
   public void testPutWithTake() throws InterruptedException {
      final int capacity = 2;
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(2);
      final CountDownLatch pleaseTake = new CountDownLatch(1);
      final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
      Thread t = newStartedThread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            for (int i = 0; i < capacity; i++)
               q.put(i);
            pleaseTake.countDown();
            q.put(86);

            pleaseInterrupt.countDown();
            try {
               q.put(99);
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
            assertFalse(Thread.interrupted());
         }});

      await(pleaseTake);
      assertEquals(0, q.remainingCapacity());
      assertEquals(0, q.take());

      await(pleaseInterrupt);
      assertThreadStaysAlive(t);
      t.interrupt();
      awaitTermination(t);
      assertEquals(0, q.remainingCapacity());
   }

   /**
    * timed offer times out if full and elements not taken
    */
   public void testTimedOffer() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(2);
      final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
      Thread t = newStartedThread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            q.put(new Object());
            q.put(new Object());
            long startTime = System.nanoTime();
            assertFalse(q.offer(new Object(), timeoutMillis(), MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            pleaseInterrupt.countDown();
            try {
               q.offer(new Object(), 2 * LONG_DELAY_MS, MILLISECONDS);
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
         }});

      await(pleaseInterrupt);
      assertThreadStaysAlive(t);
      t.interrupt();
      awaitTermination(t);
   }

   /**
    * take retrieves elements in FIFO order
    */
   public void testTake() throws InterruptedException {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.take());
      }
   }

   /**
    * Take removes existing elements until empty, then blocks interruptibly
    */
   public void testBlockingTake() throws InterruptedException {
      final BlockingQueue q = populatedQueue(SIZE);
      final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
      Thread t = newStartedThread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            for (int i = 0; i < SIZE; ++i) {
               assertEquals(i, q.take());
            }

            Thread.currentThread().interrupt();
            try {
               q.take();
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
            assertFalse(Thread.interrupted());

            pleaseInterrupt.countDown();
            try {
               q.take();
               shouldThrow();
            } catch (InterruptedException success) {/* Ignore */}
            assertFalse(Thread.interrupted());
         }});

      await(pleaseInterrupt);
      assertThreadStaysAlive(t);
      t.interrupt();
      awaitTermination(t);
   }

   /**
    * poll succeeds unless empty
    */
   public void testPoll() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.poll());
      }
      assertNull(q.poll());
   }

   /**
    * timed poll with zero timeout succeeds when non-empty, else times out
    */
   public void testTimedPoll0() throws InterruptedException {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.poll(0, MILLISECONDS));
      }
      assertNull(q.poll(0, MILLISECONDS));
   }

   /**
    * timed poll with nonzero timeout succeeds when non-empty, else times out
    */
   public void testTimedPoll() throws InterruptedException {
      CustomLinkedBlockingQueue<Integer> q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         long startTime = System.nanoTime();
         assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
         assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
      }
      long startTime = System.nanoTime();
      assertNull(q.poll(timeoutMillis(), MILLISECONDS));
      assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
      checkEmpty(q);
   }

   /**
    * Interrupted timed poll throws InterruptedException instead of
    * returning timeout status
    */
   public void testInterruptedTimedPoll() throws InterruptedException {
      final BlockingQueue<Integer> q = populatedQueue(SIZE);
      final CountDownLatch aboutToWait = new CountDownLatch(1);
      Thread t = newStartedThread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            for (int i = 0; i < SIZE; ++i) {
               long t0 = System.nanoTime();
               assertEquals(i, (int) q.poll(LONG_DELAY_MS, MILLISECONDS));
               assertTrue(millisElapsedSince(t0) < SMALL_DELAY_MS);
            }
            long t0 = System.nanoTime();
            aboutToWait.countDown();
            try {
               q.poll(MEDIUM_DELAY_MS, MILLISECONDS);
               shouldThrow();
            } catch (InterruptedException success) {
               assertTrue(millisElapsedSince(t0) < MEDIUM_DELAY_MS);
            }
         }});

      aboutToWait.await();
      waitForThreadToEnterWaitState(t, SMALL_DELAY_MS);
      t.interrupt();
      awaitTermination(t, MEDIUM_DELAY_MS);
      checkEmpty(q);
   }

   /**
    * peek returns next element, or null if empty
    */
   public void testPeek() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.peek());
         assertEquals(i, q.poll());
         assertTrue(q.peek() == null ||
                    !q.peek().equals(i));
      }
      assertNull(q.peek());
   }

   /**
    * element returns next element, or throws NSEE if empty
    */
   public void testElement() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.element());
         assertEquals(i, q.poll());
      }
      try {
         q.element();
         shouldThrow();
      } catch (NoSuchElementException success) {/* Ignore */}
   }

   /**
    * remove removes next element, or throws NSEE if empty
    */
   public void testRemove() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertEquals(i, q.remove());
      }
      try {
         q.remove();
         shouldThrow();
      } catch (NoSuchElementException success) {/* Ignore */}
   }

   /**
    * An add following remove(x) succeeds
    */
   public void testRemoveElementAndAdd() throws InterruptedException {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue();
      assertTrue(q.add(new Integer(1)));
      assertTrue(q.add(new Integer(2)));
      assertTrue(q.remove(new Integer(1)));
      assertTrue(q.remove(new Integer(2)));
      assertTrue(q.add(new Integer(3)));
      assertTrue(q.take() != null);
   }

   /**
    * contains(x) reports true when elements added but not yet removed
    */
   public void testContains() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertTrue(q.contains(new Integer(i)));
         q.poll();
         assertFalse(q.contains(new Integer(i)));
      }
   }

   /**
    * clear removes all elements
    */
   public void testClear() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      q.clear();
      assertTrue(q.isEmpty());
      assertEquals(0, q.size());
      assertEquals(SIZE, q.remainingCapacity());
      q.add(one);
      assertFalse(q.isEmpty());
      assertTrue(q.contains(one));
      q.clear();
      assertTrue(q.isEmpty());
   }

   /**
    * containsAll(c) is true when c contains a subset of elements
    */
   public void testContainsAll() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      CustomLinkedBlockingQueue p = new CustomLinkedBlockingQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         assertTrue(q.containsAll(p));
         assertFalse(p.containsAll(q));
         p.add(new Integer(i));
      }
      assertTrue(p.containsAll(q));
   }

   /**
    * retainAll(c) retains only those elements of c and reports true if changed
    */
   public void testRetainAll() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      CustomLinkedBlockingQueue p = populatedQueue(SIZE);
      for (int i = 0; i < SIZE; ++i) {
         boolean changed = q.retainAll(p);
         if (i == 0)
            assertFalse(changed);
         else
            assertTrue(changed);

         assertTrue(q.containsAll(p));
         assertEquals(SIZE-i, q.size());
         p.remove();
      }
   }

   /**
    * removeAll(c) removes only those elements of c and reports true if changed
    */
   public void testRemoveAll() {
      for (int i = 1; i < SIZE; ++i) {
         CustomLinkedBlockingQueue q = populatedQueue(SIZE);
         CustomLinkedBlockingQueue p = populatedQueue(i);
         assertTrue(q.removeAll(p));
         assertEquals(SIZE-i, q.size());
         for (int j = 0; j < i; ++j) {
            Integer I = (Integer)(p.remove());
            assertFalse(q.contains(I));
         }
      }
   }

   /**
    * toArray contains all elements in FIFO order
    */
   public void testToArray() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      Object[] o = q.toArray();
      for (int i = 0; i < o.length; i++)
         assertSame(o[i], q.poll());
   }

   /**
    * toArray(a) contains all elements in FIFO order
    */
   public void testToArray2() throws InterruptedException {
      CustomLinkedBlockingQueue<Integer> q = populatedQueue(SIZE);
      Integer[] ints = new Integer[SIZE];
      Integer[] array = q.toArray(ints);
      assertSame(ints, array);
      for (int i = 0; i < ints.length; i++)
         assertSame(ints[i], q.poll());
   }

   /**
    * toArray(incompatible array type) throws ArrayStoreException
    */
   public void testToArray1_BadArg() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      try {
         q.toArray(new String[10]);
         shouldThrow();
      } catch (ArrayStoreException success) {/* Ignore */}
   }

   /**
    * iterator iterates through all elements
    */
   public void testIterator() throws InterruptedException {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      Iterator it = q.iterator();
      while (it.hasNext()) {
         assertEquals(it.next(), q.take());
      }
   }

   /**
    * iterator.remove removes current element
    */
   public void testIteratorRemove() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(3);
      q.add(two);
      q.add(one);
      q.add(three);

      Iterator it = q.iterator();
      it.next();
      it.remove();

      it = q.iterator();
      assertSame(it.next(), one);
      assertSame(it.next(), three);
      assertFalse(it.hasNext());
   }

   /**
    * iterator ordering is FIFO
    */
   public void testIteratorOrdering() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(3);
      q.add(one);
      q.add(two);
      q.add(three);
      assertEquals(0, q.remainingCapacity());
      int k = 0;
      for (Iterator it = q.iterator(); it.hasNext();) {
         assertEquals(++k, it.next());
      }
      assertEquals(3, k);
   }

   /**
    * Modifications do not cause iterators to fail
    */
   public void testWeaklyConsistentIteration() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(3);
      q.add(one);
      q.add(two);
      q.add(three);
      for (Iterator it = q.iterator(); it.hasNext();) {
         q.remove();
         it.next();
      }
      assertEquals(0, q.size());
   }

   /**
    * toString contains toStrings of elements
    */
   public void testToString() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      String s = q.toString();
      for (int i = 0; i < SIZE; ++i) {
         assertTrue(s.contains(String.valueOf(i)));
      }
   }

   /**
    * offer transfers elements across Executor tasks
    */
   public void testOfferInExecutor() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(2);
      q.add(one);
      q.add(two);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      final CheckedBarrier threadsStarted = new CheckedBarrier(2);
      executor.execute(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            assertFalse(q.offer(three));
            threadsStarted.await();
            assertTrue(q.offer(three, LONG_DELAY_MS, MILLISECONDS));
            assertEquals(0, q.remainingCapacity());
         }});

      executor.execute(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            threadsStarted.await();
            assertSame(one, q.take());
         }});

      joinPool(executor);
   }

   /**
    * timed poll retrieves elements across Executor threads
    */
   public void testPollInExecutor() {
      final CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue(2);
      final CheckedBarrier threadsStarted = new CheckedBarrier(2);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      executor.execute(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            assertNull(q.poll());
            threadsStarted.await();
            assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
            checkEmpty(q);
         }});

      executor.execute(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            threadsStarted.await();
            q.put(one);
         }});

      joinPool(executor);
   }

   /**
    * A deserialized serialized queue has same elements in same order
    */
   public void testSerialization() throws Exception {
      Queue x = populatedQueue(SIZE);
      Queue y = serialClone(x);

      assertTrue(x != y);
      assertEquals(x.size(), y.size());
      assertEquals(x.toString(), y.toString());
      assertTrue(Arrays.equals(x.toArray(), y.toArray()));
      while (!x.isEmpty()) {
         assertFalse(y.isEmpty());
         assertEquals(x.remove(), y.remove());
      }
      assertTrue(y.isEmpty());
   }

   /**
    * drainTo(c) empties queue into another collection c
    */
   public void testDrainTo() {
      CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      ArrayList l = new ArrayList();
      q.drainTo(l);
      assertEquals(0, q.size());
      assertEquals(SIZE, l.size());
      for (int i = 0; i < SIZE; ++i)
         assertEquals(l.get(i), new Integer(i));
      q.add(zero);
      q.add(one);
      assertFalse(q.isEmpty());
      assertTrue(q.contains(zero));
      assertTrue(q.contains(one));
      l.clear();
      q.drainTo(l);
      assertEquals(0, q.size());
      assertEquals(2, l.size());
      for (int i = 0; i < 2; ++i)
         assertEquals(l.get(i), new Integer(i));
   }

   /**
    * drainTo empties full queue, unblocking a waiting put.
    */
   public void testDrainToWithActivePut() throws InterruptedException {
      final CustomLinkedBlockingQueue q = populatedQueue(SIZE);
      Thread t = new Thread(new CheckedRunnable() {
         @Override
         public void realRun() throws InterruptedException {
            q.put(new Integer(SIZE+1));
         }});

      t.start();
      ArrayList l = new ArrayList();
      q.drainTo(l);
      assertTrue(l.size() >= SIZE);
      for (int i = 0; i < SIZE; ++i)
         assertEquals(l.get(i), new Integer(i));
      t.join();
      assertTrue(q.size() + l.size() >= SIZE);
   }

   /**
    * drainTo(c, n) empties first min(n, size) elements of queue into c
    */
   public void testDrainToN() {
      CustomLinkedBlockingQueue q = new CustomLinkedBlockingQueue();
      for (int i = 0; i < SIZE + 2; ++i) {
         for (int j = 0; j < SIZE; j++)
            assertTrue(q.offer(new Integer(j)));
         ArrayList l = new ArrayList();
         q.drainTo(l, i);
         int k = (i < SIZE) ? i : SIZE;
         assertEquals(k, l.size());
         assertEquals(SIZE-k, q.size());
         for (int j = 0; j < k; ++j)
            assertEquals(l.get(j), new Integer(j));
         while (q.poll() != null) {/* Empty */}
      }
   }

}