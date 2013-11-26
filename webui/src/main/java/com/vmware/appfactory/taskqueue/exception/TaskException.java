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

package com.vmware.appfactory.taskqueue.exception;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.taskqueue.tasks.state.tasks.AppFactoryTask;
import com.vmware.thinapp.common.exception.BaseException;

/**
 * Errors encountered while creating, initializing, and updating tasks are
 * all reported using this exception class.
 */
public class TaskException
   extends BaseException
{
   private static final long serialVersionUID = 1L;
   @SuppressWarnings("unused")
   private final AppFactoryTask task;

   public TaskException(AppFactoryTask task, String reason)
   {
      super(reason);
      this.task = task;
   }

   public TaskException(AppFactoryTask task, Throwable cause)
   {
      super(cause);
      this.task = task;
   }

   public TaskException(AppFactoryTask task, String reason, Throwable cause)
   {
      super(reason,cause);
      this.task = task;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
