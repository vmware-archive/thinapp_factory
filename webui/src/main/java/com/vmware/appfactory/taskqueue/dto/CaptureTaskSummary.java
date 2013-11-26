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

package com.vmware.appfactory.taskqueue.dto;

import java.util.ArrayList;
import java.util.List;

import com.vmware.appfactory.notification.ActionAlert;

/**
 * This object represents the capture tasks summarized status.
 * Status includes, number of running tasks, and their consolidated progress.
 * The number of waiting tasks that need to be captured.
 */
public class CaptureTaskSummary
{
   /** Indicates total running capture tasks */
   private int _runTotal;

   /** Indicates the average running task progress */
   private int _runTotalProgress;

   /** Indicates the total queued up capture tasks */
   private int _waitTotal;

   /** Indicates the different action alerts user can act upon. */
   private List<ActionAlert> _actionList = new ArrayList<ActionAlert>();

   /**
    * Default constructor.
    */
   public CaptureTaskSummary()
   {
      //do nothing
   }


   /**
    * Constructor setting runTotal, runTotalProgress, waitTotal
    *
    * @param runTotal
    * @param runTotalProgress
    * @param waitTotal
    */
   public CaptureTaskSummary(int runTotal, int runTotalProgress, int waitTotal)
   {
      this._runTotal = runTotal;
      this._runTotalProgress = runTotalProgress;
      this._waitTotal = waitTotal;
   }


   /**
    * Constructor setting runTotal, runTotalProgress
    *
    * @param runTotal
    * @param runTotalProgress
    */
   public CaptureTaskSummary(int runTotal, int runTotalProgress)
   {
      this._runTotal = runTotal;
      this._runTotalProgress = runTotalProgress;
      this._waitTotal = 0;
   }


   /**
    * @return the _runTotal
    */
   public int getRunTotal()
   {
      return _runTotal;
   }

   /**
    * @param runTotal the _runTotal to set
    */
   public void setRunTotal(int runTotal) {
      this._runTotal = runTotal;
   }


   /**
    * @return the _runTotalProgress
    */
   public int getRunTotalProgress()
   {
      return _runTotalProgress < 0 ? 0 : _runTotalProgress;
   }


   /**
    * @param runTotalProgress the _runTotalProgress to set
    */
   public void setRunTotalProgress(int runTotalProgress)
   {
      this._runTotalProgress = runTotalProgress;
   }


   /**
    * @return the _waitTotal
    */
   public int getWaitTotal()
   {
      return _waitTotal;
   }


   /**
    * @param waitTotal the _waitTotal to set
    */
   public void setWaitTotal(int waitTotal)
   {
      this._waitTotal = waitTotal;
   }


   /**
    * @return the _actionList
    */
   public List<ActionAlert> getActionList()
   {
      return _actionList;
   }


   /**
    * @param actionList the _actionList to set
    */
   public void setActionList(List<ActionAlert> actionList)
   {
      this._actionList = actionList;
   }
}
