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

import javax.annotation.Nonnull;

import com.vmware.appfactory.taskqueue.tasks.state.builder.AbstractFeedStateBuilder;
import com.vmware.appfactory.taskqueue.tasks.state.builder.AbstractFeedStateImpl;

/**
 * A snapshot of the status of a FeedScanTask.
 */
public interface FeedScanState
      extends AbstractFeedState<FeedScanState,
                                FeedScanState.Builder,
                                FeedScanState.FeedScanStatus> {

   public final String TYPE = "FEED_SCAN";

   enum FeedScanStatus {
      waiting,
      scanning,
      complete,
      failed,
      cancelled
   }

   public class Builder
         extends AbstractFeedStateBuilder<FeedScanState,Builder,FeedScanStatus> {

      public Builder() {
         super(Impl.class, TYPE);
      }
   }

   public class Impl
         extends AbstractFeedStateImpl<FeedScanState,Builder,FeedScanStatus>
         implements FeedScanState {
      @Nonnull
      @Override
      public Builder newBuilderForThis() {
         return new Builder().withOriginal(this);
      }
   }
}
