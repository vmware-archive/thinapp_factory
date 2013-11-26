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

package com.vmware.thinapp.common.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.exception.ConverterException;

/**
 * Buffer for keeping track of performance data as it is obtained.  Only the
 * most recently added performance data is stored.  A count of how many entries
 * have been added is maintained, however.  This is something of a faux ring
 * buffer.  You can continue to add new items to the buffer even when it is
 * full.
 */
public class PerformanceDataBuffer {
   private final Logger log = LoggerFactory.getLogger(PerformanceDataBuffer.class);

   private final long updateFreqMillis;
   private final long buffCapacity;
   private final ImmutableMap<String, Long> performanceThresholds;

   private PerformanceData head;
   private long buffSize;

   /**
    * Construct a new buffer for keeping track of the most recent performance
    * data added and how many additions have been made.
    *
    * This class assumes that the given performance data refresh rate will not
    * change during the course of its lifetime.  If the refresh rate does
    * change, the buffer size calculation will become invalid.
    *
    * @param refreshRate How often the data source for performance data will
    *                    refresh, in seconds.
    * @param pollFreq How often conversion job status is polled, in seconds.
    * @param idleTime Number of seconds a job can be idle before it is
    *                     considered to be stalled.
    * @param performanceThresholds Mapping of performance counter string to
    *                              threshold value.  If the counter is below
    *                              the given threshold, it is considered
    *                              stalled for that counter.  All counters must
    *                              be below the given threshold for job to be
    *                              considered stalled.  Ex: "cpu.usage.average"
    *                              -> "2000" == 20% Average CPU usage threshold
    */
   public PerformanceDataBuffer(
         int refreshRate,
         long pollFreq,
         long idleTime,
         Map<String, Long> performanceThresholds) {
      // Make sure we get a valid performance data refresh rate, poll frequency,
      // and idle time; otherwise we aren't able to properly monitor.
      Preconditions.checkArgument(refreshRate > 0,
            "Invalid performance data refresh rate: {}", refreshRate);
      Preconditions.checkArgument(pollFreq > 0,
            "Invalid poll frequency: {}", pollFreq);
      Preconditions.checkArgument(idleTime > 0,
            "Invalid idle time: {}", idleTime);

      // Calculate the update frequency based on the task poll frequency and
      // the performance data refresh rate
      long updateFreqSecs = Math.max(pollFreq, refreshRate);

      // Calculate the maximum number of items in the buffer based on the
      // update frequency and the idle time threshold
      buffCapacity = (int) Math.ceil(((double) idleTime) / ((double) (updateFreqSecs)));
      if (buffCapacity < 1) {
         throw new ConverterException("buffCapacity must be > 0");
      }

      updateFreqMillis = updateFreqSecs * 1000;

      log.debug(String.format(
            "Created performance data buffer of size %s based on " +
                  "updateFreqMillis=%s pollFreqSecs=%s and idleTimeSecs=%s",
            buffCapacity, updateFreqMillis, pollFreq, idleTime));

      this.performanceThresholds = ImmutableMap.copyOf(performanceThresholds);

      // Make sure the given performance thresholds are defined for all
      // performance counter strings
      for (String perfKey : PerformanceData.QUERY_COUNTERS_ALL) {
         if (!performanceThresholds.containsKey(perfKey)) {
            throw new ConverterException(String.format(
                  "No threshold defined for performance counter %s.",
                  perfKey));
         }
      }

      clear();
   }

   /**
    * @return Capacity of the buffer.
    */
   public long capacity() {
      return buffCapacity;
   }

   /**
    * @return Number of elements that have been added to the buffer.
    */
   public synchronized long size() {
      return buffSize;
   }

   /**
    * @return true if the maximum number items have been added to this buffer,
    *         false otherwise.
    */
   public synchronized boolean isFull() {
      // buffSize should never be larger than buffCapacity, if so, we're broken
      Preconditions.checkState(buffSize <= buffCapacity,
            "Buffer size (%s) has grown larger than the maximum (%s)",
            buffSize,
            buffCapacity);

      return buffSize == buffCapacity;
   }

