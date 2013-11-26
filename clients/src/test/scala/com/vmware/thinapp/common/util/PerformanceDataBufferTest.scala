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

package com.vmware.thinapp.common.util

import java.util.Date
import org.junit.Test
import org.junit.Assert._
import com.vmware.thinapp.common.converter.dto.PerformanceData
import com.vmware.thinapp.common.converter.exception.ConverterException
import scala.collection.JavaConversions._
import java.util.HashMap
import com.google.common.collect.ImmutableSet

class PerformanceDataBufferTest {
   private val THRESHOLD_VAL: java.lang.Long = 50

   private val thresholds = PerformanceData.QUERY_COUNTERS_ALL.map(
         counter => {
            counter -> THRESHOLD_VAL
         }).toMap
   private val reqThresholds = PerformanceData.QUERY_COUNTERS_REQUIRED.map(
         counter => {
            counter -> THRESHOLD_VAL
         }).toMap
   private val underThreshold: Map[String, java.lang.Long] = Map(
         PerformanceData.CPU_USAGE_AVERAGE_SPEC -> (THRESHOLD_VAL - 1),
         PerformanceData.NET_USAGE_AVERAGE_SPEC -> (THRESHOLD_VAL - 1))
   private val overThreshold: Map[String, java.lang.Long] = Map(
         PerformanceData.CPU_USAGE_AVERAGE_SPEC -> (THRESHOLD_VAL + 1),
         PerformanceData.NET_USAGE_AVERAGE_SPEC -> (THRESHOLD_VAL - 1))

   /**
    * Refresh rates below one aren't valid
    */
   @Test(expected=classOf[IllegalArgumentException])
   def testConstructorRefreshFail {
      new PerformanceDataBuffer(-10, 10, 10, thresholds)
   }

   /**
    * Verify that containsAll works as expected.
    */
   @Test
   def testContainsAll {
      val m = new HashMap[java.lang.Integer, String]()
      m.put(new java.lang.Integer(2), "cpu.usage.average")
      m.put(new java.lang.Integer(115), "net.usage.average")
      val s = new ImmutableSet.Builder[String]()
         .add("cpu.usage.average")
         .add("net.usage.average")
         .build()
      assertTrue(m.values().containsAll(s))
   }

   /**
    * Poll frequencies rates below one aren't valid
    */
   @Test(expected=classOf[IllegalArgumentException])
   def testConstructorPollFail {
      new PerformanceDataBuffer(10, 0, 10, thresholds)
   }

   /**
    * 0 for idle time should cause the buffer size to be 0, which isn't allowed
    */
   @Test(expected=classOf[IllegalArgumentException])
   def testConstructorBuffSizeFail {
      new PerformanceDataBuffer(20, 15, 0, thresholds)
   }

   /**
    * The constructor requires thresholds to be provided for the required performance counters.
    */
   @Test(expected=classOf[ConverterException])
   def testConstructorMissingReqCounter {
      new PerformanceDataBuffer(20, 15, 60, new HashMap[String, java.lang.Long])
   }

   /**
    * The constructor requires thresholds to be provided for optional performance counters.
    */
   @Test(expected=classOf[ConverterException])
   def testConstructorMissingOptCounter {
      new PerformanceDataBuffer(20, 15, 60, reqThresholds)
   }

   /**
    * Verify that the buffer capacity is calculated properly.
    */
   @Test
   def testBuffCapacity {
      var buff = new PerformanceDataBuffer(20, 15, 60, thresholds)
      assertEquals(3, buff.capacity)
      assertEquals(0, buff.size)

      buff = new PerformanceDataBuffer(10, 15, 60, thresholds)
      assertEquals(4, buff.capacity)

      buff = new PerformanceDataBuffer(20, 15, 59, thresholds)
      assertEquals(3, buff.capacity)

      buff = new PerformanceDataBuffer(20, 15, 30, thresholds)
      assertEquals(2, buff.capacity)

      buff = new PerformanceDataBuffer(130, 15, 120, thresholds)
      assertEquals(1, buff.capacity)

      buff = new PerformanceDataBuffer(1, 1, 120, thresholds)
      assertEquals(120, buff.capacity)

      buff = new PerformanceDataBuffer(2, 1, 120, thresholds)
      assertEquals(60, buff.capacity)
   }

