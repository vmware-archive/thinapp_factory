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

import com.vmware.appfactory.taskqueue.tasks.state.FeedConvertState;


public class FeedConvertStateBuilderTest extends BuilderTesting<
      FeedConvertState.Builder,
      FeedConvertState,FeedConvertState.Impl,
      FeedConvertState.FeedConvertStatus> {

   @Override
   public FeedConvertState.Builder populateBuilder(FeedConvertState.Builder builder) {
      populateTaskStateFields(builder, FeedConvertState.FeedConvertStatus.comparing);
      builder.withMaxConverisonAttempts(1);
      builder.withConverisonWorkpoolId(2L);
      builder.withConversionDatastoreId(3L);
      builder.withConversionRuntimeId(1L);
      return builder;
   }

   @Override
   protected FeedConvertState.Impl populateImplementation(FeedConvertState.Impl impl) {
      populateImpl(impl, FeedConvertState.TYPE, FeedConvertState.FeedConvertStatus.comparing);
      impl.setConversionWorkpoolId(2L);
      impl.setConversionDatastoreId(3L);
      impl.setConversionRuntimeId(1L);
      impl.setMaxConversionAttempts(1);
      return impl;
   }

   @Override
   public FeedConvertState.Builder createBuilder() {
      return new FeedConvertState.Builder();
   }

   @Override
   protected FeedConvertState.Impl createImplementation() {
      return new FeedConvertState.Impl();
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(FeedConvertState.FeedConvertStatus.queueing);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }

   @Override
   protected FeedConvertState makeMinimallyValidObject(FeedConvertState.FeedConvertStatus status) {
      return createBuilder()
            .withNewId(idSupplier)
            .withDescription("desc")
            .withStatus(status)
            .withRecordId(100L)
            .withMaxConverisonAttempts(1)
            .withConversionRuntimeId(1L)
            .withConverisonWorkpoolId(2L)
            .withConversionDatastoreId(3L)
            .build();
   }
}
