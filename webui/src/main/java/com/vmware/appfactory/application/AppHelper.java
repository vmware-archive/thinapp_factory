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

package com.vmware.appfactory.application;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.springframework.util.StringUtils;

import com.vmware.appfactory.application.dto.ApplicationCreateRequest;
import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.datastore.DsUtil;
import com.vmware.appfactory.file.FileType;

/**
 * Helper utilities for the application package.
 *
 * This file is used for creating some of the most commonly used code.
 */
public class AppHelper
{
   /**
    * Digits regex pattern.
    */
   private static final Pattern FIND_DIGITS_PATTERN = Pattern.compile("\\d+");

   /**
    * Create an AppDownload instance from a given URL.
    *
    * @param urlStr a valid URL.
    * @return a single AppDownload.
    * @throws URISyntaxException
    */
   public static final AppDownload createDownload(String urlStr)
      throws URISyntaxException
   {
      final AppDownload download = new AppDownload();
      download.setURI(new URI(urlStr));
      return download;
   }


   /**
    * Create a list of AppInstall instances that are suitable as default
    * install commands for the specified filename.
    *
    * @param fileName - installer name with extension.
    * @return
    */
   public static List<AppInstall> createInstallsFromFile(final String fileName)
   {
      FileType fileType = FileType.parseType(fileName);
      String cmd = fileType.getDefaultInstallCommand(fileName);

      if (cmd != null) {
         List<AppInstall> list = new ArrayList<AppInstall>();
         list.add(new AppInstall(cmd));
         return list;
      }

      return null;
   }


   /**
    * Check the meta-data fields' differences between a given Application instance and
    * ApplicationCreateRequest.
    * Trim whitespace in a given ApplicationCreateRequets and check the
    * meta-data fields' differences between the application instance and
    * the ApplicationCreateRequest instance. It's case-sensitive check.
    *
    * @param app - an application instance.
    * @param request - an application create request.
    * @return true if the following fields are equal between two objects. Otherwise, return false.
    *  1. appName
    *  2. appVersion
    *  3. appRevision
    *  4. appVendor
    *  5. appLocale
    *  6. appInstallCommand (No whitespace check to allow all kinds of options)
    */
   public static boolean trimWhiteSpaceAndCheckDiff(Application app, ApplicationCreateRequest request)
   {
      if (app == null || request == null) {
         throw new IllegalArgumentException("app/request is null");
      }
      // Trim whitespace from request
      request.setAppName(StringUtils.trimWhitespace(request.getAppName()));
      request.setAppVersion(StringUtils.trimWhitespace(request.getAppVersion()));
      request.setAppVendor(StringUtils.trimWhitespace(request.getAppVendor()));
      request.setAppRevision(StringUtils.trimWhitespace(request.getAppRevision()));
      request.setAppLocale(StringUtils.trimWhitespace(request.getAppLocale()));

      String appInstall = (app.getInstall() == null ? "" : app.getInstall().getCommand());
      String requestInstall = request.getAppInstallCommandOption();

      // Check diff
      return !(app.getName().equals(request.getAppName())
            && app.getVersion().equals(request.getAppVersion())
            && app.getVendor().equals(request.getAppVendor())
            && app.getInstallerRevision().equals(request.getAppRevision())
            && app.getLocale().equals(request.getAppLocale())
            && appInstall.equals(requestInstall));
   }

   /**
    * Extract datastore id from the AppDownload URI.
    *
    * @param app an Application instance.
    * @return a datastore id or null if the input is invalid.
    */
   public static Long extractDatastoreIdFromDownloadUri(Application app) {
      if (app == null || app.getDownload() == null || app.getDownload().getURI() == null) {
         return null;
      }
      Matcher m = FIND_DIGITS_PATTERN.matcher(app.getDownload().getURI().toString());
      if (m.find()) {
         return Long.valueOf(m.group());
      } else {
         return null;
      }
   }

   /**
    * Replace internal datastore scheme with server path.
    *
    * @param in a datastore url starting with 'datastore://{id}/xxx/xx/x/installer.exe' scheme.
    * @param serverPath a share server path. E.g server/path
    * @return an empty string if the input is empty. Otherwise, a datastore url with
    *  actual server path. E.g server/path/xxx/xx/x/installer.exe
    */
   @Nonnull
   public static String replaceInternalDSSchemeWithServerPath(String in, String serverPath) {
      if (!StringUtils.hasLength(in)) {
         return "";
      }
      String out = in.replaceAll("\\\\", DsUtil.FILE_SEPARATOR);
      return out.replaceFirst(DsUtil.DATASTORE_URI_SCHEME + "://" + FIND_DIGITS_PATTERN, serverPath);
   }
}
