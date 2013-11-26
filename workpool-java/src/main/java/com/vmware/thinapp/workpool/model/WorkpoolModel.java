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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.springframework.context.ApplicationContext;

import com.google.inject.internal.ImmutableMap;
import com.vmware.thinapp.workpool.Instancer;

/**
 * Abstract model for all workpools.
 *
 * Named WorkpoolModel to avoid colliding with the very common Workpool class.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "workpool", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public abstract class WorkpoolModel implements Serializable {
   private static final long serialVersionUID = -6912174946403470892L;

   /**
    * States the workpool can be in.
    */
   public enum State {
      created,
      available,
      unavailable,
      deleted,
      deleting,
      resetting,
      deleteFailed,
      waitingForInstancer;

      private static Map<State, State> runningToFailedStates = ImmutableMap.<State, State>builder()
              .put(deleting, deleteFailed)
              .build();

      /**
       * Determine if workpool is in a state where it can be deleted.
       *
       * @return
       */
      public boolean isDeletable() {
         switch (this) {
            case created:
            case available:
            case unavailable:
            case waitingForInstancer:
            case deleteFailed:
               return true;
            case resetting:
            case deleted:
            case deleting:
         }

         return false;
      }

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
   private String name;
   private int maximumInstances;
   // Using Lists instead of Sets because of bug https://hibernate.onjira.com/browse/HHH-3799.
   private Set<InstanceModel> instances = new HashSet<InstanceModel>();
   private Set<LeaseModel> leases = new HashSet<LeaseModel>();
   @NotNull
   private State state = State.created;
   private String lastError = "";

   /**
    * Indicates whether the workpool is able to create new instances.
    *
    * @return
    */
   @Transient
   public abstract boolean isGrowable();

   @Id
   @GeneratedValue
   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
   public Set<LeaseModel> getLeases() {
      return leases;
   }

   public void setLeases(Set<LeaseModel> leases) {
      this.leases = leases;
   }

   @OneToMany(cascade = CascadeType.ALL, mappedBy = "workpool", orphanRemoval = true)
   public Set<InstanceModel> getInstances() {
      return instances;
   }

   public void setInstances(Set<InstanceModel> instances) {
      this.instances = instances;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getMaximumInstances() {
      return maximumInstances;
   }

   public void setMaximumInstances(int maximumInstances) {
      this.maximumInstances = maximumInstances;
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

   /**
    * Create an instancer that is responsible for creating new instances
    * of a specific workpool type.
    *
    * @param appCtxt Spring application context
    * @return an instancer
    */
   public abstract Instancer createInstancer(ApplicationContext appCtxt);

   @Override
   public String toString() {
      // Exclude leases because they're lazily loaded.  Maybe eagerly load them instead?
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).setExcludeFieldNames(
              new String[]{"leases", "instances"}).toString();
   }
}
