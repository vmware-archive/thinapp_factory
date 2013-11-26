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

import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.appfactory.datastore.DsUtil;


/**
 * Default implementation of the ApplicationDao interface.
 */
@Service(value = "applicationDao")
@Transactional
public class ApplicationDaoImpl
      extends AbstractDaoImpl<Application>
      implements ApplicationDao
{
   private static final String FIND_SKIPPED_HQL =
         "from " + Application.class.getName() + " where _skipped = false";

   private static final String FIND_NOT_SKIPPED_COUNT =
      "select count(*) from " + Application.class.getName() + " where _skipped = false";

   private static final String FIND_UPLOADED_APPS_HQL =
      "select a from Application as a left join a._downloads as d where a._dataSource is null and d._uriStr like :uri";

   /**
    * Get all applications (from all feeds) that are marked for inclusion. If
    * none, returns an empty list.
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<Application> findAllIncluded()
   {
      return getCurrentSession()
            .createQuery(FIND_SKIPPED_HQL)
            .list();
   }

   public Long countAllIncluded() {

      return (Long)getCurrentSession()
         .createQuery(FIND_NOT_SKIPPED_COUNT)
         .setMaxResults(1)
         .uniqueResult();
   }

   /**
    * Get all other applications that match the passed app name and are marked
    * for inclusion.
    *
    * @param app
    * @return
    */
   @Override
   public List<Application> findOtherVersionsIncluded(Application app)
   {
      Criterion c1 = Restrictions.eq("_name", app.getName()).ignoreCase();
      Criterion c2 = Restrictions.ne("_id", app.getId());
      Criterion c3 = Restrictions.eq("_skipped", false);
      return findByCriterion(Restrictions.and(Restrictions.and(c1, c2), c3));
   }


   /**
    * Find upload applications via the datastore URI prefix (datastore://{id}/).
    *
    * @param datastoreId a valid datastore id.
    * @return a list of uploaded applications.
    */
   @SuppressWarnings("unchecked")
   @Override
   public List<Application> findUploadedApps(Long datastoreId) {
      return getCurrentSession()
            .createQuery(FIND_UPLOADED_APPS_HQL)
            .setString("uri",
                  DsUtil.DATASTORE_URI_SCHEME + "://" + datastoreId + "/%")
            .list();
   }

   /**
    * Delete all uploaded applications.
    *
    * @param datastoreId a valid datastore id.
    */
   @Override
   public void deleteUploadedApps(Long datastoreId) {
      List<Application> apps = findUploadedApps(datastoreId);
      // Need to delete each app individually to trigger internal cascade on all child tables.
      delete(apps);
   }
}
