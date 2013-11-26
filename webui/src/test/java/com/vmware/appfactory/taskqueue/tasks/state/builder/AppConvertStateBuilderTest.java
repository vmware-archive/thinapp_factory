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

import java.io.IOException;

import org.junit.Test;

import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState.AppConvertStatus;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.Status;

public class AppConvertStateBuilderTest extends BuilderTesting<
      AppConvertStateBuilder,
      AppConvertState,
      AppConvertStateImpl,
      AppConvertState.AppConvertStatus> {

   @Override
   protected AppConvertStateBuilder createBuilder() {
      return new AppConvertStateBuilder();
   }

   @Override
   protected AppConvertStateBuilder populateBuilder(AppConvertStateBuilder builder) {
      populateTaskStateFields(builder, AppConvertState.AppConvertStatus.complete);
      populateCaptureStateFields(builder);
      builder.withLastCommand(new Command());
      builder.withLastError("lastError");
      builder.withLastRunningState(Status.State.acquiringVm);
      builder.withIsStalled(true);
      return builder;
   }

   @Override
   protected AppConvertStateImpl createImplementation() {
      return new AppConvertStateImpl();
   }

   @Override
   protected AppConvertStateImpl populateImplementation(AppConvertStateImpl impl) {
      populateImpl(impl, AppConvertState.TYPE, AppConvertState.AppConvertStatus.complete);
      populateCaptureStateImpl(impl);
      impl.setLastCommand(new Command());
      impl.setLastError("lastError");
      impl.setLastRunningState(Status.State.acquiringVm);
      impl.setIsStalled(true);
      return impl;
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(AppConvertStatus.build);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }

   @Override
   protected AppConvertState makeMinimallyValidObject(AppConvertState.AppConvertStatus defaultStatusValue) {
      return createBuilder()
            .withNewId(idSupplier)
            .withDescription("desc")
            .withStatus(defaultStatusValue)
            .withCaptureRequest(newCaptureRequest())
            .withRecordId(100L)
            .build();
   }
}
