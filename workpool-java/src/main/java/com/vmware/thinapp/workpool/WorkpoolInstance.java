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

import com.google.common.eventbus.Subscribe;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.LeaseModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

import akka.dispatch.Future;

/**
 * Represents a single workpool instance.
 *
 * Each workpool manages a pool of {@link com.vmware.thinapp.workpool.model.InstanceModel}s that it leases out to interested clients.  It can grow the number
 * of clones on demand based on a configured maximum size.
 */
public interface WorkpoolInstance {
   /**
    * Private.
    */
   void _onInstanceRemoved(Future<Void> param, InstanceModel instanceModel);

   /**
    * Private.
    *
    * @param result
    * @param instance
    */
   void _onInstancingComplete(Future<CloneRunner.Result> result, InstanceModel instance);

   /**
    * Callback once all VMs have been powered down during a reset.
    *
    * @param param
    */
   public void _onResetComplete(Future<Void> param);

   /**
    * Acquire a lease on an instance.
    *
    * @return
    */
   FutureWrapper<LeaseModel> acquire();

   /**
    * Delete the workpool and all the backing VMs from its instances.
    *
    * @return
    */
   Future<Void> delete();

   /**
    * Private.
    *
    * Delete a specific instance.
    *
    * @param instance
    */
   void deleteInstance(InstanceModel instance);

   /**
    * Delete the workpool and forget about all its instances.
    *
    * @return
    */
   Future<Void> forget();

   /**
    * Get instance id.
    *
    * @return
    */
   Long getId();

   /**
    * Get the underlying workpool data model.
    *
    * @return a full clone of the model
    */
   WorkpoolModel getWorkpoolModel();

   /**
    * Return a VM lease
    *
    * @param lease
    * @return future for use when sync semantics are desired
    */
   Future<Void> release(LeaseModel lease);

   /**
    * Receive state notification updates.
    *
    * @param observer
    */
   void subscribe(Object observer);

   /**
    * Stop receiving state notification updates.
    *
    * @param observer
    */
   void unsubscribe(Object observer);

   /**
    * Private.
    *
    * @param change
    */
   @Subscribe
   void update(InstancerStateChange change);

   Future<Void> updateModel(WorkpoolModel model);

   /**
    * Reset the workpool: delete all leases and attempt to shut down all VMs.
    */
   Future<Void> reset();

   /**
    * Get an instance.
    *
    * @param instanceId
    * @return
    */
   InstanceModel getInstance(long instanceId);

   /**
    * Add an existing instance to the workpool.
    *
    * @param instance
    */
   Future<InstanceModel> addInstance(InstanceModel instance);

   /**
    * Remove an instance from the workpool.
    *
    * @param instanceId
    */
   Future<Void> removeInstance(long instanceId);
}
