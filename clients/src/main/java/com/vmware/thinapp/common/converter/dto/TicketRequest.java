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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Manual mode conversion request.
 */
public class TicketRequest {
   // List of files that will be downloaded/copied into the capture VM
   // e.g. datastore://exit16/Adobe/9.0/1 or http://ninite.com/installer.exe
   private List<ProjectFile> inputFiles;
   // e.g. internal
   private String outputDatastore;
   // Commands to run in each phase of an application capture
   private Map<ConversionPhase, CommandList> commands;
   // is this request for an automatic or manual capture?
   private boolean automaticCapture;
   private Workpool workpool;
   private Long runtimeId;

   public TicketRequest() {
      // Needed for Jackson deserialization.
   }

   public TicketRequest(
           List<ProjectFile> inputFiles,
           String outputDatastore,
           Map<ConversionPhase, CommandList> commands,
           boolean automaticCapture,
           Workpool workpool,
           Long runtimeId) {
      this.inputFiles = inputFiles;
      // XXX: Remove the datastore URI scheme here for now...
      this.outputDatastore = outputDatastore.replace("datastore://", "");
      this.commands = commands;
      this.automaticCapture = automaticCapture;
      this.workpool = workpool;
      this.runtimeId = runtimeId;
   }

   public void setInputFiles(List<ProjectFile> inputFiles) {
      this.inputFiles = inputFiles;
   }

   public void setOutputDatastore(String outputDatastore) {
      this.outputDatastore = outputDatastore;
   }

   public void setCommands(Map<ConversionPhase, CommandList> commands) {
      this.commands = commands;
   }

   public void setAutomaticCapture(boolean automaticCapture) {
      this.automaticCapture = automaticCapture;
   }

   public List<ProjectFile> getInputFiles() {
      return inputFiles;
   }

   public String getOutputDatastore() {
      return outputDatastore;
   }

   public Map<ConversionPhase, CommandList> getCommands() {
      return commands;
   }

   public boolean getAutomaticCapture() {
      return automaticCapture;
   }

   public Workpool getWorkpool() {
      return workpool;
   }

   public void setWorkpool(Workpool workpool) {
      this.workpool = workpool;
   }

   public Long getRuntimeId() {
      return runtimeId;
   }

   public void setRuntimeId(Long runtimeId) {
      this.runtimeId = runtimeId;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
