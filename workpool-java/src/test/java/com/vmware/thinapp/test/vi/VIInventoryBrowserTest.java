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

package com.vmware.thinapp.test.vi;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.FolderNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.vi.InventoryBrowser;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Test the navigation using the vijava API
 * Valuable info: http://www.infoq.com/presentations/vsphere-java-api
 *
 * @author Keerthi Singri
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:workpool-context.xml")
public class VIInventoryBrowserTest {
   @Autowired
   private ServiceInstance si;

   @Autowired
   private InventoryBrowser iBrowser;

   //@Test
   public void testGetDatacenters() throws Exception {
      Folder root = si.getRootFolder();

      InventoryNavigator i = new InventoryNavigator(root);
      ManagedEntity[] dc = i.searchManagedEntities(true);
      System.out.println(dc);
      for (ManagedEntity d : dc) {
         if (d instanceof Datacenter) {
            System.out.println("Datacenter --------------------------------");
            System.out.println(d.getMOR().getVal());
         } else if (d instanceof Folder) {
            Folder f = (Folder)d;
            f.getChildEntity();
            System.out.println("--- L2 -----------------------------");
            String[] childTypes = f.getChildType();
            for (String childType : childTypes) {
               if ("Datastore".equals(childType) ||
                  "VirtualMachine".equalsIgnoreCase(childType)) {
                   //Need recursive.
               }
            }
         } else if (d instanceof VirtualMachine) {
            VirtualMachine vm = (VirtualMachine)d;
            System.out.println("------ L3 --------------------------");
            System.out.println(vm);
            VirtualMachineConfigInfo vmConf = vm.getConfig();

            // Moid?
            System.out.println(vmConf.getUuid());
            System.out.println(vm.getName());
            System.out.println(vm.getGuest().getGuestFullName());

         } else if (d instanceof Datastore) {
            Datastore ds = (Datastore) d;
            System.out.println("------ Datastore --------------------------");
            System.out.println(ds);
         } else {
            System.out.println("-- unknown -------------------------");
         }
         System.out.println("Managedentity: " + d);
         System.out.println("Name:          " + d.getName());
      }
   }

   @Test
   public void browseAllDatastoreForISO() {
      System.out.println("Datastore file browsing... [LUN1] on vc");
      // vc.your.company.com
      VINode dsNode = new VINode("LUN1", "ds1", VINode.Type.Datastore, "[LUN1]");
      recursiveBrowseDatastoreForISO(dsNode, 0);
   }

   private void recursiveBrowseDatastoreForISO(VINode dsNode, int level)
   throws BaseRuntimeException {
      List<FileNode> nodeList = iBrowser.browseDatastoreForISO(dsNode);
      for (int i = 0; nodeList != null && i < nodeList.size(); i++) {
         String newPath = nodeList.get(i).getPath();
         for( int space = 0; space <= level; space++) {
            System.out.print("   ");
         }
         System.out.print("newPath: " + newPath);
         System.out.print("\n");
         if (nodeList.get(i) instanceof FolderNode) {
            dsNode.setPath(newPath);
            recursiveBrowseDatastoreForISO(dsNode, level+1);
         }
      }
   }

   @Test
   public void loadVITreeUsingComputeResourceByType() {

      System.out.println("Compute Resource base tree browsing...");

      System.out.println("Display: Datastore VIEW:");
      long start = System.currentTimeMillis();
      VINode node = iBrowser.loadVITreeFromComputeResource(VINode.Type.Datastore);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start));
      System.out.println("-----------------------------");
      System.out.println(node.constructTree(0));

      System.out.println("Display: Network VIEW:");
      start = System.currentTimeMillis();
      node = iBrowser.loadVITreeFromComputeResource(VINode.Type.Network);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start));
      System.out.println("-----------------------------");
      System.out.println(node.constructTree(0));

      System.out.println("Display: VirtualMachine VIEW:");
      start = System.currentTimeMillis();
      node = iBrowser.loadVITreeFromComputeResource(VINode.Type.VirtualMachine);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start));
      System.out.println("-----------------------------");
      System.out.println(node.constructTree(0));
   }

}
