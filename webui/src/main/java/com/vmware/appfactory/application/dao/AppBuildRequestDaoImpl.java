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

package com.vmware.appfactory.application.dao;

import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.application.model.AppBuildRequest;
import com.vmware.appfactory.application.model.AppBuildRequest.RequestStage;
import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * Implementation of AppBuildRequestDao operations.
 */
@Service(value = "appBuildRequestDao")
@Transactional
public class AppBuildRequestDaoImpl extends AbstractDaoImpl<AppBuildRequest>
   implements AppBuildRequestDao {

   private static final String UPDATE_REQUEST_STAGE_HQL =
      "update " + AppBuildRequest.class.getName()
            + " set _requestStage = :stage, _modified = :modified where _id = :id";

   private static final String UPDATE_BUILD_ID_HQL =
      "update " + AppBuildRequest.class.getName()
            + " set _buildid = :buildId, _modified = :modified where _id = :id";

   /**
    * Get the list of buildRequests for a given app.
    *
    * @param appId
    * @return
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<AppBuildRequest> findBuildRequestForApp(Long appId) {
      if (appId == null) {
         return Collections.EMPTY_LIST;
      }
      Criteria criteria = getCurrentSession().createCriteria(AppBuildRequest.class);
      criteria.add(Restrictions.eq("_application._id", appId));

      // Return distinct root entities.
      criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
      return criteria.list();
   }

   /**
    * Update the reuqestStage for the record for the param appBuildRequestId
    *
    * @param appBuildRequestId
    * @param stage
    */
   @Override
   public void updateBuildRequestStage(
         Long appBuildRequestId,
         RequestStage stage) {
      if (appBuildRequestId == null) {
         return;
      }
      getCurrentSession().createQuery(UPDATE_REQUEST_STAGE_HQL)
            .setString("stage", stage.name())
            .setLong("modified", AfCalendar.Now())
            .setLong("id", appBuildRequestId)
            .executeUpdate();
   }

   /**
    * Update the buildId for the record for the param appBuildRequestId
    *
    * @param appBuildRequestId
    * @param buildId
    */
   @Override
   public void updateBuildRequestBuildId(
         Long appBuildRequestId,
         Long buildId) {
      if (appBuildRequestId == null) {
         return;
      }
      getCurrentSession().createQuery(UPDATE_BUILD_ID_HQL)
            .setLong("buildId", buildId)
            .setLong("modified", AfCalendar.Now())
            .setLong("id", appBuildRequestId)
            .executeUpdate();
   }
}
