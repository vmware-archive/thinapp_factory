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

package com.vmware.thinapp.workpool;

import java.util.Collection;

import com.google.common.eventbus.Subscribe;
import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

import akka.dispatch.Future;
import scala.Option;

/**
 * Manages a collection of {@link WorkpoolInstance}.
 */
public interface WorkpoolManager {
   /**
    * Create a new WorkpoolInstance.
    *
    * @param workpool a configured workpool data model
    * @return a new instance
    */
   WorkpoolInstance create(WorkpoolModel workpool);

   /**
    * Delete a workpool instance.
    *
    * @param id workpool instance id
    * @param deleteMethod how the workpool should be deleted
    * @return future with no value to be used when synchronous behavior is needed
    */
   Future<Void> delete(long id, DeleteMethod deleteMethod);

   /**
    * Attempt to find an existing WorkpoolInstance by its name.
    *
    * @param name a workpool name
    * @return maybe the associated workpool instance
    */
   Option<WorkpoolInstance> findByName(String name);

   /**
    * Retrieve a WorkpoolInstance by its id.
    *
    *
    * @param id workpool model id
    * @return
    */
   Option<WorkpoolInstance> get(long id);

   /**
    * Return a list of all workpool data models.
    *
    * @return
    */
   Collection<WorkpoolInstance> list();

   /**
    * Private.
    *
    * @param stateChange
    */
   @Subscribe
   void update(WorkpoolStateChange stateChange);

   /**
    * Reset all workpools and images by deleting any current leases and
    * attempting to shut down all running VMs.
    */
   void reset();
}
