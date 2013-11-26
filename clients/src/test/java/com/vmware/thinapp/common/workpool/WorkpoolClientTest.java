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

package com.vmware.thinapp.common.workpool;

import org.junit.Test;

import com.vmware.thinapp.common.workpool.client.WorkpoolClient;
import com.vmware.thinapp.common.workpool.dto.CustomWorkpool;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;

public class WorkpoolClientTest {
   WorkpoolClient workpoolClient = new WorkpoolClient("http://localhost:9090/mm/workpool/workpools");

   @Test
   public void testGetDatastore() throws Exception {
      System.out.println(workpoolClient.list());
   }

   @Test
   public void testAddInstance() {
      Workpool workpool = workpoolClient.list().get(0);
      InstanceInfo instance = new InstanceInfo();
      instance.setMoid("vm-1234");
      instance.setGuestPassword("pass");
      instance.setGuestUsername("username");
      instance.setAutologon(true);
      InstanceInfo created = workpoolClient.addInstance(workpool, instance);
      System.out.println(created);

      workpoolClient.removeInstance(workpool, created.getId());
   }

   @Test
   public void createCustomWorkpool() {
      CustomWorkpool workpool = new CustomWorkpool();
      OsType osType = new WinXPProOsType();
      workpool.setOsType(osType);
      workpool.setName("my wp!");

      CustomWorkpool created = (CustomWorkpool) workpoolClient.create(workpool);

      for (String moid : new String[]{"vm-500", "501", "502", "506"}) {
         InstanceInfo instance = new InstanceInfo();
         instance.setMoid(moid);
         instance.setGuestUsername("administrator");
         instance.setGuestPassword("thinstalled!");
         instance.setAutologon(true);
         InstanceInfo createdInstance = workpoolClient.addInstance(created, instance);
         System.out.println(createdInstance);
      }
   }
}
