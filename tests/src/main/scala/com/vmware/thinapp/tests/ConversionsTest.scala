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

import com.vmware.thinapp.common.converter.dto._
import scala.collection.JavaConversions._
import org.testng.annotations.{DataProvider, Test}
import org.testng.Assert
import com.vmware.thinapp.common.workpool.dto._

class ConversionsTest extends TestUtils {
   @DataProvider
   def workpools = {
      val wp = new CustomWorkpool
      wp.setId(Config.root.get("customWorkpool").getLongValue)
      printf("Using workpool: %s.\n", wp)
      Array(Array(wp))
   }

   @Test(description = "Test that a conversion with installer files from a HTTP source succeeds.",
         timeOut = 15 * 60 * 1000L, dataProvider = "workpools")
   def testConversionFromHttp(workpool: Workpool) {
      val conversionClient = Config.getConversionClient

      val dsLocation = new DsLocation
      dsLocation.setUrl("datastore://1") // internal

      val commandList = new CommandList(List(new Command("installation", "txpeng540.exe /S /V/quiet")))

      val steps = Map(ConversionPhase.install -> commandList)
      val runtimeId = 519532 // 4.7.0
      val inputFiles = List(new ProjectFile("txpeng540.exe",
         "http://taf.company.com/taf-static/installers/txpeng540.exe"))

      val conversionRequest = new ConversionRequest(inputFiles, dsLocation, steps, workpool, runtimeId)
      val conversion = conversionClient.create(conversionRequest)

      println(conversion)

      var status: ConversionJobStatus = null

      do {
         status = conversionClient.get(conversion.getJobId)
         println(status)
      } while (status.getState != ConversionJobStatus.JobState.finished && {
         Thread.sleep(5000L)
         true
      })

      Assert.assertEquals(status.getResult.getDisposition, ConversionResult.Disposition.succeeded,
         "Conversion failed: %s." format status)
   }

   @Test(description = "Test that a conversion with installer files from a datastore succeeeds.")
   def testConversionFromDatastore = skip

   @Test def testConversionIdleDetection = skip

   @Test def testConversionCancellation = skip

   @Test def testManualConversion = skip

   @Test def testMixedDatastoreHttpConversion = skip

   @Test def testRecipeConversionWithMultipleFilesAndCommands = skip
}