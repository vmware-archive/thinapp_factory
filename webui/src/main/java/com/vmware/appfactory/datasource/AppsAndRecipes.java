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

package com.vmware.appfactory.datasource;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.recipe.model.Recipe;

/**
 * Contains a list of applications and a list of recipes.
 * All data sources can supply this information.
 *
 * TODO: This class is bogus. The feed/fileshare/datasource code needs a
 * huge refactoring one day.
 */
public class AppsAndRecipes
{
   private List<Application> _apps;

   private List<Recipe> _recipes;


   /**
    * Get all the applications.
    * @return
    */
   public List<Application> getApplications()
   {
      return _apps;
   }

   /**
    * Set the applications.
    * @param apps
    */
   public void setApplications(List<Application> apps)
   {
      _apps = apps;
   }


   /**
    * Get all the recipes.
    * @return
    */
   public List<Recipe> getRecipes()
   {
      return _recipes;
   }


   /**
    * Set all the recipes.
    * @param recipes
    */
   public void setRecipes(List<Recipe> recipes)
   {
      _recipes = recipes;
   }


   /**
    * Return true if there are no applications AND no recipes.
    * @return
    */
   public boolean isEmpty()
   {
      return CollectionUtils.isEmpty(_apps) && CollectionUtils.isEmpty(_recipes);
   }
}
