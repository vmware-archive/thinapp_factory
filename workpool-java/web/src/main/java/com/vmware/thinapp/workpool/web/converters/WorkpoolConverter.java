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

package com.vmware.thinapp.workpool.web.converters;

import org.apache.commons.lang.NotImplementedException;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.vmware.thinapp.common.workpool.dto.CustomWorkpool;
import com.vmware.thinapp.common.workpool.dto.FullWorkpool;
import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.LinkedWorkpool;
import com.vmware.thinapp.common.workpool.dto.Workpool;
import com.vmware.thinapp.workpool.VmImageManager;
import com.vmware.thinapp.workpool.WorkpoolInstance;
import com.vmware.thinapp.workpool.model.CustomWorkpoolModel;
import com.vmware.thinapp.workpool.model.FullWorkpoolModel;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.LinkedWorkpoolModel;
import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.thinapp.workpool.model.VmPatternModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

/** Convert to and from Workpool DTO and model. */
public class WorkpoolConverter {
   public static Workpool toDto(WorkpoolInstance instance, VmImageManager vmImageManager) {
      Workpool dto;
      WorkpoolModel model = instance.getWorkpoolModel();

      if (model instanceof FullWorkpoolModel) {
         FullWorkpool fullDto = new FullWorkpool();
         FullWorkpoolModel fullWorkpool = (FullWorkpoolModel) model;
         fullDto.setVmPattern(VmPatternConverter.toDto(fullWorkpool.getInstancer()));
         dto = fullDto;
      } else if (model instanceof LinkedWorkpoolModel) {
         LinkedWorkpool linkedDto = new LinkedWorkpool();
         LinkedWorkpoolModel linkedModel = (LinkedWorkpoolModel) model;
         linkedDto.setVmImage(VmImageConverter.toDto(vmImageManager.get(linkedModel.getInstancer().getId())));
         dto = linkedDto;
      } else if (model instanceof CustomWorkpoolModel) {
         CustomWorkpool customDto = new CustomWorkpool();
         CustomWorkpoolModel customModel = (CustomWorkpoolModel) model;
         customDto.setOsType(OsInfoConverter.toDto(customModel.getOsInfo()));
         dto = customDto;
      } else {
         throw new RuntimeException("Unknown workpool type: " + model.getClass());
      }

      dto.setMaximum(model.getMaximumInstances());
      dto.setName(model.getName());
      dto.setId(model.getId());
      dto.setState(Workpool.State.valueOf(model.getState().name()));
      dto.setLastError(model.getLastError());
      dto.getInstances().addAll(Collections2.transform(model.getInstances(),
              new Function<InstanceModel, InstanceInfo>() {
                 @Override
                 public InstanceInfo apply(InstanceModel input) {
                    return InstanceConverter.toDto(input);
                 }
              }));

      return dto;
   }

   public static WorkpoolModel toModel(Workpool body) {
      WorkpoolModel model;

      if (body instanceof LinkedWorkpool) {
         LinkedWorkpool linkedWorkpool = (LinkedWorkpool) body;
         LinkedWorkpoolModel linkedModel = new LinkedWorkpoolModel();
         com.vmware.thinapp.common.workpool.dto.VmImage vmImage = linkedWorkpool.getVmImage();
         // XXX: Not sure if this is ok...
         VmImageModel vmImageModel = new VmImageModel();
         vmImageModel.setId(vmImage.getId());
         linkedModel.setInstancer(vmImageModel);
         model = linkedModel;
      } else if (body instanceof FullWorkpool) {
         FullWorkpool fullWorkpool = (FullWorkpool) body;
         FullWorkpoolModel fullModel = new FullWorkpoolModel();
         VmPatternModel vmPattern = VmPatternConverter.toModel(fullWorkpool.getVmPattern());
         fullModel.setInstancer(vmPattern);
         model = fullModel;
      } else if (body instanceof CustomWorkpool) {
         CustomWorkpool customWorkpool = (CustomWorkpool) body;
         CustomWorkpoolModel customModel = new CustomWorkpoolModel();
         customModel.setOsInfo(OsInfoConverter.toModel(customWorkpool.getOsType()));
         model = customModel;
      } else {
         throw new NotImplementedException();
      }

      model.setName(body.getName());
      // XXX: Only applies to growable workpools.  Could move into that class.
      model.setMaximumInstances(body.getMaximum());
      return model;
   }
}
