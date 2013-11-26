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

import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.thinapp.common.converter.dto.Command;

public class AppConvertStateBuilder
   extends AbstractCaptureStateBuilder<
      AppConvertState,
      AppConvertStateBuilder,
      AppConvertState.AppConvertStatus>
{
   public AppConvertStateBuilder() {
      super(AppConvertStateImpl.class, AppConvertState.TYPE);
   }

   public AppConvertStateBuilder withLastRunningState(@Nullable com.vmware.thinapp.common.converter.dto.Status.State lastRunningState) {
      addChange("lastRunningState",lastRunningState);
      return this;
   }

   public AppConvertStateBuilder withLastCommand(@Nullable Command lastCommand) {
      addChange("lastCommand",lastCommand);
      return this;
   }

   public AppConvertStateBuilder withLastError(@Nullable String lastError) {
      addChange("lastError",lastError);
      return this;
   }

   public AppConvertStateBuilder withIsStalled(boolean isStalled) {
      addChange("isStalled", isStalled);
      return this;
   }

   @Override
   public AppConvertStateBuilder withOriginal(@Nonnull AppConvertState original) {
      addChanges(original);
      return this;
   }
}
