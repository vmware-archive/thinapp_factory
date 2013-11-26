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

package com.vmware.thinapp.tests

import org.testng.annotations.Test
import com.vmware.thinapp.common.datastore.dto.Datastore

class DatastoresTest {
   // TODO: Need to set ConverterServiceBindAddress to 0.0.0.0 so we can reach CWS externally.
   private var datastore: Datastore = null

   @Test
   def testCreateCifsDatastore: Unit = {
      val dsClient = Config.getDatastoreClient
      val ds = new Datastore()
      ds.setName("test-datastore")
      ds.setUsername("testing")
      ds.setPassword("testing")
      ds.setServer("taf.company.com")
      ds.setShare("testing")
      ds.setMountAtBoot(true)
      ds.setType(Datastore.Type.cifs)
      datastore = dsClient.create(ds)
   }

   @Test
   def testMountDatastore = {
      val dsClient = Config.getDatastoreClient
      dsClient.online(datastore.getId)
   }

   @Test
   def testUnmountDatastore = {
      val dsClient = Config.getDatastoreClient
      dsClient.offline(datastore.getId)
   }

   @Test(alwaysRun = true)
   def testDeleteDatastore = {
      val dsClient = Config.getDatastoreClient
      dsClient.delete(datastore.getId)
   }
}