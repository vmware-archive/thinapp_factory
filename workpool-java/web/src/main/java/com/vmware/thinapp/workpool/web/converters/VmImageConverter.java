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

import com.vmware.thinapp.common.workpool.dto.ExistingVm;
import com.vmware.thinapp.common.workpool.dto.VmImage;
import com.vmware.thinapp.common.workpool.dto.VmPattern;
import com.vmware.thinapp.common.workpool.dto.VmSource;
import com.vmware.thinapp.workpool.VmImageInstance;
import com.vmware.thinapp.workpool.model.OsInfoModel;
import com.vmware.thinapp.workpool.model.OsRegistrationModel;
import com.vmware.thinapp.workpool.model.VmImageModel;

/**
 * Convert to and from VmImage DTO and model.
 */
public class VmImageConverter {
   public static VmImage toDto(VmImageInstance instance) {
      VmImageModel model = instance.getVmImage();
      VmImage vmImageDto = new VmImage();
      VmSource vmSource;
      vmImageDto.setId(model.getId());
      vmImageDto.setName(model.getName());
      vmImageDto.setState(VmImage.State.valueOf(model.getState().name()));
      vmImageDto.setLastError(model.getLastError());

      if (model.getVmPattern() != null) {
         vmSource = VmPatternConverter.toDto(model.getVmPattern());
      } else {
         vmSource = new ExistingVm(OsInfoConverter.toDto(
                 model.getOsInfo()),
                 OsRegistrationConverter.toDto(model.getOsRegistration()),
                 model.getMoid());
      }

      vmImageDto.setVmSource(vmSource);
      return vmImageDto;
   }

   public static VmImageModel toModel(VmImage body) {
      if (body.getVmSource() instanceof VmPattern) {
         VmPattern pattern = (VmPattern) body.getVmSource();
         VmImageModel model = new VmImageModel();

         model.setName(body.getName());
         model.setVmPattern(VmPatternConverter.toModel(pattern));
         model.setOsInfo(OsInfoConverter.toModel(pattern.getOsType()));
         model.setOsRegistration(OsRegistrationConverter.toModel(pattern.getOsRegistration()));
         return model;
      } else if (body.getVmSource() instanceof ExistingVm) {
         ExistingVm existing = (ExistingVm) body.getVmSource();
         VmImageModel vmImage = new VmImageModel();

         vmImage.setName(body.getName());
         vmImage.setMoid(existing.getMoid());

         OsInfoModel osInfo = OsInfoConverter.toModel(existing.getOsType());
         OsRegistrationModel osRegistration = OsRegistrationConverter.toModel(existing.getOsRegistration());

         vmImage.setOsInfo(osInfo);
         vmImage.setOsRegistration(osRegistration);

         return vmImage;
      } else {
         throw new IllegalArgumentException("A VmPattern or ExistingVm must be provided.");
      }
   }
}
