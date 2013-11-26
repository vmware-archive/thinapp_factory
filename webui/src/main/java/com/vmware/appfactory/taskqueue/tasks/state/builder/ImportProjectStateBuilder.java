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

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;

public class ImportProjectStateBuilder
      extends TaskStateBuilder<
      ImportProjectState,
      ImportProjectStateBuilder,
      ImportProjectState.ImportProjectStatus> {

   public ImportProjectStateBuilder() {
      super(ImportProjectStateImpl.class, ImportProjectState.TYPE);
   }

   public ImportProjectStateBuilder withImportedProjectIds(@Nonnull Set<Long> ids) {
      addChange("importedProjectIds",ids);
      return this;
   }

   public ImportProjectStateBuilder withFailedProjectIds(@Nonnull Set<Long> ids) {
      addChange("failedProjectIds",ids);
      return this;
   }

   @Override
   public ImportProjectStateBuilder withOriginal(@Nonnull ImportProjectState original) {
      addChanges(original);
      return this;
   }

   public ImportProjectStateBuilder withLastError(@Nullable String lastError) {
      addChange("lastError",lastError);
      return this;
   }

   public ImportProjectStateBuilder withNumFound(int numFound) {
      addChange("numFound",numFound);
      return this;
   }

   public ImportProjectStateBuilder withNumImported(int numImported) {
      addChange("numImported",numImported);
      return this;
   }

   public ImportProjectStateBuilder withErrors(@Nullable List<String> errors) {
      addChange("errors",errors);
      return this;
   }
}
