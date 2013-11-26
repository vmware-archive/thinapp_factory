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

import com.google.common.collect.ImmutableSet;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;

public class ImportProjectStateBuilderTest extends BuilderTesting<
      ImportProjectStateBuilder,
      ImportProjectState,
      ImportProjectStateImpl,
      ImportProjectState.ImportProjectStatus> {

   @Override
   protected ImportProjectStateBuilder createBuilder() {
      return new ImportProjectStateBuilder();
   }

   @Override
   protected ImportProjectStateBuilder populateBuilder(ImportProjectStateBuilder builder) {
      populateTaskStateFields(builder, ImportProjectState.ImportProjectStatus.CANCELLED);

      builder.withFailedProjectIds(ImmutableSet.of(1L,2L,3L,4L,5L));
      builder.withImportedProjectIds(ImmutableSet.of(6L,7L,8L,9L,10L,/*...*/11L,12L/* do do do dee do*/));

      return builder;
   }

   @Override
   protected ImportProjectStateImpl createImplementation() {
      return new ImportProjectStateImpl();
   }

   @Override
   protected ImportProjectStateImpl populateImplementation(ImportProjectStateImpl impl) {
      populateImpl(impl, ImportProjectState.TYPE, ImportProjectState.ImportProjectStatus.CANCELLED);

      impl.setFailedProjectIds(ImmutableSet.of(1L,2L,3L,4L,5L));
      impl.setImportedProjectIds(ImmutableSet.of(6L,7L,8L,9L,10L,/*...*/11L,12L/* do do do dee do*/));

      return impl;
   }

   @Override
   @Test
   public void testEquals() {
      super.testEquals();
   }

   @Test
   public void testInvalidObject() {
      super.testInvalidObject(ImportProjectState.ImportProjectStatus.CANCELLED);
   }

   @Override
   @Test
   public void testRoundTripThroughJson() throws IOException {
      super.testRoundTripThroughJson();
   }
}
