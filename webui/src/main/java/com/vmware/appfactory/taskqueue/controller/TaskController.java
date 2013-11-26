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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.vmware.appfactory.common.base.AbstractUiController;
import com.vmware.appfactory.common.base.SelectedTab;
import com.vmware.appfactory.taskqueue.tasks.TaskQueue;
import com.vmware.thinapp.common.util.AfUtil;


/**
 * Handles all MVC requests for the "Tasks" pages.
 * @author levans
 */
@Controller
@SelectedTab(value= SelectedTab.Tabs.Tasks)
public class TaskController
   extends AbstractUiController
{
   /**
    * Show a list of all tasks, optionally filtered by meta-status.
    *
    * @param request
    * @param metaStatusStr
    * @param locale
    * @return
    * @throws Exception
    */
   @RequestMapping(
         value={"/tasks","/tasks/index","/newui/tasks"},
         method=RequestMethod.GET)
   public ModelAndView index(
         HttpServletRequest request,
         @RequestParam(required=false,value="metaStatus") String metaStatusStr,
         Locale locale)
      throws Exception
   {
      ModelMap mm = getBaseModel(request, locale, "T.TASKS.ALL");

      if (!AfUtil.anyEmpty(metaStatusStr)) {
         TaskQueue.MetaStatus metaStatus = TaskQueue.MetaStatus.valueOf(metaStatusStr);
         mm.put("metaStatus", metaStatus.name());

         String statusT = tr(locale, "T.TASKS.METASTATUS." + metaStatusStr);
         mm.put(PAGE_TITLE_KEY, fr(locale, "T.TASKS.TASKS_FOR_METASTATUS", statusT));
      }

      return new ModelAndView("tasks/tasks-index", mm);
   }
}