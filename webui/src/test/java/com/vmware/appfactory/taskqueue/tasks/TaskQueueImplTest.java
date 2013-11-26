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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.TestState;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.appfactory.taskqueue.tasks.state.tasks.SleepyTask;

@SuppressWarnings("BusyWait")
public class TaskQueueImplTest {

   private static final Logger log =
         LoggerFactory.getLogger(TaskQueueImplTest.class);

   TaskQueue queue;
   TestTaskHelperFactory factory;

   @Before
   public void setup() {
      factory = new TestTaskHelperFactory();
      queue = new TaskQueueImpl(
            "testQueue",
            3,
            Suppliers.ofInstance(3),   // 3 concurrent tasks
            Suppliers.ofInstance(2)    // 2 finished tasks
      );
   }

   @After
   public void teardown() {
      queue.gracefulShutdown(true);
   }

   @Test
   public void testAddTask() throws Exception {

      Iterable<TaskState> tasks = queue.getAllTasks();
      assertNotNull(tasks);
      assertTrue(Iterables.isEmpty(tasks));

      // call all the search methods to ensure nothing is found
      assertTrue(Iterables.isEmpty(queue.findActiveTasksForApp(123L)));
      assertTrue(Iterables.isEmpty(queue.findActiveTasksForFeed(123L)));
      assertNull(queue.findTaskById(123L));

      AppFactoryTask task = newSleepyTask();
      Long id = task.getCurrentTaskState().getId();
      assertNotNull(id);
      assertTrue(id > 0);
      queue.addTask(task);

      TaskState taskState = queue.findTaskById(id);
      assertNotNull(taskState);
      assertEquals(id, taskState.getId());
      assertTrue(taskState.getQueued() > 0);
      assertNotNull(taskState.getQueueStatus());
      assertTrue(taskState.getQueueStatus().ordinal() >= TaskQueue.MetaStatus.WAITING.ordinal());

      assertFalse(Iterables.isEmpty(queue.getAllTasks()));
      assertTrue(Iterables.isEmpty(queue.findActiveTasksForApp(id)));
      assertTrue(Iterables.isEmpty(queue.findActiveTasksForFeed(id)));
      assertTrue(Iterables.isEmpty(queue.findActiveTasksForFeed(123L)));

      assertEquals(id, queue.getAllTasks().iterator().next().getId());


      // wait for the task to complete
      queue.gracefulShutdown(false);

      taskState = queue.findTaskById(id);
      assertNotNull(taskState);
      assertEquals(TestState.TestStatus.complete, taskState.getStatus());
      assertEquals(100, taskState.getProgress());
      assertEquals(TaskQueue.MetaStatus.FINISHED, taskState.getQueueStatus());
      assertFalse(taskState.isAborted());

      assertTrue(factory.resetNumEventsFired() > 0);
   }

   @Test
   public void testQueueing() throws Exception {
      // todo: start lots of tasks, make sure they're queued
      // cancel them all, make sure they're stopped
   }


   @SuppressWarnings("ConstantConditions")
   @Test(timeout = 500)    // only give 500ms to run
   public void testAbortTask() throws Exception {
      SleepyTask sleepyTask = newSleepyTask(60);  // sleep for one minute.  It won't take this long,
                  // since we'll be interrupted.  But this guarantees
                  // that we won't finish the task normally

      long id = sleepyTask.getCurrentTaskState().getId();

      queue.addTask(sleepyTask);

      // wait for the task to start running
      while (!TaskQueue.MetaStatus.RUNNING.equals(queue.findTaskById(id).getQueueStatus())) {
         Thread.sleep(20);
      }
      assertTrue(TaskQueue.MetaStatus.RUNNING.equals(queue.findTaskById(id).getQueueStatus()));

      queue.abortTask(id);
      // now wait for it to be finished
      while (TaskQueue.MetaStatus.RUNNING.equals(queue.findTaskById(id).getQueueStatus())) {
         Thread.sleep(20);
      }

      TaskState taskState = queue.findTaskById(id);
      assertNotNull(taskState);
      assertEquals(TaskQueue.MetaStatus.FINISHED, taskState.getQueueStatus());
      assertTrue(taskState.isAborted());
      assertTrue(sleepyTask.isInterrupted());
      if (-1 == taskState.getStarted()) {
         // task went straight from waiting to finished
         assertFalse(sleepyTask.isRun());
         assertEquals(TestState.TestStatus.testing, taskState.getStatus());
      } else {
         // task started, but then aborted
         assertTrue(sleepyTask.isRun());
         assertEquals(TestState.TestStatus.swearing, taskState.getStatus());
      }
      assertEquals(-1, taskState.getProgress());

      // also, try aborting a non-existent task to make
      // sure we don't crash
      queue.abortTask(-123L);

      assertTrue(factory.resetNumEventsFired() > 0);
   }

