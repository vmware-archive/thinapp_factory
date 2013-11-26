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

package com.vmware.appfactory.taskqueue.dto;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.workpool.dto.Workpool;


public interface CaptureRequest {

   /**
    * @return the _applicationId
    */
   @Nonnull
   Long getApplicationId();

   /**
    * @return the _icons
    */
   List<? extends AfIcon> getIcons();

   /**
    * @return the _displayName
    */
   @Nonnull
   String getDisplayName();

   /**
    * @return the _buildName
    */
   @Nonnull
   String getBuildName();

   /**
    * @return the _workpoolId
    */
   Long getWorkpoolId();

   @Nullable
   Workpool getWorkpool();

   /**
    * @return the _recipeId
    */
   Long getRecipeId();

   @Nullable
   Recipe getRecipe();

   /**
    * @return the _datastoreId
    */
   Long getDatastoreId();

   @Nullable
   DsDatastore getDatastore();

   /**
    * @return the _runtimeId
    */
   Long getRuntimeId();

   @Nullable
   ThinAppRuntime getRuntime();

   @Nullable
   boolean isAddHorizonIntegration();

   /**
    * @return the _recipeVariableValues
    */
   Map<String, String> getRecipeVariableValues();

   /**
    * Validate the required fields needed for conversion.
    * Ensure workpool, runtime and datastoreIds exist.
    */
   void validateRequiredFields();
}
