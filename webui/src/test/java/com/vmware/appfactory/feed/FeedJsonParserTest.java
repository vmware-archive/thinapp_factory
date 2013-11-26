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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import com.vmware.appfactory.application.model.AppDownload;
import com.vmware.appfactory.application.model.AppIcon;
import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.feed.exception.FeedJsonFormatException;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.thinapp.common.util.AfJson;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests reading in feeds.
 */
public class FeedJsonParserTest {

   @Test
   public void testReadStream() throws Exception {
      {
         Feed feedV4 = loadFromResource("/feeds/ninite-v4.json");
         assertNumAppsAndRecipes(feedV4, 88, 0);

         Application appExpected = createExpectedApp(feedV4);
         Application app = feedV4.getApplications().get(0);
         Assert.assertEquals(appExpected, app);

         // the next one better be different, though
         Application otherApp = feedV4.getApplications().get(1);
         assertThat(otherApp, is(not(equalTo(appExpected))));

         // change appExpected and make sure it's not equal
         appExpected.setLastRemoteUpdate(-1L);
         assertThat(app, is(not(equalTo(appExpected))));

         testRoundTripThroughJson(feedV4,Feed.class);
         testRoundTripThroughJson(app,Application.class);
         testRoundTripThroughJson(otherApp,Application.class);
      }

      {
         Feed feedV3 = loadFromResource("/feeds/ninite-v3.json");
         assertNumAppsAndRecipes(feedV3, 88, 0);

         Application appExpected = createExpectedApp(feedV3);
         Application app = feedV3.getApplications().get(0);
         Assert.assertEquals(appExpected, app);

         // the next one better be different, though
         Application otherApp = feedV3.getApplications().get(1);
         assertThat(otherApp , is(not(equalTo(appExpected))));

//         testRoundTripThroughJson(feedV3,Feed.class);
//         testRoundTripThroughJson(app,Application.class);
//         testRoundTripThroughJson(otherApp,Application.class);
      }

      {
         Feed feedOffice = loadFromResource("/feeds/office2010.json");
         assertNumAppsAndRecipes(feedOffice, 0, 1);
//         testRoundTripThroughJson(feedOffice,Feed.class);
      }

      {
         Feed feedOffice = loadFromResource("/feeds/office2007.json");
         assertNumAppsAndRecipes(feedOffice, 0, 1);
//         testRoundTripThroughJson(feedOffice,Feed.class);
      }

      {
         Feed feedViewClient = loadFromResource("/feeds/view-client.json");
         assertNumAppsAndRecipes(feedViewClient, 1, 1);
//         testRoundTripThroughJson(feedViewClient,Feed.class);
      }
   }

   private static Application createExpectedApp(Feed feedV4) {
      AppDownload appDownload = new AppDownload();
      appDownload.setURI(URI.create("https://ninite.com/api/installer?app=chrome&key=vmware"));

      AppInstall appInstall = new AppInstall("%D /nocache /silent /disableautoupdate");

      AppIcon appIcon = new AppIcon();
      appIcon.setUrl("http://ninite.com/static/app_icons/chrome2.png");
      appIcon.setContentType("image/png");
      appIcon.setSize(32);

      Application appExpected = new Application();
      appExpected.setEula(new AfText());
      appExpected.setDownload(appDownload);
      appExpected.setInstalls(appInstall);
      appExpected.setLastRemoteUpdate(1325822825000L);
      appExpected.setSkipped(false);
      appExpected.addIcon(appIcon);
      appExpected.setDataSource(feedV4);
      appExpected.setOverrideMetadata(false);
      appExpected.setFailCount(0);
      appExpected.setBuildRequestsTotal(0);
      appExpected.setName("Chrome");
      appExpected.setVersion("16.0.912.75");
      appExpected.setLocale("");
      appExpected.setInstallerRevision("");
      appExpected.setVendor("Google");
      appExpected.setArchitecture(AbstractApp.Architecture.any);
      appExpected.setDescription(
            AfText.plainTextInstance("Fast Browser by Google 16.0.912.75")
      );
      appExpected.setCategoriesEncoded("Web Browsers:");
      appExpected.setId(null);
      appExpected.setCreated(0L);
      appExpected.setModified(0L);

      return appExpected;
   }

   @Test
   public void testInvalidFeeds() throws Exception {
      try {
         loadFromResource("/feeds/not-a-feed.json");
         fail("Invalid feed input did not cause error");
      } catch (FeedJsonFormatException e) {
         // expected
      }

      try {
         loadFromResource("/feeds/not-json.html");
         fail("Invalid feed input did not cause error");
      } catch (FeedJsonFormatException e) {
         // expected
      }
   }

   private static void assertNumAppsAndRecipes(Feed feed, int numApps, int numRecipes) {

      assertNotNull(feed);

      assertEquals("wrong number of applications for feed "
                   + feed.getName(),
                   numApps, feed.getApplications().size());

      assertEquals("wrong number of recipes for feed "
                   + feed.getName(),
                   numRecipes, feed.getRecipes().size());

      assertEquals("recipes list size did not match getNumRecipes for feed "
                   + feed.getName(),
                   numRecipes, feed.getRecipes().size());
   }

   private Feed loadFromResource(String resource)
         throws IOException, FeedJsonFormatException {
      InputStream in = getClass().getResourceAsStream(resource);
      try {
         return FeedJsonParser.readStream(in);
      } finally {
         if (null != in) {
            in.close();
         }
      }
   }

   private static <T> void testRoundTripThroughJson(T obj, Class<T> klass)
         throws IOException {

      ObjectMapper mapper = AfJson.ObjectMapper();
      String str = mapper.writeValueAsString(obj);
      T newObj = mapper.readValue(str, klass);
      assertEquals(obj, newObj);
   }
}