   private AppFactoryTask newSleepyTask() {
      return newSleepyTask(0);
   }

   private SleepyTask newSleepyTask(int sleepTime) {
      return new SleepyTask(
            factory,
            123L,
            "test task",
            TestState.TestStatus.testing,
            sleepTime);
   }

//   @Test(timeout = 500)    // only give 500ms to run
   @Test
   public void testCleanup() throws Exception {

      // by default we only have space for 2 completed tasks, but we're going to start 10
      testWithMaxFinishedSize(10, 2);

      queue.gracefulShutdown(false);

      // now make a new queue with space for 10 completed tasks
      queue = new TaskQueueImpl(
            "testQueue",
            3,
            Suppliers.ofInstance(3),   // 3 concurrent tasks
            Suppliers.ofInstance(10)    // 2 finished tasks
      );

      testWithMaxFinishedSize(10, 10);

      queue.gracefulShutdown(false);

      // make another queue, and try to clean it up
      // before anything happens, just to make sure
      // we don't error
      queue = new TaskQueueImpl(
            "testQueue",
            3,
            Suppliers.ofInstance(3),   // 3 concurrent tasks
            Suppliers.ofInstance(10)    // 2 finished tasks
      );
      queue.cleanup();

      assertTrue(0 == factory.resetNumEventsFired());
   }

   private void testWithMaxFinishedSize(int tasksToAdd,
                                        int maxFinishedSize) throws InterruptedException {

      // add 10 tasks
      final int expectedFinishedTasks = Math.min(tasksToAdd, maxFinishedSize);

      // add 10 tasks, wait for them all to finish,
      // then clean them all up and ensure they're gone
      assertTrue(Iterables.isEmpty(queue.getAllTasks()));

      for (int i = 0; i < tasksToAdd; ++i) {
         queue.addTask(newSleepyTask());
      }

      while (!Iterables.isEmpty(queue.getTasks(MetaStatusPredicate.NOT_FINISHED))) {
         Thread.sleep(20);
      }

      // wait for the queue to be drained
      queue.gracefulShutdown(false);

      log.debug(
           Joiner.on("\n\t").join(queue.getAllTasks())
      );
      assertEquals(expectedFinishedTasks, Iterables.size(queue.getTasks(MetaStatusPredicate.FINISHED)));
      assertEquals(0, Iterables.size(queue.getTasks(MetaStatusPredicate.RUNNING)));
      assertEquals(0, Iterables.size(queue.getTasks(MetaStatusPredicate.WAITING)));


      // look through each task, make sure it's finished and has a timestamp
      for (TaskState state : queue.getAllTasks()) {
         assertEquals(TaskQueue.MetaStatus.FINISHED, state.getQueueStatus());
         assertTrue(state.getQueued() > 0);
         assertTrue(state.getStarted() > 0);
         assertTrue(state.getFinished() > 0);
         assertTrue(state.getQueued() <= state.getStarted());
         assertTrue(state.getStarted() <= state.getFinished());
      }

      // now empty the queue!
      queue.cleanup();

      log.debug("REMAINING TASKS ARE:\n"+
            Joiner.on("\n\t").join(queue.getAllTasks())
      );
      assertTrue(Iterables.isEmpty(queue.getAllTasks()));

      assertTrue(factory.resetNumEventsFired() > 0);
   }

   @Test
   public void testUnstallConversion() throws Exception {
      /* Empty */
   }
}
