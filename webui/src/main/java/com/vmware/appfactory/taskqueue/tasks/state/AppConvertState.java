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

import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.tasks.state.builder.AppConvertStateBuilder;
import com.vmware.thinapp.common.converter.dto.Command;

/**
 * A snapshot of the status of an AppConvertTask.
 */
public interface AppConvertState
   extends AbstractCaptureState<AppConvertState, AppConvertStateBuilder, AppConvertState.AppConvertStatus> {

   String TYPE = "CONVERSION";

   /**
    * Conversion status values.
    * We have more states than CWS so we convert CWS states into these, plus
    * use some extra ones.
    */
   enum AppConvertStatus {
      newtask,
      queued,
      created,
      downloading,
      provisioning,
      precapture,
      preinstall,
      install,
      postinstall,
      postcapture,
      generate,
      prebuild,
      build,
      refresh,
      finishing,
      complete,
      failed,
      cancelling,
      cancelled
   }

   /**
    * Only set on some failures.
    *
    * @return the stage in the CWS processing at which the error occurred.
    */
   @Nullable
   com.vmware.thinapp.common.converter.dto.Status.State getLastRunningState();

   /**
    * Only set on some failures.
    *
    * @return the recipe command that was running when the failure occurred.
    */
   @Nullable
   Command getLastCommand();

   /**
    * Only set on some failures.
    *
    * @return When set, contains an error message
    * from the backend CWS about why the error occurred.
    */
   @Nullable
   String getLastError();

   /**
    * @return true when the conversion is stalled, false otherwise
    */
   boolean getIsStalled();
}
