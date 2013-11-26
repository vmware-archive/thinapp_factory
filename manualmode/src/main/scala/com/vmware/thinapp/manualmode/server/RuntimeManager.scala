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

import org.slf4j.LoggerFactory

import com.vmware.thinapp.common.converter.dto.ThinAppRuntime

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

/**
 * ThinApp runtime manager.
 *
 * @param runtimesPath base directory that includes the ThinApp runtimes
 */
class RuntimeManager(val runtimesPath: File) {
   val log = LoggerFactory.getLogger(classOf[RuntimeManager])

   @BeanProperty
   val runtimes: java.util.List[ThinAppRuntime] = getRuntimesList

   private def getRuntimesList: List[ThinAppRuntime] = {
      log.info("Loading ThinApp runtimes from path: {}", runtimesPath)
      // Runtimes are located per-directory.
      val runtimeDirectories = runtimesPath.listFiles().filter(p => p.isDirectory).toList
      // Extract a list of runtimes from the list of directories.
      val runtimeList: List[Option[ThinAppRuntime]] = runtimeDirectories.map((file: File) => {
         // Look for directories of the form version-build.
         file.getName.split('-').toList match {
            case version :: build :: Nil => Some(ThinAppRuntime(version, build.toInt, file.getAbsolutePath))
            case _ => {
               log.error("Unable to extract runtime version or build information from: {}.", file)
               None
            }
         }
      })

      // Get rid of empty Options.
      runtimeList.flatten
   }
}