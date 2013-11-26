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

import javax.annotation.Nullable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.web.client.RestTemplate;

public class Project {
   public static enum State {
      created, available, deleting, deleted, dirty, rebuilding
   }

   private Long id;
   private List<ProjectFile> files;
   private State state;
   private Long datastoreId;
   private String subdir;
   private Long runtimeId;
   @Nullable private String iconUrl;
   @JsonIgnore
   private String baseUrl;
   @JsonIgnore
   private final RestTemplate template = new RestTemplate();

   /**
    * Refresh the contents of the project
    */
   public void refresh() {
      template.postForLocation(baseUrl + "/refresh", "");
   }

   /**
    * Delete the project.
    */
   public void delete() {
      template.postForLocation(baseUrl + "/delete", "");
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public List<ProjectFile> getFiles() {
      return files;
   }

   public void setFiles(List<ProjectFile> files) {
      this.files = files;
   }

   public State getState() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
   }

   public Long getDatastoreId() {
      return datastoreId;
   }

   public void setDatastoreId(Long datastoreId) {
      this.datastoreId = datastoreId;
   }

   public String getSubdir() {
      return subdir;
   }

   public void setSubdir(String subdir) {
      this.subdir = subdir;
   }

   public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
   }

   public Long getRuntimeId() {
      return runtimeId;
   }

   public void setRuntimeId(Long runtimeId) {
      this.runtimeId = runtimeId;
   }

   public String getIconUrl() {
      return iconUrl;
   }

   public void setIconUrl(String iconUrl) {
      this.iconUrl = iconUrl;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
