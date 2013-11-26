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

package com.vmware.thinapp.manualmode.server

import java.io.File

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import com.google.common.io.{ByteStreams, Files}
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime

/**
 * Class for creating self-extracting ThinApp runtime packages.
 */
@Component
class ThinAppRuntimePackager @Autowired()(appCtxt: ApplicationContext) {
   /**
    * Create a self-extracting package of a ThinApp runtime.
    *
    * @param runtime
    */
   def createPackage(runtime: ThinAppRuntime): File = {
      val tempdir = Files.createTempDir()
      // The zip self-extract bootstrapper.
      val sfxFile = new File(tempdir, "sfx.exe")
      // Zip file of the runtime.
      val zipFile = new File(tempdir, "package.zip")
      // The final self-extracting runtime package.
      val packageFile = new File(tempdir, "package.exe")

      // Easier to use external zip utility to recursively create a zip
      // file than the built-in Zip class.
      //
      // Note that vmw.lic is a symlink in the source directory.  By default zip
      // will dereference symlinks which is what's desired.
      val proc = Runtime.getRuntime.exec(
         Array("zip", "-r", zipFile.getAbsolutePath, "."), null, new File(runtime.path))
      proc.waitFor

      // Copy self-extracting stub into our temp directory.
      val sfx = appCtxt.getResource("classpath:unzipsfx.exe").getInputStream
      ByteStreams.copy(sfx, Files.newOutputStreamSupplier(sfxFile))

      // Join the stub with the generated zip file.
      val joined = ByteStreams.join(Files.newInputStreamSupplier(sfxFile),
                                    Files.newInputStreamSupplier(zipFile))

      // Write joined contents to final package output.
      ByteStreams.copy(joined, Files.newOutputStreamSupplier(packageFile))

      sfxFile.delete
      zipFile.delete

      packageFile
   }
}