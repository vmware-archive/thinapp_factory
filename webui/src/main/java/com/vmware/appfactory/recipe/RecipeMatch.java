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

import org.apache.commons.lang.StringUtils;


/**
 * A value which is used to test how well a recipe matches an application.
 * Values must be declared in increasing order of precision.
 */
public enum RecipeMatch
{
   /**
    * There is no match: the recipe should not be used with this application.
    */
   none,

   /**
    * The recipe matches, but only because it contains no keys, or the best
    * matching key has no values specified (i.e. it is designed to match
    * all applications).
    */
   wild,

   /**
    * The recipe matches, but only because one of its keys was a partial match
    * i.e. one value of the key matched, but other values were not specified.
    */
   partial,

   /**
    * The recipe matches exactly; it contains at least one key that matches the
    * application on all fields.
    */
   precise;

   /**
    * Compare a string value from a recipe key to a string value from an
    * application, and return what kind of a match that is.
    * @param keyValue
    * @param appValue
    * @return
    */
   public static RecipeMatch match(String keyValue, String appValue)
   {
      if (StringUtils.isEmpty(keyValue) && StringUtils.isEmpty(appValue)) {
         /* Both empty: that's a precise match */
         return precise;
      }
      else if (StringUtils.isEmpty(keyValue) && StringUtils.isNotEmpty(appValue)) {
         /* Key is empty, value is not: that's a partial match */
         return partial;
      }
      else if (keyValue.equals(appValue)) {
         /* Both the same: that's a precise match */
         return precise;
      }

      return none;
   }


   public static RecipeMatch bestOf(RecipeMatch m1, RecipeMatch m2) {
      return m1.ordinal() > m2.ordinal() ? m1 : m2;
   }


   public static RecipeMatch worstOf(RecipeMatch m1, RecipeMatch m2) {
      return m1.ordinal() < m2.ordinal() ? m1 : m2;
   }
}
