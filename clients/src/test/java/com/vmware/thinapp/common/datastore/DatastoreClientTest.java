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

package com.vmware.thinapp.common.datastore;

import org.junit.Test;

import com.vmware.thinapp.common.datastore.client.DatastoreClient;
import com.vmware.thinapp.common.datastore.dto.Datastore;

public class DatastoreClientTest {

   @Test
   public void testGetDatastore() throws Exception {
      DatastoreClient datastoreClient = new DatastoreClient("http://localhost:5000/storage/");
      @SuppressWarnings("unused")
      Datastore ds = datastoreClient.getDatastore("internal");
   }
}
