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

package com.vmware.thinapp.workpool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Service to monitor blocking processes and enable another thread to kill the
 * process.
 */
@Service
public class ProcessMonitorService {
   private final Map<Long, Process> commandIdToProcess;

   public ProcessMonitorService() {
      commandIdToProcess = new ConcurrentHashMap<Long, Process>();
   }

   /**
    * Cancel the running process that was associated with the given command ID.
    * If the given command ID is null or no running process is associated with
    * the ID, this method does nothing.
    *
    * @param commandId the ID of the command to cancel
    */
   public void cancel(Long commandId) {
      if (commandId != null) {
         Process proc = commandIdToProcess.get(commandId);
         if (proc != null) {
            proc.destroy();
         }
      }
   }

   /**
    * Associate the given process with the given command ID.  This process can
    * be terminated by calling {@link #cancel(Long)} with the same command ID.
    *
    * @param commandId the command ID to associate with the given process
    * @param process the process
    */
   public void add(Long commandId, Process process) {
      if (commandId != null) {
         commandIdToProcess.put(commandId, process);
      }
   }

   /**
    * Remove the process with the given command ID.  Does nothing if no process
    * is currently associated with the given command ID.
    *
    * @param commandId the ID of the process to remove.
    */
   public void remove(Long commandId) {
      if (commandId != null) {
         commandIdToProcess.remove(commandId);
      }
   }
}