   /**
    * Update the buffer with the given performance data.  The data will be
    * added if it meets the following criteria:
    * -it is more recent than the previous data item that was added
    * -it falls on the next timestamp based on the calculated update frequency
    * -all performance counter values are present and less than the specified
    *  thresholds
    *
    * @param perfData
    */
   public synchronized void add(PerformanceData perfData) {
      // Make sure we have some performance data to work with
      if (perfData == null ||
            perfData.getDate() == null ||
            perfData.getValues() == null ||
            perfData.getValues().keySet().isEmpty()) {
         return;
      }

      // Make sure at least one data item has been inserted
      if (isEmpty()) {
         addPerfData(perfData);
      } else {
         // Get the most recent performance data entry
         PerformanceData head = head();

         long headDateMillis =
               AfCalendar.parseUtc(head.getDate()).getTime();
         long perfDateMillis =
               AfCalendar.parseUtc(perfData.getDate()).getTime();
         long nowDateMillis = AfCalendar.Now();

         // Sanity check: make sure we didn't get old data somehow...
         if (perfDateMillis < headDateMillis) {
            clear();
            log.debug(String.format(
                  "Received old performance data, " +
                        "clearing performance buffer: " +
                        "perfDate=%s headDate=%s nowDate=%s updateFreq=%s",
                  perfDateMillis,
                  headDateMillis,
                  nowDateMillis,
                  updateFreqMillis));
            // Ignore repeated performance data
         } else if (perfDateMillis == headDateMillis) {
            // TODO: Figure out how to handle the case where the buffer is full,
            //       and the performance data stops getting updated.  How should
            //       this be handled?  Stop the stall or keep the job stalled?
            // Make sure we haven't missed performance data for too long
            //if (nowDateMillis >=
            //   (headDateMillis + (2 * updateFreqMillis))) {
            //   clear();
            //   logger.debug(String.format(
            //         "Missed an update interval, " +
            //         "clearing performance buffer: "+
            //         "perfDate=%s headDate=%s nowDate=%s updateFreq=%s",
            //         perfDateMillis,
            //         headDateMillis,
            //         nowDateMillis,
            //         updateFreqMillis));
            //}
            // Performance data has been updated
         } else if (perfDateMillis > headDateMillis) {
            // Start over if we missed an interval of performance data
            if(perfDateMillis >=
                  (headDateMillis + (2 * updateFreqMillis))) {
               clear();
               log.debug(String.format(
                     "Missed an update interval, " +
                           "clearing performance buffer: "+
                           "perfDate=%s headDate=%s nowDate=%s updateFreq=%s",
                     perfDateMillis,
                     headDateMillis,
                     nowDateMillis,
                     updateFreqMillis));
            }
            addPerfData(perfData);
         }
      }
   }

   /**
    * Clear this performance data buffer.
    */
   public synchronized void clear() {
      head = null;
      buffSize = 0;
   }

   private void insert(PerformanceData data) {
      head = data;
      buffSize = (buffSize < buffCapacity) ? (buffSize + 1) : (buffCapacity);
   }

   private PerformanceData head() {
      return head;
   }

   private boolean isEmpty() {
      return buffSize == 0;
   }

   private void addPerfData(PerformanceData perfData) {
      // Make sure the performance data has all the required values
      for (String perfKey : PerformanceData.QUERY_COUNTERS_REQUIRED) {
         Long perfValue = perfData.getValues().get(perfKey);
         Long perfThreshold = performanceThresholds.get(perfKey);

         // Is the required performance counter available?
         if (perfValue == null) {
            clear();
            log.debug("Performance data does not contain a value for a required counter {}.", perfKey);
            return;
         // Is the performance counter value below the defined threshold?
         } else if (perfValue > perfThreshold) {
            // Only log this if there was at least one other item in the performance buffer
            if (!isEmpty()) {
               log.debug(String.format("Required performance counter %s is above " +
                     "defined threshold: value=%s threshold=%s",
                     perfKey,
                     perfValue,
                     perfThreshold));
            }
            clear();
            return;
         }
      }

      // Make sure any available optional performance data is below the defined threshold
      for (String perfKey : PerformanceData.QUERY_COUNTERS_OPTIONAL) {
         Long perfValue = perfData.getValues().get(perfKey);
         Long perfThreshold = performanceThresholds.get(perfKey);

         // If the optional performance counter data is available, is it below the defined threshold?
         if (perfValue != null && perfValue > perfThreshold) {
            // Only log this if there was at least one other item in the performance buffer
            if (!isEmpty()) {
               log.debug(String.format("Optional Performance counter %s is above " +
                     "defined threshold: value=%s threshold=%s",
                     perfKey,
                     perfValue,
                     perfThreshold));
            }
            clear();
            return;
         }
      }

      if (isEmpty()) {
         log.debug(String.format(
               "Adding initial performance data to buffer (%s/%s): " +
                     "date=%s keys=%s values=%s",
               buffSize + 1,
               buffCapacity,
               perfData.getDate(),
               perfData.getValues().keySet(),
               perfData.getValues().values()));
      } else {
         log.debug(String.format(
               "Adding more performance data to buffer (%s/%s): " +
                     "date=%s keys=%s values=%s",
               (isFull()) ? (buffSize) : (buffSize + 1),
               buffCapacity,
               perfData.getDate(),
               perfData.getValues().keySet(),
               perfData.getValues().values()));
      }

      // Finally insert the data, it meets all of our criteria
      insert(perfData);
   }
}