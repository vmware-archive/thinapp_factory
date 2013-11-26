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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.fileshare.CifsToFeedConverterImpl;
import com.vmware.appfactory.fileshare.IFeedConverter;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfConstant;

import static org.junit.Assert.*;

/**
 * Test CIFS shared Exit16 installer to App factory JSON feed converter implementation.
 *
 * @author saung
 * @sine v1.0 4/5/2011
 */
@RunWith(SpringJUnit4ClassRunner.class)
// specifies the Spring configuration to load for this test fixture
@ContextConfiguration(locations={
      "classpath:spring/test-app-config.xml"
      })
public class CifsToFeedConverterImplTest {

   @Resource
   private IFeedConverter<Application> cifsToAppsFeedConverter;

   private static final String CIFS_SHARED_URL_PATH = "smb://10.20.132.65/foo/cifs_shared/";
   private static final String GROUP = "WORKGROUP";
   private static final String USERNAME = "foo";
   private static final String PASSWORD = "bar";

   @Before
   public void setUp() {
      // redirect output to Maven's target folder
      AfConstant.WEB_CONTEXT_REAL_PATH = "target" + AfConstant.FILE_SEPARATOR;
   }

   /**
    * Test method for {@link CifsToFeedConverterImpl#scanObjects(String,String,String,String,long,Long)}
    */
   @Test
   public void testScanApps() throws Exception {
      assertNotNull(cifsToAppsFeedConverter);
      List<Application> appList = cifsToAppsFeedConverter.scanObjects(CIFS_SHARED_URL_PATH,
            GROUP, USERNAME, PASSWORD, AfCalendar.Now(), Long.valueOf(1));
      assertNotNull(appList);
      assertTrue(appList.size() == 9);
   }

   @Test
   public void testGenerateJsonFeed() throws Exception {
      assertNotNull(cifsToAppsFeedConverter);
      List<Application> appList = cifsToAppsFeedConverter.scanObjects(CIFS_SHARED_URL_PATH,
            GROUP, USERNAME, PASSWORD, AfCalendar.Now(), Long.valueOf(1));
      assertNotNull(appList);
   }
}
