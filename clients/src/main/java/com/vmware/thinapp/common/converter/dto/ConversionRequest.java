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

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Class that defines the JSON structure which we pass to the
 * Converter Web Service.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class ConversionRequest implements Comparable<ConversionRequest> {
   private Long id;
   private List<ProjectFile> files;
   private DsLocation output;
   private Map<ConversionPhase, CommandList> steps;
   private Workpool workpool;
   private Long runtimeId;

   public ConversionRequest() {
      /* Do nothing */
   }

   /**
    * Constructor to create an instance with fields:
    * input, output, installationCommand, packageSettings, vm
    */
   public ConversionRequest(List<ProjectFile> files, DsLocation output,
         Map<ConversionPhase, CommandList> steps, Workpool workpool, Long runtimeId) {
      this.files = files;
      this.output = output;
      this.steps = steps;
      this.workpool = workpool;
      this.runtimeId = runtimeId;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public DsLocation getOutput() {
      return output;
   }

   public void setOutput(DsLocation output) {
      this.output = output;
   }

   public List<ProjectFile> getFiles() {
      return files;
   }

   public void setFiles(List<ProjectFile> files) {
      this.files = files;
   }

   public Map<ConversionPhase, CommandList> getSteps() {
      return steps;
   }

   public void setSteps(Map<ConversionPhase, CommandList> steps) {
      this.steps = steps;
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
   public int compareTo(ConversionRequest other) {
      return id.compareTo(other.id);
   }
}
