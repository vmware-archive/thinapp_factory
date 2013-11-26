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

import akka.actor.TypedActor;
import com.google.common.eventbus.EventBus;

/**
 * Common functionality for instancers.
 */
public abstract class InstancerBase extends TypedActor {
   protected Instancer self;
   protected EventBus eventBus = new EventBus();
   protected Instancer.State state = Instancer.State.unavailable;

   @Override
   public void preStart() {
      self = getContext().getSelfAs();
   }

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
   public void subscribe(Object observer) {
      eventBus.register(observer);
      // Sends current state directly to listener when subscribing to sync up.
      eventBus.post(new InstancerStateChange(state, state, self));
   }

   public void unsubscribe(Object observer) {
      eventBus.unregister(observer);
   }

   protected void setState(Instancer.State state) {
      InstancerStateChange change = new InstancerStateChange(this.state, state, self);
      this.state = state;
      eventBus.post(change);
   }
}
