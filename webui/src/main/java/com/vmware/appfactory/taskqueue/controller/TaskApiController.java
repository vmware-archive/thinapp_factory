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

package com.vmware.appfactory.taskqueue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.appfactory.common.exceptions.AfNotFoundException;
import com.vmware.appfactory.taskqueue.tasks.MetaStatusPredicate;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;


/**
 * This controller handles all the job-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@SuppressWarnings("MissortedModifiers")
@Controller
public class TaskApiController
   extends AbstractApiController
{
   /**
    * Get a JSON array containing all the jobs in AppFactory, regardless
    * of state.
    *
    * @param metaStatus If not null, return only jobs with this type of status.
    * @return
    */
   @RequestMapping(
         value="/tasks",
         method=RequestMethod.GET)
   public @ResponseBody Iterable<TaskState> getAllTasks(
         @RequestParam(required=false) TaskQueue.MetaStatus metaStatus)
   {
      Iterable<TaskState> tasks;

      if (metaStatus != null) {
         tasks = _conversionsQueue.getTasks(MetaStatusPredicate.custom(metaStatus));
      }
      else {
         tasks = _conversionsQueue.getAllTasks();
      }

      return tasks;
   }


   /**
    * Get a single task.
    * @param taskId
    * @return
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/tasks/{taskId}",
         method=RequestMethod.GET)
   public @ResponseBody
   TaskState getTaskState(@PathVariable Long taskId)
      throws AfNotFoundException
   {
      TaskState task = _conversionsQueue.findTaskById(taskId);
      if (task == null) {
         throw new AfNotFoundException("Invalid task ID " + taskId);
      }

      return task;
   }


   /**
    * Delete all FINISHED tasks from AppFactory.
    */
   @RequestMapping(
         value="/tasks/cleanup",
         method=RequestMethod.POST)
   public @ResponseBody void cleanup()
   {
      _conversionsQueue.cleanup();
   }


   /**
    * Move a single queued task to the top of the queue.
    *
    * @param taskId
    * task to move
    *
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value="/tasks/{taskId}/moveToTop",
         method=RequestMethod.POST)
   public Boolean moveToTop(@PathVariable Long taskId)
         throws AfNotFoundException
   {
      return _conversionsQueue.moveToHead(taskId);
   }

   /**
    * Move a single queued task to the bottom of the queue.
    *
    * @param taskId
    * task to move
    *
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value="/tasks/{taskId}/moveToBottom",
         method=RequestMethod.POST)
   public Boolean moveToBottom(@PathVariable Long taskId)
   {
      return _conversionsQueue.moveToTail(taskId);
   }


   /**
    * Move a single queued task to the bottom of the queue.
    *
    * @param taskId
    * task to move
    * @param taskIdBefore
    * task to follow
    *
    * @throws AfNotFoundException
    */
   @ResponseBody
   @RequestMapping(
         value="/tasks/{taskId}/moveBefore",
         method=RequestMethod.POST)
   public Boolean moveToBottom(@PathVariable Long taskId,
                               @RequestParam(required=true) Long taskIdBefore)
   {
      return _conversionsQueue.moveBefore(taskId, taskIdBefore);
   }


   /**
    * Abort all tasks.
    *
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/tasks/abortAll",
         method=RequestMethod.POST)
   public @ResponseBody void abortAllTasks()
         throws AfNotFoundException
   {
      _conversionsQueue.abortAllTasks();
   }

   /**
    * Abort a single task.
    *
    * @param taskId
    * @throws AfNotFoundException
    */
   @RequestMapping(
         value="/tasks/{taskId}/abort",
         method=RequestMethod.POST)
   public @ResponseBody void abortTask(@PathVariable Long taskId)
      throws AfNotFoundException
   {
      TaskState task = _conversionsQueue.abortTask(taskId);
      if (task == null) {
         throw new AfNotFoundException("Invalid task ID " + taskId);
      }
   }

   /**
    * Clean up a single task.
    *
    * @param taskId task ID to remove.  This should represent a FINISHED task.
    *
    * @throws AfNotFoundException
    * @return  true if a finished task with the given ID was found and removed,
    * false otherwise
    */
   @RequestMapping(
         value="/tasks/{taskId}/cleanup",
         method=RequestMethod.POST)
   public @ResponseBody boolean cleanTask(@PathVariable Long taskId)
         throws AfNotFoundException
   {
      return _conversionsQueue.cleanup(taskId);
   }

   /**
    * Unstall a task that is in the stalled state.
    *
    * @param taskId
    * @throws AfNotFoundException
    * @throws AfBadRequestException
    */
   @RequestMapping(
         value="/tasks/{taskId}/unstall",
         method=RequestMethod.POST)
   public @ResponseBody void unstallTask(@PathVariable Long taskId)
      throws AfNotFoundException, AfBadRequestException
   {
      TaskState state = _conversionsQueue.unstallConversion(taskId);
      if (state == null) {
         throw new AfNotFoundException("Invalid task ID " + taskId);
      }
   }
}
