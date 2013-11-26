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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.vmware.appfactory.common.dto.SelectOptions;

/**
 * This is a wrapper class for containing the build components such as
 * destination datastore, workpool, thinapp runtime, package options etc.
 */
public class BuildComponents
{
   /** Drop down values for picking workpool */
   private SelectOptions workpoolOptions;

   /** Drop down values for picking datastore */
   private SelectOptions datastoreOptions;

   /** Drop down values for picking runtime */
   private SelectOptions runtimeOptions;

   /** Flag denoting if this is used for rebuild or capture */
   private boolean isRebuild;


   /**
    * @return the workpoolOptions
    */
   public SelectOptions getWorkpoolOptions()
   {
      return workpoolOptions;
   }


   /**
    * @param workpoolOptions the workpoolOptions to set
    */
   public void setWorkpoolOptions(SelectOptions workpoolOptions)
   {
      this.workpoolOptions = workpoolOptions;
   }


   /**
    * @return the datastoreOptions
    */
   public SelectOptions getDatastoreOptions()
   {
      return datastoreOptions;
   }


   /**
    * @param datastoreOptions the datastoreOptions to set
    */
   public void setDatastoreOptions(SelectOptions datastoreOptions)
   {
      this.datastoreOptions = datastoreOptions;
   }


   /**
    * @return the runtimeOptions
    */
   public SelectOptions getRuntimeOptions()
   {
      return runtimeOptions;
   }


   /**
    * @param runtimeOptions the runtimeOptions to set
    */
   public void setRuntimeOptions(SelectOptions runtimeOptions)
   {
      this.runtimeOptions = runtimeOptions;
   }


   /**
    * @return the isRebuild
    */
   public boolean isRebuild()
   {
      return isRebuild;
   }


   /**
    * @param isRebuild the isRebuild to set
    */
   public void setRebuild(boolean isRebuild)
   {
      this.isRebuild = isRebuild;
   }


   @Override
   public String toString()
   {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }
}
