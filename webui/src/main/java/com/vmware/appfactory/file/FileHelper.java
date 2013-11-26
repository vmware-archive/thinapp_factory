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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharSetUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * Utility helper methods to handle file system. Provides a set of wrapper
 * functions around the java.io.File API
 *
 * TODO: See what {@link FileUtils} can do for us instead.
 *
 * @author Keerthi Singri
 */
public class FileHelper
{

   /** The list of characters that are invalid for file or directory names in
    * unix, win and max osx.
    *
    * '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\','<', '>', '|', '\"', ':'
    */
   private static final String ILLEGAL_FILE_NAME_CHARS = "/\n\r\t\0\f`?*\\<>|\":";

   /**
    * Default directory layout mapping for application info (meta-data)
    */
   public static final String DEFAULT_APPLICATION_DIR_LAYOUT = "vendor/name/version/locale/revision";

   /**
    * Directory to upload application installers.
    */
   public static final String UPLOAD_DIR = "upload";

   /**
    * Remove characters that may result in invalid file names in win,
    * unix or osx systems.
    *
    * @param fileName - file name
    * @return
    */
   public static String purgeInvalidCharFromFileName(String fileName) {
      if (StringUtils.isEmpty(fileName)){
         return fileName;
      }
      return CharSetUtils.delete(fileName, ILLEGAL_FILE_NAME_CHARS);
   }


   /**
    * Remove characters that may result in invalid directory name in win,
    * unix or osx systems.
    *
    * @param dirName - directory Name
    * @see #FileHelper.parseDirs(String, Map)
    * @return - FileName that
    */
   public static String purgeInvalidCharFromDirName(String dirName) {
      if (StringUtils.isEmpty(dirName)){
         return dirName;
      }
      return CharSetUtils.delete(dirName, ILLEGAL_FILE_NAME_CHARS + " ");
   }

   /**
    * Create a list of directory based on the given layout and name-value meta-data pairs.
    * @param dirLayout - a valid TAF file share supported directory layout.
    * @param metaMap - a meta-data map.
    * @return a list of directory.
    */
   public static List<String> parseDirs(String dirLayout, Map<String, String> metaMap) {
      if (StringUtils.isEmpty(dirLayout) || CollectionUtils.isEmpty(metaMap)) {
         return null;
      }

      final String[] metanames = dirLayout.split("/");
      final List<String> dirs = new ArrayList<String>();

      for (String name : metanames) {
         String value = metaMap.get(name);
         if (!StringUtils.isEmpty(value)) {
            dirs.add(purgeInvalidCharFromDirName(value));
         }
      }

      return dirs;
   }

   /**
    *
    * @param name
    * @return
    */
   public static boolean fileExists(String name){
      if (StringUtils.isEmpty(name)) {
         return false;
      }
      File file = new File(name);
      return file.exists();
   }


   /**
    * Given a basePath and a list of directory names, check if these
    * construct into a path and if that path exists.
    *
    * @param basePath
    * @param dirNameList
    * @param separator - fileSeperator to be used.
    * @return - true if dir exists, false otherwise/if one of the inputs is null.
    */
   public static boolean dirExists(
         String basePath,
         char separator,
         String... dirNameList)
   {
      File file = new File(constructFilePath(basePath, separator, dirNameList));
      return file.isDirectory();
   }

   /**
    * Given a basePath and a list of directory names, check if these
    * construct into a path and if that path exists.
    *
    * Uses default system specific default pathSeparator
    *
    * @see #FileHelper.dirExists(String, char, String...)
    */
   public static boolean dirExists(String basePath, String... dirNameList)
   {
      return dirExists(basePath, File.separatorChar, dirNameList);
   }


   /**
    * Check if a directory contains files in them.
    *
    * @param dirPath
    * @return
    */
   public static boolean filesExistInDir(String dirPath)
   {
      File file = new File(dirPath);
      if (file.isDirectory()) {
         return (file.list().length != 0);
      }
      return false;
   }


   /**
    * Kind of like constructFilePath(), but without requiring a basePath
    * first.
    *
    * FIXME: Fixing constructFilePath() is too much refactoring for me
    * right now, so do it later.
    *
    * @param separator
    * @param pathComponents
    * @return
    */
   public static String constructFilePath2(
         String separator,
         String... pathComponents)
   {
      String result = "";

      for (String dirName : pathComponents) {
         if (StringUtils.isEmpty(dirName)) {
            continue;
         }

         // Append file separator if not exists.
         if (!result.endsWith(separator) && !dirName.startsWith(separator)) {
            result += separator;
         }
         result += dirName;
      }

      return result;
   }


   /**
    * Concatenate the list of directory names that are passed to the basePath
    * and creates the directory path string.
    *
    * If one if the input params is null, skip it.
    *
    * @param basePath
    * @param dirNameList
    * @param separator - fileSeperator to be used.
    * @return - dirPath.
    */
   public static String constructFilePath(
         final String basePath,
         char separator,
         String... dirNameList)
   {
      // Concatenate dir names with necessary file separators.
      String curDir = basePath;
      for (String dirName : dirNameList) {
         if(StringUtils.isEmpty(dirName)) {
            return null;
         }
         // Append file separator if not exists.
         if (curDir.charAt(curDir.length()-1) != separator){
            curDir += separator;
         }
         curDir += dirName;
      }
      return curDir;
   }

   /**
    * Concatenate the list of directory names that are passed to the basePath
    * and creates the directory path string.
    *
    * Uses default system specific default pathSeparator
    *
    * @see #FileHelper.dirExists(String, char, String...)
    */
   public static String constructFilePath(String basePath, String... dirNameList)
   {
      return constructFilePath(basePath, File.separatorChar, dirNameList);
   }


   /**
    * Creates if any missing directories that can be constructed from the
    * input of basePath and dirNameList.
    *
    * @see #FileHelper.constructFilePath(String, String...)
    *
    * @param basePath
    * @param dirNameList
    * @param separator - fileSeperator to be used.
    * @return the create file, null otherwise.
    * @throws IOException
    */
   public static File createDirIfNotExists(
         final String basePath,
         char separator,
         String... dirNameList)
      throws IOException
   {
      String dirPath = constructFilePath(basePath, separator, dirNameList);

      if( dirPath == null) {
         return null;
      }

      final File dirs = new File(dirPath);
      if (!dirs.exists()) {
         FileUtils.forceMkdir(dirs);
      }

      return dirs;
   }

   /**
    * Creates if any missing directories that can be constructed from the
    * input of basePath and dirNameList.
    *
    * Uses default system specific default pathSeparator
    *
    * @see #FileHelper.constructFilePath(String, String...)
    * @see #FileHelper.dirExists(String, char, String...)
    */
   public static File createDirIfNotExists(
         String basePath,
         String... dirNameList)
      throws IOException
   {
      return createDirIfNotExists(basePath, File.separatorChar, dirNameList);
   }

}
