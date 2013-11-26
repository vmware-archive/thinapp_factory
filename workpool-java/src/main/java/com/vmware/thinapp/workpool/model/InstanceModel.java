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

package com.vmware.thinapp.workpool.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.inject.internal.ImmutableMap;

/**
 * A singly leasable entity.
 */
@Entity
@Table(name = "instance", uniqueConstraints = @UniqueConstraint(columnNames = "moid"))
public class InstanceModel implements Serializable {
   private static final long serialVersionUID = -8873859879114650071L;

   public enum State {
      created,
      deleting,
      deleteFailed,
      instancing,
      instancingFailed,
      available;

      private static ImmutableMap<State, State> runningToFailedStates = ImmutableMap.<State, State>builder()
              .put(deleting, deleteFailed)
              .put(instancing, instancingFailed)
              .build();

      /**
       * Map running state to its failed state.
       *
       * @return failed state if it exists, otherwise self
       */
      public State getFailedStateOrSelf() {
         if (runningToFailedStates.containsKey(this)) {
            return runningToFailedStates.get(this);
         } else {
            return this;
         }
      }
   }

   private long id;
   // moids must be unique so it can't default to an empty string.
   private String moid;
   @NotEmpty
   private String guestUsername;
   @NotNull
   private String guestPassword;
   @NotNull
   private WorkpoolModel workpool;
   @NotNull
   private boolean autologon = false;
   @NotNull
   private State state = State.created;
   private String lastError = "";

   @Id
   @GeneratedValue
   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getMoid() {
      return moid;
   }

   public void setMoid(String moid) {
      this.moid = moid;
   }

   public String getGuestUsername() {
      return guestUsername;
   }

   public void setGuestUsername(String guestUsername) {
      this.guestUsername = guestUsername;
   }

   public String getGuestPassword() {
      return guestPassword;
   }

   public void setGuestPassword(String guestPassword) {
      this.guestPassword = guestPassword;
   }

   public boolean getAutologon() {
      return autologon;
   }

   public void setAutologon(boolean autologon) {
      this.autologon = autologon;
   }

   @ManyToOne
   @JoinColumn(name = "workpool_id")
   public WorkpoolModel getWorkpool() {
      return workpool;
   }

   public void setWorkpool(WorkpoolModel workpool) {
      this.workpool = workpool;
   }

   @Enumerated(EnumType.STRING)
   public State getState() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
   }

   public String getLastError() {
      return lastError;
   }

   public void setLastError(String error) {
      this.lastError = error;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof InstanceModel) {
         InstanceModel other = (InstanceModel) o;
         return new EqualsBuilder().append(getId(), other.getId()).isEquals();
      }

      return false;
   }

   @Override
   public int hashCode() {
      return new HashCodeBuilder(27, 35).append(getId()).toHashCode();
   }

   @Override
   public String toString() {
      return new ReflectionToStringBuilder(this)
              .setExcludeFieldNames(new String[]{"guestPassword"}).toString();
   }
}
