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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.collect.ImmutableSet;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;

class ImportProjectStateImpl
   extends TaskStateImpl<
      ImportProjectState,
      ImportProjectStateBuilder,
      ImportProjectState.ImportProjectStatus>
   implements ImportProjectState {

   private Set<Long> _importedProjectIds = Collections.emptySet();
   private Set<Long> _failedProjectIds = Collections.emptySet();

   private int _numFound = 0;

   private int _numImported = 0;

   private String _lastError;

   private List<String> _errors = Collections.emptyList();

   @Nonnull
   @Override
   public Set<Long> getImportedProjectIds() {
      return _importedProjectIds;
   }

   @Nonnull
   @Override
   public Set<Long> getFailedProjectIds() {
      return _failedProjectIds;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface", "TypeMayBeWeakened"})
   public void setImportedProjectIds(@Nonnull Set<Long> importedProjectIds) {
      this._importedProjectIds = ImmutableSet.copyOf(importedProjectIds);
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface", "TypeMayBeWeakened"})
   public void setFailedProjectIds(@Nonnull Set<Long> failedProjectIds) {
      this._failedProjectIds = ImmutableSet.copyOf(failedProjectIds);
   }

   @Nonnull
   @Override
   public ImportProjectStateBuilder newBuilderForThis() {
      return new ImportProjectStateBuilder().withOriginal(this);
   }


   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }

   /* (non-Javadoc)
    * @see com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState#getLastError()
    */
   @Nullable
   @Override
   public String getLastError() {
      return _lastError;
   }

   public void setLastError(String lastError) {
      _lastError = lastError;
   }

   /**
    * @return the numFound
    */
   @Override
   public int getNumFound() {
      return _numFound;
   }

   /**
    * @return the numImported
    */
   @Override
   public int getNumImported() {
      return _numImported;
   }

   /**
    * @param numFound the numFound to set
    */
   public void setNumFound(int numFound) {
      _numFound = numFound;
   }

   /**
    * @param numImported the numImported to set
    */
   public void setNumImported(int numImported) {
      _numImported = numImported;
   }

   /**
    * @return the errors
    */
   @Override
   public List<String> getErrors() {
      return _errors;
   }

   /**
    * @param errors the errors to set
    */
   public void setErrors(List<String> errors) {
      _errors = errors;
   }
}
