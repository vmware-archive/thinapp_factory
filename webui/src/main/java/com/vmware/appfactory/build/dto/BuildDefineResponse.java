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

import com.vmware.appfactory.recipe.model.Recipe;

/**
 * Returned from the BuildApiController when the user asks for default
 * settings for one or more new build requests. We return back some defaults
 * for each requested application, plus some global data.
 */
public class BuildDefineResponse {
   /** All recipes that are available */
   private List<Recipe> _recipes= new ArrayList<Recipe>();

   /** Runtime info as selectOption object */
   private BuildComponents _buildComponents = new BuildComponents();

   /** Flag to indicate horizon is enabled */
   private boolean _horizonEnabled;

   /** Suggested build settings, one per requested application */
   private BuildRequestList _requests = new BuildRequestList();

   /**
    * @return the recipes
    */
   public List<Recipe> getRecipes()
   {
      return _recipes;
   }

   /**
    * @param recipes the recipes to set
    */
   public void setRecipes(List<Recipe> recipes)
   {
      _recipes = recipes;
   }

   /**
    * @return the horizonEnabled
    */
   public boolean isHorizonEnabled()
   {
      return _horizonEnabled;
   }

   /**
    * @param horizonEnabled the horizonEnabled to set
    */
   public void setHorizonEnabled(boolean horizonEnabled)
   {
      _horizonEnabled = horizonEnabled;
   }

   /**
    * @return the buildComponents
    */
   public BuildComponents getBuildComponents()
   {
      return _buildComponents;
   }

   /**
    * @param buildComponents the buildComponents to set
    */
   public void setBuildComponents(BuildComponents buildComponents)
   {
      _buildComponents = buildComponents;
   }

   /**
    * @return the requests
    */
   public BuildRequestList getRequests()
   {
      return _requests;
   }

   /**
    * @param requests the requests to set
    */
   public void setRequests(BuildRequestList requests)
   {
      _requests = requests;
   }
}
