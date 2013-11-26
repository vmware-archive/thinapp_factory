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



package com.vmware.thinapp.common.workpool.client;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.OsRegistration;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.VmPattern;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.Win8OsType;
import com.vmware.thinapp.common.workpool.dto.WinVistaOsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.dto.Workpool.State;



public class WorkpoolClientTestNG {

	/**
	 * TODO: Implement Factory method for the class.
	 * Currently all parameters are passed directly to the methods
	 */
   /*private String wpUrl;

   protected WorkpoolClientTestNG(String url){
      this.setWpUrl(url);
   }

   public String getWpUrl() {
      return wpUrl;
   }

   public void setWpUrl(String wpUrl) {
      this.wpUrl = wpUrl;
   }

   @Factory ()
   @Parameters({ "wpUrl"})
   public static Object create(String wpUrl) {
      System.out.println("Initializing .. In factory method");
      System.out.println("Url: " + wpUrl);
      return (new WorkpoolClientTestNG(wpUrl));
   }*/

   @BeforeClass
   @AfterClass
   @Parameters("wpUrl")
   public void CleanupEnvironment(String wpUrl)
   {
      System.out.println("Clean up environment workpool test-cases" );

      String workPoolUrl =  wpUrl + "/workpools";
      WorkpoolClient wpClient = new WorkpoolClient(workPoolUrl);
      List<Workpool> wpList = wpClient.list();
      if(wpList != null && !wpList.isEmpty())
      {
         int wpSize = wpList.size();
         System.out.println("# Workpools existing: " + wpSize);
         for(int i =0; i < wpSize; i++)
         {
            System.out.println("Deleting the workpool: " + wpList.get(i).getName());
            wpClient.delete(wpList.get(i));
         }
      }

      String imageClienturl = wpUrl + "/vmimages";
      VmImageClient vmImageClient = new VmImageClient(imageClienturl);
      List<VmImage> vmImageList = vmImageClient.list();
      if(vmImageList != null && !vmImageList.isEmpty())
      {
         int vmImageListSize = vmImageList.size();
         System.out.println("# VmImages existing: " + vmImageListSize);
         for(int i =0; i < vmImageListSize; i++)
         {
            System.out.println("Deleting the workpool: " + vmImageList.get(i).getName());
            vmImageClient.delete(vmImageList.get(i));
         }
      }
   }

   /**
    * Place-holder for adding DataProviders for the test methods
    **/
   /*@DataProvider
   public Object[][] dp() {
      return new Object[][] {
            new Object[] { 1, "a" },
            new Object[] { 2, "b" },
      };
   }*/


