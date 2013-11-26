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

package com.vmware.thinapp.workpool.web.controllers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.workpool.VmImageManager;
import com.vmware.thinapp.workpool.WorkpoolInstance;
import com.vmware.thinapp.workpool.WorkpoolManager;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;
import com.vmware.thinapp.workpool.web.converters.InstanceConverter;
import com.vmware.thinapp.workpool.web.converters.WorkpoolConverter;
import com.vmware.thinapp.workpool.web.exceptions.NotFoundExeption;

import scala.Option;

import static com.vmware.thinapp.workpool.web.controllers.ControllerUtil.*;

/**
 * Workpool REST controller.
 */
@Controller
@RequestMapping("/workpools")
public class WorkpoolController extends BaseController {
   private static final Logger log = LoggerFactory.getLogger(WorkpoolController.class);

   @Autowired
   private WorkpoolManager workpoolManager;
   @Autowired
   private VmImageManager vmImageManager;

   /**
    * Create a new workpool.
    *
    * @param body
    * @return
    */
   @RequestMapping(method = RequestMethod.POST)
   public String create(@RequestBody Workpool body) {
      log.debug("Received workpool create request: {}.", body);
      WorkpoolModel model = WorkpoolConverter.toModel(body);
      WorkpoolInstance instance = workpoolManager.create(model);
      return redirect("/workpools/" + instance.getWorkpoolModel().getId());
   }

   /**
    * Get status of an existing workpool.
    *
    * @param id
    * @return
    */
   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   @ResponseBody
   public Workpool get(@PathVariable long id) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         return WorkpoolConverter.toDto(wp.get(), vmImageManager);
      } else {
         throw new NotFoundExeption();
      }
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
   @ResponseBody
   public void update(@PathVariable long id, @RequestBody Workpool workpool) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         WorkpoolModel model = WorkpoolConverter.toModel(workpool);
         wp.get().updateModel(model).get();
      } else {
         throw new NotFoundExeption();
      }
   }

   /**
    * Get a list of all workpools.
    *
    * @return
    */
   @RequestMapping(method = RequestMethod.GET)
   @ResponseBody
   public Workpool[] list() {
      // Can't return Collection<Workpool> otherwise @class member doesn't get set.
      Collection<Workpool> workpools = Collections2.transform(workpoolManager.list(), new Function<WorkpoolInstance, Workpool>() {
         @Override
         public Workpool apply(WorkpoolInstance workpool) {
            return WorkpoolConverter.toDto(workpool, vmImageManager);
         }
      });
      return workpools.toArray(new Workpool[workpools.size()]);
   }

   /**
    * Delete a workpool.
    *
    * @param id
    */
   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   @ResponseBody
   public void delete(@PathVariable long id, @RequestParam DeleteMethod deleteMethod) {
      workpoolManager.delete(id, deleteMethod).get();
   }

   /**
    * Reset a workpool with the given id.
    *
    * @param id
    */
   @RequestMapping(value = "/{id}/reset", method = RequestMethod.POST)
   @ResponseBody
   public void reset(@PathVariable long id) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         wp.get().reset().get();
      } else {
         throw new NotFoundExeption();
      }
   }

   /**
    * Reset the leases of all workpools and attempt to shut down all running VM
    * instances.
    *
    * This is intended to be used as a last resort in cases where one does not
    * want to clean up all instances manually.
    */
   @RequestMapping(value = "/reset", method = RequestMethod.POST)
   @ResponseBody
   public void reset() {
      workpoolManager.reset();
   }

   @RequestMapping(value = "/{id}/instances", method = RequestMethod.POST)
   @ResponseBody
   public void addInstance(@PathVariable long id, @RequestBody InstanceInfo instance,
           HttpServletRequest request, HttpServletResponse response) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         InstanceModel model = wp.get().addInstance(InstanceConverter.toModel(instance)).get();
         response.setHeader("Location", String.format("%s/%d", request.getRequestURL(), model.getId()));
      } else {
         throw new NotFoundExeption();
      }
   }

   @RequestMapping(value = "/{id}/instances/{instanceId}", method = RequestMethod.GET)
   @ResponseBody
   public InstanceInfo getInstance(@PathVariable long id, @PathVariable long instanceId) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         InstanceModel model = wp.get().getInstance(instanceId);
         return InstanceConverter.toDto(model);
      } else {
         throw new NotFoundExeption();
      }
   }

   @RequestMapping(value = "/{id}/instances/{instanceId}", method = RequestMethod.DELETE)
   @ResponseBody
   public void removeInstance(@PathVariable long id, @PathVariable long instanceId) {
      Option<WorkpoolInstance> wp = workpoolManager.get(id);
      if (wp.isDefined()) {
         wp.get().removeInstance(instanceId).get();
      } else {
         throw new NotFoundExeption();
      }
   }
}
