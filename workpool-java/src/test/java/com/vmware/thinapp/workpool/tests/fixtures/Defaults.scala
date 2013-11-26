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

package com.vmware.thinapp.workpool.tests.fixtures

import com.vmware.thinapp.workpool.model._
import scala.util.Random
import com.vmware.thinapp.workpool.HardwareConfiguration

object Defaults extends Defaults {

}

class Defaults {
   var vmImageInUse: VmImageModel = null

   val clones = List("vm-9999", "vm-8888")

   var workpoolName = "TestWorkpool" + Random.nextInt()
   val vmPattern = new VmPatternModel
   val osRegistration = new OsRegistrationModel
   val osInfo = new OsInfoModel
   vmPattern.setOsRegistration(osRegistration)
   vmPattern.setOsInfo(osInfo)

   osInfo.setOsType(OsInfoModel.OsType.winXPPro)
   osInfo.setVariant("")
   osRegistration.setLicenseKey("XXXXX-00000-XXXXX-00000-XXXXX")
   osRegistration.setOrganization("My Company")
   osRegistration.setUserName("taf")
   osRegistration.setKmsServer("")

   vmPattern.setNetworkName("")
   vmPattern.setSourceIso("[TAF-storage] iso/xp_sp3_x86.iso")

   val vmHardware = HardwareConfiguration.DEFAULT

   val vmLocation = new VmLocationModel
   vmLocation.setDatastoreName("TAF-storage")
   vmLocation.setComputeResource("vc.company.com")
   vmLocation.setResourcePool("")

   val vcConfig = new VCConfigModel()
   vcConfig.setVmLocation(vmLocation)
   vcConfig.setHost("vc.company.com")
   vcConfig.setDatacenter("TAF Data Center")
   vcConfig.setUsername("taf")
   vcConfig.setPassword("vc-password")

   val wsConfig = new VCConfigModel
   val wsVmLocation = new VmLocationModel
   // Not sure why this is the compute resource name according to Workstation.

   wsVmLocation.setComputeResource("user-vm.company.com")
   wsVmLocation.setDatastoreName("standard")
   wsVmLocation.setResourcePool("")
   wsConfig.setVmLocation(wsVmLocation)
   wsConfig.setHost("taf-esx.company.com")
   wsConfig.setDatacenter("")
   wsConfig.setUsername("user")
   wsConfig.setPassword("user-password")

   val vmImage = new VmImageModel
   vmImage.setOsInfo(osInfo)
   vmImage.setOsRegistration(osRegistration)
   vmImage.setMoid("vm-9999")
   vmImage.setName("test")
   vmImage.setVmPattern(vmPattern)

   val vmImageExisting = new VmImageModel
   vmImageExisting.setOsInfo(osInfo)
   vmImageExisting.setOsRegistration(osRegistration)
   vmImageExisting.setMoid("vm-8888")
   vmImageExisting.setName("workpooltest" + Random.nextInt())
}
