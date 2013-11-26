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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.vmware.appfactory.common.exceptions.AfConflictException;
import com.vmware.appfactory.workpool.WorkpoolClientService;
import com.vmware.thinapp.common.exception.BaseException;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.common.workpool.exception.WpException;

/**
 * This is a test class to invoke the workpoolClientService methods using JUnit.
 *
 * @author Keerthi Singri
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
      "classpath:spring/test-hibernate-config.xml",
      "classpath:spring/test-full-app-config.xml"
   })
public class WorkpoolClientTest
{
   private static final Logger _log = LoggerFactory.getLogger(WorkpoolClientTest.class);

   private final RestTemplate _rest = new RestTemplate();

   private final String url = "http://localhost:8080/webui/wp";

   public String baseUrl()
   {
      return url;
   }


   @Resource
   WorkpoolClientService _wpClient;

   // @Test
   public void getAllWorkpools()
   {
      try {
         List<Workpool> wpList = _wpClient.getAllWorkpools();
         System.out.println(wpList);
      }
      catch (WpException e) {
         _log.error(e.getMessage(), e);
      }
   }


   // @Test
   public void findWorkpoolById()
   {
      _log.info("findWorkpoolById: req URL used from test-config.properties");
      try {
         Long id = Long.valueOf(2L);
         Workpool w = _wpClient.getWorkpoolById(id);
         _log.info("Workpool name for id: " + id + " is: " + w.getName());
      }
      catch (BaseException e) {
         _log.error(e.getMessage(), e);
      }
   }


   // @Test
   public void updateWorkpool()
   throws WpException
   {
      Long id = new Long(5L);
      LinkedWorkpool workpool = new LinkedWorkpool();
      workpool.setId(id);
      workpool.setMaximum(Integer.valueOf(5));
      workpool.setName("WP-newName");
      VmImage image = new VmImage();
      image.setId(Long.valueOf(2));
      workpool.setVmImage(image);

      try {
         //_wpClient.updateWorkpool(workpool);

         _rest.put(baseUrl() + "/workpools/{id}", workpool, id);
      }
      catch (RestClientException e) {
         throw new WpException("Workpool cannot be updated: " + e.getMessage());
      }
   }

   @Test
   public void createWorkpool()
   {
      LinkedWorkpool workpool = new LinkedWorkpool();
      workpool.setMaximum(Integer.valueOf(5));
      workpool.setName("WP-newNamesTEST");
      VmImage image = new VmImage();
      image.setId(Long.valueOf(1));
      workpool.setVmImage(image);

      try {
         _wpClient.createWorkpool(workpool);
      }
      catch (AfConflictException e) {
         _log.error(e.getMessage(), e);
      }
      catch (WpException e) {
         _log.error("Workpool cannot be created: " + e.getMessage(), e);
      }
   }


   @Test
   public void findWorkpoolByName() {

      _log.info("findWorkpoolByName, req URL used from test-config.properties");
      try {
         String name = "Dummy Pool #1";
         Workpool w = _wpClient.getWorkpoolByName(name);
         _log.info("Workpool name for name: " + name + " is: " + w.getId());
      }
      catch (BaseException e) {
         _log.error(e.getMessage(), e);
      }
   }


}
