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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.thinapp.common.converter.dto.Command;

class AppConvertStateImpl
   extends AbstractCaptureStateImpl<AppConvertState,AppConvertStateBuilder,AppConvertState.AppConvertStatus>
      implements AppConvertState
{
   private com.vmware.thinapp.common.converter.dto.Status.State _lastRunningState;

   private Command _lastCommand;

   private String _lastError;

   private boolean _isStalled;

   protected AppConvertStateImpl() {
      // empty constructor
   }

   @Nullable
   @Override
   public com.vmware.thinapp.common.converter.dto.Status.State getLastRunningState() {
      return _lastRunningState;
   }

   @Nullable
   @Override
   public Command getLastCommand() {
      return _lastCommand;
   }

   @Nullable
   @Override
   public String getLastError() {
      return _lastError;
   }

   @Override
   public boolean getIsStalled() {
      return _isStalled;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setLastRunningState(@Nullable com.vmware.thinapp.common.converter.dto.Status.State lastRunningState) {
      this._lastRunningState = lastRunningState;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setLastCommand(@Nullable Command lastCommand) {
      this._lastCommand = lastCommand;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setLastError(@Nullable String lastError) {
      this._lastError = lastError;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setIsStalled(boolean isStalled) {
      this._isStalled = isStalled;
   }

   @Nonnull
   @Override
   public AppConvertStateBuilder newBuilderForThis() {
      return new AppConvertStateBuilder().withOriginal(this);
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

