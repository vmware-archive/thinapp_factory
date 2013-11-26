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

package com.vmware.thinapp.common.datastore.client;

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.vmware.thinapp.common.datastore.dto.Datastore;

public class DatastoreClient {
   private final String baseUrl;

   private final RestTemplate template = new RestTemplate();

   public DatastoreClient(String baseUrl) {
      // Strip to prevent URLs like /storage//internal that won't work.
      this.baseUrl = StringUtils.stripEnd(baseUrl, "/");
   }

   public Datastore create(Datastore datastore) {
      URI uri = template.postForLocation(baseUrl, datastore);
      return template.getForObject(uri, Datastore.class);
   }

   public Datastore getDatastore(String id) {
      Datastore datastore = template.getForObject("{baseUrl}/{datastore}", Datastore.class, baseUrl, id);
      return datastore;
   }

   public void online(long id) {
      template.postForObject("{baseUrl}/{id}/online", null, String.class, baseUrl, id);
   }

   public void offline(long id) {
      template.postForObject("{baseUrl}/{id}/offline", null, String.class, baseUrl, id);
   }

   public void delete(long id) {
      template.delete("{baseUrl}/{id}", baseUrl, id);
   }
}
