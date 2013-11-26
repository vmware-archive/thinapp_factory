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
import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.tasks.state.builder.TaskStateBuilder;

/**
 * A snapshot of the status of a capture task, such as an automatic conversion
 * (AppConvertTask) or a manual mode capture (ManualModeTask).
 */
public interface AbstractCaptureState
      <Api extends TaskState<Api, Builder, StatusEnum>,
       Builder extends TaskStateBuilder<Api, Builder, StatusEnum>,
       StatusEnum extends Enum<StatusEnum>>
   extends TaskState<Api,Builder,StatusEnum> {

   /**
    * @return the settings the user supplied when requesting the conversion.
    */
   @Nonnull
   CaptureRequest getCaptureRequest();

   /**
    * @return CWS ticket id
    */
   long getConverterId();

   /**
    * @return local database ID of the local build object, if one is created.
    */
   @Nullable
   Long getBuildId();

   /**
    * @return buildRequestId thats stored once the build is created.
    */
   @Nullable
   Long getBuildRequestId();

   /**
    * @return buildRequestId thats stored once the build is created.
    */
   @Nullable
   void setBuildRequestId(Long buildRequestId);

   /**
    * @return
    * ID of the thinApp project that will be created when this build
    * is complete.  This project will only be valid if the build succeeds.
    */
   @Nullable
   Long getProjectId();
}
