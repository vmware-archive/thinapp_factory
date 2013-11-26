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

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import com.vmware.thinapp.common.exception.BaseException;
import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.FolderNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.vi.util.VIConstants;
import com.vmware.thinapp.vi.InventoryBrowser;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

/**
 * Test the navigation using the vijava API
 * Valuable info: http://www.infoq.com/presentations/vsphere-java-api
 *
 * @author Keerthi Singri
 */
public class VIInventoryBrowserTestStandalone extends InventoryBrowser {


   /**
    * Returns the ServieInstance handle, used to browse the VI inventory.
    *
    * @return
    * @throws BaseException
    */
   @Override
   public ServiceInstance getServiceInstance() {
      try {
         ServiceInstance si2 = new ServiceInstance(
            new URL("https://vc.your.company.com/sdk"), "vc-user", "vc-password", true);
         return si2;
      }
      catch (RemoteException e) {
         throw new BaseRuntimeException("Cannot connect to VI: " + e.getMessage() , e);
      }
      catch (MalformedURLException e) {
         throw new BaseRuntimeException("Invalid connection url: " + e.getMessage(), e);
      }
   }

   @Override
   public ComputeResource getComputeResource() {
      try {
         InventoryNavigator i = new InventoryNavigator(getServiceInstance().getRootFolder());
         ManagedEntity[] meArray = i.searchManagedEntities(VIConstants.COMPUTE_RESOURCE);
         return (ComputeResource)meArray[0];
      } catch (RemoteException e) {
         throw new BaseRuntimeException(e);
      }
   }


   // @TODO remove after local testing.
   public static void main(String args[]) {
      VIInventoryBrowserTestStandalone iBrowser = new VIInventoryBrowserTestStandalone();

      try {
         loadVITreeUsingComputeResourceByType(iBrowser);
         VINode dsNode = new VINode("LUN1", "ds1", VINode.Type.Datastore, "[LUN1]");

         // Workstation node:
         // VINode dsNode = new VINode("standard", "Datastore-1", VINode.Type.Datastore, "[standard]");
         System.out.println("\n\nDatastore file browsing... " + dsNode.getPath()
               +" (path):(moid) " + dsNode.getMorValue());
         recursiveBrowseDatastoreForISO(dsNode, iBrowser, 0);
      }
      catch (BaseException e) {
         System.out.println(e);
      }
   }


   private static void recursiveBrowseDatastoreForISO(VINode dsNode, InventoryBrowser iBrowser, int level)
   throws BaseException {
      List<FileNode> nodeList = iBrowser.browseDatastoreForISO(dsNode);
      for (int i = 0; nodeList != null && i < nodeList.size(); i++) {
         String newPath = nodeList.get(i).getPath();
         for( int space = 0; space <= level; space++) {
            System.out.print("   ");
         }
         System.out.print("|--> " + newPath);
         System.out.print("\n");
         if (nodeList.get(i) instanceof FolderNode) {
            dsNode.setPath(newPath);
            recursiveBrowseDatastoreForISO(dsNode, iBrowser, level+1);
         }
      }
   }

   private static void loadVITreeUsingComputeResourceByType(InventoryBrowser iBrowser)
   throws BaseException {
      System.out.println("Compute Resource base tree browsing...");
      VINode node = null;
      System.out.println("Display:   Datastore VIEW:");
      long start = System.currentTimeMillis();
      node = iBrowser.loadVITreeFromComputeResource(VINode.Type.Datastore);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start));
      System.out.println("---------------------------");
      System.out.println(node.constructTree(0));

      System.out.println("\n\nDisplay:   Network VIEW:");
      start = System.currentTimeMillis();
      node = iBrowser.loadVITreeFromComputeResource(VINode.Type.Network);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start));
      System.out.println("---------------------------");
      System.out.println(node.constructTree(0));

      System.out.println("\n\nDisplay: VirtualMachine VIEW:");
      long start2 = System.currentTimeMillis();
      node = iBrowser.loadVITreeFromComputeResource(VINode.Type.VirtualMachine);
      System.out.println("Timetaken: " + (System.currentTimeMillis() - start2));
      System.out.println("-----------------------------");
      System.out.println(node.constructTree(0));
   }


}
