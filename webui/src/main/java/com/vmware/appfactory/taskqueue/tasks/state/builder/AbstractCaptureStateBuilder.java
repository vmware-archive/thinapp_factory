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

package com.vmware.appfactory.taskqueue.tasks.state.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;

@SuppressWarnings("unchecked")
public abstract class AbstractCaptureStateBuilder<
      API extends AbstractCaptureState<API, Builder, StatusEnum>,
      Builder extends AbstractCaptureStateBuilder<API, Builder, StatusEnum>,
      StatusEnum extends Enum<StatusEnum>>
   extends TaskStateBuilder<API, Builder, StatusEnum> {

   public AbstractCaptureStateBuilder(Class<? extends API> resultClass, String type) {
      super(resultClass, type);
   }

   public Builder withConverterId(long ticketId) {
      addChange("converterId", ticketId);
      return (Builder)this;
   }

   public Builder withCaptureRequest(@Nonnull CaptureRequest captureRequest) {
      addChange("captureRequest", captureRequest);
      return (Builder)this;
   }

   public Builder withBuildId(@Nullable Long buildId) {
      addChange("buildId", buildId);
      return (Builder)this;
   }

   public Builder withProjectId(@Nullable Long projectId) {
      addChange("projectId", projectId);
      return (Builder)this;
   }

   @SuppressWarnings("ConstantConditions")
   @Override
   public API build() {
      API result = super.build();

      // verify basic fields of CaptureRequest
      Preconditions.checkNotNull(
            result.getCaptureRequest().getWorkpoolId(),
            "Workpool must be supplied"
      );
      Preconditions.checkNotNull(
            result.getCaptureRequest().getDatastoreId(),
            "DataStore must be supplied"
      );
      Preconditions.checkNotNull(
            result.getCaptureRequest().getRuntimeId(),
            "ThinApp runtime version must be supplied"
      );
      Preconditions.checkNotNull(
            result.getCaptureRequest().getApplicationId(),
            "Application ID to capture must be supplied"
      );
      Preconditions.checkArgument(
            !Strings.isNullOrEmpty(result.getCaptureRequest().getBuildName()),
            "A non-empty build name for must be supplied"
      );
      Preconditions.checkArgument(
            !Strings.isNullOrEmpty(result.getCaptureRequest().getDisplayName()),
            "A non-empty display name for the capture operation must be supplied"
      );

      return result;
   }
}
