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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.taskqueue.tasks.state.AbstractFeedState;

public abstract class AbstractFeedStateImpl
      <API extends AbstractFeedState<API, Builder, StatusEnum>,
            Builder extends AbstractFeedStateBuilder<API, Builder, StatusEnum>,
            StatusEnum extends Enum<StatusEnum>>
      extends TaskStateImpl<API, Builder, StatusEnum>
      implements AbstractFeedState<API, Builder, StatusEnum> {

   private int _maxConversionAttempts;
   private long _conversionRuntimeId;
   private long _conversionWorkpoolId;
   private long _conversionDatastoreId;

   @Override
   public int getMaxConversionAttempts() {
      return _maxConversionAttempts;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setMaxConversionAttempts(int conversionRetries) {
      this._maxConversionAttempts = conversionRetries;
   }

   @Override
   public long getConversionWorkpoolId() {
      return _conversionWorkpoolId;
   }

   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setConversionWorkpoolId(long conversionWorkpoolId) {
      this._conversionWorkpoolId = conversionWorkpoolId;
   }

   @Override
   public long getConversionDatastoreId() {
      return _conversionDatastoreId;
   }

   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setConversionDatastoreId(long conversionDatastoreId) {
      this._conversionDatastoreId = conversionDatastoreId;
   }

   @Override
   public long getConversionRuntimeId() {
      return _conversionRuntimeId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setConversionRuntimeId(long conversionRuntimeId) {
      this._conversionRuntimeId = conversionRuntimeId;
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
}
