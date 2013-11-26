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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.vmware.thinapp.common.workpool.dto.CustomWorkpool;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.VmSource;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.WinVistaOsType;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * Junit Test class to test json serialize and de-serialize cases for workpool
 */
public class WorkpoolJsonSerializeTest {
   ObjectMapper enabledMapper = new ObjectMapper();
   ObjectMapper disabledMapper= new ObjectMapper();

   @Before
   public void init() {
      enabledMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY);
      disabledMapper.disableDefaultTyping();
   }

   @Test
   public void testLinkedWorkpool() throws IOException {
      Workpool mock = getLinkedWorkpool();
      String json = enabledMapper.writeValueAsString(mock);
      System.out.println("Serialized linked: " + json);
      Workpool deserialized = enabledMapper.readValue(json, Workpool.class);
      System.out.println("Deserialized linked: " + deserialized);
      Assert.assertNotNull(deserialized);
      Assert.assertEquals(deserialized.getName(), mock.getName());
   }

   @Test
   public void testCustomWorkpool() throws IOException {
      Workpool mock = getCustomWorkpool();
      String json = enabledMapper.writeValueAsString(mock);
      System.out.println("Serialized custom: " + json);
      Workpool deserialized = enabledMapper.readValue(json, Workpool.class);
      System.out.println("Deserialized custom: " + deserialized);
      Assert.assertNotNull(deserialized);
      Assert.assertEquals(deserialized.getName(), mock.getName());
   }

   @Test
   public void testWorkpoolList() throws IOException {

      List<Workpool> wpList = new ArrayList<Workpool>();
      wpList.add(getLinkedWorkpool());
      wpList.add(getCustomWorkpool());

      String json = disabledMapper.writeValueAsString(wpList);
      System.out.println("Serialized list: " + json);
      Collection<?> deserialized = disabledMapper.readValue(json, Collection.class);
      System.out.println("Deserialized list: " + deserialized);
      Assert.assertNotNull(deserialized);
      Assert.assertTrue(deserialized.isEmpty() == false);
      Assert.assertEquals(deserialized.size(), wpList.size());
   }


   private Workpool getLinkedWorkpool() {
      Workpool w = new LinkedWorkpool();
      w.setId(Long.valueOf(2));
      w.setMaximum(3);
      w.setState(Workpool.State.available);
      w.setName("Linked-wp1");
      VmImage image = new VmImage();
      VmSource vmSource = new VmSource();
      image.setVmSource(vmSource);
      vmSource.setOsType(new Win7OsType(Win7OsType.Variant.enterprise));
      image.setId(Long.valueOf(4));
      image.setName("wp1-img1");
      image.setState(VmImage.State.available);
      ((LinkedWorkpool)w).setVmImage(image);
      return w;
   }

   private Workpool getCustomWorkpool() {
      Workpool w = new CustomWorkpool();
      w.setId(Long.valueOf(1));
      w.setMaximum(1);
      w.setState(Workpool.State.created);
      w.setName("Custom-wp2");
      w.setOsType(new WinVistaOsType(WinVistaOsType.Variant.enterprise));
      return w;
   }
}
