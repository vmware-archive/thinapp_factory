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

/**
 * Strategy for instantiating a VM.
 */
public interface Instancer {
   /**
    * Add a new instance.
    *
    * @param instance data model of instance to add
    * @return instancing result
    */
   FutureWrapper<CloneRunner.Result> addInstance(InstanceModel instance);

   /**
    * Callback for VmImage state changes.
    *
    * @param stateChange
    */
   @Subscribe
   void update(StateChange<Instancer.State, Instancer.State> stateChange);

   /**
    * Subscribe to observable events.
    *
    * @param observer client to notify
    */
   void subscribe(Object observer);

   /**
    * Unsubscribe to observable events.
    *
    * @param observer client to unsubscribe
    */
   void unsubscribe(Object observer);

   /**
    * Possible instancer states.
    */
   public enum State {
      available,
      unavailable,
      failed
   }
}
