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

package com.vmware.appfactory.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.appfactory.recipe.model.Recipe;

/**
 * Basically just an alias for a map of an array of recipes.
 */
public class RecipeMatches
   extends HashMap<RecipeMatch,List<Recipe>>
{
   private static final long serialVersionUID = -8748651217563421937L;

   /**
    * Create a new instance.
    * Each key is assigned a default (empty) list of recipes.
    */
   public RecipeMatches()
   {
      for (RecipeMatch m : RecipeMatch.values()) {
         put(m, new ArrayList<Recipe>());
      }
   }

   /**
    * Convert from a map whose values are recipes into a map whose values
    * are just names. This allows us to send much less data when serializing
    * into JSON.
    *
    * @param includeEmptyLists Include lists that are empty.
    * @return
    */
   public Map<RecipeMatch, List<Long>> toIdMap(boolean includeEmptyLists)
   {
      Map<RecipeMatch, List<Long>> map = new HashMap<RecipeMatch, List<Long>>();

      for (RecipeMatch m : keySet()) {
         List<Recipe> recipes = get(m);

         if (get(m).isEmpty() && !includeEmptyLists) {
            continue;
         }

         List<Long> ids = new ArrayList<Long>();
         map.put(m, ids);

         for (Recipe recipe : recipes) {
            ids.add(recipe.getId());
         }
      }

      return map;
   }
}
