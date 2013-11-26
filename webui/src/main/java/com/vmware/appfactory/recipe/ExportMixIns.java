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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * When recipes are exported to ZIP files, we want to exclude the "internal"
 * fields used by TAF and leave only those fields which are publicly
 * documented. This will help prevent confusion when users export a recipe and
 * then edit the JSON by hand.
 *
 * The are several ways to achieve this. The simplest way, which involves no
 * change to existing code, is to use a Jackson serializer mix-in. The mix-ins
 * for all recipe classes are defined here.
 *
 * NOTE: Fields that are inherited from AbstractRecord ("id", "created", etc)
 * appear in every mix-in. If you can find a shortcut, you are better than me.
 */
public abstract class ExportMixIns
{
   /**
    * Mix-in for the {@link Recipe} class.
    */
   @JsonIgnoreProperties({
      "readOnly",
      "id",
      "created",
      "createdElapsed",
      "modified",
      "modifiedElapsed",
      "dataSourceName",
      "dataSourceType"})
   public static class Recipe
   {
      /* Empty */
   }

   /**
    * Mix-in for the {@link RecipeFile} class.
    */
   @JsonIgnoreProperties({
      "id",
      "created",
      "createdElapsed",
      "modified",
      "modifiedElapsed",
      "absoluteUrl"})
   @JsonSerialize(include=Inclusion.NON_NULL)
   public static class RecipeFile
   {
      /* Empty */
   }

   /**
    * For recipe classes that have no specific fields of their own to ignore,
    * but inherit from {@link AbstractRecord}, apply this mix-in.
    */
   @JsonIgnoreProperties({
      "id",
      "created",
      "createdElapsed",
      "modified",
      "modifiedElapsed"})
   public static class Record
   {
      /* Empty */
   }
}
