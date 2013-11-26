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

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.tasks.state.builder.ImportProjectStateBuilder;

public interface ImportProjectState
   extends TaskState<
      ImportProjectState,
      ImportProjectStateBuilder,
      ImportProjectState.ImportProjectStatus> {

   public static final String TYPE = "IMPORT_PROJECTS";

   /**
    * Import status values.
    */
   public enum ImportProjectStatus {
      NEW,
      CREATING_PROJECTS, /* Progress - 5% */
      REFRESHING_PROJECTS, /* Progress - 75% */
      SAVING_PROJECTS, /* Progress - 20% */
      FAILED,
      COMPLETE,
      CANCELLING,
      CANCELLED
   }

   /**
    * @return
    * A list of project IDs which have been succesfully imported.
    */
   @Nonnull
   Set<Long> getImportedProjectIds();

   /**
    * @return
    * A list of project IDs which could not be imported.
    */
   @Nonnull
   Set<Long> getFailedProjectIds();

   /**
    * Only set on some failures.
    *
    * @return When set, contains an error message
    * from the backend CWS about why the error occurred.
    */
   @Nullable
   String getLastError();

   /**
    * @return the numFound
    */
   int getNumFound();

   /**
    * @return the numImported
    */
   int getNumImported();

   List<String> getErrors();
}
