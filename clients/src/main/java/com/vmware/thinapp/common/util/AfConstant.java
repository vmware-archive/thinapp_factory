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

package com.vmware.thinapp.common.util;

/**
 * This class defines all global constant variables.
 */
public final class AfConstant {
   /**
    * text/plain MIME type
    */
   public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

   /**
    * image/png MIME type
    */
   public static final String MIME_TYPE_IMAGE_PNG = "image/png";

   /**
    * Environment-specific file separator.
    */
   public static final String FILE_SEPARATOR = System.getProperty("file.separator");

   /**
    * a directory to store all generated feeds
    */
   public static final String JSON_DIR = "data";

   /**
    * a json file extension.
    */
   public static final String JSON_FILE_EXTENSION = ".json";

   /**
    * A recipe file extension.
    */
   public static final String RECIPE_FILE_EXTENSION = ".recipe";

   /**
    * Static shared variable for deployed web context real absolute path.
    */
   public static String WEB_CONTEXT_REAL_PATH = null;

   /**
    * Expired license http error code
    */
   public static final int EXPIRED_LICENSE_HTTP_CODE = 420;

   /**
    * Built-in variable used to refer to the application installer file itself.
    * Used in recipes and application install commands.
    */
   public static final String APPFILE_VARIABLE = "appfile";

   /**
    * Regex used to validate simple variable names, such as those used
    * in recipes.
    */
   public static final String VARIABLE_NAME_REGEX = "^[A-Za-z]+[A-Za-z0-9_]*$";

   /**
    * Constant value for a message bundle key representing Not available.
    */
   public static final String NOT_AVAIL = "T.COMMON.NOT_AVAILABLE";

   /** This constant is a translate key for displaying no workpools created. */
   public static final String NO_WORKPOOL = "T.WORKPOOL.NONE";

   /** This constant is a translate key for displaying horizon enabled message. */
   public static final String RUNTIME_HORIZON_ENABLE = "T.RUNTIME.HORIZON_ENABLE_DESC";

   /** This constant identifies the select optgroup for horizon enabled runtime */
   public static final String RUNTIME_GROUP_HORIZON = "runtimeGroupHorizon";

   private AfConstant() {
      //Dummy do nothing and not accessible.
   }
}
