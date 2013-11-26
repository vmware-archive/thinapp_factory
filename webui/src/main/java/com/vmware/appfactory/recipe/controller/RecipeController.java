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

package com.vmware.appfactory.recipe.controller;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;
import com.vmware.thinapp.common.util.AfJson;


/**
 * Handles all MVC requests for the recipe editors.
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Installers)
@RequestMapping(value="/recipes")
public class RecipeController
   extends AbstractUiController
{
   /**
    * Show a listing of all recipes in a table.
    *
    * @param request
    * @param locale
    * @param recipeId - currently selected recipe for the app, if any
    * @return
    */
   @RequestMapping(
         value={"","/index"},
         method=RequestMethod.GET)
   public ModelAndView recipeIndex(
         HttpServletRequest request,
         @RequestParam(required=false) Long appId,
         @RequestParam(required=false) String selectionData,
         @RequestParam(required=false) Long recipeId,
         Locale locale)
   throws AfBadRequestException
   {
      ModelMap mm = getBaseModel(request, locale, "T.RECIPES");
      mm.put("phases", ConversionPhase.values());
      mm.put("selectionData", selectionData);
      mm.put("recipeId", recipeId);

      // If picking up a recipe, load app info to display.
      handleAppLoadForRecipe(appId, mm);

      return new ModelAndView("recipes/recipe-index", mm);
   }


   /**
    * Create a recipe.
    *
    * @param request
    * @param locale
    * @return
    */
   @RequestMapping(
         value="/create",
         method=RequestMethod.GET)
   public ModelAndView createRecipe(
         HttpServletRequest request,
         @RequestParam(required=false) Long appId,
         @RequestParam(required=false) String selectionData,
         @RequestParam(required=false) Long recipeId,
         Locale locale)
   throws AfBadRequestException
   {
      ModelMap mm = getBaseModel(request, locale, "T.RECIPES.EDIT_RECIPE");
      mm.put("phases", ConversionPhase.values());
      mm.put("selectionData", selectionData);
      // recipeId is loaded here to indicate that the app page had a recipe selected, and to use on cancel.
      // Do not load this recipe here, this method creates a new recipe.
      mm.put("recipeId", recipeId);

      // If picking up a recipe, load app info to display.
      handleAppLoadForRecipe(appId, mm);

      return new ModelAndView("recipes/recipe-edit", mm);
   }


   /**
    * Edit a recipe.
    *
    * @param id
    * @param request
    * @param locale
    * @return
    * @throws AfBadRequestException
    */
   @RequestMapping(
         value="/edit/{id}",
         method=RequestMethod.GET)
   public ModelAndView editRecipe(
         @PathVariable Long id,
         HttpServletRequest request,
         @RequestParam(required=false) Long appId,
         @RequestParam(required=false) String selectionData,
         Locale locale)
      throws AfBadRequestException
   {
      if (id == null) {
         throw new AfBadRequestException("Invalid/missing recipe id");
      }

      ModelMap mm = getBaseModel(request, locale, "T.RECIPES.EDIT_RECIPE");
      mm.put("phases", ConversionPhase.values());
      // loadRecipeId indicates loading this recipe on the edit recipe page.
      mm.put("loadRecipeId", id);
      // recipeId simply indicates that the app page had a recipe selected, and to use when navigating back.
      mm.put("recipeId", id);
      mm.put("selectionData", selectionData);

      // If picking up a recipe, load app info to display.
      handleAppLoadForRecipe(appId, mm);

      return new ModelAndView("recipes/recipe-edit", mm);
   }


   /**
    * Load Application specific info that gets displayed on the recipe page.
    *
    * @param appId
    * @param mm
    * @throws AfBadRequestException
    */
   private void handleAppLoadForRecipe(Long appId, ModelMap mm) throws AfBadRequestException
   {
      if(appId == null) {
         return;
      }
      Application app = _daoFactory.getApplicationDao().find(appId);
      if (app == null) {
         throw new AfBadRequestException("Invalid application id: " + appId);
      }
      // Put this app and the JSON for its icons on the modelmap.
      mm.put("app", app);
      mm.put("appId", appId);

      try {
         // Note: Unfortunately we need a way to pass the list of icons to the client as JSON.
         // Rather than making a separate AJAX request for this, we can just embed the JSON in the output
         // of the velocity template and use that.
         // In an ideal world, we'd handle icon processing all server side, but that would require some reworking
         // of the icon caching code.
         mm.put("icons", AfJson.ObjectMapper().writeValueAsString(app.getIcons()));
      } catch(IOException ex) {
         _log.debug("Unable to generate JSON for list of application icons.");

         // Provide an empty list of icons instead
         mm.put("icons", "[]");
      }
   }
}