   /**
    * Test-case to Create a new Workpool from an ISO on a vCenter datastore and verify
    * using the get and list api's to see if creation successful
    * @param wpUrl
    * @param sourceIso
    * @param networkName
    * @param osType
    * @param osVariant
    * @param vmUserNameIn
    * @param vmOrg
    * @param vmLicKey
    * @param vmKMSServer
    * @param vmImageCreatedName
    * @param isoWorkPoolCreatedName
    * @param workPoolMaximum
    */
   @Test
   @Parameters({ "wpUrl", "sourceIso", "networkName", "osType","osVariant", "vmUserName", "vmOrg",
      "vmLicKey", "vmKMSServer", "vmImageCreatedName", "isoWorkPoolCreatedName", "workPoolMaximum" })
   public void createLinkedWP_FromISO(
      String wpUrl,String sourceIso,String networkName,String osType,String osVariant,
      String vmUserNameIn,String vmOrg,String vmLicKey,String vmKMSServer,String vmImageCreatedName,
      String isoWorkPoolCreatedName,int workPoolMaximum)  {

      System.out.println("Executing TC Create Workpool from ISO and Delete it");

      String vmUserName = ""; // Note: vmUserNameIn is ignored
      String imageClienturl = wpUrl + "/vmimages";
      VmImageClient vmImageClient = new VmImageClient(imageClienturl);

      VmPattern vmPattern = new VmPattern();
      vmPattern.setSourceIso(sourceIso);
      vmPattern.setNetworkName(networkName);

      OsType os = null;
      if(osType.equals("WinXPProOsType"))
         os = new WinXPProOsType();
      else if((osType.equals("WinVistaOsType")))
         os = new WinVistaOsType(WinVistaOsType.Variant.valueOf(osVariant));
      else if((osType.equals("Win7OsType")))
         os = new Win7OsType(Win7OsType.Variant.valueOf(osVariant));
      else if((osType.equals("Win8OsType")))
         os = new Win8OsType(Win8OsType.Variant.valueOf(osVariant));

      vmPattern.setOsType(os);

      OsRegistration osRegistration = new OsRegistration();
      osRegistration.setUserName(vmUserName);
      osRegistration.setOrganization(vmOrg);
      osRegistration.setLicenseKey(vmLicKey);
      osRegistration.setKmsServer(vmKMSServer);
      vmPattern.setOsRegistration(osRegistration);

      VmImage vmImage = new VmImage();
      vmImage.setVmSource(vmPattern);
      vmImage.setName(vmImageCreatedName);

      System.out.println("Creating the VMImage using Create API");
      VmImage vmImageRet = vmImageClient.create(vmImage);
      //Long vmImageId = vmImageRet.getId();
      System.out.println("Details of the VMImage returned in Create");
      System.out.println(vmImageRet.getName());
      System.out.println(vmImageRet.getId());
      System.out.println(vmImageRet.getState());

      VmPattern vmPatternRet = (VmPattern) vmImageRet.getVmSource();
      System.out.println(vmPatternRet.getOsType().toString());
      System.out.println(vmPatternRet.getOsRegistration().getUserName());
      System.out.println(vmPatternRet.getOsRegistration().getLicenseKey());

      assertNotNull(vmImageRet);
      assertNotNull(vmImageRet.getId());
      assertNotNull(vmImageRet.getState());
      assertEquals(vmImageCreatedName, vmImageRet.getName());
      assertEquals(sourceIso, vmPatternRet.getSourceIso());
      assertEquals(networkName, vmPatternRet.getNetworkName());
      assertEquals(osType, vmPatternRet.getOsType().toString());
      assertEquals(vmUserName, vmPatternRet.getOsRegistration().getUserName());
      assertEquals(vmOrg, vmPatternRet.getOsRegistration().getOrganization());

      LinkedWorkpool wp = new LinkedWorkpool();
      Long id;
      wp.setName(isoWorkPoolCreatedName);
      wp.setMaximum(workPoolMaximum);
      wp.setVmImage(vmImageRet);
      String workPoolUrl =  wpUrl + "/workpools";
      WorkpoolClient wpClient = new WorkpoolClient(workPoolUrl);
      LinkedWorkpool wpRet= (LinkedWorkpool)wpClient.create(wp);
      System.out.println("Details of the WP returned in Create");
      System.out.println(wpRet.getName());
      System.out.println(wpRet.getId());
      System.out.println(wpRet.getMaximum());
      System.out.println(wpRet.getVmImage().getName());
      System.out.println(wpRet.getVmImage().getId());

      assertNotNull(wpRet);
      assertNotNull(wpRet.getId());

      assertEquals(isoWorkPoolCreatedName, wpRet.getName());
      assertEquals(workPoolMaximum, wpRet.getMaximum());

      System.out.println("Calling the get API on the workpool just created");
      id = wpRet.getId();

      LinkedWorkpool wpGet = (LinkedWorkpool)wpClient.get(id);
      System.out.println("Details of the WP using Get");
      System.out.println(wpGet.getName());
      System.out.println(wpGet.getId());
      System.out.println(wpGet.getState());
      System.out.println(wpGet.getMaximum());
      System.out.println(wpGet.getVmImage().getName());
      System.out.println(wpGet.getVmImage().getId());
      System.out.println(wpGet.getVmImage().getState());

      assertNotNull(wpGet);
      assertNotNull(wpGet.getId());
      assertNotNull(wpGet.getState());
      assertEquals(isoWorkPoolCreatedName, wpGet.getName());
      assertEquals(workPoolMaximum, wpGet.getMaximum());
      assertEquals(vmImageCreatedName, wpGet.getVmImage().getName());

      int i = 0;
      while(!wpGet.getState().equals(State.available) && i < 240)
      {
         try {
            Thread.sleep(30000);
            wpGet = (LinkedWorkpool)wpClient.get(id);
            //System.out.println(wpGet.getName());
            System.out.println(wpGet.getVmImage().getState());
            if (i%20 == 0)
            {
               System.out.println(wpGet.getName());
               System.out.println(wpGet.getState());
               System.out.println(wpGet.getVmImage().getName());
               System.out.println(wpGet.getVmImage().getState());
               System.out.println("================");
            }
            i++;

         } catch (InterruptedException e) {
            System.out.println(e.getMessage());
         }
      }

      assertTrue(wpGet.getState().equals(State.available), "Workpool failed to become available. Time out");

      List<Workpool> wpList = wpClient.list();
      System.out.println("Current Workpool size");
      assertNotNull(wpList);
      assertFalse(wpList.size() <= 0);

      System.out.println(wpList.size());
   }


