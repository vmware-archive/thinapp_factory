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

import com.vmware.appfactory.taskqueue.tasks.state.RebuildState;


public class RebuildStateBuilderTest extends BuilderTesting<
      RebuildState.Builder,
      RebuildState,RebuildState.Impl,
      RebuildState.RebuildStatus> {

   @Override
   public RebuildState.Builder populateBuilder(RebuildState.Builder builder) {
      populateTaskStateFields(builder, RebuildState.RebuildStatus.REBUILDING);
      return builder;
   }

   @Override
   protected RebuildState.Impl populateImplementation(RebuildState.Impl impl) {
      populateImpl(impl, RebuildState.TYPE, RebuildState.RebuildStatus.REBUILDING);
      return impl;
   }

   @Override
   public RebuildState.Builder createBuilder() {
      return new RebuildState.Builder();
   }

   @Override
   protected RebuildState.Impl createImplementation() {
      return new RebuildState.Impl();
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(RebuildState.RebuildStatus.NEW);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }
}
