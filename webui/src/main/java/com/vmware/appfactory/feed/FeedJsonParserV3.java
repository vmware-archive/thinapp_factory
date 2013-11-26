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

package com.vmware.appfactory.feed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppIcon;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.common.AfHash;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * This class reads a "version 3" ThinAppFactory feed in JSON format.
 */
public class FeedJsonParserV3
   extends AbstractFeedParser
{
   @Override
   public Feed parse(JsonNode rootNode)
      throws FeedJsonFormatException
   {
      JsonNode descN = rootNode.get("description");
      JsonNode nameN = rootNode.get("name");
      JsonNode appsN = rootNode.get("applications");

      /* Create feed instance */
      Feed feed = new Feed();

      /* Feed description (optional) */
      if (descN != null) {
         feed.setDescription(parseText(descN));
      }

      /* Feed name (optional) */
      if (nameN != null) {
         feed.setName(nameN.getTextValue());
      }

      /* Application list (optional) */
      List<FeedApplication> apps = parseApplications(appsN);
      for (FeedApplication app : apps) {
         feed.addApplication(app);
      }

      return feed;
   }


   /**
    * Parse an application node from a JSON feed, format version 3.
    * If the node cannot be parsed, an exception is throw, else a valid
    * AfApplication is returned.
    *
    * @param appNode JSON node defining a feed application.
    */
   @Override
   public FeedApplication parseApplication(JsonNode appNode)
      throws FeedJsonFormatException
   {
      String[] reqNodeNames = { "name", "version", "download", "install" };
      Map<String, JsonNode> reqNodes = new HashMap<String, JsonNode>();

      for (String name : reqNodeNames) {
         JsonNode node = appNode.get(name);
         if (node == null) {
            throw new FeedJsonFormatException(
                  appNode,
                  "Application missing field \"" + name + "\"");
         }
         reqNodes.put(name, node);
      }

      /* Create application, with all required data */
      FeedApplication app = new FeedApplication();
      app.setName(reqNodes.get("name").getValueAsText());
      app.setVersion(reqNodes.get("version").getValueAsText());
      app.setDownload(parseAppDownloadOrArray(reqNodes.get("download")));
      app.setInstalls(parseAppInstallOrArray(reqNodes.get("install")));

      /* Description? */
      JsonNode descN = appNode.get("description");
      if (descN != null) {
         try {
            app.setDescription(parseText(descN));
         }
         catch(Exception ex) {
            throw new FeedJsonFormatException(
                  appNode,
                  "Invalid \"description\" for application \"" +
                  app.getDisplayName() + "\"",
                  ex);
         }
      }

      /* Categories? */
      JsonNode catsN = appNode.get("categories");
      if (catsN != null && catsN.isArray()) {
         Set<String> categories = new HashSet<String>();

         Iterator<JsonNode> it = catsN.iterator();
         while (it.hasNext()) {
            String name = it.next().getTextValue();
            categories.add(name);
         }

         app.setCategories(categories);
      }

      /* Vendor? */
      JsonNode vendorN = appNode.get("vendor");
      if (vendorN != null) {
         app.setVendor(vendorN.getTextValue());
      }

      /* Icons? */
      JsonNode iconsN = appNode.get("icons");
      if (iconsN != null && iconsN.isArray()) {
         List<AppIcon> icons = new ArrayList<AppIcon>();

         Iterator<JsonNode> it = iconsN.iterator();
         while (it.hasNext()) {
            AppIcon icon = parseIcon(it.next());
            icons.add(icon);
         }

         app.setIcons(icons);
      }

      /* EULA? */
      JsonNode eulaN = appNode.get("eula");
      if (eulaN != null) {
         app.setEula(parseText(eulaN));
      }

      /* Last remote update? */
      JsonNode updateN = appNode.get("lastRemoteUpdate");
      if (updateN != null && StringUtils.hasLength(updateN.getValueAsText())) {
         try {
            long when = AfCalendar.Parse(updateN.getValueAsText());
            app.setLastRemoteUpdate(when);
         }
         catch(Exception ex) {
            throw new FeedJsonFormatException(
               appNode,
               "Invalid \"lastRemoteUpdate\" for application \"" +
               app.getDisplayName() + "\" (" + ex.getMessage() + ")",
               ex);
         }
      }

      JsonNode localeN = appNode.get("locale");
      if (localeN != null && StringUtils.hasLength(localeN.getValueAsText())) {
         try {
            app.setLocale(localeN.getValueAsText());
         }
         catch(IllegalArgumentException ex) {
            throw new FeedJsonFormatException(
                  appNode,
                  "Invalid \"locale\" for application \"" +
                  app.getDisplayName() + "\" (" + ex.getMessage() + ")",
                  ex);
         }
      }

      JsonNode instRevNode = appNode.get("installerRevision");
      if (instRevNode != null && StringUtils.hasLength(instRevNode.getValueAsText())) {
         try {
            app.setInstallerRevision(instRevNode.getValueAsText());
         }
         catch(IllegalArgumentException ex) {
            throw new FeedJsonFormatException(
                  appNode,
                  "Invalid \"installerRevision\" for application \"" +
                  app.getDisplayName() + "\" (" + ex.getMessage() + ")",
                  ex);
         }
      }

      return app;
   }


   /**
    * Parse an application download node or array of nodes.
    * Note: We only support one download, so any additional ones are
    * silently discarded. No existing use cases have more than one file though.
    *
    * @param downloadNode
    * @return
    * @throws FeedJsonFormatException
    */
   private AppDownload parseAppDownloadOrArray(JsonNode downloadNode)
      throws FeedJsonFormatException
   {
      AppDownload download = null;

      if (downloadNode.isArray()) {
         /* Parse only the first array item */
         Iterator<JsonNode> it = downloadNode.iterator();
         if (it.hasNext()) {
            download = parseAppDownloadNode(it.next());
         }
      }
      else {
         /* Just a single download */
         download = parseAppDownloadNode(downloadNode);
      }

      return download;
   }


   /**
    * Parse a single "install" node.
    *
    * @param downloadNode
    * @return
    * @throws FeedJsonFormatException
    */
   private AppDownload parseAppDownloadNode(JsonNode downloadNode)
      throws FeedJsonFormatException
   {
      JsonNode urlN = downloadNode.get("url");
      JsonNode hashN = downloadNode.get("hash");

      /* Check for required fields */
      if (urlN == null) {
         throw new FeedJsonFormatException(
               downloadNode,
               "Download node is missing \"url\"");
      }

      /* Create the AfAppDownload instance */
      AppDownload download = new AppDownload();
      download.setLocation(urlN.getValueAsText());

      /* Hash (optional) */
      if (hashN != null) {
         AfHash hash = parseHash(hashN);
         download.setHash(hash);
      }

      return download;
   }


   /**
    * Parse an application install node or array of install nodes.
    *
    * @param installNode
    * @return
    * @throws FeedJsonFormatException
    */
   private List<AppInstall> parseAppInstallOrArray(JsonNode installNode)
      throws FeedJsonFormatException
   {
      List<AppInstall> installs = new ArrayList<AppInstall>();

      if (installNode.isArray()) {
         /* Parse each array item */
         Iterator<JsonNode> it = installNode.iterator();
         while (it.hasNext()) {
            installs.add(parseAppInstallNode(it.next()));
         }
      }
      else {
         /* Just a single download */
         installs.add(parseAppInstallNode(installNode));
      }

      return installs;
   }


   /**
    * Parse an application install node.
    *
    * @param installNode
    * @return
    * @throws FeedJsonFormatException
    */
   private AppInstall parseAppInstallNode(JsonNode installNode)
      throws FeedJsonFormatException
   {
      JsonNode commandN = installNode.get("command");

      /* Check for required fields */
      if (commandN == null) {
         throw new FeedJsonFormatException(
               installNode,
               "Install node is missing \"command\"");
      }

      /* Create the AfAppDownload instance */
      AppInstall install = new AppInstall();
      install.setCommand(commandN.getValueAsText());

      return install;
   }
}