   /**
    * Verify that clear actually works.
    */
   @Test
   def testClear {
      val buff = new PerformanceDataBuffer(10, 10, 20, thresholds)
      assertEquals(2, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)
      buff.clear
      assertEquals(0, buff.size)

      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)
      val nextMillis = nowMillis + 10000
      buff.add(PerformanceData.create(60, new Date(nextMillis), underThreshold))
      assertEquals(2, buff.size)
      buff.clear
      assertEquals(0, buff.size)
   }

   /**
    * Verify that the isFull method works as expected when the buffer has a
    * capacity of 1.
    */
   @Test
   def testIsFull1 {
      val buff = new PerformanceDataBuffer(60, 10, 60, thresholds)
      assertEquals(1, buff.capacity)
      assertEquals(0, buff.size)

      buff.add(PerformanceData.create(60, AfCalendar.NowString, underThreshold))
      assertEquals(1, buff.size)
      assertTrue(buff.isFull)
   }

   /**
    * Verify that the isFull method works as expected when the buffer has a
    * capacity of 2.
    */
   @Test
   def testIsFull2 {
      val buff = new PerformanceDataBuffer(10, 10, 20, thresholds)
      assertEquals(2, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)
      assertFalse(buff.isFull)

      val nextMillis = nowMillis + 10000
      buff.add(PerformanceData.create(60, new Date(nextMillis), underThreshold))
      assertEquals(2, buff.size)
      assertTrue(buff.isFull)
   }

   /**
    * Verify that there is a bit of padding time between when updates can be
    * added before the buffer considers the add missing the update interval.
    */
   @Test
   def testWithinInterval {
      val buff = new PerformanceDataBuffer(10, 10, 20, thresholds)
      assertEquals(2, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)

      val nextMillis = nowMillis + 19000
      buff.add(PerformanceData.create(60, new Date(nextMillis), underThreshold))
      assertEquals(2, buff.size)
   }

   /**
    * Verify that an update with a timestamp beyond the padding time is
    * considered as missing the update interval.
    */
   @Test
   def testMissedInterval {
      val buff = new PerformanceDataBuffer(10, 10, 20, thresholds)
      assertEquals(2, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)

      val nextMillis = nowMillis + 20000
      buff.add(PerformanceData.create(60, new Date(nextMillis), underThreshold))
      // Note size == 1, not 2 since the interval was missed
      assertEquals(1, buff.size)
   }

   /**
    * Verify that the adding when full doesn't break and leaves size == capacity.
    */
   @Test
   def testAddWhenFull {
      val buff = new PerformanceDataBuffer(60, 10, 60, thresholds)
      assertEquals(1, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)
      assertTrue(buff.isFull)

      val nextMillis = nowMillis + 10000
      buff.add(PerformanceData.create(60, new Date(nextMillis), underThreshold))
      assertEquals(1, buff.size)
      assertTrue(buff.isFull)
   }

   /**
    * Verify that the adding a value over the given threshold clears the buffer.
    */
   @Test
   def testOverThreshold {
      val buff = new PerformanceDataBuffer(10, 10, 20, thresholds)
      assertEquals(2, buff.capacity)
      assertEquals(0, buff.size)

      val nowMillis = AfCalendar.Now
      buff.add(PerformanceData.create(60, new Date(nowMillis), overThreshold))
      assertEquals(0, buff.size)

      buff.add(PerformanceData.create(60, new Date(nowMillis), underThreshold))
      assertEquals(1, buff.size)

      val nextMillis = nowMillis + 10000
      buff.add(PerformanceData.create(60, new Date(nextMillis), overThreshold))
      assertEquals(0, buff.size)
   }

   /**
    * Verify that the adding a data item with a missing required counter clears the buffer.
    */
   @Test
   def testMissingCounter {
      val buff = new PerformanceDataBuffer(60, 10, 60, thresholds)
      assertEquals(1, buff.capacity)
      assertEquals(0, buff.size)

      buff.add(PerformanceData.create(
            60,
            AfCalendar.NowDate,
            Map(PerformanceData.CPU_USAGE_AVERAGE_SPEC -> new java.lang.Long(THRESHOLD_VAL - 1))))
      assertEquals(0, buff.size)
   }

   /**
    * Verify that the adding a data item with an optional counter over the threshold clears
    * the buffer.
    */
   @Test
   def testOptionalOver {
      val buff = new PerformanceDataBuffer(60, 10, 60, thresholds)
      assertEquals(1, buff.capacity)
      assertEquals(0, buff.size)

      buff.add(PerformanceData.create(
            60,
            AfCalendar.NowDate,
            Map(
                  PerformanceData.CPU_USAGE_AVERAGE_SPEC -> new java.lang.Long(THRESHOLD_VAL - 1),
                  PerformanceData.NET_USAGE_AVERAGE_SPEC -> new java.lang.Long(THRESHOLD_VAL - 1),
                  PerformanceData.DISK_USAGE_AVERAGE_SPEC -> new java.lang.Long(THRESHOLD_VAL + 1))))
      assertEquals(0, buff.size)
   }
}
