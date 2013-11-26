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

package com.vmware.thinapp.workpool.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.thinapp.workpool.VirtualMachine;
import com.vmware.thinapp.workpool.VirtualMachineImpl;
import com.vmware.thinapp.workpool.WorkpoolStub;
import com.vmware.vim25.mo.ServiceInstance;

@ContextConfiguration("classpath:workpoolContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class VirtualMachineImplTest {
   private VirtualMachine vm;

   @Autowired
   ServiceInstance si;

   @Before
   public void setup() {
      vm = new VirtualMachineImpl(si, WorkpoolStub.vcInfo, WorkpoolStub.vmInfo);
   }

   @Test
   public void testPowerOnOperation() throws Exception {
      try {
         vm.powerOn();
         assertEquals(vm.getPowerState(), VirtualMachine.PowerState.poweredOn);
      } finally {
         if (vm != null) {
            vm.powerOff();
         }
      }
   }

   @Test
   public void testWaitForTools() throws Exception {
      try {
         vm.powerOn();
         vm.waitForTools();

         assertEquals(vm.getPowerState(), VirtualMachine.PowerState.poweredOn);
      } finally {
         if (vm != null) {
            vm.powerOff();
         }
      }
   }
}
