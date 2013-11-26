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
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.common.collect.ImmutableMap;

/**
 * Describes a concrete VM that exists in VC.
 */
@Entity
@Table(name = "vmimage", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "moid"}))
public class VmImageModel extends InstanceableModel implements Serializable {
   private static final long serialVersionUID = -8375040833687876039L;

   @NotEmpty
   protected String name;
   @NotNull
   protected String moid = "";
   @Embedded
   @NotNull
   protected OsInfoModel osInfo;
   @Embedded
   @NotNull
   protected OsRegistrationModel osRegistration;
   @NotNull
   protected State state = State.created;
   protected String lastError = "";

   // Not set for custom VMs.
   protected VmPatternModel vmPattern;
   protected Set<LinkedWorkpoolModel> workpools = new HashSet<LinkedWorkpoolModel>();

   public enum State {
      /** Initial state. */
      created,
      /** Image is ready to be used. */
      available,
      /** Request to delete the backing VM. */
      deleteRequested,
      /** Signal instance should be deleted. */
      deleted,
      /** Backing VM was expected to exist but does not.  Cannot continue. */
      vmDoesNotExist,
      /** Installation is in progress. */
      installing,
      /** Installation failed. */
      installFailed,
      /** Installation finished. */
      installFinished,
      /** VM is being prepared. */
      creatingBaseSnapshot,
      /** VM was successfully prepared. */
      snapshotted,
      /** VM preparation failed. */
      snapshotFailed,
      /** Existing VM was provided and needs to be imported. */
      waitingImport,
      /** Waiting to start installation for a new VM. */
      waitingInstall,
      /** Backing VM deletion is in progress. */
      deleting,
      /** Deletion failed. */
      deleteFailed,
      /** Unknown state. */
      unknown;

      private static Map<State, State> runningToFailedStates = ImmutableMap.<State, State>builder()
              .put(installing, installFailed)
              .put(creatingBaseSnapshot, snapshotFailed)
              .put(deleting, deleteFailed)
              .build();

      /**
       * Determine if the instance can be deleted in the given state.
       *
       * @return
       */
      public boolean isDeletable() {
         switch (this) {
            case unknown:
            case created:
            case available:
            case vmDoesNotExist:
            case installFailed:
            case installFinished:
            case snapshotted:
            case snapshotFailed:
            case waitingImport:
            case waitingInstall:
            case deleteFailed:
               return true;
            case deleteRequested:
            case deleted:
            case installing:
            case creatingBaseSnapshot:
            case deleting:
         }

         return false;
      }

      /**
       * Return true if it is in a failure state.
       *
       * @return
       */
      public boolean isFailure() {
         switch (this) {
            case vmDoesNotExist:
            case installFailed:
            case snapshotFailed:
            case deleteFailed:
               return true;
            case unknown:
            case created:
            case available:
            case installFinished:
            case snapshotted:
            case waitingImport:
            case waitingInstall:
            case deleteRequested:
            case deleted:
            case installing:
            case creatingBaseSnapshot:
            case deleting:
         }

         return false;
      }

      /**
       * Return true if it is ready to be used.
       *
       * @return
       */
      public boolean isReady() {
         switch (this) {
            case available:
               return true;
            case vmDoesNotExist:
            case installFailed:
            case snapshotFailed:
            case unknown:
            case created:
            case installFinished:
            case snapshotted:
            case waitingImport:
            case waitingInstall:
            case deleteRequested:
            case deleted:
            case installing:
            case creatingBaseSnapshot:
            case deleting:
            case deleteFailed:
         }

         return false;
      }

      /**
       * Map running state to its failed state.
       *
       * @return correpsonding failed state if it exists, otherwise self
       */
      public State getFailedStateOrSelf() {
         if (runningToFailedStates.containsKey(this)) {
            return runningToFailedStates.get(this);
         } else {
            return this;
         }
      }
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getMoid() {
      return moid;
   }

   public void setMoid(String moid) {
      this.moid = moid;
   }

   public OsInfoModel getOsInfo() {
      return osInfo;
   }

   public void setOsInfo(OsInfoModel osInfo) {
      this.osInfo = osInfo;
   }

   public OsRegistrationModel getOsRegistration() {
      return osRegistration;
   }

   public void setOsRegistration(OsRegistrationModel osRegistration) {
      this.osRegistration = osRegistration;
   }

   @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
   @JoinColumn(name = "vmpattern_id")
   public VmPatternModel getVmPattern() {
      return vmPattern;
   }

   public void setVmPattern(VmPatternModel vmPattern) {
      this.vmPattern = vmPattern;
   }

   @OneToMany(cascade = CascadeType.ALL, mappedBy = "instancer")
   public Set<LinkedWorkpoolModel> getWorkpools() {
      return workpools;
   }

   public void setWorkpools(Set<LinkedWorkpoolModel> workpools) {
      this.workpools = workpools;
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
   public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
              .setExcludeFieldNames(new String[]{"guestPassword", "workpools"})
              .toString();
   }
}