   /**
    * Test-case to Create a new Workpool from an existing vm image  on a vCenter datastore and verify
    * using the get and list api's to see if creation successful and delete the vm pool created

    * @param wpUrl
    * @param vmImageCreatedName
    * @param existingImageWorkpoolCreatedName
    * @param workPoolMaximum
    */
   @Test(dependsOnMethods = "createLinkedWP_FromISO")
   @Parameters({ "wpUrl" , "vmImageCreatedName", "existingImageWorkpoolCreatedName", "workPoolMaximum"})
   public void createLinkedWP_deleteWP_ExistingVMImage(
         String wpUrl,String vmImageCreatedName,String existingImageWorkpoolCreatedName,
         int workPoolMaximum) {

      System.out.println("Executing TC Create Existing Image Workpool and Delete it");

      String imageClienturl = wpUrl + "/vmimages";
      VmImageClient vmImageClient = new VmImageClient(imageClienturl);
      List<VmImage> vmImageList = vmImageClient.list();
      assertNotNull(vmImageList);
      assertEquals(vmImageList.size(), 1);
      VmImage vmImage = vmImageList.get(0);
      System.out.println("Details of the VMImage returned");
      System.out.println(vmImage.getName());
      System.out.println(vmImage.getId());
      System.out.println(vmImage.getState());

      LinkedWorkpool wp = new LinkedWorkpool();
      Long id;
      wp.setName(existingImageWorkpoolCreatedName);
      wp.setMaximum(workPoolMaximum);
      wp.setVmImage(vmImage);

      String workPoolUrl = wpUrl + "/workpools";
      WorkpoolClient wpClient = new WorkpoolClient(workPoolUrl);
      LinkedWorkpool wpRet= (LinkedWorkpool)wpClient.create(wp);
      System.out.println("Details of the WP returned in Create");
      System.out.println(wpRet.getName());
      System.out.println(wpRet.getId());
      System.out.println(wpRet.getMaximum());
      System.out.println(wpRet.getVmImage().getName());
      System.out.println(wpRet.getVmImage().getId());

      assertNotNull(wpRet);
      assertNotNull(wpRet.getId());
      assertEquals(existingImageWorkpoolCreatedName, wpRet.getName());
      assertEquals(workPoolMaximum, wpRet.getMaximum());

      System.out.println("Do a get() on the Workpool just created");
      id = wpRet.getId();

      LinkedWorkpool wpGet = (LinkedWorkpool)wpClient.get(id);
      System.out.println("Details of the WP using Get");
      System.out.println(wpGet.getName());
      System.out.println(wpGet.getId());
      System.out.println(wpGet.getMaximum());
      System.out.println(wpGet.getVmImage().getName());
      System.out.println(wpRet.getVmImage().getId());

      assertNotNull(wpGet);
      assertNotNull(wpGet.getId());
      assertNotNull(wpGet.getState());
      assertEquals(existingImageWorkpoolCreatedName, wpGet.getName());
      assertEquals(workPoolMaximum, wpGet.getMaximum());
      assertEquals(vmImageCreatedName, wpGet.getVmImage().getName());

      int i = 0;
      while(!wpGet.getState().equals(State.available) && i < 60)
      {
         try {
            Thread.sleep(3000);
            wpGet = (LinkedWorkpool)wpClient.get(id);
            //System.out.println(wpGet.getName());
            System.out.println(wpGet.getVmImage().getState());
            if (i%20 == 0)
            {
               System.out.println(wpGet.getName());
               System.out.println(wpGet.getState());
               System.out.println(wpGet.getVmImage().getName());
               System.out.println(wpGet.getVmImage().getState());
               System.out.println("================");
            }
            i++;

         } catch (InterruptedException e) {
            System.out.println(e.getMessage());
         }
      }

      System.out.println("Workpool state: " + wpGet.getState().toString());
      assertTrue(wpGet.getState().equals(State.available), "Workpool failed to become available. Time out");

      System.out.println("Do a list() on the Workpool just created");
      List<Workpool> wpList = wpClient.list();
      assertNotNull(wpList);
      assertFalse(wpList.size() <= 0);
      int numPoolsBeforeDelete = wpList.size();
      System.out.println("Current Pool size Before pool deletion: " + numPoolsBeforeDelete);

      System.out.println("Deleting the WP obtained using Get");
      wpClient.delete(wpGet);

      System.out.println("Do a list() on the Workpool after deleting the pool");
      wpList = wpClient.list();
      System.out.println(wpList.size());
      int numPoolsAfterDelete = wpList.size();
      System.out.println("Current Pool size After pool deletion: " + numPoolsAfterDelete);
      assertTrue((numPoolsBeforeDelete -1 ) == numPoolsAfterDelete,"Pool Sizes do not match after deletion");

   }

