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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;
import com.vmware.thinapp.common.converter.dto.PerformanceData;
import com.vmware.thinapp.common.converter.exception.ConverterException;
import com.vmware.thinapp.workpool.Util;
import com.vmware.thinapp.workpool.VCManager;
import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.PerformanceManager;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

import scala.Option;

/**
 * Service to monitor performance of capture VMs.
 */
@Service
public class JobMonitorService {
   private static final int MIN_PERF_TASK_THREADS = 1;
   private final Logger log = LoggerFactory.getLogger(JobMonitorService.class);
   private final Map<JobMonitorTicket, ScheduledFuture<?>> perfTasks;
   private final ScheduledExecutorService executor;

   @Autowired
   private VCManager vcManager;

   public static class JobMonitorTicket implements Comparable<JobMonitorTicket> {
      private final String id;
      private final Integer refreshRate;

      private JobMonitorTicket(String id, Integer refreshRate) {
         this.id = id;
         this.refreshRate = refreshRate;
      }

      public String getId() {
         return id;
      }

      public Integer getRefreshRate() {
         return refreshRate;
      }

      @Override
      public int hashCode() {
         return new HashCodeBuilder(53, 85).append(id).toHashCode();
      }

      @Override
      public String toString() {
         return ReflectionToStringBuilder.toString(this);
      }

      @Override
      public int compareTo(JobMonitorTicket o) {
         return new CompareToBuilder().append(this.id, o.id).toComparison();
      }
   }

   public JobMonitorService() {
      perfTasks = new ConcurrentHashMap<JobMonitorTicket, ScheduledFuture<?>>();
      executor = Executors.newScheduledThreadPool(MIN_PERF_TASK_THREADS);
   }

   /**
    * Begin monitoring the VM with the given moid.
    *
    * @param vmMoid the moid of the VM to begin monitoring.
    * @return the refresh rate for monitoring the given VM
    */
   public JobMonitorTicket startMonitoring(
         String vmMoid,
         PerformanceDataListener listener) {
      log.debug("Attempting to start performance monitoring for VM {}.", vmMoid);
      ServiceInstance scv = vcManager.getConnection().get().get();
      VirtualMachine vm = Util.findVm(vmMoid, scv);
      if (vm == null) {
         throw new ConverterException(
               String.format("Virtual Machine '%s' cannot be found.", vmMoid));
      }

      PerformanceManager perfMgr = scv.getPerformanceManager();

      // Get the refresh rate for the VC we're connected to
      Option<Integer> refreshRate = getRefreshRate(perfMgr, vm, log);
      if (refreshRate.isEmpty()) {
         throw new ConverterException("Unable to obtain performance data refresh rate.");
      }
      log.debug("Obtained performance data refresh rate: {}", refreshRate.get());

      // Create a query to use when polling for performance data
      Map<Integer, String> counterIdToName = new HashMap<Integer, String>();
      Option<PerfQuerySpec> querySpec = createQuerySpec(
               refreshRate.get(),
               vm,
               perfMgr,
               counterIdToName,
               log,
               PerformanceData.QUERY_COUNTERS_ALL);
      if (querySpec.isEmpty()) {
         throw new ConverterException("Unable to create query spec for monitoring performance.");
      } else if (!counterIdToName.values().containsAll(PerformanceData.QUERY_COUNTERS_REQUIRED)) {
         throw new ConverterException("Creating query spec failed: Unable to create query with " +
               "all required performance counters.");
      }

      // Create a task that can poll for performance data
      PerformancePollTask task = new VCPerformancePollTask(
            vmMoid,
            perfMgr,
            querySpec.get(),
            counterIdToName,
            listener);

      // Attempt to query once initially.  If this query fails, assume no other
      // queries will succeed and don't monitor the VM.
      try {
         task.run();
      } catch (ConverterException ex) {
         log.debug("Initial query failed, not monitoring VM {}.", vmMoid);
         // Querying failed, don't monitor this VM
         throw ex;
      }

      // The query worked, so schedule a task to poll for performance data
      ScheduledFuture<?> res = executor.scheduleWithFixedDelay(
            task,
            refreshRate.get(),
            refreshRate.get(),
            TimeUnit.SECONDS);

      // Remember the task handle so we can cancel it later when the job stops
      JobMonitorTicket ticket = getTicket(vmMoid, refreshRate.get());
      perfTasks.put(ticket, res);

      return ticket;
   }

