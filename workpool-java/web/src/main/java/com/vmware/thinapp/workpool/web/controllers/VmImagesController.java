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
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.workpool.VmImageInstance;
import com.vmware.thinapp.workpool.VmImageManager;
import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.thinapp.workpool.web.converters.VmImageConverter;

import static com.vmware.thinapp.workpool.web.controllers.ControllerUtil.redirect;

/** VMImage REST controller. */
@Controller
@RequestMapping("/vmimages")
public class VmImagesController {
   @Autowired
   private VmImageManager vmImageManager;

   @RequestMapping(method = RequestMethod.POST)
   public String create(@RequestBody VmImage body) {
      VmImageModel model = VmImageConverter.toModel(body);
      VmImageInstance i = vmImageManager.create(model);
      return redirect("/vmimages/" + i.getVmImage().getId());
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
   @ResponseBody
   public void delete(
           @PathVariable long id,
           @RequestParam DeleteMethod deleteMethod) {
      vmImageManager.delete(id, deleteMethod).get();
   }

   @RequestMapping(value = "/{id}", method = RequestMethod.GET)
   @ResponseBody
   public VmImage get(@PathVariable long id) {
      VmImageInstance vmImageInstance = vmImageManager.get(id);
      return VmImageConverter.toDto(vmImageInstance);
   }

   @RequestMapping(method = RequestMethod.GET)
   @ResponseBody
   public VmImage[] list() {
      // Can't return Collection<VmImage> otherwise @class member doesn't get set.
      Collection<VmImage> vmImages = Collections2.transform(vmImageManager.list(), new Function<VmImageInstance, VmImage>() {
         @Override
         public VmImage apply(VmImageInstance vmImage) {
            return VmImageConverter.toDto(vmImage);
         }
      });
      return vmImages.toArray(new VmImage[vmImages.size()]);
   }
}
