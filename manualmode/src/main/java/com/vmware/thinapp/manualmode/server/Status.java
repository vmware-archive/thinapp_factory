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


package com.vmware.thinapp.manualmode.server;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.collect.ImmutableSortedSet;
import com.vmware.thinapp.common.converter.dto.Command;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.manualmode.util.PerformanceDataListener;

/**
 * Internal representation of a conversion job's status including phase and
 * performance data.
 */
public class Status implements PerformanceDataListener {
   private final SortedSet<State> states =
      new ConcurrentSkipListSet<State>();
   private final SortedSet<State> endingStates;
   private Lease lease;
   private Long projectId;
   private Integer refreshRate;
   private PerformanceData perfData;
   private Command lastCommand;
   private String lastError = "";

   public Status() {
      // Build an immutable set of ending states
      ImmutableSortedSet.Builder<State> builder =
         ImmutableSortedSet.naturalOrder();
      for (State state : State.values()) {
         if (isEndingState(state)) {
            builder.add(state);
         }
      }
      endingStates = builder.build();

      // Initial state is State.created
      setCurrentState(State.created);
   }

   public SortedSet<State> getStates() {
      return Collections.unmodifiableSortedSet(states);
   }

   public void setLease(Lease lease) {
      this.lease = lease;
   }

   public Lease getLease() {
      return lease;
   }

   public void setRefreshRate(Integer refreshRate) {
      this.refreshRate = refreshRate;
   }

   public Integer getRefreshRate() {
      return refreshRate;
   }

   public PerformanceData getPerfData() {
      return perfData;
   }

   public void setLastCommand(Command lastCommand) {
      this.lastCommand = lastCommand;
   }

   public Command getLastCommand() {
      return lastCommand;
   }

   public void setLastError(String error) {
      this.lastError = error;
   }

   public String getLastError() {
      return lastError;
   }

   public boolean isRequestCancelling() {
      return getStates().contains(State.cancelling);
   }

   public boolean isRequestCancelled() {
      return getStates().contains(State.cancelled);
   }

   public boolean isRequestSuccess() {
      return getStates().contains(State.success);
   }

   public State getCurrentState() {
      return getStates().last();
   }

   @Override
   public void update(Date date, Map<String, Long> values) {
      perfData = PerformanceData.create(refreshRate, date, values);
   }

   public void update(String date, Map<String, Long> values) {
      perfData = PerformanceData.create(refreshRate, date, values);
   }

   /**
    * Waits until the request is in the given state <i>or</i> the request is
    * canceled. The caller should check the cancel state before continuing after
    * waitUntil returns.
    *
    * @param someState
    */
   public synchronized void waitUntil(State someState) {
      while (true) {
         if (getCurrentState().compareTo(someState) >= 0 || isRequestCancelling()) {
            return;
         }

         try {
            wait(1000);
         } catch (InterruptedException ignored) {
            /* Ignore */
         }
      }
   }

   /**
    * Set the current state of the conversion job.
    *
    * @param state the state to set
    */
   public synchronized void setCurrentState(State state) {
      states.add(state);
      notifyAll();
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

   public int getPercent() {
      int percent = -1;
      switch(this.getCurrentState()) {
         case created:
            percent = 5;
            break;

         case acquiringVm:
            percent = 10;
            break;

         case vmAcquired:
            percent = 15;
            break;

         case poweringOnVm:
            percent = 20;
            break;

         case waitingForTools:
            percent = 25;
            break;

         case needsLoginWait:
            percent = 30;
            break;

         case needsLoginDone:
         case installingThinApp:
            percent = 35;
            break;

         case downloading:
         case mountingFileSharesToGuest:
            percent = 40;
            break;

         case preCaptureWait:
            percent = 45;
            break;

         case preCaptureDone:
         case takingPreCaptureSnapshot:
            percent = 50;
            break;

         case preInstallationWait:
            percent = 55;
            break;

         case preInstallationDone:
         case installationWait:
            percent = 60;
            break;

         case installationDone:
         case postInstallationWait:
            percent = 65;
            break;

         case postInstallationDone:
         case takingPostCaptureSnapshot:
            percent = 70;
            break;

         case generatingProject:
            percent = 75;
            break;

         case preProjectBuildWait:
            percent = 80;
            break;

         case preProjectBuildDone:
         case buildingProject:
            percent = 85;
            break;

         case vmReleased:
            percent = 90;
            break;

         case refreshingProject:
            percent = 95;
            break;

         case cancelling:
            percent = 99;
            break;

         case refreshingProjectDone:
         case failure:
         case success:
         case cancelled:
         case installerDownloadFailed:
         case finished:
            percent = 100;
            break;
      }
      return percent;
   }

   /**
    * Get the most recent non-ending state.
    *
    * @return the most recent non-ending state
    */
   public State getLastRunningState() {
      return getStates().headSet(endingStates.first()).last();
   }

   /**
    * Determine if the given state is an ending state.
    *
    * Ending states are cancelling, failure, success, cancelled, and finished.
    *
    * @return true if the given state is a running state, false otherwise.
    */
   public static boolean isEndingState(State state) {
      boolean isEnding = false;
      switch(state) {
         case created:
         case acquiringVm:
         case vmAcquired:
         case poweringOnVm:
         case waitingForTools:
         case needsLoginWait:
         case needsLoginDone:
         case installingThinApp:
         case downloading:
         case installerDownloadFailed:
         case mountingFileSharesToGuest:
         case preCaptureWait:
         case preCaptureDone:
         case takingPreCaptureSnapshot:
         case preInstallationWait:
         case preInstallationDone:
         case installationWait:
         case installationDone:
         case postInstallationWait:
         case postInstallationDone:
         case takingPostCaptureSnapshot:
         case generatingProject:
         case preProjectBuildWait:
         case preProjectBuildDone:
         case buildingProject:
         case refreshingProject:
         case refreshingProjectDone:
            isEnding = false;
            break;
         case vmReleased:
         case cancelling:
         case failure:
         case success:
         case cancelled:
         case finished:
            isEnding = true;
      }
      return isEnding;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
