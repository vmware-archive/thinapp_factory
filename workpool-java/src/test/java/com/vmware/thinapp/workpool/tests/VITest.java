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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:workpool-context.xml")
public class VITest {
   @Autowired
   private ServiceInstance si;

   @Test
   public void testGetDatacenters() throws Exception {
      Folder root = si.getRootFolder();
      // Datacenter dc = si.getRootFolder().

      // System.out.println(si.getRootFolder().getChildType()[0]);
      // ManagedEntity[] e = si.getRootFolder().getChildEntity();
      // System.out.println(e[0].getClass().equals(Datacenter.class));
      // Task t = v.createSnapshot_Task("Manual mode snapshot",
      // "Temporary snapshot", false, false);

      InventoryNavigator i = new InventoryNavigator(root);

      ManagedEntity[] dc = i.searchManagedEntities(true);
      System.out.println(dc);
      for (ManagedEntity d : dc) {
         if (d instanceof Datacenter) {
            System.out.println("Name:          " + d.getName());
            System.out.println("Managedentity: " + d);
            System.out.println(d.getMOR().getVal());
         }

      }

   }
}
