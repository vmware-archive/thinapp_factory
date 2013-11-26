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

package com.vmware.appfactory.notification.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.vmware.appfactory.build.service.BuildService;
import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.dto.WorkpoolAndImageFailCount;
import com.vmware.appfactory.common.runner.WorkpoolTracker;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.notification.ActionAlert;
import com.vmware.appfactory.notification.ActionAlert.AlertType;
import com.vmware.appfactory.notification.ActionAlert.Group;
import com.vmware.appfactory.notification.Event;
import com.vmware.appfactory.notification.NotificationService;
import com.vmware.appfactory.taskqueue.dto.CaptureTaskSummary;
import com.vmware.appfactory.taskqueue.tasks.MetaStatusPredicate;
import com.vmware.appfactory.taskqueue.tasks.state.AbstractCaptureState;
import com.vmware.appfactory.taskqueue.tasks.state.AppConvertState;
import com.vmware.appfactory.taskqueue.tasks.state.ManualModeState;
import com.vmware.appfactory.taskqueue.tasks.state.RebuildState;
import com.vmware.appfactory.taskqueue.tasks.state.TaskState;
import com.vmware.thinapp.common.datastore.dto.Datastore;

/**
 * This controller implements Api to retrieve events from
 * the notification service.
 *
 * @author saung
 * @since M8 8/19/2011
 */
@SuppressWarnings("MissortedModifiers")
@Controller
public class NotificationApiController extends AbstractApiController
{
   /** The fail state for Convert, MM, Rebuild, Feed tasks. */
   private static final String TASK_STATE_FAILED = "failed";

   @Resource
   WorkpoolTracker _wpTracker;

   @Resource
   BuildService _buildService;

   final static int LOW_FREE_DISK_SPACE_THRESHOLD_PCT = 10;

   /**
    * Get all events from the notification service since last call. It sets
    * most recent event's time stamp in the response so that the latter request
    * can pull the new events only.\
    *
    * @param request - a HttpServletRequest
    * @param response - a HttpServeltResponse
    * @return
    *  1. all events if a client is calling first time.
    *  2. null if there is no new events since the last call.
    *  3. a list of events created after the last call.
    * @throws Exception
    */
   @RequestMapping(
         value="/notification",
         method=RequestMethod.GET)
   public @ResponseBody Iterator<Event> get(HttpServletRequest request,
         HttpServletResponse response)
      throws Exception
   {
      final Event mostRecentEvent = NotificationService.INSTANCE.getMostRecentEvent();
      // set default value to the new token.
      String newToken = "-1";

      if (mostRecentEvent != null) {
         newToken = Long.toString(mostRecentEvent.getTimeStamp());
      }

      String prevToken = applyETagCache(request, response, newToken, false);
      if (prevToken == null) {
         // First-time calling: return all events.
         return NotificationService.INSTANCE.iterator();
      } else if (prevToken.equals(newToken)) {
         // Nothing has changed from the last call.
         return null;
      } else {
         long timeStamp = Long.valueOf(prevToken).longValue();
         return NotificationService.INSTANCE.iteratorAfter(timeStamp);
      }
   }


   /**
    * Get a consolidated status of all conversison's progress and a list of
    * alert actions.
    *
    * @return
    */
   @RequestMapping(
         value="/notify/alertAndProgress",
         method=RequestMethod.GET)
   public @ResponseBody CaptureTaskSummary getCaptureAndAlertSummary()
   {
      long progressTotal = 0;
      int taskCount = 0;
      Iterable<TaskState> taskList = _conversionsQueue.getTasks(MetaStatusPredicate.NOT_FINISHED);
      Iterable<TaskState> captureStates = Iterables.filter(
                  taskList, Predicates.instanceOf(AbstractCaptureState.class));
      for (TaskState state : captureStates) {
         progressTotal += state.getProgress();
         taskCount++;
      }
      Iterable<TaskState> runningTasks = Iterables.filter(taskList,
            Predicates.and(
                  MetaStatusPredicate.RUNNING,
                  Predicates.instanceOf(AbstractCaptureState.class)
            ));

      // Compute the average progress
      if (taskCount != 0) {
         progressTotal /= taskCount;
      }

      // Create the dto and set the computed values and queued up capture tasks
      CaptureTaskSummary summary = new CaptureTaskSummary(
            Iterables.size(runningTasks),
            (int)progressTotal,
            getWaitingCaptureTaskCount());

      // Set the number of failed and wait-on-user alerts.
      List<ActionAlert> aaList = getWorkpoolAndImageFailedAlerts();
      aaList.addAll(getUserWaitAndFailedAlerts());
      aaList.addAll(getFeedFailedAlerts());
      aaList.addAll(getLowDiskSpaceAlerts());

      // Add the list of action alerts with the latest alert event appearing on top.
      Collections.sort(aaList);
      summary.setActionList(aaList);

      // Return the summary info for display.
      return summary;
   }


