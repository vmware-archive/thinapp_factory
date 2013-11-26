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

package com.vmware.thinapp.manualmode.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.thinapp.manualmode.model.ConversionJobModel;

@Repository
public class ConversionJobRepository extends AbstractDAO<ConversionJobModel> {
   public ConversionJobRepository() {
      super(ConversionJobModel.class);
   }

   /**
    * Create and persist a new conversion job.
    *
    * @return the created conversion job
    */
   @Transactional
   public ConversionJobModel createNewConversionJob() {
      ConversionJobModel job = new ConversionJobModel();
      this.save(job);
      return job;
   }

   /**
    * Get a conversion job given it's ID
    *
    * @param id the job's ID
    * @return the job with the given ID
    */
   @Transactional
   public ConversionJobModel getJobById(Long id) {
      return this.get(id);
   }
}
