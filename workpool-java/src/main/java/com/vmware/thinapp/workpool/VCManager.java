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

import akka.dispatch.Future;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.thinapp.common.workpool.dto.VCConfig.ApiType;

/**
 * Manages VC connection.
 */
public interface VCManager {
   FutureWrapper<ServiceInstance> getConnection();
   void connect();
   void connectDone(Future<ServiceInstance> result);
   void refresh();

   void update(VCConfigModel vcConfig);

   VCConfigModel getConfig();

   /**
    * Determine if cloning is supported on the currently configured ComputeResource.
    *
    * @return
    */
   boolean isCloningSupported();

   /**
    * Determine the API type (VirtualCenter vs HostAgent) supported by the currently VI service
    *
    * @return
    */
   ApiType getApiType();
}
