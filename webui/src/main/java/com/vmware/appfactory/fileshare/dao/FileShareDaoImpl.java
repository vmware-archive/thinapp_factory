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

package com.vmware.appfactory.fileshare.dao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.appfactory.fileshare.model.FileShare;

/**
 * This class implements all fileshare-specific DAO operations.
 *
 * @author saung
 * @since v1.0 4/27/2011
 */
@Service
@Transactional
public class FileShareDaoImpl extends AbstractDaoImpl<FileShare> implements FileShareDao {

   private static final String FIND_BY_NAME_HQL =
      "from " + FileShare.class.getName() + " where _name = :name";

   private static final String FIND_BY_DS_ID_HQL =
      "from " + FileShare.class.getName() + " where _datastoreid = :dsId";

   /**
    * @see com.vmware.appfactory.fileshare.dao.FileShareDao#findByName(java.lang.String)
    */
   @Override
   public FileShare findByName(String name) {
      List<?> list = getCurrentSession().
         createQuery(FIND_BY_NAME_HQL).
         setParameter("name", name).
         list();

      return (list.isEmpty() ? null : (FileShare) list.get(0));
   }


   /**
    * @see com.vmware.appfactory.fileshare.dao.FileShareDao#findByDatastoreId(java.lang.Long)
    */
   @Override
   public FileShare findByDatastoreId(Long id) {
      List<?> list = getCurrentSession().
         createQuery(FIND_BY_DS_ID_HQL).
         setLong("dsId", id.longValue()).
         list();

      return (list.isEmpty() ? null : (FileShare) list.get(0));
   }

}
