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

package com.vmware.appfactory.taskqueue.tasks.state.tasks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.AfFailure;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.feed.FeedJsonParser;
import com.vmware.appfactory.feed.dao.FeedDao;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.notification.Event.Component;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.recipe.model.RecipeFile;
import com.vmware.appfactory.taskqueue.exception.TaskException;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.FeedScanState;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * A task that will download JSON data from a feed's URL, and use that data to
 * update the applications inside the feed. This might cause applications to
 * be added to, remove from, or changed in the feed.
 */
class FeedScanTask extends FeedTask<
      FeedScanState,
      FeedScanState.Builder,
      FeedScanState.FeedScanStatus>
{
   @Override
   protected void doCleanup() throws TaskException {
      // nothing to do
   }

   enum Delta {
      add,
      delete,
      change
   }


   /**
    * Create a new instance for scanning for specified feed.
    * Nothing will be done until this task is added to the task queue.
    *
    * @param feed Feed to scan.
    * @param taskHelperFactory     Accessor to other factory objects.
    * @param conversionsQueue      Queue on which to place new conversion tasks.
    * @param maxConversionAttempts Number of times to attempt a conversion when auto-capturing
    *                              apps from this feed.  Must be at least 1.
    * @param conversionWorkpoolId  Workpool to use when auto-capturing apps from this feed.
    *                              If no workpool is available, pass -1, which will disable
    *                              auto-conversion.
    * @param conversionDatastoreId Datastore on which to place auto-captured apps from this feed.
    *                              Must be specified.  If no datastore is available, pass -1,
    *                              which will disable auto-conversion.
    * @param conversionRuntimeId   Runtime id to use when auto-capturing apps from
    *                              this feed.  Must be specified.
    */
   FeedScanTask(@Nonnull Feed feed,
                @Nonnull TaskHelperFactory taskHelperFactory,
                @Nonnull TaskQueue conversionsQueue,
                int maxConversionAttempts,
                long conversionWorkpoolId,
                long conversionDatastoreId,
                long conversionRuntimeId)
   {
      super(conversionsQueue,
            taskHelperFactory,
            new FeedScanState.Builder()
                  .withNewId(taskHelperFactory.getTaskIdSupplier())
                  .withStatus(FeedScanState.FeedScanStatus.waiting)
                  .withRecordId(feed.getId())
                  .withDescription("Scanning feed \"" + feed.getName() + '"')
                  .withMaxConverisonAttempts(maxConversionAttempts)
                  .withConverisonWorkpoolId(conversionWorkpoolId)
                  .withConversionDatastoreId(conversionDatastoreId)
                  .withConversionRuntimeId(conversionRuntimeId)
                  .build()
      );
   }


   @Override
   protected void runFeedTask(@Nonnull FeedDao feedDao, @Nonnull Feed feed)
      throws TaskException
   {
      long now = AfCalendar.Now();
      AfFailure failure = null;

      updateProgressAndStatus(0, FeedScanState.FeedScanStatus.scanning);
      try {
         /* tell the original feed that a scan is in progress */
         {
            AfFailure status = new AfFailure();
            status.setSummary(Feed.SCANNING);
            status.setDetails("The feed is currently being scanned.");
            feed.setFailure(status);

            feedDao.update(feed);
         }

         /* Parse JSON from the feed's URL */
         Feed newFeed = readFromUrl(feed.getUrl());

         /* Perform any required relative -> absolute URL conversions */
         calculateAbsoluteUrls(newFeed);

         /* Get an updated list of applications. */
         List<Delta> appDeltas = updateListInPlace(
               feed.getApplications(),
               newFeed.getApplications(),
               now,
               new AbstractApp.AppEquivalence<Application>());

         if (appDeltas == null) {
            /* Must have been aborted */
            markScanComplete(FeedScanState.FeedScanStatus.cancelled);
            return;
         }

         /* Make sure applications refer to parent feed */
         for (Application app : feed.getApplications()) {
            app.setDataSource(feed);
         }

         if (!appDeltas.isEmpty()) {
           _log.debug("Read " + appDeltas.size() +
                 " application deltas from "+ feed.getUrl());
           feed.setLastRemoteChange(now);
         }

         /* Get an updated list of recipes. */
         List<Delta> recipeDeltas = updateListInPlace(
               feed.getRecipes(),
               newFeed.getRecipes(),
               now,
               new Equivalence<Recipe>() {
                  @Override
                  protected boolean doEquivalent(Recipe a, Recipe b) {
                     return Objects.equal(a.getName(),b.getName());
                  }

                  @Override
                  protected int doHash(Recipe recipe) {
                     return recipe.getName().hashCode();
                  }
               });

         if (recipeDeltas == null) {
            /* Must have been aborted */
            markScanComplete(FeedScanState.FeedScanStatus.cancelled);
            return;
         }

         /* Make sure recipes refer to parent feed */
         for (Recipe recipe : feed.getRecipes()) {
            recipe.setDataSource(feed);
         }

         if (!recipeDeltas.isEmpty()) {
            _log.debug("Read " + recipeDeltas.size() +
                  " recipe deltas from "+ feed.getUrl());
            feed.setLastRemoteChange(now);
         }

         /* our scan was successful, so clear any error flag */
         feed.setFailure(null);

         /* Save any and all changes to the feed */
         feedDao.update(feed);

         // Cache feed icons locally: must be called after flushing changes to DB
         // since it needs access to app IDs
         cacheApplicationIcons(feed);

         _log.debug(feed + " is scanned (" +
               appDeltas.size() + " application deltas, " +
               recipeDeltas.size() + " recipe deltas)");
         markScanComplete(FeedScanState.FeedScanStatus.complete);
         feed.setLastScan(now);
         feedDao.update(feed);

          // only show a notification if there are changes!
          if (!appDeltas.isEmpty() || !recipeDeltas.isEmpty()) {
             NotificationService.INSTANCE.newInfoEvent(
                   appDeltas.size() + " application changes and " +
                   recipeDeltas.size() + " recipe changes found in " +
                   feed.getName() + " feed.",
                   Component.feeds);
          }

         if (feed.isOkToConvert()) {
            if (getCurrentTaskState().getConversionWorkpoolId() < 0) {
               _log.warn("Not auto-converting application becuase no default workpool exists");
            } else if (getCurrentTaskState().getConversionDatastoreId() < 0) {
               _log.warn("Not auto-converting application becuase no default datastore exists");
            } else {
               AppFactoryTask convTask = new FeedConvertTask(feed,
                                                             getTaskHelperFactory(),
                                                             _conversionsQueue,
                                                             getCurrentTaskState().getMaxConversionAttempts(),
                                                             getCurrentTaskState().getConversionWorkpoolId(),
                                                             getCurrentTaskState().getConversionDatastoreId(),
                                                             getCurrentTaskState().getConversionRuntimeId());
               _conversionsQueue.addTask(convTask);
            }
         }
      }
      catch (FileNotFoundException ex) {
         failure = new AfFailure();
         failure.setSummary("URL resource not found.");
         throw new TaskException(this, ex);
      }
      catch(UnknownHostException ex) {
         failure = new AfFailure();
         failure.setSummary("Unknown host " + ex.getMessage());
         throw new TaskException(this, ex);
      }
      catch (Exception ex) {
         _log.error("General feed scan error", ex);
         failure = new AfFailure(ex);
         throw new TaskException(this, ex);
      }
      finally {
         if (failure != null) {
            /* Mark task feedScanStatus as 'failed' */
            markScanComplete(FeedScanState.FeedScanStatus.failed);

            /* Update feedScanStatus in the ORIGINAL feed */
            Feed originalFeed = feedDao.find(feed.getId());
            if (originalFeed == null) {
               _log.warn("Feed named: {} ({}) no longer exists. Hence skipping scan process.", feed.getName(), feed.getId());
               return;
            }
            originalFeed.setFailure(failure);
            feedDao.update(originalFeed);

            /* Log the error */
            _log.error("Failed scan of feed \"" + feed.getName() +
                  "\": " + failure.getSummary());

            /* Record an event */
            NotificationService.INSTANCE.newErrorEvent(
                  feed.getName() + " feed scan failed.",
                  Component.feeds);
         }
      }
   }

   private void updateProgressAndStatus(
         final int pct,
         final FeedScanState.FeedScanStatus feedScanStatus) {

      updateState(new Function<FeedScanState.Builder, FeedScanState>() {
         @Override
         public FeedScanState apply(FeedScanState.Builder builder) {

            return builder.withStatus(feedScanStatus).withProgress(pct).build();
         }
      });
   }

   private void markScanComplete(FeedScanState.FeedScanStatus feedScanStatus) {
      updateProgressAndStatus(-1, feedScanStatus);
   }


   /**
    * Read the feed at the specified URL and update this feed to match.
    * This will change some feed attributes (most notably the applications) but
    * anything set by the user will remain untouched.
    */
   private static Feed readFromUrl(URL url)
      throws IOException, FeedJsonFormatException
   {
      /* Open the feed URL. */
      InputStream is = url.openStream();
      Feed feed;
      try {
         feed = FeedJsonParser.readStream(is);
      } finally {
         if (null != is) {
            is.close();
         }
      }

      feed.setUrl(url);
      return feed;
   }

   /**
    * Update an existing list of applications or recipes with the contents from
    * a new list. Items no longer in the new list are removed from the old list.
    * Items in the new list not present in the old list are added to it. Items
    * in both are updated, performing a deep copy from the new item into the old
    * item.
    *
    *
    * @param oldItems
    * @param newItems
    * @param now
    * @param fnEquals
    * an equivalence relation by which two apps or recipes
    * are considered "the same".  We use this instead of
    * the .equals() method, as we need looser semantics.
    *
    * @return A list of changes that were made, or null if the task looks
    * aborted.
    */
   private <T extends AbstractRecord> List<Delta> updateListInPlace(
           List<T> oldItems,
           List<T> newItems,
           long now,
           Equivalence<T> fnEquals)
   {
      float progressDelta = 100f / (oldItems.size() + newItems.size());
      float progress = 0f;
      List<Delta> deltas = new ArrayList<Delta>(Math.max(oldItems.size(),newItems.size()));

      /* Examine the items we know about already. */
      Iterator<T> it = oldItems.iterator();
      while (it.hasNext()) {
         T oldItem = it.next();

         // note: indexOf uses the object's equals() methods to determine if the
         // old and new object are the same.  When detecting changes from a feed,
         // we have to be more subtle than this, because:
         //  - the user might have changed the app's or recipe's metadata in the meantime
         //  - the feed might have changed the app's or recipe's metadata
         //
         // instead, use the passed-in equivalence function to determine if the
         // old and new record should be considered equivalent or not
         //
         T newItem = Iterables.find(newItems, fnEquals.equivalentTo(oldItem), null);

         if (null == newItem) {
            /* Old item no longer in the list: skip it */
            it.remove();
            deltas.add(Delta.delete);
            _log.debug("{} removed from feed", oldItem);
         }
         else {
            /* Old item still in the new list: update it */
            int numChanges = oldItem.deepCopy(newItem);
            if (numChanges > 0) {
               deltas.add(Delta.change);
               _log.debug("{} changed in feed", oldItem);
            }
         }

         progress += progressDelta;
         updateProgress(Math.round(progress));
         if (getCurrentTaskState().isAborted()) {
            return null;
         }
      }

      /* Look for new items we don't have yet */
      for (T newItem : newItems) {

         T oldItem = Iterables.find(oldItems, fnEquals.equivalentTo(newItem), null);
         if (null == oldItem) {
            /* A new item we don't have yet */

             // note: if we don't set lastModified, then the value
             //       will be 0 for all Applications in the table.
             //       If the user later adds a second feed without making
             //       any other changes, the second feed will also have
             //       timestamps of 0.  The timestamp here didn't change.
             //       Since the frontend uses the most recent modified timestamp
             //       to tell when it has more AJAX data to load, this means
             //       the user won't see it.
             //
            newItem.setModified(now);

            oldItems.add(newItem);
            deltas.add(Delta.add);
            _log.debug("{} is new in feed", newItem);
         }

         progress += progressDelta;
         updateProgress((int) progress);
         if (getCurrentTaskState().isAborted()) {
            return null;
         }
      }

      return deltas;
   }


   private static void calculateAbsoluteUrls(Feed feed)
      throws URISyntaxException
   {
      URI feedURI = AfUtil.parentUri((feed.getUrl().toURI()));

      /* Make sure every application has a URI */
      for (Application app : feed.getApplications()) {
         AppDownload download = app.getDownload();
         if (null != download) {
            if (download.getURI() == null) {
               download.setURI(AfUtil.relToAbs(download.getPath(), feedURI));
            }
         }
      }

      /* Make sure every recipe file has a URI */
      for (Recipe recipe : feed.getRecipes()) {
         for (RecipeFile file : recipe.getFiles()) {
            if (file.getURI() == null) {
               file.setURI(AfUtil.relToAbs(file.getPath(), feedURI));
            }
         }
      }
   }

   private void cacheApplicationIcons(Feed feed) {
      Preconditions.checkNotNull(feed);
      for (Application app : feed.getApplications()) {
         List<? extends AfIcon> icons = app.getIcons();
         if (icons != null) {
            for (int iconPos = 0; iconPos < icons.size(); iconPos++) {
               AfIcon icon = icons.get(iconPos);
               if (icon != null) {
                  AfUtil.IconInfo iconInfo = AfUtil.getIconInfo(icon.getUrl());
                  if (iconInfo != null) {
                     icon.setIconBytes(iconInfo.data);
                     icon.setContentType(iconInfo.contentType);

                     // Compute the md5 hash of the icon data
                     String iconHash = DigestUtils.md5Hex(iconInfo.data);
                     icon.setIconHash(iconHash);

                     // Compute the local URL for the icon resource
                     String localUrl = buildIconUrl("apps", app.getId(), iconPos, icon.getIconHash());
                     icon.setLocalUrl(localUrl);
                  }
               }
            }
         }
      }
   }
}
