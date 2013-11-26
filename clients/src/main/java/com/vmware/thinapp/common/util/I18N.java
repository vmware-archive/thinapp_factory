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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class that contains the translation helper functions.
 */
public abstract class I18N
{
   /**
    * Get the resource bundle for the given locale.
    *
    * @param locale
    * @return
    */
   public static ResourceBundle getResourceBundle(Locale locale) {
      return ResourceBundle.getBundle("messages", locale);
   }

   /**
    * Format a message using the named facility.
    *
    * @param locale
    * @param key
    * @param args
    * @return
    */
   public static String format(Locale locale, String key, Object... args) {
      return MessageFormat.format(translate(locale, key), args);
   }

   /**
    * Format a message using the named facility.
    *
    * @param locale
    * @param key
    * @return
    */
   public static String translate(Locale locale, String key) {
      String result = getTranslation(locale, key.toUpperCase());

      if (result == null) {
         result = locale.getDisplayName() + ": " + key;
      }
      return result;
   }

   /**
    * Return the translated text for the given key.
    * Note that ResourceBundle.getBundle caches the bundles it finds.
    *
    * @param locale Intended language
    * @param key Key to translate
    * @return Translation of key, or null
    */
   private static String getTranslation(Locale locale, String key) {
      try {
         ResourceBundle rb = ResourceBundle.getBundle("messages", locale);
         if (rb != null) {
            String translation = rb.getString(key);

            /* This allows translations to refer to other keys */
            if (translation != null && translation.startsWith("T.")) {
               translation = getTranslation(locale, translation);
            }

            return translation;
         }
      } catch (MissingResourceException ex) {
         /* Ignore */
      }

      return null;
   }
}
