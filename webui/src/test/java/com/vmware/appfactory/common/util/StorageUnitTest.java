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

package com.vmware.appfactory.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Simple tests for storage unit formatter
 */
public class StorageUnitTest {
   @Test
   public void testFormat() {
      String temp;

      temp = StorageUnit.format(123L);
      assertEquals("123 bytes", temp);

      temp = StorageUnit.format(0L);
      assertEquals("0 bytes", temp);

      temp = StorageUnit.format(-123L);
      assertEquals("-123 bytes", temp);

      temp = StorageUnit.format(1024L);
      assertEquals("1.0 KB", temp);

      temp = StorageUnit.format(1536L);
      assertEquals("1.5 KB", temp);

      temp = StorageUnit.format(1485L);
      assertEquals("1.5 KB", temp);

      temp = StorageUnit.format(1520435L);
      assertEquals("1.4 MB", temp);

      temp = StorageUnit.format(1520436L);
      assertEquals("1.5 MB", temp);

      temp = StorageUnit.format(1556925644L);
      assertEquals("1.4 GB", temp);

      temp = StorageUnit.format(1556925645L);
      assertEquals("1.5 GB", temp);

      temp = StorageUnit.format(1594291860275L);
      assertEquals("1.4 TB", temp);

      temp = StorageUnit.format(1594291860276L);
      assertEquals("1.5 TB", temp);

      temp = StorageUnit.format(1737499995238606L);
      assertEquals("1,580.2 TB", temp);

      temp = StorageUnit.format(1484800000000000L);
      assertNotNull(temp);
      assertTrue(temp.length() > 0L);
   }
}
