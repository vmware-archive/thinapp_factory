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

package com.vmware.appfactory.build.dto;

import java.util.List;
import java.util.Map;

/**
 * A DTO to handle response from CWS '/projects/import' API.
 */
public class ProjectImportResponse {
   /**
    * A map of newly imported projects: id -> name.
    */
   private Map<Long, String> newProjects;

   /**
    * A map contains (invalid) project name <key> and a list of missing files <value>
    */
   private Map<String, List<String>> errors;

   /**
    *
    * @return the non-null newProjects map
    */
   public Map<Long, String> getNewProjects() {
      return newProjects;
   }

   /**
    * @return the non-null errors map
    */
   public Map<String, List<String>> getErrors() {
      return errors;
   }

   /**
    * @param newProjects the newProjects map to set
    */
   public void setNewProjects(Map<Long, String> newProjects) {
      this.newProjects = newProjects;
   }

   /**
    * @param errors the errors to set
    */
   public void setErrors(Map<String, List<String>> errors) {
      this.errors = errors;
   }
}
