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

package com.vmware.appfactory.taskqueue.tasks.state.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.util.PerformanceDataBuffer;

public class StallDetector {

   private final Logger parentLogger;
   private final ImmutableMap<String, Long> perfThresholds;
   private final int pollFreq;
   private final long stallTimeout;

   @Nullable
   private PerformanceDataBuffer perfBuff;

   public StallDetector(@Nonnull Logger parentLogger,
                        long stallCpu,
                        long stallNet,
                        long stallDisk,
                        long stallTimeout,
                        int pollFreq) {

      this.pollFreq = pollFreq;
      this.perfBuff = null;
      this.perfThresholds = ImmutableMap.of(
            PerformanceData.CPU_USAGE_AVERAGE_SPEC,
            stallCpu,
            PerformanceData.DISK_USAGE_AVERAGE_SPEC,
            stallNet ,
            PerformanceData.NET_USAGE_AVERAGE_SPEC,
            stallDisk);

      this.stallTimeout = stallTimeout;
      this.parentLogger = parentLogger;
   }

   public void addPerformanceData(@Nullable PerformanceData perfData) {
      if (null == perfData) {
         // nothing to add
         return;
      }

      if (null == perfBuff) {
         if (null != perfData.getRefreshRate() && perfData.getRefreshRate() > 0) {
            perfBuff = new PerformanceDataBuffer(
                  perfData.getRefreshRate(),
                  pollFreq,
                  stallTimeout,
                  perfThresholds
            );
         } else {
            // couldn't add, ignore silently because the simulator
            // will do this to us
            return;
         }
      }

      try {
         perfBuff.add(perfData);
      } catch (IllegalArgumentException e) {
         parentLogger.warn("Could not add performance data, ignoring", e);
      }
   }

   public boolean isStalled() {
      return (perfBuff != null) && perfBuff.isFull();
   }

   public void unstall() {
      if (perfBuff != null) {
         perfBuff.clear();
      }
   }
}