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

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

/**
 * Filters a list of task state based on their queue state: WAITING, RUNNING, or FINISHED.
 */
public class MetaStatusPredicate {

   public static final Predicate<? super TaskState> WAITING = new PredicateObj<TaskState>(TaskQueue.MetaStatus.WAITING);
   public static final Predicate<? super TaskState> RUNNING = new PredicateObj<TaskState>(TaskQueue.MetaStatus.RUNNING);
   public static final Predicate<? super TaskState> FINISHED = new PredicateObj<TaskState>(TaskQueue.MetaStatus.FINISHED);
   public static final Predicate<? super TaskState> NOT_FINISHED = new PredicateObj<TaskState>(TaskQueue.MetaStatus.WAITING, TaskQueue.MetaStatus.RUNNING);

   public static <T extends TaskState> Predicate<T> custom(TaskQueue.MetaStatus... metas) {
      return new PredicateObj<T>(metas);
   }

   /**
    * Get a Predicate that can filter tasks by datastore id.
    *
    * @param <T> any class that extends TaskState
    * @param datastoreId a datastore id
    * @return a Predicate instance.
    */
   public static <T extends TaskState> Predicate<T> withDatastoreFilter(Long datastoreId) {
      return new DatastoreFilterPredicateObj<T>(datastoreId);
   }

   protected static class PredicateObj<T extends TaskState> implements Predicate<T> {
      private final Set<TaskQueue.MetaStatus> metas;

      protected PredicateObj(TaskQueue.MetaStatus... metas) {
         this.metas = ImmutableSet.copyOf(metas);
      }

      @Override
      public boolean apply(T taskState) {
         return (null != taskState) && (metas.contains(taskState.getQueueStatus()));
      }
   }

   /**
    * Predicate class for finding tasks that use the given datastore.
    */
   protected static class DatastoreFilterPredicateObj<T extends TaskState> implements Predicate<T> {
      private final Long datastoreId;

      protected DatastoreFilterPredicateObj(Long datastoreId) {
         this.datastoreId = datastoreId;
      }

      @Override
      public boolean apply(T taskState) {
         if (taskState == null || !(taskState instanceof AbstractCaptureState)) {
            return false;
         }
         AbstractCaptureState captureState = (AbstractCaptureState) taskState;
         Long dsId = captureState.getCaptureRequest().getDatastoreId();
         return datastoreId.equals(dsId);
      }
   }

}
