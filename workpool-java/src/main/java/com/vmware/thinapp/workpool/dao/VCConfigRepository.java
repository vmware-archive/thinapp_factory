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

package com.vmware.thinapp.workpool.dao;

import com.vmware.thinapp.workpool.model.VCConfigModel;
import org.springframework.stereotype.Repository;

@Repository
public class VCConfigRepository extends AbstractDAO<VCConfigModel> {
   // We currently only supporting having one VC configuration at a time.
   private static final String MASTER_VC = "master";

   protected VCConfigRepository() {
      super(VCConfigModel.class);
   }

   public VCConfigModel getConfig() {
      return findByField("name", MASTER_VC);
   }

   public void update(VCConfigModel vcConfig) {
      log.info("Updating VC config: {}.", vcConfig);
      vcConfig.setName(MASTER_VC);
      saveOrUpdate(vcConfig);
   }
}
