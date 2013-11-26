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

package com.vmware.appfactory.push;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Iterables;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.config.model.TaskEvent;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.ImportProjectState;
import com.vmware.appfactory.taskqueue.tasks.state.ManualModeState;
import com.vmware.appfactory.taskqueue.tasks.state.RebuildState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;

/**
 * This controller handles all the job-related API calls to AppFactory.
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class PushController
      extends AbstractApiController
      implements ApplicationListener<TaskEvent>
{
   private final ObjectMapper mapper;
   private final EventBroadcastingRunnable eventBroadcastingRunnable;
   private final Semaphore broadcastSemaphore;

   private Broadcaster taskEventBroadcaster;

   public PushController() throws InstantiationException, IllegalAccessException, ServletException {
      mapper = new ObjectMapper();
      eventBroadcastingRunnable = new EventBroadcastingRunnable();
      broadcastSemaphore = new Semaphore(1, true);
   }

   /**
    * Handle an application event.
    *
    * @param event the event to respond to
    */
   @Override
   public void onApplicationEvent(@Nullable TaskEvent event) {
      if (null != event) {
         if (event.getTaskType().equals(AppConvertState.TYPE)
               || event.getTaskType().equals(ManualModeState.TYPE)
               || event.getTaskType().equals(ImportProjectState.TYPE)
               || event.getTaskType().equals(RebuildState.TYPE)) {
            eventBroadcastingRunnable.addEvent(event);
         }
      }
   }

   /**
    * On connection, produces a list of tasks.
    * If not tasks are available, produces the string "NO_TASKS".
    *
    * Afterwards, the connection will remain open, sending events about each
    * change to the task queue.
    * @param event   supplied by the Atmosphere servlet.  Used to broadcast
    *                events to the client.
    *
    * @throws IOException
    * If the response could not be serialized.
    *
    * @throws InterruptedException
    * If we are blocked while writing responses.
    */
   @ResponseBody
   @RequestMapping(
         value="/conversion-tasks-comet",
         method=RequestMethod.GET)
   public void getConversionTasksComet(final AtmosphereResource<HttpServletRequest,HttpServletResponse> event)
         throws IOException, InterruptedException {

      initializeBroadcaster();

      // note: calling setScope() will CHANGE the value returned by getBroadcaster() !
      event.getBroadcaster().setScope(Broadcaster.SCOPE.REQUEST);
      final Broadcaster localBroadcaster = event.getBroadcaster();

      // synchronize on "event" to block other updates from being sent until we
      // send out our task list.
      boolean acquired = broadcastSemaphore.tryAcquire(20, TimeUnit.SECONDS);
      if (!acquired) {
         throw new IllegalStateException("Could not get broadcast semaphore");
      }
      try {
         event.suspend();
         taskEventBroadcaster.addAtmosphereResource(event);

         // spit out a break in between the atmosphere header and the first line,
         // so that the client can split messages apart.
         localBroadcaster.broadcast("\n\n");

         Iterable<TaskState> tasks = _conversionsQueue.getAllTasks();
         if (Iterables.isEmpty(tasks)) {
            localBroadcaster.broadcast("{\"type\":\"NO-TASKS\"}\n\n");
         }
         for (TaskState state: tasks) {
            String message = mapper.writeValueAsString(TaskEvent.newTaskAdded(state));
            localBroadcaster.broadcast(message + "\n\n");
         }
      } finally {
         broadcastSemaphore.release();
      }
   }

   private void initializeBroadcaster() {
      if (null == taskEventBroadcaster) {
         BroadcasterFactory factory = BroadcasterFactory.getDefault();
         if (null != factory) {
            taskEventBroadcaster = factory.get(DefaultBroadcaster.class, "task-updates");
            Executors.newSingleThreadExecutor().submit(eventBroadcastingRunnable);
         }
      }
      if (null == taskEventBroadcaster) {
         throw new IllegalStateException("Could not create broadcaster");
      }
   }

   private class EventBroadcastingRunnable implements Runnable {

      private final BlockingQueue<TaskEvent> notificationEvents;

      private EventBroadcastingRunnable() {
         notificationEvents = new LinkedBlockingQueue<TaskEvent>(1000);
      }

      /**
      * Loop forever, pulling from the notificationEvents queue.
      */
      @Override
      public void run() {
        while (true) {
           try {
              TaskEvent event = notificationEvents.take();
              Object shortEvent = null;

              try {
                 String message = mapper.writeValueAsString(event);
                 while (true) {
                    boolean acquired = broadcastSemaphore.tryAcquire(20, TimeUnit.SECONDS);
                    if (acquired) {
                       try {
                          taskEventBroadcaster.broadcast(message + "\n\n");
                          break;
                       } finally {
                          broadcastSemaphore.release();
                       }
                    } else {
                       _log.error("timed out while waiting for broadcastSemaphore!");
                    }
                 }
              } catch (IOException e) {
                 _log.error("Could not convert event to string " + shortEvent);
              } catch (InterruptedException e) {
                 // interrupted while waiting for semaphore
                 _log.error("Thread interrupted while waiting for semaphore, aborting", e);
                 // re-set the interrupted flag
                 Thread.currentThread().interrupt();
              }
           } catch (InterruptedException e) {
              _log.debug("Interrupted while waiting for more notification events.  May be shutting down.", e);
              break;
           }
        }
      }

      public void addEvent(@Nonnull TaskEvent event) {
         boolean added = notificationEvents.offer(event);
         if (!added) {
            _log.error("Could not add event to broadcast queue: " + event);
         }
      }
   }
}