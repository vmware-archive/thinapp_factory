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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApp;


/**
 * This is an object that holds the builds grouped by Application.
 */
public class BuildGroupByApp implements Comparable<BuildGroupByApp>
{
   private AbstractApp _baseApp;

   private List<Build.Status> _stateList = new ArrayList<Build.Status>();

   private List<Long> _datastoreIdList = new ArrayList<Long>();

   private long _recentBuiltTimestamp;

   private long _recentBuiltBuildId;


   /**
    * Create a buildGroupByApp object from a build object itself.
    * @param build
    */
   public BuildGroupByApp(AbstractApp.AppIdentity identity, Build build)
   {
      this._baseApp = identity;
      this._stateList.add(build.getStatus());
      this._datastoreIdList.add(build.getDatastoreId());
      this._recentBuiltTimestamp = build.getBuilt();
      this._recentBuiltBuildId = build.getId();
   }


   /**
    * Add the status, datastoreId to respective lists and set the timestamp if
    * its the latest.
    */
   public void updateBuildGroup(Build build)
   {
      this._stateList.add(build.getStatus());
      this._datastoreIdList.add(build.getDatastoreId());

      // Set only if the param is the latest.
      if (this._recentBuiltTimestamp < build.getBuilt()) {
         this._recentBuiltTimestamp = build.getBuilt();
         this._recentBuiltBuildId = build.getId();
      }
   }


   /**
    * @return the baseApp
    */
   public AbstractApp getBaseApp()
   {
      return _baseApp;
   }


   /**
    * @return the stateList
    */
   public List<Build.Status> getStateList()
   {
      return _stateList;
   }


   /**
    * @param stateList the stateList to set
    */
   public void setStateList(List<Build.Status> stateList)
   {
      _stateList = stateList;
   }


   /**
    * @return the datastoreIdList
    */
   public List<Long> getDatastoreIdList()
   {
      return _datastoreIdList;
   }


   /**
    * @param datastoreIdList the datastoreIdList to set
    */
   public void setDatastoreIdList(List<Long> datastoreIdList)
   {
      _datastoreIdList = datastoreIdList;
   }


   /**
    * @return the recentBuiltTimestamp
    */
   public long getRecentBuiltTimestamp()
   {
      return _recentBuiltTimestamp;
   }


   /**
    * @param recentBuiltTimestamp the recentBuiltTimestamp to set
    */
   public void setRecentBuiltTimestamp(long recentBuiltTimestamp)
   {
      _recentBuiltTimestamp = recentBuiltTimestamp;
   }


   /**
    * @return the recentBuiltBuildId
    */
   public long getRecentBuiltBuildId()
   {
      return _recentBuiltBuildId;
   }


   /**
    * @param recentBuiltBuildId the recentBuiltBuildId to set
    */
   public void setRecentBuiltBuildId(long recentBuiltBuildId)
   {
      _recentBuiltBuildId = recentBuiltBuildId;
   }


   /**
    * This object is equals to the other as long as the application it contains
    * are the same.
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (obj instanceof BuildGroupByApp) {
         return EqualsBuilder.reflectionEquals(
               this._baseApp,
               ((BuildGroupByApp)obj).getBaseApp());
      }
      return false;
   }


   /**
    * The BuildGroupByApp is different only if the app it contains is
    * different from the other object's app.
    */
   @Override
   public int compareTo(BuildGroupByApp o)
   {
      if (this._baseApp != null) {
         if (o.getBaseApp() == null) {
            return 1;
         }
         return this._baseApp.compareTo(o.getBaseApp());
      }
      return -1;
   }


   @Override
   public int hashCode()
   {
      return (this._baseApp == null)? 0 : this._baseApp.hashCode();
   }
}
