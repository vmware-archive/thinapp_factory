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

import com.vmware.thinapp.common.workpool.dto.OsRegistration;
import com.vmware.thinapp.workpool.model.OsRegistrationModel;

/**
 * Convert to and from OsRegistration DTO and model.
 */
public class OsRegistrationConverter {
   public static OsRegistration toDto(OsRegistrationModel model) {
      OsRegistration dto = new OsRegistration(
              model.getLicenseKey(),
              model.getUserName(),
              model.getOrganization(),
              model.getKmsServer()
      );
      return dto;
   }

   public static OsRegistrationModel toModel(OsRegistration dto) {
      OsRegistrationModel model = new OsRegistrationModel(
              dto.getLicenseKey(),
              dto.getUserName(),
              dto.getOrganization(),
              dto.getKmsServer()
      );
      return model;
   }
}