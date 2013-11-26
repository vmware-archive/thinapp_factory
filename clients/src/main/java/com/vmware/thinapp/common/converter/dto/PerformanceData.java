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

import java.util.Date;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * DTO representing various performance data about a running VM.
 */
public class PerformanceData {
   public static final String CPU_USAGE_AVERAGE_SPEC = "cpu.usage.average";
   public static final String NET_USAGE_AVERAGE_SPEC = "net.usage.average";
   public static final String DISK_USAGE_AVERAGE_SPEC = "disk.usage.average";

   /**
    * Collection of performance counter keys that will be provided.
    */
   public static final ImmutableSet<String> QUERY_COUNTERS_REQUIRED = ImmutableSet.<String>of(
         CPU_USAGE_AVERAGE_SPEC,
         NET_USAGE_AVERAGE_SPEC);

   /**
    * Collection of performance counter keys that may be provided in addition
    * to the required keys.
    */
   public static final ImmutableSet<String> QUERY_COUNTERS_OPTIONAL = ImmutableSet.<String>of(
         DISK_USAGE_AVERAGE_SPEC);

   /**
    * Collection of all performance counter keys (required and optional).
    */
   public static final ImmutableSet<String> QUERY_COUNTERS_ALL = new ImmutableSet.Builder<String>()
         .addAll(QUERY_COUNTERS_REQUIRED)
         .addAll(QUERY_COUNTERS_OPTIONAL)
         .build();

   private Integer refreshRate;
   private String date;
   private Map<String, Long> values;

   public static PerformanceData create(Integer refreshRate, Date date, Map<String, Long> values) {
      return create(refreshRate, AfCalendar.formatUtc(date), values);
   }

   public static PerformanceData create(Integer refreshRate, String date, Map<String, Long> values) {
      PerformanceData dataItem = new PerformanceData();
      dataItem.setRefreshRate(refreshRate);
      dataItem.setDate(date);
      dataItem.setValues(values);
      return dataItem;
   }

   public void setRefreshRate(Integer refreshRate) {
      this.refreshRate = refreshRate;
   }

   /**
    * @return the refresh rate of the service providing this data, in seconds.
    *         i.e. this data will never be updated more often than this value.
    */
   public Integer getRefreshRate() {
      return refreshRate;
   }

   public void setDate(String date) {
      this.date = date;
   }

   /**
    * @return the date/time that this performance data was obtained.
    */
   public String getDate() {
      return date;
   }

   public void setValues(Map<String, Long> values) {
      this.values = values;
   }

   /**
    * @return Performance data values.  Each key is a performance counter
    *         specification (for example, cpu.usage.average) and each value is
    *         a long integer for that performance counter.  60.3% is
    *         represented as the integer 6030.
    */
   public Map<String, Long> getValues() {
      return values;
   }
}