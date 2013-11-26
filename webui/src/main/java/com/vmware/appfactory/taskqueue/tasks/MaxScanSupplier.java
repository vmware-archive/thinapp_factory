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
import com.vmware.appfactory.feed.dao.FeedDao;

/**
 * Calculates the max number of concurrent scan tasks (feed and fileshare)
 * that can run at once.
 */
class MaxScanSupplier implements Supplier<Integer> {

   @Resource
   ConfigRegistry _config;

   @Resource
   private FeedDao _feedDao;

   @Override
   public Integer get() {
      /* First, get configuration setting */
      int maxRunning = _config.getInteger(ConfigRegistryConstants.TASKQ_MAX_CONCURRENT_SCANS);

      if (maxRunning < 0) {
         try {
            maxRunning = (int)_feedDao.countAll();
         } catch (Exception ex) {
            /* Database fail; allow at least one task */
            maxRunning = 1;
         }
      }
      if (maxRunning < 0) {
         maxRunning = 1;
      }

      return maxRunning;
   }
}
