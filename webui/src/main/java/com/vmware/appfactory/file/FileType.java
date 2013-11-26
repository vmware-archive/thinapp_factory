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

package com.vmware.appfactory.file;

import org.springframework.util.StringUtils;

import com.vmware.thinapp.common.util.AfConstant;

/**
 * This enum defines all supported file types that Manual mode use.

 * @since v1.0 6/1/2011
 */
public enum FileType {
   /**
    * Windows executable files.
    */
   exe(".exe") {
      @Override
      public String getDefaultInstallCommand(String fileName) {
         return "$" + AfConstant.APPFILE_VARIABLE;
      }
   },

   /**
    * MSI installer files.
    */
   msi(".msi") {
      @Override
      public String getDefaultInstallCommand(String fileName) {
         return "msiexec /i " + fileName;
      }
   },

   /**
    * MSI templates.
    */
   mst(".mst"),

   /**
    * ISO file archives.
    */
   iso(".iso"),

   unknown((String[]) null);


   /**
    * Parse to an enum type from string filename's extension.
    * @param filename - a filename to be parsed.
    * @return an enum of AfFileType
    */
   public static FileType parseType(String filename) {
      if (!StringUtils.hasLength(filename)) {
         return unknown;
      }

      for (FileType type : values()) {
         if (type._extensions != null) {
            for (String ext : type._extensions) {
               if (filename.toLowerCase().endsWith(ext)) {
                  return type;
               }
            }
         }
      }

      return unknown;
   }


   /**
    * By default, a file has no default install command. Each enum value should
    * override this if it can supply one.
    * @param fileName
    * @return
    */
   public String getDefaultInstallCommand(String fileName) {
      return null;
   }


   private String[] _extensions;


   private FileType(String... extensions) {
      _extensions = extensions;
   }
}
