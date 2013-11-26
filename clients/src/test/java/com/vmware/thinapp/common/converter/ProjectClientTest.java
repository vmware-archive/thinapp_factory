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

package com.vmware.thinapp.common.converter;

import org.junit.Before;
import org.junit.Test;

import com.vmware.thinapp.common.converter.client.ProjectClient;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.datastore.dto.Datastore;

import static org.junit.Assert.assertEquals;

public class ProjectClientTest {
   ProjectClient projectClient;
   private ThinAppRuntime runtime = ThinAppRuntime.apply("4.0.4", 216012, "/home/user/src/appfactory/runtimes/4.0" +
           ".4-216012");

   @Before
   public void setup() {
      projectClient = new ProjectClient("http://localhost:5000/projects/");
   }

   @Test
   public void testGetProject() throws Exception {
      Project project = projectClient.getProject(Long.valueOf(1));
      System.out.println(project);
   }

   @Test
   public void createProject() throws Exception {
      Datastore datastore = new Datastore();
      datastore.setName("internal");
      Project project = projectClient.create(datastore, runtime);
      assertEquals(Project.State.created, project.getState());
   }

   @Test
   public void testRefreshProject() throws Exception {
      Datastore datastore = new Datastore();
      datastore.setName("internal");
      Project project = projectClient.create(datastore, runtime);
      project.refresh();
   }
}
