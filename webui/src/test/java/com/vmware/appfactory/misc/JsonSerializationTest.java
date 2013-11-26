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

package com.vmware.appfactory.misc;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.datastore.DsDatastoreCifs;
import com.vmware.appfactory.feed.dto.FeedRequest;
import com.vmware.appfactory.feed.model.Feed;
import com.vmware.appfactory.fileshare.dto.ApplicationInfoDelta;
import com.vmware.appfactory.fileshare.dto.FileShareRequest;
import com.vmware.thinapp.common.datastore.dto.Datastore;
import com.vmware.thinapp.common.util.AfJson;

import static org.junit.Assert.*;

/**
 * Tests that the main classes we use for:
 *  - our public REST API
 *  - calling the converter (back-end) REST API
 *
 * can be serialized through JSON, and wind up
 * the same on the other end.
 */
public class JsonSerializationTest {

   @Test
   public void testFeed() throws IOException {
      Feed feed = new Feed("name",
                           AfText.plainTextInstance("description"),
                           true,
                           false);
      testRoundTripThroughJson(feed, Feed.class);
   }

   @Test
   public void testFeedRequest() throws IOException {
      FeedRequest feedRequest = new FeedRequest();
      feedRequest.appIncludes = ImmutableMap.of(123L, true, 456L, false);
      feedRequest.name = "name";
      feedRequest.description = "description";
      feedRequest.authPassword = "password";
      feedRequest.authRequired = true;
      feedRequest.authUsername = "username";
      feedRequest.okToConvert = true;
      feedRequest.okToScan = true;
      feedRequest.realUrl = new URL("http://taf.your.company.com");
      feedRequest.url = "http://your.company.com";

      testRoundTripThroughJson(feedRequest, FeedRequest.class);
   }

   @Test
   public void testFileshare() throws IOException {
      ApplicationInfoDelta applicationInfoDelta = new ApplicationInfoDelta();
      applicationInfoDelta.setInstallCommand("installCommand");
      applicationInfoDelta.setKey("key");
      applicationInfoDelta.setLang("lang");
      applicationInfoDelta.setName("name");
      applicationInfoDelta.setRevision("revision");
      applicationInfoDelta.setVendor("vendor");
      applicationInfoDelta.setVersion("version");

      FileShareRequest fsRequest = new FileShareRequest();
      fsRequest.setServerPath("\\\\share\\path");
      fsRequest.setAppDeltas(ImmutableList.of(applicationInfoDelta));
      fsRequest.setAppsToSkip(ImmutableList.of("this","that"));
      fsRequest.setAuthPassword("authPassword");
      fsRequest.setAuthRequired(true);
      fsRequest.setAuthUsername("authUsername");
      fsRequest.setDescription("test description");
      fsRequest.setFileShareId(123L);
      fsRequest.setName("test name");
      fsRequest.setOkToConvert(true);

      testRoundTripThroughJson(fsRequest, FileShareRequest.class);
   }

   @Test
   public void testDsDatastore() throws IOException {
      Datastore ds = new DsDatastoreCifs(
            "dataStoreName",
            "Server",
            "\\\\path\\to\\share",
            "DOMAIN",
            "username",
            "password",
            "mountPath");

      // note: this is what the REST api client code does
      testRoundTripThroughJson(ds, Datastore.class);
   }

   private static <T> void testRoundTripThroughJson(T obj, Class<T> klass)
         throws IOException {

      String jsonStr = AfJson.ObjectMapper().writeValueAsString(obj);
      T newObj = AfJson.ObjectMapper().readValue(jsonStr, klass);
      assertEquals("Object did not round-trip through JSON into an equivalent object.  Intermediate JSON= " + jsonStr,
                   obj, newObj);
   }
}
