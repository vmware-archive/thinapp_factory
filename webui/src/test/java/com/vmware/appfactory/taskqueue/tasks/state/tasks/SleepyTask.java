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

import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.state.TestState;

/**
 * Dummy task for testing the TaskQueue.
 */
public class SleepyTask extends AbstractTask
      <TestState, TestState.Builder, TestState.TestStatus> {

   private boolean ran;
   private boolean interrupted;
   private final int sleepSeconds;

   public SleepyTask(TaskHelperFactory taskHelperFactory,
                     Long recordId,
                     String description,
                     TestState.TestStatus status,
                     int sleepSeconds) {
      super(taskHelperFactory, new TestState.Builder()
            .withNewId(taskHelperFactory.getTaskIdSupplier())
            .withRecordId(recordId)
            .withDescription(description)
            .withStatus(status)
            .build()
      );
      ran = false;
      interrupted = false;
      this.sleepSeconds = sleepSeconds;
   }

   @Override
   protected void doRun() throws TaskException {
      try {
         synchronized (this) {
            _log.debug(getCurrentTaskState().getId() + " Running!");
            updateProgress(9);
            updateStatus(TestState.TestStatus.compiling);
            incrementProgressBy(1);
         }
         Thread.sleep(sleepSeconds * 1000L);
         synchronized (this) {
            _log.debug(getCurrentTaskState().getId() + " Completed!");
            updateStatus(TestState.TestStatus.complete);
            updateProgress(100);
         }
      } catch (InterruptedException e) {
         synchronized (this) {
            _log.debug(getCurrentTaskState().getId() + " Interrupted!");
            interrupted = true;
            updateStatus(TestState.TestStatus.swearing);
            updateProgress(-1);
         }
         throw new TaskException(this, "was interrupted!");
      } finally {
         synchronized (this) {
            ran = true;
         }
      }
   }

   @Override
   protected void doCleanup() throws TaskException {
      // nothing to do
   }

   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }

   public synchronized boolean isInterrupted() {
      return interrupted;
   }

   public synchronized boolean isRun() {
      return ran;
   }
}
