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

package com.vmware.appfactory.datasource.dto;

/**
 * DTO for the datastore usage API.
 */
public class DatastoreUsageResponse {
   /**
    * Number of projects that are stored on the datastore.
    */
   private int _numProjects;
   /**
    * Number of applications that are uploaded onto the datastore.
    */
   private int _numApplications;

   /**
    * @param numProjects
    * @param numApplications
    */
   public DatastoreUsageResponse(int numProjects, int numApplications) {
      super();
      _numProjects = numProjects;
      _numApplications = numApplications;
   }

   /**
    * @return the numProjects
    */
   public int getNumProjects() {
      return _numProjects;
   }

   /**
    * @return the numApplications
    */
   public int getNumApplications() {
      return _numApplications;
   }

   /**
    * @param numProjects the numProjects to set
    */
   public void setNumProjects(int numProjects) {
      _numProjects = numProjects;
   }

   /**
    * @param numApplications the numApplications to set
    */
   public void setNumApplications(int numApplications) {
      _numApplications = numApplications;
   }
}
