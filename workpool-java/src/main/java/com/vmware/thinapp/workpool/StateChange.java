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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Class to describe a state transition.
 *
 * @param <T> the state type
 * @param <V> the sender type
 */
public class StateChange<T, V> {
   private T newState;
   private T previousState;
   private V sender;

   /**
    * Constructor
    * @param previousState the object's old state
    * @param newState the object's new state
    * @param sender the sender generating the event
    */
   public StateChange(T previousState, T newState, V sender) {
      this.previousState = previousState;
      this.newState = newState;
      this.sender = sender;
   }

   public T getPreviousState() {
      return previousState;
   }

   public T getNewState() {
      return newState;
   }

   public V getSender() {
      return sender;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
