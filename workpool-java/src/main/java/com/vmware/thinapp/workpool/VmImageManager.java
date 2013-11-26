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

import akka.dispatch.Future;
import com.google.common.eventbus.Subscribe;
import scala.Option;

import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.workpool.model.VmImageModel;

public interface VmImageManager {
   VmImageInstance create(VmImageModel vmImage);

   VmImageInstance get(long id);

   /**
    * Deletes the VM image instance.
    *
    * @param id
    * @param deleteMethod indicates if the VM should just be forgotten
    * about or actually deleted from vSphere
    * @return future with no value to be used when synchronous behavior is needed
    */
   Future<Void> delete(long id, DeleteMethod deleteMethod);

   /**
    * Retrieve a VmImageInstance with the given name.
    *
    * @param name
    * @return
    */
   Option<VmImageInstance> findByName(String name);

   Collection<VmImageInstance> list();

   @Subscribe
   void update(VmImageStateChange arg);
}
