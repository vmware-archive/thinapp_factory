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

import java.util.Collection;
import java.util.Map;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.datastore.DsDatastore;

/**
 * This is an object that holds the builds and a Collection of datastoresMap.
 * DatastoresMap are needed to display related info on the sidebar.
 */
public class BuildAllResponse {

   private boolean _hzEnabled;

   private Collection<Build> _builds;

   private Map<Long, DsDatastore> _datastoresMap;

   public BuildAllResponse() {
      // Empty constructor
   }

   public BuildAllResponse(boolean hzEnabled, Collection<Build> builds, Map<Long, DsDatastore> datastoresMap) {
      this._hzEnabled = hzEnabled;
      this._builds = builds;
      this._datastoresMap = datastoresMap;
   }

   /**
    * @return the builds
    */
   public Collection<Build> getBuilds()
   {
      return _builds;
   }

   /**
    * @param builds the builds to set
    */
   public void setBuilds(Collection<Build> builds) {
      _builds = builds;
   }

   /**
    * @return the datastoresMap
    */
   public Map<Long, DsDatastore> getDatastoresMap() {
      return _datastoresMap;
   }

   /**
    * @param datastoresMap the datastoresMap to set
    */
   public void setDatastoresMap(Map<Long, DsDatastore> datastoresMap) {
      _datastoresMap = datastoresMap;
   }

   /**
    * @return the _hzEnabled
    */
   public boolean isHzEnabled() {
      return _hzEnabled;
   }

   /**
    * @param hzEnabled the _hzEnabled to set
    */
   public void setHzEnabled(boolean hzEnabled) {
      this._hzEnabled = hzEnabled;
   }
}
