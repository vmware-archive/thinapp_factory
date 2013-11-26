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

package com.vmware.appfactory.inventory.controller;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.controller.AppApiController;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.build.dao.BuildDao;
import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.fileshare.dao.FileShareDao;
import com.vmware.appfactory.fileshare.model.FileShare;
import com.vmware.appfactory.inventory.InventoryItem;
import com.vmware.appfactory.taskqueue.tasks.MetaStatusPredicate;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * The "Inventory" controller provides the inventory for the user
 * interface. This includes all the main functional areas of the
 * application, as well as shortcuts to some common tasks (e.g. "Show
 * all running tasks") and data items (e.g. show feed "X").
 */
@Controller
public class InventoryApiController
   extends AbstractApiController
{
   /**
    * Returns the current inventory in JSON format.
    * @param locale
    * @return The inventory root
    * @throws Exception
    */
   @ResponseBody
   @RequestMapping(
         value = "/inventory",
         method = RequestMethod.GET)
   public InventoryItem getInventory(Locale locale)
      throws Exception
   {
      return createInventoryTree(locale);
   }


   /**
    * Get the AppFactory inventory that is used for top-level navigation in
    * the user interface. Note that the root item should not be displayed.
    * A 'tree' is created for each child of the inventory root.
    *
    * @return
    */
   private InventoryItem createInventoryTree(Locale locale)
   {
      InventoryItem root = new InventoryItem(
            tr(locale, "T.MISC.INVENTORY"),
            null);

      root.addChild(createDashboardInventory(locale));
      root.addChild(createSourcesInventory(locale));
      root.addChild(createAppInventory(locale));
      root.addChild(createRecipeInventory(locale));
      root.addChild(createBuildInventory(locale));
      root.addChild(createTaskInventory(locale));

      return root;
   }


   /**
    * Get the dashboard inventory item.
    * This is just a "leaf" item that links to the dashboard page.
    */
   private InventoryItem createDashboardInventory(Locale locale)
   {
      InventoryItem dashInv = new InventoryItem(
            tr(locale, "T.DASHBOARD"),
            "/dashboard/index");

      return dashInv;
   }


   /**
    * Get the inventory of Application sources.
    * This provides links to the parent feeds tree and file shares tree.
    *
    * -Applications Sources
    * --File Shares
    * ----x
    * --Feeds
    * ----x
    *
    * @return a parent inventory item called "Application Sources", with
    * two sub trees: "File Shares" and "Feeds".
    */
   private InventoryItem createSourcesInventory(Locale locale)
   {
      InventoryItem sourcesInv = new InventoryItem(
            tr(locale, "T.DATA_SOURCES"),
            "/sources/index");

      sourcesInv.addChild(createFeedInventory(locale));
      sourcesInv.addChild(createFileShareInventory(locale));

      return sourcesInv;
   }

   /**
    * Get the inventory of File Shares, with a link to each file share.
    *
    * --File Shares
    * ----x
    * ----y
    *
    * @return a parent inventory of "File Shares".
    */
   private InventoryItem createFileShareInventory(Locale locale)
   {
      final InventoryItem fsInv = new InventoryItem(
            tr(locale, "T.FILE_SHARES"),
            "/fileshare/index");

      /* Get all file share, create a sub tree */
      FileShareDao fileShareDao = _daoFactory.getFileShareDao();
      final List<FileShare> fileshareList = fileShareDao.findAll();
      fsInv.setCount(fileshareList.size());

      for (FileShare fs : fileshareList) {
         String name = fs.getName();
         InventoryItem fileShareItem = new InventoryItem(
               name,
               "/fileshare/edit/" + fs.getId());
         fsInv.addChild(fileShareItem);
      }

      return fsInv;
   }

   /**
    * Get the inventory of Feeds, with a link to each feed.
    *
    * --Feeds
    * ----x
    * ----y
    *
    * @return a parent inventory of "Feeds".
    */
   private InventoryItem createFeedInventory(Locale locale)
   {
      /* Get all feeds based on the config flag, sort them */
      final List<Feed> feeds = super.getFeedsList();
      Collections.sort(feeds);

      InventoryItem feedInv = new InventoryItem(
            tr(locale, "T.FEEDS"),
            "/feeds/index");
      feedInv.setCount(feeds.size());

      for (Feed feed : feeds) {
         String name = feed.getName();
         feedInv.addChild(
               new InventoryItem(
                     name,
                     "/feeds/edit/" + feed.getId()));
      }

      return feedInv;
   }

   /**
    * Get the inventory of all applications.
    * This provides links to the applications index, plus a link to each
    * category.
    *
    * @return A parent inventory item called "Applications", with a child
    * item per category.
    */
   private InventoryItem createAppInventory(Locale locale)
   {
      /* Top level item: no view */
      InventoryItem appInv = new InventoryItem(
            tr(locale, "T.APPS"),
            null);

      /* Need to show this so that user can do 'Add Application' when there were no apps */
      InventoryItem allAppsItem = new InventoryItem(
            tr(locale, "T.APPS.ALL"),
            "/apps/index");
      appInv.addChild(allAppsItem);

      /* Get all applications, sort them */
      ApplicationDao appDao = _daoFactory.getApplicationDao();
      List<Application> apps = appDao.findAllIncluded();

      if(!CollectionUtils.isEmpty(apps)) {
         allAppsItem.setCount(apps.size());

         Collections.sort(apps);

         /* All unique categories */
         boolean emptyCat = false;
         Set<String> cats = new TreeSet<String>();
         for (Application app : apps) {
            Set<String> appCats = app.getCategories();
            if (appCats.isEmpty()) {
               emptyCat = true;
            }
            else {
               cats.addAll(appCats);
            }
         }

         /* Make child node per category */
         for (String cat : cats) {
            allAppsItem = new InventoryItem(
                  cat,
                  AfUtil.escapeUrlPath("/apps/index", "category=" + cat));
            appInv.addChild(allAppsItem);
         }

         /* Create item for applications with no categories */
         if (emptyCat) {
            allAppsItem = new InventoryItem(
                  "(" + tr(locale, "T.APPS.NO_CATEGORY") + ")",
                  "/apps/index?category=" + AppApiController.NO_CATEGORY_REQUEST);
            appInv.addChild(allAppsItem);
         }
      }

      return appInv;
   }


   /**
    * Get the inventory of tasks.
    *
    * @return
    */
   private InventoryItem createTaskInventory(Locale locale)
   {
      /* Top level item: no view */
      InventoryItem taskInv = new InventoryItem(
            tr(locale, "T.TASKS"),
            null);

      /* Item for all tasks */
      InventoryItem all =
         new InventoryItem(
               tr(locale, "T.TASKS.ALL"),
               "/tasks/index");
      taskInv.addChild(all);

      /* Items per meta-status */
      int total = 0;
      for (TaskQueue.MetaStatus meta : TaskQueue.MetaStatus.values()) {
         if (TaskQueue.MetaStatus.INIT.equals(meta)) {
            // no tasks should ever be in the INIT state, other than for
            // the very brief time between when they're created and then
            // added to the task queue.
            //
            // The UI can't see any tasks that aren't in the task queue,
            // so don't show a header section for it.
            //
            // See bug 822193 for more info.
            continue;
         }
         InventoryItem child = new InventoryItem(
               tr(locale, "T.TASKS.METASTATUS." + meta.name()),
               "/tasks/index?metaStatus=" + meta.name());

         int count = Iterables.size(_conversionsQueue.getTasks(MetaStatusPredicate.custom(meta)));
         total += count;

         if (meta == TaskQueue.MetaStatus.RUNNING && count > 0) {
            child.setCount(count);
         }

         taskInv.addChild(child);
      }

      if (total > 0) {
         all.setCount(total);
      }

      return taskInv;
   }


   /**
    * Get the inventory of builds
    *
    * This includes child items to show builds arranged in certain ways,
    * such as all builds, builds grouped by application, etc.
    *
    * @return
    */
   private InventoryItem createBuildInventory(Locale locale)
   {
      /* Top level item: no view */
      InventoryItem item = new InventoryItem(
            tr(locale, "T.BUILDS"),
            null);

      item.addChild(
            new InventoryItem(
                  tr(locale, "T.BUILDS_BY_APP"),
                  "/builds/index?view=byapp"));

      BuildDao buildDao = _daoFactory.getBuildDao();
      for (Build.Status s : Build.Status.values()) {
         int count = buildDao.findForStatus(s).size();
         item.addChild(
               new InventoryItem(
                     tr(locale, "T.BUILDS." + s.name()),
                     "/builds/index?status=" + s.name(),
                     count));
      }

      item.addChild(
            new InventoryItem(
                  tr(locale, "T.BUILDS.ALL"),
                  "/builds/index"));

      return item;
   }


   /**
    * Create a new "Recipe" inventory item.
    */
   private InventoryItem createRecipeInventory(Locale locale)
   {
      /* Top level item: all recipes */
      InventoryItem dsInv = new InventoryItem(
            tr(locale, "T.RECIPES"),
            "/recipes/index");

      return dsInv;
   }
}
