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

package com.vmware.thinapp.workpool.tests.functional

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import com.vmware.thinapp.workpool.tests.fixtures.Defaults
import com.vmware.thinapp.workpool.{WorkpoolManager, VmImageManager, VCManager}
import org.junit.{Assert, Test}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("classpath:workpool-context.xml"))
class ScratchTest {
   @Autowired
   val vcManager: VCManager = null
   @Autowired
   val vmImageManager: VmImageManager = null
   @Autowired
   val workpoolManager: WorkpoolManager = null

   @Test
   def testWorkpoolWithCloneStrategy: Unit = {
     /*
      vcManager.update(Defaults.vcConfig)

      val vmImage = vmImageManager.findByName("test").getOrElse {
         vmImageManager.create(Defaults.vmImageExisting)
      }
      val workpool = workpoolManager.findByName("test").getOrElse {
         val wp = new LinkedWorkpool
         wp.setMaximumInstances(4)
         wp.setName("test")
         wp.setInstancer(vmImage.getVmImage)
         workpoolManager.create(wp)
      }

      val future1 = workpool.acquire.get
      val future2 = workpool.acquire.get
      val future3 = workpool.acquire.get
      val future4 = workpool.acquire.get
      future1.get
      future2.get
      future3.get
      future4.get
      */
   }

   @Test
   def testWorkpoolWithCloneStrategyNewInstall: Unit = {
     /*
      vcManager.update(Defaults.vcConfig)

      val vmImage = vmImageManager.findByName("test-install").getOrElse {
         vmImageManager.create(Defaults.vmImage)
      }
      val workpool = workpoolManager.findByName("test").getOrElse {
         val wp = new LinkedWorkpool
         wp.setMaximumInstances(4)
         wp.setName("test")
         wp.setInstancer(vmImage.getVmImage)
         workpoolManager.create(wp)
      }

      val future1 = workpool.acquire.get
      val future2 = workpool.acquire.get
      val future3 = workpool.acquire.get
      val future4 = workpool.acquire.get
      future1.get
      future2.get
      future3.get
      future4.get
      */
   }

   @Test
   def testWorkpoolWithInstallStrategy: Unit = {
     /*
      vcManager.update(Defaults.vcConfig)

      val wp = new FullWorkpool
      wp.setName("test-install3")
      wp.setInstancer(Defaults.vmPattern)
      wp.setMaximumInstances(1)
      val workpool = workpoolManager.create(wp)
      val future = workpool.acquire.get
      future.get
      */
   }

   @Test
   def testWorkstationCloneSupported: Unit = {
      vcManager.update(Defaults.wsConfig)
      vcManager.getConnection.get().get
      Assert.assertFalse(vcManager.isCloningSupported)
   }

   @Test
   def testVsphereCloneSupported: Unit = {
      vcManager.update(Defaults.vcConfig)
      vcManager.getConnection.get().get
      Assert.assertTrue(vcManager.isCloningSupported)
   }
}
