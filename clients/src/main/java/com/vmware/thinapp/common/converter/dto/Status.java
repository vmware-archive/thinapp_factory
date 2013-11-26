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


package com.vmware.thinapp.common.converter.dto;

import java.util.SortedSet;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.vmware.thinapp.common.workpool.dto.Lease;

/**
 * DTO that represents MM job status.  See {@link ConversionJobStatus} for the DTO that
 * represents automatic conversion job status.
 */
public class Status {
   public static enum State {
      created,
      acquiringVm,
      vmAcquired,
      poweringOnVm,
      waitingForTools,
      needsLoginWait,
      needsLoginDone,
      installingThinApp,
      downloading,
      installerDownloadFailed,
      mountingFileSharesToGuest,

      preCaptureWait,
      preCaptureDone,

      takingPreCaptureSnapshot,

      preInstallationWait,
      preInstallationDone,
      installationWait,
      installationDone,
      postInstallationWait,
      postInstallationDone,

      takingPostCaptureSnapshot,
      generatingProject,
      preProjectBuildWait,
      preProjectBuildDone,
      buildingProject,
      refreshingProject,
      refreshingProjectDone,
      vmReleased,

      cancelling,
      failure,
      success,
      cancelled,

      // Terminus state.
      finished
   }

   private SortedSet<State> states;
   private Lease lease;
   private Long projectId;

   public SortedSet<State> getStates() {
      return states;
   }

   public void setStates(SortedSet<State> states) {
      this.states = states;
   }

   @JsonIgnore
   protected void setCurrentState(@SuppressWarnings("unused") State state) {
      // no-op settter so that we can serialize the currentState value.
   }

   public void setLease(Lease lease) {
      this.lease = lease;
   }

   public Lease getLease() {
      return lease;
   }

   public State getCurrentState() {
      return getStates().last();
   }

   public void setProjectId(Long projectId) {
      this.projectId = projectId;
   }

   /**
    * On success gives the resulting projectId.
    *
    * @return projectId
    */
   public Long getProjectId() {
      return projectId;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
