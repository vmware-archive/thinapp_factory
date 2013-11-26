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

package com.vmware.appfactory.build.dao;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.build.model.Build;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.base.AbstractDaoImpl;

/**
 * Implementation of the AfBuildDao interface.
 */
@Service
@Transactional
class BuildDaoImpl
   extends AbstractDaoImpl<Build>
   implements BuildDao
{
   private static final String FIND_FOR_APP_HQL =
      "from " + Build.class.getName() + " where " +
      "_name = :name and " +
      "_version = :version and " +
      "_locale = :locale and " +
      "_installerrev = :rev";

   private static final String FIND_BY_APP_NAME_HQL =
      "from " + Build.class.getName() + " where " +
      "_name = :name";

   private static final String FIND_BY_STATUS_HQL =
      "from " + Build.class.getName() + " where _status = :status";

   private static final String FIND_BY_DS_HQL =
      "from " + Build.class.getName() + " where _datastoreid = :dsId";

   /**
    * Find all builds belonging to an application.
    * Only exact application matches (name plus version) are returned.
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<Build> findForApp(AbstractApp app)
   {
      return getCurrentSession().
         createQuery(FIND_FOR_APP_HQL).
         setParameter("name", app.getName()).
         setParameter("version", app.getVersion()).
         setParameter("locale", app.getLocale()).
         setParameter("rev", app.getInstallerRevision()).
         list();
   }


   /**
    * Find all builds that have the given application name.
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<Build> findForAppName(String appName)
   {
      return getCurrentSession().
         createQuery(FIND_BY_APP_NAME_HQL).
         setParameter("name", appName).
         list();
   }


   /**
    * Find all builds that have the given state.
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<Build> findForStatus(Build.Status status)
   {
      return getCurrentSession().
         createQuery(FIND_BY_STATUS_HQL).
         setParameter("status", status).
         list();
   }


   @Override
   public Build findByProjectId(Long projectId)
   {
      if (projectId == null) {
         return null;
      }
      Criterion c = Restrictions.eq("_converterProjectId", projectId);
      List<Build> builds = findByCriterion(c);

      // Since projectId is unique, there is always at most 1 Build.
      // Return that single build.
      return CollectionUtils.isNotEmpty(builds)? builds.get(0) : null;
   }

   /**
    * @see com.vmware.appfactory.build.dao.BuildDao#deleteAll(java.lang.Long)
    */
   @Override
   public void deleteAll(Long datastoreId) {
      /**
       * Build entity has two orphans (BuildFile & BuildIcon) and
       * the HQL delete doesn't trigger the internal cascades.
       * So, delete each one individually.
       */
      List<Build> builds = findByDatastore(datastoreId);
      delete(builds);
   }

   /**
    * @see com.vmware.appfactory.build.dao.BuildDao#findByDatastore(java.lang.Long)
    */
   @Override
   @SuppressWarnings("unchecked")
   public List<Build> findByDatastore(Long datastoreId) {
      return getCurrentSession().createQuery(FIND_BY_DS_HQL).
         setParameter("dsId", datastoreId).list();
   }
}
