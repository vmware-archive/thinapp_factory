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

package com.vmware.thinapp.workpool.tests.integration

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import com.vmware.thinapp.workpool.tests.mocks.InstallRunnerMock
import akka.actor.TypedActor
import org.springframework.beans.factory.annotation.Autowired
import com.vmware.thinapp.workpool.tests.fixtures.Defaults
import com.vmware.thinapp.workpool.{Util, VmImageInstance, VmImageInstanceImpl, InstallRequest}
import com.vmware.thinapp.workpool.dao.{VmImageRepository, VCConfigRepository}
import org.springframework.test.annotation.Timed
import org.junit.{After, Test}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("classpath:workpool-context.xml"))
class VmImageInstanceTest {
   @Autowired
   val vcConfig: VCConfigRepository = null
   @Autowired
   val util: Util = null
   @Autowired
   private var vmImageDao: VmImageRepository = null

   @After
   def cleanup: Unit = {
      val vm = vmImageDao.findByField("name", "test-vm")
      if (vm != null) {
         vmImageDao.delete(vm)
      }
   }

   @Test
   @Timed(millis=1000L)
   def testStateIsAvailableAfterSuccessfulInstall: Unit = {
     /*
      vcConfig.update(Defaults.vcConfig)
      val vmPattern = Defaults.vmPattern
      val vmImage = new VmImage()
      vmImage.setName("test-vm")
      vmImage.setVmPattern(vmPattern)

      class VM(vmImage: VmImage) extends VmImageInstanceImpl(vmImage) {
         protected override def getInstallerRunner(request: InstallRequest) = {
            new InstallRunnerMock(true, Defaults.clones.head)
         }
      }

      val vm = TypedActor.newInstance(classOf[VmImageInstance], util.autowire(new VM(vmImage)))
      while (vm.getState() != VmImageInstance.State.available) {
         Thread.sleep(50)
      }
      */
   }

   @Test
   @Timed(millis=1000L)
   def testStateIsUnavailableAfterFailedInstall: Unit = {
     /*
      vcConfig.update(Defaults.vcConfig)
      val vmPattern = Defaults.vmPattern
      val vmImage = new VmImage()
      vmImage.setName("test-vm")
      vmImage.setVmPattern(vmPattern)

      class VM(vmImage: VmImage) extends VmImageInstanceImpl(vmImage) {
         protected override def getInstallerRunner(request: InstallRequest) = {
            new InstallRunnerMock(false, "")
         }
      }

      val vm = TypedActor.newInstance(classOf[VmImageInstance], util.autowire(new VM(vmImage)))
      while (vm.getState() != VmImageInstance.State.unavailable) {
         Thread.sleep(50)
      }
      */
   }
}
