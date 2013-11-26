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

import com.vmware.thinapp.workpool.model.InstanceModel;

public class NullInstancer extends InstancerBase implements Instancer {
   public NullInstancer() {
      setState(State.available);
   }

   @Override
   public FutureWrapper<CloneRunner.Result> addInstance(InstanceModel instance) {
      throw new RuntimeException("Null instancer cannot instantiate new instances.");
   }

   @Override
   public void update(StateChange<State, State> stateChange) {
      // Needed to satisfy interface but unused for this implementation.
   }
}