   /**
    * Test-case to delete the Workpool created from an ISO on the vCenter datastore.

    * @param wpUrl
    * @param isoWorkPoolCreatedName
    */
   @Test(dependsOnMethods = {"createLinkedWP_FromISO", "createLinkedWP_deleteWP_ExistingVMImage"})
   @Parameters({ "wpUrl",  "isoWorkPoolCreatedName" })
   public void deleteLinkedWP_FromISO(String wpUrl, String isoWorkPoolCreatedName) {
      String workPoolUrl =  wpUrl + "/workpools";
      WorkpoolClient wpClient = new WorkpoolClient(workPoolUrl);

      System.out.println("Do a list() on the Workpool after deleting the pool");
      List<Workpool> wpList = wpClient.list();
      assertNotNull(wpList);
      assertFalse(wpList.size() <= 0);
      assertEquals(isoWorkPoolCreatedName,wpList.get(0).getName());

      int numPoolsBeforeDelete = wpList.size();
      System.out.println("Current Pool size Before pool deletion: " + numPoolsBeforeDelete);

      System.out.println("Deleting the WP obtained using Get");
      wpClient.delete(wpList.get(0));

      System.out.println("Do a list() on the Workpool after deleting the pool");
      wpList = wpClient.list();
      System.out.println(wpList.size());
      int numPoolsAfterDelete = wpList.size();
      System.out.println("Current Pool size After pool deletion: " + numPoolsAfterDelete);
      assertTrue((numPoolsBeforeDelete -1 ) == numPoolsAfterDelete,"Pool Sizes do not match after deletion");

   }

   @Test
   public void get() {
      throw new RuntimeException("Test not implemented");
   }

   @Test
   public void list() {
      throw new RuntimeException("Test not implemented");
   }

   @Test
   public void acquire() {
      throw new RuntimeException("Test not implemented");
   }

   @Test
   public void release() {
      throw new RuntimeException("Test not implemented");
   }

}
