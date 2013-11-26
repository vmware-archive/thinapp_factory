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

package com.vmware.thinapp.common.converter.client;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.datastore.dto.CreateRequest;
import com.vmware.thinapp.common.datastore.dto.CreateResponse;
import com.vmware.thinapp.common.datastore.dto.Datastore;

public class ProjectClient {
   private final String baseUrl;
   private final RestTemplate template = new RestTemplate();

   public ProjectClient(String baseUrl) {
      // Strip to prevent URLs like /projects//5 that won't work.
      this.baseUrl = StringUtils.stripEnd(baseUrl, "/");
   }

   public Project getProject(Long id) {
      String url = String.format("%s/{id}", baseUrl);
      Project project = template.getForObject(url, Project.class, id);
      project.setBaseUrl(String.format("%s/%d", baseUrl, id));
      return project;
   }

   /**
    * Create a new, empty project.
    *
    * @param datastore the datastore the project should be created on
    * @param thinAppRuntime runtime to associate the project with
    * @return a new project
    */
   public Project create(Datastore datastore, ThinAppRuntime thinAppRuntime) {
      CreateRequest request = new CreateRequest();
      // FIXME: Update CreateRequest's datastore type
      request.setDatastore(datastore.getId());
      request.setRuntimeId(thinAppRuntime.getId());
      // Note: These the create/response types must live in their own class files.
      CreateResponse response = template.postForObject(baseUrl, request, CreateResponse.class);
      return getProject(response.getId());
   }
}
