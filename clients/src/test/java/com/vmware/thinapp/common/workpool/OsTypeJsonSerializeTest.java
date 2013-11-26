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

import junit.framework.Assert;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;

/**
 * Junit Test class to test json serialize and de-serialize cases for osType
 */
public class OsTypeJsonSerializeTest {
   ObjectMapper enabledMapper = new ObjectMapper();
   ObjectMapper disabledMapper= new ObjectMapper();

   @Before
   public void init() {
      enabledMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY);
      disabledMapper.disableDefaultTyping();
   }

   @Test
   public void testWin7EntOsType() throws IOException {
      OsType mock = new Win7OsType(Win7OsType.Variant.enterprise);
      String json = enabledMapper.writeValueAsString(mock);
      System.out.println("Serialized: " + json);
      OsType deserialized = enabledMapper.readValue(json, OsType.class);
      System.out.println("deserialized: " + deserialized);
      Assert.assertNotNull(deserialized);

      if (deserialized instanceof Win7OsType) {
         Win7OsType win7 = (Win7OsType)deserialized;
         Assert.assertEquals(win7.getVariant(), ((Win7OsType)mock).getVariant());
      }
   }

   @Test
   public void testWinXPOsType() throws IOException {
      OsType mock = new WinXPProOsType();
      String json = enabledMapper.writeValueAsString(mock);
      System.out.println("Serialized: " + json);
      OsType deserialized = enabledMapper.readValue(json, OsType.class);
      System.out.println("deserialized: " + deserialized);
      Assert.assertNotNull(deserialized);
      //Assert.assertTrue(deserialized.hasVariant());
      if (deserialized instanceof WinXPProOsType) {
         WinXPProOsType winxp = (WinXPProOsType)deserialized;
         System.out.println(winxp.getClass().getSimpleName());
      }
   }
}