   /**
    * Get the number of waiting capture tasks.
    *
    * @return
    */
   private int getWaitingCaptureTaskCount()
   {
      // Now compute the number of waiting capture tasks.
      Iterable<TaskState> waitingCaptureStates =
            Iterables.filter(
                  _conversionsQueue.getTasks(MetaStatusPredicate.WAITING),
                  Predicates.instanceOf(AbstractCaptureState.class));
      return Iterables.size(waitingCaptureStates);
   }


   /**
    * Get the workpool and vmImage fail alerts using workpoolTracker.
    *
    * @return
    */
   private List<ActionAlert> getWorkpoolAndImageFailedAlerts()
   {
      WorkpoolAndImageFailCount wpImgFail = _wpTracker.getWpImgFailCount();

      List<ActionAlert> alerts = new ArrayList<ActionAlert>(2);
      if (wpImgFail.getWpFailCount() > 0) {
         ActionAlert wpAlert = new ActionAlert(
               AlertType.error,
               Group.workpool,
               wpImgFail.getWpFailCount(),
               wpImgFail.getTimestamp());
         alerts.add(wpAlert);
      }
      if (wpImgFail.getVmImageFailCount() > 0) {
         ActionAlert imgAlert = new ActionAlert(
               AlertType.error,
               Group.image,
               wpImgFail.getVmImageFailCount(),
               wpImgFail.getTimestamp());
         alerts.add(imgAlert);
      }
      return alerts;
   }


   /**
    * Get the various event types and actions handlers for each.
    *
    * @return
    */
   private List<ActionAlert> getUserWaitAndFailedAlerts()
   {
      ActionAlert buildAction = new ActionAlert(AlertType.error, Group.build);
      ActionAlert captureAction = new ActionAlert(
            AlertType.error, Group.capture);

      List<ActionAlert> actionList = new ArrayList<ActionAlert>();

      // Now compute the number of waiting capture tasks.
      Iterable<TaskState> taskList = _conversionsQueue.getTasks(MetaStatusPredicate.FINISHED);

      for (TaskState currentTaskState : taskList) {
         if (TASK_STATE_FAILED.equals(currentTaskState.getStatus().name())) {
            if (currentTaskState.getType().equals(ManualModeState.TYPE)
                  || currentTaskState.getType().equals(AppConvertState.TYPE)) {
               captureAction.incrementCount();
               captureAction.setIfRecentTimestamp(currentTaskState.getFinished());
            }
            else if (currentTaskState.getType().equals(RebuildState.TYPE)) {
               buildAction.incrementCount();
               buildAction.setIfRecentTimestamp(currentTaskState.getFinished());
            }
         }
      }
      if (buildAction.getCount() != 0) {
         actionList.add(buildAction);
      }
      if (captureAction.getCount() != 0) {
         actionList.add(captureAction);
      }

      // Now get the manual mode tasks that are in running mode, but are
      // blocked and waiting for user.
      taskList = _conversionsQueue.getTasks(MetaStatusPredicate.RUNNING);
      for (TaskState taskState : taskList) {
         if (ManualModeState.ManualModeStatus.WAITING_FOR_USER.equals(taskState.getStatus())
               && taskState instanceof ManualModeState) {
            AbstractCaptureState mmState = (AbstractCaptureState)taskState;

            String statusLink = null;
            if (mmState.getStatusLink() != null) {
               statusLink = mmState.getStatusLink();
            }
            actionList.add(new ActionAlert(
                  AlertType.warn,
                  Group.manualCapture,
                  1,
                  mmState.getFinished(),
                  statusLink,
                  mmState.getCaptureRequest().getBuildName()));
         }
      }

      Collections.sort(actionList);

      return actionList;
   }

   /**
    * Add alerts for feeds being in error.
    *
    * @return  a list of action alerts to add regarding feed errors.
    * At the moment, this will be either an empty list or a list of
    * a single element.
    */
   private List<ActionAlert> getFeedFailedAlerts()
   {
      long failedCount = _daoFactory.getFeedDao().countFailed();
      if (failedCount > 0) {
         ActionAlert feedAction = new ActionAlert(AlertType.error, Group.feed);
         feedAction.setCount((int)failedCount);
         return ImmutableList.of(feedAction);
      }

      return Collections.emptyList();
   }


   /**
    * Returns a single alert if one or more datastores is "low" on disk
    * space.  This is determined by a datastore
    * @return
    */
   private Collection<ActionAlert> getLowDiskSpaceAlerts()
   {
      Collection<DsDatastore> dsList = _buildService.getCachedDatastores();
      int countLowDiskSpace = 0;
      for (DsDatastore datastore : dsList) {
         if (datastore.getStatus() == Datastore.Status.online) {
            int pctFree = datastore.getPctFree();
            if (pctFree != -1 && pctFree <= LOW_FREE_DISK_SPACE_THRESHOLD_PCT) {
               ++countLowDiskSpace;
            }
         }
      }
      if (countLowDiskSpace > 0) {
            ActionAlert dsAlert = new ActionAlert(AlertType.warn, Group.datastore);
            dsAlert.setCount(countLowDiskSpace);
            return ImmutableList.of(dsAlert);
         }
      return Collections.emptyList();
   }
}