   /**
    * Stop monitoring the VM with the given moid.
    *
    * @param vmMoid the moid of the VM to stop monitoring.
    */
   public void stopMonitoring(JobMonitorTicket ticket) {
      if (ticket != null) {
         ScheduledFuture<?> taskHandle = perfTasks.remove(ticket);
         if (taskHandle != null) {
            log.debug("Stopping performance monitoring for VM {}.", ticket.getId());
            taskHandle.cancel(true);
         }
      }
   }

   private JobMonitorTicket getTicket(String id, Integer refreshRate) {
      return new JobMonitorTicket(id, refreshRate);
   }

   /**
    * Create a query spec using the given refresh rate, VM,
    * performance manager, and counter strings.  The query will specify
    * performance counters for as many of the counter strings as were able to
    * be found.  It is the caller's responsibility to verify that the metricId
    * array has the proper number of values.
    *
    * @param refreshRate
    * @param vm
    * @param perfMgr
    * @param counters strings such as "cpu.usage.average", and "disk.usage.average"
    * @return the query if processing was successful, empty otherwise
    */
   private static Option<PerfQuerySpec> createQuerySpec(
         Integer refreshRate,
         VirtualMachine vm,
         PerformanceManager perfMgr,
         Map<Integer, String> counterIdToName,
         Logger log,
         ImmutableSet<String> counters) {
      // Obtain all available performance metric IDs
      PerfMetricId[] perfMetricIds = null;
      try {
         perfMetricIds = perfMgr.queryAvailablePerfMetric(vm, null, null,
            refreshRate);
      } catch (RemoteException ex) {
         log.debug("Unable to obtain collection of available performance metris:", ex);
         return Option.empty();
      }
      if (perfMetricIds == null) {
         log.debug("Unable to obtain collection of available performance metrics.");
         return Option.empty();
      }
      log.debug("Obtained collection of available performance metrics with size={}", perfMetricIds.length);

      // Build a set of just the performance counter IDs and build a map from
      // these IDs to the associated PerfMetricId
      int[] perfCounterIds = new int[perfMetricIds.length];
      Map<Integer, PerfMetricId> perfCounterIdToMetricId = new HashMap<Integer, PerfMetricId>();
      for(int i = 0; i < perfMetricIds.length; i++) {
         PerfMetricId id = perfMetricIds[i];
         perfCounterIds[i]=id.counterId;
         perfCounterIdToMetricId.put(id.counterId, perfMetricIds[i]);
      }

      // Get all performance counter info objects
      PerfCounterInfo[] perfCounterInfos = null;
      try {
         perfCounterInfos = perfMgr.queryPerfCounter(perfCounterIds);
      } catch (RemoteException ex) {
         log.debug("Unable to obtain collection of available performance counter infos:", ex);
         return Option.empty();
      }
      if (perfCounterInfos == null) {
         log.debug("Unable to obtain collection of available performance counter infos.");
         return Option.empty();
      }
      log.debug("Obtained collection of performance counter infos with size={}", perfCounterIds.length);

      // Now search for each of the given counter strings
      List<PerfMetricId> queryMetricIds = new ArrayList<PerfMetricId>();
      for (PerfCounterInfo perfCounterInfo : perfCounterInfos) {
         String counter = createCounterString(perfCounterInfo);
         if (counters.contains(counter)) {
            PerfMetricId currMetricId = perfCounterIdToMetricId.get(perfCounterInfo.getKey());
            if (currMetricId == null) {
               log.debug("Found matching perfCounterInfo but no metric ID: {}", counter, perfCounterInfo.getKey());
               log.debug(prettyPrint(perfCounterInfo));
            } else {
               if (!counterIdToName.containsKey(currMetricId.counterId)) {
                  queryMetricIds.add(currMetricId);
                  counterIdToName.put(currMetricId.counterId, counter);
                  log.debug("Found matching perfCounterInfo: {}", counter);
               } else {
                  log.debug("Found duplicate perfCounterInfo: {}", counter);
               }
               log.debug(prettyPrint(perfCounterInfo));
               log.debug(prettyPrint(currMetricId));
            }
         }
      }

      // Create the query spec using the counters we were able to find
      PerfQuerySpec qSpec = createPerfQuerySpec(vm, queryMetricIds.toArray(new PerfMetricId[queryMetricIds.size()]), refreshRate);
      return Option.apply(qSpec);
   }

