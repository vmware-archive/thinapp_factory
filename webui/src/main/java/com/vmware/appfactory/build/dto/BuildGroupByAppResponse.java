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

import com.vmware.appfactory.datastore.DsDatastore;


/**
 * This is an object that holds the builds grouped by Application and a Collection of
 * associated datastores. The datastores are needed to display datastore related
 * info on the sidebar.
 */
public class BuildGroupByAppResponse
{
   private Collection<BuildGroupByApp> _buildGroups;

   private Collection<DsDatastore> _datastores;


   public BuildGroupByAppResponse()
   {
      // Empty constructor
   }


   public BuildGroupByAppResponse(
         Collection<BuildGroupByApp> buildGroups,
         Collection<DsDatastore> datastores)
   {
      this._buildGroups = buildGroups;
      this._datastores = datastores;
   }


   /**
    * @return the buildGroups
    */
   public Collection<BuildGroupByApp> getBuildGroups()
   {
      return _buildGroups;
   }


   /**
    * @param buildGroups
    *           the buildGroups to set
    */
   public void setBuildGroups(Collection<BuildGroupByApp> buildGroups)
   {
      _buildGroups = buildGroups;
   }


   /**
    * @return the datastores
    */
   public Collection<DsDatastore> getDsDatastores()
   {
      return _datastores;
   }


   /**
    * @param datastores
    *           the datastores to set
    */
   public void setDsDatastores(Collection<DsDatastore> datastores)
   {
      _datastores = datastores;
   }
}
