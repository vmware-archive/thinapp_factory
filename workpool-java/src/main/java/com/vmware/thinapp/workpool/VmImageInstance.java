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

import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.vim25.mo.ServiceInstance;

import akka.dispatch.Future;

public interface VmImageInstance {
   /**
    * Private.
    */
   void _onBackingVmDeletionFinished();

   /**
    * Private.
    */
   void _onCreateCloningSnapshotFinished(Future<Void> param);

   /**
    * Private.
    */
   void _onInstallationFinished(Future<InstallRunner.Result> param, ServiceInstance si);

   /**
    * Deletes the instance along with its backing VM.
    *
    * @return Future that is set once the instance goes into the deleting status
    * but before the VM is likely to have been deleted
    */
   Future<Void> delete();

   /**
    * Deletes the instance but keeps the backing VM.
    *
    * @return Future that is set once the instance is deleted
    */
   Future<Void> forget();

   /**
    * Get the instance id.
    *
    * @return
    */
   long getId();

   /**
    * Retrieves the instance's data.
    *
    * @return a full clone
    */
   VmImageModel getVmImage();

   /**
    * Subscribes to receive state notifications from this actor.
    * <p/>
    * When subscribing, the subscriber will immediately get a notification of the current state to ensure that the
    * initial state is known and that all state transitions are seen.
    * <p/>
    * WARNING: Caller is responsible for also unsubscribing to avoid leaking memory.
    *
    * @param observer
    */
   void subscribe(Object observer);

   /**
    * Unsubscribe to state notifications from this actor.
    *
    * @param observer
    */
   void unsubscribe(Object observer);
}
