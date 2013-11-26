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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.vmware.thinapp.common.converter.exception.ConverterException;

@JsonSerialize(include=Inclusion.NON_NULL)
public class ConversionJobStatus {
   public static enum JobState {
      created,
      downloading,
      provisioning,
      precapture,
      preinstall,
      install,
      postinstall,
      postcapture,
      projectgen,
      prebuild,
      projectbuild,
      projectrefresh,
      finishing,
      finished,
      cancelling,
      cancelled;

      public JobState getNext() {
         return this.ordinal() < JobState.values().length - 1 ?
               JobState.values()[this.ordinal() + 1] : null;
      }
   }

   private Long jobId;
   private Long projectId;
   private JobState state;
   private int percent;
   private ConversionResult result;
   private PerformanceData performanceData;

   @JsonProperty("id")
   public Long getJobId() {
      return jobId;
   }

   @JsonProperty("id")
   public void setJobId(Long jobId) {
      this.jobId = jobId;
   }

   @JsonProperty("project_id")
   public Long getProjectId() {
      return projectId;
   }

   @JsonProperty("project_id")
   public void setProjectId(Long projectId) {
      this.projectId = projectId;
   }

   public JobState getState() {
      return state;
   }

   public void setState(JobState state) {
      this.state = state;
   }

   public int getPercent() {
      return percent;
   }

   public void setPercent(int percent) {
      this.percent = percent;
   }

   public ConversionResult getResult() {
      return result;
   }

   public void setResult(ConversionResult result) {
      this.result = result;
   }

   public PerformanceData getPerformanceData() {
      return performanceData;
   }

   public void setPerformanceData(PerformanceData performanceData) {
      this.performanceData = performanceData;
   }

   public static JobState toJobState(Status.State status, boolean isCancelled) {
      switch (status) {
         case created:
            return JobState.created;

         case acquiringVm:
         case vmAcquired:
         case poweringOnVm:
         case waitingForTools:
         case needsLoginWait:
         case needsLoginDone:
         case installingThinApp:
            return JobState.provisioning;

         case downloading:
            return JobState.downloading;

         case mountingFileSharesToGuest:
         case preCaptureWait:
         case preCaptureDone:
         case takingPreCaptureSnapshot:
            return JobState.precapture;

         case preInstallationWait:
         case preInstallationDone:
            return JobState.preinstall;

         case installationWait:
         case installationDone:
            return JobState.install;

         case postInstallationWait:
         case postInstallationDone:
            return JobState.postinstall;

         case takingPostCaptureSnapshot:
            return JobState.postcapture;

         case generatingProject:
            return JobState.projectgen;

         case preProjectBuildWait:
         case preProjectBuildDone:
            return JobState.prebuild;

         case buildingProject:
            return JobState.projectbuild;

         case refreshingProject:
         case refreshingProjectDone:
            return JobState.projectrefresh;

         case cancelled:
         case installerDownloadFailed:
         case failure:
         case success:
         case vmReleased:
            return JobState.finishing;

         case cancelling:
            return JobState.cancelling;

         case finished:
            return (isCancelled ? JobState.cancelled : JobState.finished);
      }
      throw new ConverterException("Unhandled Status");
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
