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

package com.vmware.appfactory.feed;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.recipe.RecipeMatch;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeAppKey;

/**
 * When we read applications from a feed, it might have one or more recipes
 * associated with it. Since we don't include recipes in the application
 * class, we use this derived version of Application to keep track of both the
 * application itself, plus the recipes that belong to it.
 */
@JsonSerialize(as=Application.class)
public class FeedApplication
   extends Application
{
   @SuppressWarnings("CollectionWithoutInitialCapacity")
   private final List<Recipe> _recipes = new ArrayList<Recipe>();


   /**
    * @return the recipes that were found for this application in
    * the feed.
    */
   @JsonIgnore
   public List<Recipe> getRecipes()
   {
      return _recipes;
   }


   /**
    * Add a new recipe to this application which we found while parsing
    * the feed.
    * @param recipe   recipe to append
    */
   public void addRecipe(@Nonnull Recipe recipe)
   {
      _recipes.add(recipe);
   }


   /**
    * Set the recipes for this application which we found while parsing
    * the feed.
    *
    * This will entirely replace any previously-added recipes.
    *
    * @param recipes    Set of recipes to add, or null to remove all recipes.
    */
   @SuppressWarnings("TypeMayBeWeakened")
   @JsonIgnore
   public void setRecipes(@Nullable List<Recipe> recipes)
   {
      _recipes.clear();
      if (recipes != null) {
         _recipes.addAll(recipes);
      }
   }


   /**
    * Make sure all the recipes are actually linked to the application
    * they belong to.
    */
   public void linkRecipesToApp()
   {
      for (Recipe recipe : _recipes) {
         RecipeMatch currentMatch = recipe.matchToApplication(this);
         if (currentMatch != RecipeMatch.precise) {
            recipe.addAppKey(createPreciseMatchAppKey());
         }
      }
   }


   /**
    * @return a new RecipeAppKey instance, which we add to any of
    * the recipes for this application.
    */
   private RecipeAppKey createPreciseMatchAppKey()
   {
      RecipeAppKey key = new RecipeAppKey();

      key.setName(getName());
      key.setVersion(getVersion());
      key.setLocale(getLocale());
      key.setInstallerRevision(getInstallerRevision());

      return key;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder(100);
      sb.append("FeedApplication");
      sb.append("{_name=").append(getName());
      sb.append('}');
      return sb.toString();
   }
}