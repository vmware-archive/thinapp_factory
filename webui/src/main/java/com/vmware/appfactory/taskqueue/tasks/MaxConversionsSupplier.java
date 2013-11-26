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

import javax.annotation.Resource;

import com.google.common.base.Supplier;
import com.vmware.appfactory.config.ConfigRegistry;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * Calculates the max number of concurrent conversion tasks that can run
 * at once.
 */
class MaxConversionsSupplier implements Supplier<Integer> {

   @Resource
   ConfigRegistry _config;

   @Resource
   WorkpoolClientService _wpClient;

   @Override
   public Integer get() {
      /* First, get configuration setting */
      int maxRunning = _config.getInteger(ConfigRegistryConstants.TASKQ_MAX_CONCURRENT);

      if (maxRunning < 0) {
         maxRunning = 0;

         try {
            for (Workpool wp : _wpClient.getAllWorkpools()) {
               maxRunning += wp.getMaximum();
            }
         } catch (WpException ex) {
            /* Workpool service fail; assume at least one image */
            maxRunning = 1;
         }
      }

      // TODO: this behavior is less than ideal when we have multiple
      // workpools.  For more info, see bug 791959 comment 3.

      return maxRunning;
   }
}
