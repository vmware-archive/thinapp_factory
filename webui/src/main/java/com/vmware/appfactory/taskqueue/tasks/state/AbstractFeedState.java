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

package com.vmware.appfactory.taskqueue.tasks.state;

import com.vmware.appfactory.taskqueue.tasks.state.builder.AbstractFeedStateBuilder;

/**
 * A snapshot of the status of a feed-related task, such as FeedConvertTask
 * and FeedScanTask.
 */
public interface AbstractFeedState
      <Api extends AbstractFeedState<Api, Builder, StatusEnum>,
      Builder extends AbstractFeedStateBuilder<Api, Builder, StatusEnum>,
      StatusEnum extends Enum<StatusEnum>>
      extends TaskState<Api,Builder,StatusEnum> {

   /**
    * Number of times to attempt to convert each app that is auto-converted
    * from the feed.  Must be at least 1.
    * @return
    */
   int getMaxConversionAttempts();

   /**
    * @return Id of the workpool to use for automated conversions.
    */
   long getConversionWorkpoolId();

   /**
    * @return Id of the datastore to use for automated conversions.
    */
   long getConversionDatastoreId();

   /**
    * @return Id of the ThinApp runtime to use for automated conversions.
    */
   long getConversionRuntimeId();
}
