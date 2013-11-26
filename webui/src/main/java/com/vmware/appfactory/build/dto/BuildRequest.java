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

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.vmware.appfactory.build.controller.BuildApiController;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.recipe.RecipeMatch;
import com.vmware.appfactory.taskqueue.dto.CaptureRequest;
import com.vmware.appfactory.taskqueue.dto.CaptureRequestImpl;


/**
 * This class is used in two ways. Firstly, when the user wants to convert some
 * applications, we provide a list of build requests, one per application, that
 * have some suggested settings already set. Secondly, after the user has
 * chosen the options they want, build requests are submitted to the server and
 * conversion tasks are created from them.
 *
 * @see BuildApiController#getDefaultBuildDefinitions
 * @see BuildApiController#buildApplications
 */
public class BuildRequest
{
   /** Recipes which match this application */
   private Map<RecipeMatch, List<Long>> _recipeMatches;

   /** Icons of the application to build */
   private List<? extends AfIcon> _icons;

   /** Entities related to ThinApp conversion request */
   private CaptureRequest _captureRequest;


   /**
    * Default constructor
    */
   public BuildRequest()
   {
      _captureRequest = new CaptureRequestImpl();
   }


   public BuildRequest(
         List<? extends AfIcon> icons,
         CaptureRequest captureRequest,
         Map<RecipeMatch, List<Long>> recipeMatches)
   {
      this._icons = icons;
      this._captureRequest = captureRequest;
      this._recipeMatches = recipeMatches;
   }

   /**
    * @return the _recipeMatches
    */
   public Map<RecipeMatch, List<Long>> getRecipeMatches()
   {
      return _recipeMatches;
   }


   /**
    * @param recipeMatches the _recipeMatches to set
    */
   public void setRecipeMatches(Map<RecipeMatch, List<Long>> recipeMatches)
   {
      this._recipeMatches = recipeMatches;
   }


   /**
    * @return the _icons
    */
   public List<? extends AfIcon> getIcons()
   {
      return _icons;
   }


   /**
    * @param icons the _icons to set
    */
   public void setIcons(List<? extends AfIcon> icons)
   {
      this._icons = icons;
   }


   /**
    * @return the _captureRequest
    */
   public CaptureRequest getCaptureRequest()
   {
      return _captureRequest;
   }


   /**
    * @param captureRequest the _captureRequest to set
    */
   @JsonDeserialize(as=CaptureRequestImpl.class)
   public void setCaptureRequest(CaptureRequest captureRequest)
   {
      _captureRequest = captureRequest;
   }
}
