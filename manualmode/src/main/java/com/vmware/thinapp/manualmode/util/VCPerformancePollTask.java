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

package com.vmware.thinapp.manualmode.util;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PerfSampleInfo;
import com.vmware.vim25.mo.PerformanceManager;

public class VCPerformancePollTask implements PerformancePollTask {
   private final Logger log = LoggerFactory.getLogger(VCPerformancePollTask.class);
   private final String vmMoid;
   private final PerformanceManager perfMgr;
   private final PerfQuerySpec qSpec;
   private final Map<Integer, String> counterIdToName;
   private final PerformanceDataListener listener;

   public VCPerformancePollTask(
         String vmMoid,
         PerformanceManager perfMgr,
         PerfQuerySpec qSpec,
         Map<Integer, String> counterIdToName,
         PerformanceDataListener listener) {
      this.vmMoid = vmMoid;
      this.perfMgr = perfMgr;
      this.qSpec = qSpec;
      this.counterIdToName = counterIdToName;
      this.listener = listener;
   }

   /**
    * Query for performance data about the VM with the given moid.
    *
    * @param vmMoid the moid of the VM
    */
   @Override
   public void run() {
      PerfEntityMetricBase[] pValues = null;
      try {
         pValues = perfMgr.queryPerf(
               new PerfQuerySpec[] { qSpec });
      } catch (RemoteException ex) {
         throw new ConverterException(
               String.format("Quering performance data for VM %s failed.", vmMoid),
               ex);
      }

      if (pValues != null && pValues.length == 1) {
         processPerformanceValues(pValues[0]);
      } else {
         if (pValues == null) {
            log.debug("Performance query returned null.");
         } else {
            log.debug("Performance query returned more than one value: {}", pValues.length);
         }
         throw new ConverterException("Performance query failed.");
      }
   }

   private void processPerformanceValues(PerfEntityMetricBase pmeBase) {
      if (!(pmeBase instanceof PerfEntityMetric)) {
         throw new ConverterException("Unexpected sub-type of PerfEntityMetricBase.");
      }

      PerfEntityMetric pme = (PerfEntityMetric) pmeBase;

      PerfSampleInfo[] perfSampleInfoArray = pme.getSampleInfo();
      if (perfSampleInfoArray == null) {
         throw new ConverterException("Was not given any sample info values.");
      } else if (perfSampleInfoArray.length != 1) {
         log.debug("perfSampleInfoArray.length={}", perfSampleInfoArray.length);
         throw new ConverterException("Was not given a single sample info value.");
      }

      Date timestamp;
      try {
         timestamp = perfSampleInfoArray[0].getTimestamp().getTime();
      } catch (NullPointerException ex) {
         throw new ConverterException(
               "Unable to get timestamp for performance data.",
               ex);
      }

      PerfMetricSeries[] seriesArray = pme.getValue();
      StringBuilder perfResults = new StringBuilder();
      Map<String, Long> perfValues = new HashMap<String, Long>();

      for (int i = 0; i<seriesArray.length; i++) {
         PerfMetricSeries series = seriesArray[i];

         int perfCounterId;
         try {
            perfCounterId = series.getId().getCounterId();
         } catch (NullPointerException ex) {
            throw new ConverterException(
                  "Unable to get performance counter ID for current series.",
                  ex);
         }

         long perfValue;
         if (series instanceof PerfMetricIntSeries) {
            PerfMetricIntSeries intSeries = (PerfMetricIntSeries) series;
            long[] longs = intSeries.getValue();
            if (longs == null || longs.length != 1) {
               throw new ConverterException(
                     "Was not given a single data point to log.");
            }
            perfValue = longs[0];
         } else {
            throw new ConverterException(
                  "Performance series object is an unexpected type:" +
                  series.getClass().toString() +
                  " (expected PerfMetricIntSeries).");
         }

         perfResults.append(String.format("%s=%s ",
               counterIdToName.get(perfCounterId),
               perfValue));

         // perfValue is -1 when VC isn't yet providing performance data
         if (perfValue > -1) {
            perfValues.put(counterIdToName.get(perfCounterId), perfValue);
         }
      }

      listener.update(timestamp, perfValues);

      try {
         log.debug(String.format("Performance data for %s %s %s: %s",
               pme.getEntity().getType(),
               pme.getEntity().get_value(),
               timestamp,
               perfResults.toString().trim()));
      } catch (NullPointerException ex) {
         throw new ConverterException(
               "Unable to create performance log message.",
               ex);
      }
   }
}
