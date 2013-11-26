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

package com.vmware.appfactory.recipe.dao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.common.ApplicationKey;
import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.appfactory.recipe.RecipeMatch;
import com.vmware.appfactory.recipe.RecipeMatches;
import com.vmware.appfactory.recipe.model.Recipe;

/**
 * Default implementation of the RecipeDaoImpl interface.
 * @see RecipeDao
 */
@Service
@Transactional
class RecipeDaoImpl
   extends AbstractDaoImpl<Recipe>
   implements RecipeDao
{
   private static final String FIND_BY_NAME_HQL =
      "from " + Recipe.class.getName() + " where _name = :name";

   @Override
   public Recipe findByName(String name)
   {
      List<?> list = getCurrentSession()
         .createQuery(FIND_BY_NAME_HQL)
         .setParameter("name", name)
         .list();

      return (list.isEmpty() ? null : (Recipe) list.get(0));
   }


   @Override
   public String findUniqueName(String initialGuess)
   {
      return findUniqueValue("name", initialGuess);
   }


   @Override
   public RecipeMatches findMatchesForApp(ApplicationKey application)
   {
      /* Initialize a results map */
      RecipeMatches matches = new RecipeMatches();

      // TODO: Make this more efficient!
      for (Recipe recipe : findAll()) {
         RecipeMatch match = recipe.matchToApplication(application);
         matches.get(match).add(recipe);
      }

      return matches;
   }
}
