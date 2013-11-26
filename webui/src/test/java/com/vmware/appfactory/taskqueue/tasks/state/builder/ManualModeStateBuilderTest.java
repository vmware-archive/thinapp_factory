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

import com.vmware.appfactory.taskqueue.tasks.state.ManualModeState;


public class ManualModeStateBuilderTest extends BuilderTesting<
      ManualModeState.Builder,
      ManualModeState,ManualModeState.Impl,
      ManualModeState.ManualModeStatus> {

   @Override
   public ManualModeState.Builder populateBuilder(ManualModeState.Builder builder) {
      populateTaskStateFields(builder, ManualModeState.ManualModeStatus.ACQUIRING_VM);
      populateCaptureStateFields(builder);
      return builder;
   }

   @Override
   protected ManualModeState.Impl populateImplementation(ManualModeState.Impl impl) {
      populateImpl(impl, ManualModeState.TYPE, ManualModeState.ManualModeStatus.ACQUIRING_VM);
      populateCaptureStateImpl(impl);
      return impl;
   }

   @Override
   public ManualModeState.Builder createBuilder() {
      return new ManualModeState.Builder();
   }

   @Override
   protected ManualModeState.Impl createImplementation() {
      return new ManualModeState.Impl();
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(ManualModeState.ManualModeStatus.BUILDING);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }

   @Override
   protected ManualModeState makeMinimallyValidObject(ManualModeState.ManualModeStatus defaultStatusValue) {
      return createBuilder()
            .withNewId(idSupplier)
            .withDescription("desc")
            .withStatus(defaultStatusValue)
            .withCaptureRequest(newCaptureRequest())
            .withRecordId(100L)
            .build();
   }
}
