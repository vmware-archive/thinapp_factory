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

package com.vmware.appfactory.application.dto;

import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.build.model.Build;


/**
 * This class adds more data to the AppBuildRequest object, based on its state
 * and pulls up additional info: recipe Name.
 */
public class AppCaptureHistory
{
   public static final String TASK_PAGE_URL = "/tasks/index";
   public static final String BUILD_PAGE_URL = "/builds/like/";

   private AppBuildRequest _appBuildRequest;

   private String _recipeName;

   /** The following defines what type of action key should be displayed */
   public enum ActionType
   {
      NONE,  // No action text / link needed.
      TASK,  // Task exists, link to tasks page.
      BUILD, // build exists, link to builds page.
      BUILD_DELETED // Build existed earlier, not anymore.
   }

   private ActionType _actionType;

   private String _actionUrl;

   private Build.Status _buildStatus;

   private long _lastUpdated = 0;

   public AppCaptureHistory()
   {
      // Empty default constructor.
   }


   public AppCaptureHistory(
         AppBuildRequest request,
         String recipeName,
         ActionType type,
         long lastUpdated,
         Build.Status status)
   {
      this._appBuildRequest = request;
      this._recipeName = recipeName;
      this._actionType = type;
      this._lastUpdated = lastUpdated;
      this._buildStatus = status;

      if (ActionType.TASK == type) {
         this._actionUrl = TASK_PAGE_URL;
      }
      else if (ActionType.BUILD == type) {
         // Link to all build for this app.
         this._actionUrl = BUILD_PAGE_URL + request.getBuildId();
      }
   }


   /**
    * @return the appBuildRequest
    */
   public AppBuildRequest getAppBuildRequest()
   {
      return _appBuildRequest;
   }


   /**
    * @param appBuildRequest
    *           the appBuildRequest to set
    */
   public void setAppBuildRequest(AppBuildRequest appBuildRequest)
   {
      _appBuildRequest = appBuildRequest;
   }


   /**
    * @return the recipeName
    */
   public String getRecipeName()
   {
      return _recipeName;
   }


   /**
    * @param recipeName
    *           the recipeName to set
    */
   public void setRecipeName(String recipeName)
   {
      _recipeName = recipeName;
   }


   /**
    * @return the actionType
    */
   public ActionType getActionType()
   {
      return _actionType;
   }


   /**
    * @param actionType
    *           the actionType to set
    */
   public void setActionType(ActionType actionType)
   {
      _actionType = actionType;
   }


   /**
    * @return the actionUrl
    */
   public String getActionUrl()
   {
      return _actionUrl;
   }


   /**
    * @param actionUrl
    *           the actionUrl to set
    */
   public void setActionUrl(String actionUrl)
   {
      _actionUrl = actionUrl;
   }


   /**
    * @return the taskPageUrl
    */
   public static String getTaskPageUrl()
   {
      return TASK_PAGE_URL;
   }


   /**
    * @return the buildPageUrl
    */
   public static String getBuildPageUrl()
   {
      return BUILD_PAGE_URL;
   }


   /**
    * @return the lastUpdated
    */
   public long getLastUpdated()
   {
      return _lastUpdated;
   }


   /**
    * @param lastUpdated the lastUpdated to set
    */
   public void setLastUpdated(long lastUpdated)
   {
      _lastUpdated = lastUpdated;
   }


   /**
    * @return the status
    */
   public Build.Status getBuildStatus()
   {
      return _buildStatus;
   }


   /**
    * @param status the status to set
    */
   public void setBuildStatus(Build.Status status)
   {
      _buildStatus = status;
   }
}
