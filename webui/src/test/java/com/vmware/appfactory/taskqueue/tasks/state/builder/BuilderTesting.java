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
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

import static org.junit.Assert.*;

abstract class BuilderTesting
      <Builder extends TaskStateBuilder<Api,Builder,StatusEnum>,
            Api extends TaskState<Api,Builder,StatusEnum>,
            Implementation extends TaskStateImpl<Api,Builder,StatusEnum>,
            StatusEnum extends Enum<StatusEnum>> {

   private static final Pattern OBJ_ID_PATTERN = Pattern.compile("@([0-9a-f]+)\\[");
   protected static final Supplier<Long> idSupplier = Suppliers.ofInstance(123456L);

   protected abstract Builder createBuilder();
   protected abstract Builder populateBuilder(Builder builder);

   protected abstract Implementation createImplementation();
   protected abstract Implementation populateImplementation(Implementation network);

   public void testEquals() {
      Builder builder = createBuilder();
      populateBuilder(builder);

      Implementation impl = createImplementation();
      populateImplementation(impl);

      Api builtObject = builder.build();
      assertEquals(impl, builtObject);
      assertEquals(impl.hashCode(), builtObject.hashCode());
      String s = impl.toString();
      String s1 = builtObject.toString();
      assertEquals(stripObjectId(s), stripObjectId(s1));

      // assure that when we rebuild an object using
      // newBuilderForThis that all of the properties
      // are captured
      {
         Api rebuiltObject = builtObject.newBuilderForThis().build();
         assertEquals(impl, rebuiltObject);
         assertEquals(builtObject, rebuiltObject);
      }
   }

   public void testInvalidObject(StatusEnum defaultStatusValue) {

      try {
         createBuilder().build();
         fail("did not require id");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      try {
         createBuilder().withNewId(idSupplier).build();
         fail("did not require description");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      try {
         createBuilder().withNewId(idSupplier).withDescription("desc").build();
         fail("did not require task status");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      try {
         createBuilder().withNewId(idSupplier).withDescription("desc").withStatus(defaultStatusValue).build();
         fail("did not require record");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      Api minimallyValidObject = makeMinimallyValidObject(defaultStatusValue);
      assertNotNull(minimallyValidObject);
      assertNotNull(minimallyValidObject.getId());
      assertTrue(-1 == minimallyValidObject.getProgress());
      assertTrue(TaskQueue.MetaStatus.INIT == minimallyValidObject.getQueueStatus());
      assertTrue(defaultStatusValue.equals(minimallyValidObject.getStatus()));
      assertTrue("desc".equals(minimallyValidObject.getDescription()));


      // make sure we can rebuild a minimal object
      createBuilder().withOriginal(minimallyValidObject).build();

      try {
         minimallyValidObject.newBuilderForThis().withProgress(-2).build();
         fail("allowed task percent < -1");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      try {
         minimallyValidObject.newBuilderForThis().withProgress(101).build();
         fail("allowed task percent > 100");
      } catch (IllegalArgumentException ignore) {/* Ignore */}

      try {
         minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.WAITING).build();
         fail("allowed waiting task without queued time");
      } catch (IllegalArgumentException ignore) {/* Ignore */}
      minimallyValidObject = minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.WAITING).withQueued(1L).build();
      // should be valid

      try {
         minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.RUNNING).build();
         fail("allowed running task without started timestamp");
      } catch (IllegalArgumentException ignore) {/* Ignore */}
      minimallyValidObject = minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.RUNNING).withStarted(2L).build();

      try {
         minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.FINISHED).build();
         fail("allowed finished task without finished timestamp");
      } catch (IllegalArgumentException ignore) {/* Ignore */}
//      minimallyValidObject = minimallyValidObject.newBuilderForThis().withQueueStatus(TaskQueue.MetaStatus.FINISHED).withFinished(3L).build();

   }

   public static String stripObjectId(CharSequence in) {
      return OBJ_ID_PATTERN.matcher(in).replaceAll("");
   }

   protected Api makeMinimallyValidObject(StatusEnum defaultStatusValue) {
      return createBuilder()
            .withNewId(idSupplier)
            .withDescription("desc")
            .withStatus(defaultStatusValue)
            .withRecordId(100L)
            .build();
   }


   protected void populateTaskStateFields(Builder builder, StatusEnum defaultStatus) {
      builder.withNewId(idSupplier)
             .withStatus(defaultStatus)
             .withAborted(true)
             .withDescription("This is a description")
             .withFinished(123L)
             .withProgress(12)
             .withQueued(1L)
             .withQueueStatus(TaskQueue.MetaStatus.FINISHED)
             .withStarted(12L)
             .withRecordId(100L)
             .withStatusLink("AppConvertStatus link");
   }

   protected static void populateCaptureStateFields(AbstractCaptureStateBuilder builder) {
      builder.withConverterId(999L);
      builder.withCaptureRequest(newCaptureRequest());
   }

   protected static void populateCaptureStateImpl(AbstractCaptureStateImpl impl) {
      impl.setConverterId(999L);
      impl.setCaptureRequest(newCaptureRequest());
   }

   protected static CaptureRequest newCaptureRequest() {
      // todo: supply a TestTaskHelperFactory when we separate the REST classes from interfaces, and can
      //       then properly mock them up
      return new CaptureRequestImpl(123L, null, "display name", "build name", 1L, 2L, 100L, false, null);
   }


   protected void populateImpl(Implementation impl, String type, StatusEnum status) {
      impl.setId(idSupplier.get());
      impl.setType(type);
      impl.setRecordId(100L);
      impl.setStatus(status);
      impl.setAborted(true);
      impl.setDescription("This is a description");
      impl.setFinished(123L);
      impl.setProgress(12);
      impl.setQueued(1L);
      impl.setQueueStatus(TaskQueue.MetaStatus.FINISHED);
      impl.setStarted(12L);
      impl.setStatusLink("AppConvertStatus link");
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testRoundTripThroughJson() throws IOException {
      JsonFactory factory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(factory);


      Api state = populateBuilder(createBuilder()).build();
      String str = mapper.writeValueAsString(state);

      Implementation newState = mapper.readValue(str, (Class<Implementation>) createImplementation().getClass());

      assertEquals(state, newState);
   }

}
