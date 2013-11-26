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

import javax.annotation.Nonnull;

import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.appfactory.taskqueue.tasks.state.builder.TaskStateBuilder;

public interface AppFactoryTask
      <T extends TaskState<T,S,E>,
      S extends TaskStateBuilder<T,S,E>,
      E extends Enum<E>>
   extends StoresFuture
{
   String getTaskHandle();

   @Nonnull
   T getCurrentTaskState();

   void abort();

   // note: only used on some classes!
   void unstall();

   void cleanup() throws TaskException;
}
