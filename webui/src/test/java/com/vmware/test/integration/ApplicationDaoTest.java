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

package com.vmware.test.integration;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.appfactory.application.dao.AppBuildRequestDao;
import com.vmware.appfactory.application.dao.ApplicationDao;
import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.application.model.Application;

/**
 * This is a test class to invoke the ApplicationDao methods using JUnit.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
      "classpath:spring/test-hibernate-config.xml",
      "classpath:spring/test-full-app-config.xml"
   })
public class ApplicationDaoTest
{
   private static final Logger _log = LoggerFactory.getLogger(ApplicationDaoTest.class);

   private static Long appId;
   private static Long appBuildRequestId;

   @Resource
   ApplicationDao appDao;

   @Resource
   AppBuildRequestDao appBuildRequestDao;

   /**
    * Test to load appId and check if there are any applications.
    */
   @Test
   public void getApplication()
   {
      try {
         List<Application> appList = appDao.findAll();
         Assert.assertNotNull(appList);
         if (CollectionUtils.isNotEmpty(appList)) {
            // Get the appId and assign to variable.
            appId = appList.get(0).getId();
            _log.info(
                  "Junit testing for App: {} for appId: {}",
                  appList.get(0).getDisplayName(),
                  appId);
         }
      }
      catch (Exception e) {
         _log.error(e.getMessage(), e);
      }
   }


   /**
    * Test to create and update a appBuildRequest record for the app
    * selected earlier.
    */
   @Test
   public void performBuildRequest()
   {
      if (appId == null) {
         _log.debug("appId is null, skip appBuildRequest tests.");
      }

      try {
         appBuildRequestId = createBuildRequest(appId);
         Assert.assertNotNull(appBuildRequestId);
         if (appBuildRequestId == null) {
            _log.info("appBuildRequestId is null, create failed.");
            return;
         }
         List<AppBuildRequest> list = fetchChildrenForApp(appId);
         Assert.assertNotNull(list);
         Assert.assertNotSame(list.get(list.size() - 1).getId(), appBuildRequestId);
         _log.info("AppBuildRequest count for appId: {} is: {}", appId, list.size());
         updateAppBuildRequestStage(appBuildRequestId, RequestStage.failed);
      }
      catch (Exception e) {
         _log.error(e.getMessage(), e);
      }
   }


   private Long createBuildRequest(Long appId)
   throws Exception
   {
      Application app = new Application();
      app.setId(appId);

      // Now create the AppBuildRequest object here.
      AppBuildRequest buildRequest = new AppBuildRequest();
      buildRequest.setOsType("OsTypeWin7");
      buildRequest.setOsVariant("Enterprise");
      buildRequest.setRuntime("2.7.0");
      buildRequest.setManualMode(false);
      buildRequest.setRequestStage(RequestStage.created);
      buildRequest.setBuildId(RandomUtils.nextLong());
      buildRequest.setDatastoreId(1L);
      buildRequest.setApplication(app);

      // Create a record and return it.
      return appBuildRequestDao.create(buildRequest);
   }


   /**
    * Update the requestStage.
    *
    * @param requestStage
    * @param appBuildReqId
    */
   private void updateAppBuildRequestStage(
         Long appBuildReqId,
         RequestStage requestStage)
   {
      if (appBuildReqId == null) {
         return;
      }
      appBuildRequestDao.updateBuildRequestStage(
            appBuildReqId,
            requestStage);
   }


   /**
    * Fetch the AppBuildReqest entities for the given appId.
    *
    * @param appId
    * @return
    * @throws Exception
    */
   private List<AppBuildRequest> fetchChildrenForApp(Long appId)
   throws Exception
   {
      if (appId == null) {
         return null;
      }
      return appBuildRequestDao.findBuildRequestForApp(appId);
   }
}
