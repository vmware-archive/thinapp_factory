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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A callable that has a handle to its own future.
 *
 * The task queue uses this for two purposes:
 *  1. when aborting a running task, this can provide the Future
 *     that we can cancel() using {@link Future#cancel(boolean)}
 *  2. the pending tasks queue is actually a queue of Futures, not
 *     tasks.  This interface lets us map from a task to a Future,
 *     so that we can find the element in the pending tasks queue
 *     which corresponds to a given task.  We can then re-order
 *     the queue as we would like.
 */
public interface StoresFuture
      extends Callable<Void> {

   void setFuture(Future<Void> future);

   Future<Void> getFuture();
}
