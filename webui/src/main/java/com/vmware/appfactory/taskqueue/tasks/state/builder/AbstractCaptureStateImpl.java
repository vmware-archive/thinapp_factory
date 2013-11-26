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
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;

public abstract class AbstractCaptureStateImpl
      <API extends AbstractCaptureState<API, Builder, StatusEnum>,
       Builder extends AbstractCaptureStateBuilder<API, Builder, StatusEnum>,
       StatusEnum extends Enum<StatusEnum>>
      extends TaskStateImpl<API, Builder, StatusEnum>
      implements AbstractCaptureState<API, Builder, StatusEnum> {

   // Use to store the AppBuildRequestId.
   private Long _buildRequestId;
   private long _converterId;
   private Long _buildId;
   private CaptureRequest _captureRequest;
   private Long _projectId;

   @Override
   public long getConverterId() {
      return _converterId;
   }

   @Override
   @Nonnull
   public CaptureRequest getCaptureRequest() {
      return _captureRequest;
   }

   @Override
   @Nullable
   public Long getProjectId() {
      return _projectId;
   }

   @Override
   @Nullable
   public Long getBuildId() {
      return _buildId;
   }


   /** Used internally to update AppBuildRequest */
   @Override
   @JsonIgnore
   public Long getBuildRequestId()
   {
      return _buildRequestId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setConverterId(long ticketId) {
      this._converterId = ticketId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   @JsonDeserialize(as=CaptureRequestImpl.class)
   public void setCaptureRequest(@Nonnull CaptureRequest captureRequest) {
      this._captureRequest = captureRequest;
   }

   /** Used internally to update AppBuildRequest */
   @Override
   @JsonIgnore
   public void setBuildRequestId(Long buildRequestId)
   {
      _buildRequestId = buildRequestId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setBuildId(@Nullable Long buildId) {
      this._buildId = buildId;
   }

   /** Used by builder.  Do not call directly **/
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setProjectId(@Nullable Long projectId) {
      this._projectId = projectId;
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
