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

package com.vmware.thinapp.common.converter.dto


import org.apache.commons.lang.builder.ToStringBuilder
import org.codehaus.jackson.annotate.JsonCreator

import com.vmware.thinapp.common.JsonAnnotations._

import scala.reflect.BeanProperty
import java.util.Comparator

/**
 * Represents a specific ThinApp runtime.
 */
class ThinAppRuntime (
        @BeanProperty val version: String,
        @BeanProperty val build: Long,
        @BeanProperty val path: String,
        @BeanProperty val id: Long) {
   override def toString = ToStringBuilder.reflectionToString(this)
}

object ThinAppRuntime {
   @JsonCreator
   def apply(@JsonPropertyField("version") version: String,
             @JsonPropertyField("build") build: Long,
             @JsonPropertyField("path") path: String,
             @JsonPropertyField("id") id: Long) =
      new ThinAppRuntime(version, build, path, id)

   class ThinAppRuntimeBuildComparator extends Comparator[ThinAppRuntime] {
      def compare(o1: ThinAppRuntime, o2: ThinAppRuntime): Int = {
         o1.build.compareTo(o2.build)
      }
   }

   val buildComparator = new ThinAppRuntimeBuildComparator

   /**
    * Get a comparator to sort by build number.
    */
   def getBuildComparator = buildComparator

   def apply(version: String, build: Long, path: String): ThinAppRuntime = apply(version, build, path, build)
}