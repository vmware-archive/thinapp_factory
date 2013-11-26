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

package com.vmware.appfactory.file;

import junit.framework.Assert;

import org.junit.Test;

/**
 * This class tests all type of supported files in TAF and their default install command.
 */
public class FileTypeTest {
   /**
    * filename or installer name without extension.
    */
   private static final String FILE_NAME = "fileName";

   /**
    * Test all types of FileType enum.
    */
   @Test
   public void testParseType() {
      FileType actual = FileType.parseType(".notsupported");
      Assert.assertEquals(FileType.unknown, actual);
      Assert.assertNull(actual.getDefaultInstallCommand(FILE_NAME));

      actual = FileType.parseType(".eXe");
      Assert.assertEquals(FileType.exe, actual);
      Assert.assertEquals("$appfile", actual.getDefaultInstallCommand(FILE_NAME));

      actual = FileType.parseType(".MSI");
      Assert.assertEquals(FileType.msi, actual);
      Assert.assertEquals("msiexec /i " + FILE_NAME, actual.getDefaultInstallCommand(FILE_NAME));

      actual = FileType.parseType(".mst");
      Assert.assertEquals(FileType.mst, actual);
      Assert.assertNull(actual.getDefaultInstallCommand(FILE_NAME));

      actual = FileType.parseType(".iso");
      Assert.assertEquals(FileType.iso, actual);
      Assert.assertNull(actual.getDefaultInstallCommand(FILE_NAME));
   }
}
