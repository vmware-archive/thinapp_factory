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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.vmware.thinapp.common.workpool.dto.OsType;
import com.vmware.thinapp.common.workpool.dto.Win7OsType;
import com.vmware.thinapp.common.workpool.dto.WinVistaOsType;
import com.vmware.thinapp.common.workpool.dto.WinXPProOsType;
import com.vmware.thinapp.workpool.model.OsInfoModel;

/**
 * Convert to and from OsInfo DTO and model.
 */
public class OsInfoConverter {
   private final static BiMap<String, String> variantDtoToModel = HashBiMap.create(3);

   static {
      variantDtoToModel.put(Win7OsType.Variant.enterprise.name(), "Windows 7 ENTERPRISE");
      variantDtoToModel.put(Win7OsType.Variant.professional.name(), "Windows 7 PROFESSIONAL");
      variantDtoToModel.put(Win7OsType.Variant.ultimate.name(), "Windows 7 ULTIMATE");
   }

   public static OsType toDto(OsInfoModel model) {
      String varStr = null;
      OsType dto = null;
      switch (model.getOsType()) {
      case winXPPro:
         dto = new WinXPProOsType();
         break;
      case winvista:
         // TODO Add vista support
         throw new RuntimeException("osType: winvista is not supported.");
      case windows7:
         varStr = variantDtoToModel.inverse().get(model.getVariant());
         dto = new Win7OsType(Win7OsType.Variant.valueOf(varStr));
         break;
      }
      return dto;
   }

   public static OsInfoModel toModel(OsType dto) {
      OsInfoModel model = new OsInfoModel();

      if (dto instanceof WinXPProOsType) {
         model.setOsType(OsInfoModel.OsType.winXPPro);
      } else if (dto instanceof Win7OsType) {
         model.setOsType(OsInfoModel.OsType.windows7);
         model.setVariant(variantDtoToModel.get(dto.getOsVariantName()));
      } else if (dto instanceof WinVistaOsType) {
         // TODO Add vista support
         throw new RuntimeException("osType: winvista is not supported.");
      }
      return model;
   }
}
