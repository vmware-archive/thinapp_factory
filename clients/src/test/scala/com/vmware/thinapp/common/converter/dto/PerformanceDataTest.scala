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

package com.vmware.thinapp.common.converter.dto;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.vmware.thinapp.common.util.AfCalendar;

import org.junit.Assert._

import scala.collection.JavaConversions._

class PerformanceDataTest {
   @Test
   def testBuilderDate {
      val values: Map[String, java.lang.Long] = Map(
            "test.counter" -> 1000,
            "test.other" -> 6)
      val dateStr = "2011-11-15 10:10:10 UTC"
      val date = AfCalendar.parseUtc(dateStr)
      val data = PerformanceData.create(20, date, asJavaMap(values))

      assertEquals(20, data.getRefreshRate)

      assertEquals(dateStr, data.getDate)

      assertTrue(data.getValues.containsKey("test.counter"))
      assertEquals(1000L, data.getValues.get("test.counter"))
      assertTrue(data.getValues.containsKey("test.other"))
      assertEquals(6L, data.getValues.get("test.other"))
   }

   @Test
   def testBuilderStr {
      val values: Map[String, java.lang.Long] = Map(
         "test.counter" -> 50,
         "test.other" -> 0)
      val dateStr = "2000-1-1 1:1:1 UTC"
      val data = PerformanceData.create(20, dateStr, values)

      assertEquals(20, data.getRefreshRate)

      assertEquals(dateStr, data.getDate)

      assertTrue(data.getValues.containsKey("test.counter"))
      assertEquals(50L, data.getValues.get("test.counter"))
      assertTrue(data.getValues.containsKey("test.other"))
      assertEquals(0L, data.getValues.get("test.other"))
   }

   @Test
   def testQueryCounters {
      assertEquals(3, PerformanceData.QUERY_COUNTERS_ALL.size);

      assertTrue(PerformanceData.QUERY_COUNTERS_REQUIRED.forall(
            PerformanceData.QUERY_COUNTERS_ALL.contains(_)))

      assertTrue(PerformanceData.QUERY_COUNTERS_OPTIONAL.forall(
         PerformanceData.QUERY_COUNTERS_ALL.contains(_)))
   }
}
