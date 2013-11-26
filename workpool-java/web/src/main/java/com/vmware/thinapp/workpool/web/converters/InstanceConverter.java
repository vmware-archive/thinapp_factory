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

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.workpool.model.InstanceModel;

public class InstanceConverter {
   public static InstanceInfo toDto(InstanceModel model) {
      InstanceInfo dto = new InstanceInfo();
      dto.setId(model.getId());
      dto.setAutologon(model.getAutologon());
      dto.setGuestUsername(model.getGuestUsername());
      dto.setGuestPassword(model.getGuestPassword());
      dto.setMoid(model.getMoid());
      dto.setState(InstanceInfo.State.valueOf(model.getState().name()));
      dto.setVmxPath("");
      return dto;
   }

   public static InstanceModel toModel(InstanceInfo instance) {
      InstanceModel model = new InstanceModel();
      model.setAutologon(instance.getAutologon());
      model.setGuestUsername(instance.getGuestUsername());
      model.setGuestPassword(instance.getGuestPassword());
      model.setMoid(instance.getMoid());
      return model;
   }
}
