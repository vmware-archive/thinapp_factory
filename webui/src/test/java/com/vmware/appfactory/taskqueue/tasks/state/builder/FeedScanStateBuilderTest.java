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

import com.vmware.appfactory.taskqueue.tasks.state.FeedScanState;


public class FeedScanStateBuilderTest extends BuilderTesting<
      FeedScanState.Builder,
      FeedScanState,FeedScanState.Impl,
      FeedScanState.FeedScanStatus> {

   @Override
   public FeedScanState.Builder populateBuilder(FeedScanState.Builder builder) {
      populateTaskStateFields(builder, FeedScanState.FeedScanStatus.scanning);
      builder.withConverisonWorkpoolId(2L);
      builder.withConversionDatastoreId(3L);
      builder.withMaxConverisonAttempts(1);
      builder.withConversionRuntimeId(1L);
      return builder;
   }

   @Override
   protected FeedScanState.Impl populateImplementation(FeedScanState.Impl impl) {
      populateImpl(impl, FeedScanState.TYPE, FeedScanState.FeedScanStatus.scanning);
      impl.setConversionWorkpoolId(2L);
      impl.setConversionDatastoreId(3L);
      impl.setConversionRuntimeId(1L);
      impl.setMaxConversionAttempts(1);
      return impl;
   }

   @Override
   public FeedScanState.Builder createBuilder() {
      return new FeedScanState.Builder();
   }

   @Override
   protected FeedScanState.Impl createImplementation() {
      return new FeedScanState.Impl();
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(FeedScanState.FeedScanStatus.cancelled);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }

   @Override
   protected FeedScanState makeMinimallyValidObject(FeedScanState.FeedScanStatus status) {
      return createBuilder()
            .withNewId(idSupplier)
            .withDescription("desc")
            .withStatus(status)
            .withRecordId(100L)
            .withMaxConverisonAttempts(1)
            .withConverisonWorkpoolId(2L)
            .withConversionDatastoreId(3L)
            .withConversionRuntimeId(1L)
            .build();
   }
}