   private static String prettyPrint(PerfMetricId perfMetricId) {
      return String.format("PerfMetricId: counterId=%s instance=%s",
            perfMetricId.getCounterId(),
            perfMetricId.getInstance());
   }

   private static String prettyPrint(PerfCounterInfo perfCounterInfo) {
      StringBuilder builder = new StringBuilder();
      builder.append("PerfCounterInfo:\n");
      builder.append(String.format("  key=%s level=%s rollupType=%s statsType=%s\n",
            perfCounterInfo.getKey(),
            perfCounterInfo.getLevel(),
            perfCounterInfo.getRollupType(),
            perfCounterInfo.getStatsType()));
      builder.append(String.format("  groupInfo=\n%s\n",
            prettyPrint(perfCounterInfo.getGroupInfo())));
      builder.append(String.format("  nameInfo=\n%s\n",
            prettyPrint(perfCounterInfo.getNameInfo())));
      builder.append(String.format("  unitInfo=\n%s",
            prettyPrint(perfCounterInfo.getUnitInfo())));

      return builder.toString();
   }

   private static String prettyPrint(ElementDescription elementDescription) {
      StringBuilder builder = new StringBuilder();
      builder.append("    ElementDescription:\n");
      builder.append(String.format("      key=%s\n", elementDescription.key));
      builder.append(String.format("      label=%s\n", elementDescription.label));
      builder.append(String.format("      summary=%s", elementDescription.summary));

      return builder.toString();
   }

   private static String createCounterString(PerfCounterInfo info) {
      return String.format("%s.%s.%s",
            info.getGroupInfo().getKey(),
            info.getNameInfo().getKey(),
            info.getRollupType());
   }

   private static PerfQuerySpec createPerfQuerySpec(
         ManagedEntity me,
         PerfMetricId[] metricIds,
         int interval) {
      PerfQuerySpec qSpec = new PerfQuerySpec();
      qSpec.setEntity(me.getMOR()); // Gather data from the given ME
      qSpec.setMaxSample(1); // Just gather a single data point, no historical data
      qSpec.setMetricId(metricIds); // Gather data for the given counter IDs
      qSpec.setFormat("normal"); // Normal reults, "csv" is another option
      qSpec.setIntervalId(interval); // Define the refresh interval
      return qSpec;
   }

   /**
    * Obtain the fastest refresh rate for the given performance manager and VM.
    *
    * @param perfMgr
    * @param vm
    * @return the fastest supported refresh rate or empty if query fails
    */
   private static Option<Integer> getRefreshRate(
         PerformanceManager perfMgr,
         VirtualMachine vm,
         Logger log) {
      PerfProviderSummary pps;
      try {
         pps = perfMgr.queryPerfProviderSummary(vm);
      } catch (RemoteException ex) {
         log.debug("Unable to obtain performance provider summary");
         return Option.empty();
      }

      return Option.apply(pps.getRefreshRate());
   }
}
