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

import java.util.List;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

@Repository
public class WorkpoolRepository extends AbstractDAO<WorkpoolModel> {
   public WorkpoolRepository() {
      super(WorkpoolModel.class);
   }

   /**
    * Return a list of available (fully installed and not in use) instances.
    *
    * @param workpool
    * @return
    */
   public List<InstanceModel> getFreeInstances(WorkpoolModel workpool) {
      Session session = sessionFactory.getCurrentSession();

      @SuppressWarnings("unchecked")
      List<InstanceModel> res = session.createQuery(
              // We use right join since lease has the direct relationship to instance.
              "select instance from LeaseModel lease " +
              "   right join lease.instance instance where instance.workpool = :workpool " +
              "   and lease.id is null " +
              "and instance.state = :instanceState")
              .setParameter("workpool", workpool)
              .setParameter("instanceState", InstanceModel.State.available).list();
      return res;
   }
}
