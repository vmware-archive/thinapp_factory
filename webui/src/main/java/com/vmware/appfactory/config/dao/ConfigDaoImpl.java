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

package com.vmware.appfactory.config.dao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.appfactory.config.model.ConfigSetting;

/**
 * Default implementation of the AfConfigDao interface.
 */
@Service
@Transactional
class ConfigDaoImpl
	extends AbstractDaoImpl<ConfigSetting>
	implements ConfigDao
{
   private static final String FIND_BY_KEY_HQL =
      "from " + ConfigSetting.class.getName() + " where _key = :key";


   @Override
   public ConfigSetting findForKey(String key)
   {
      List<?> list = getCurrentSession().
         createQuery(FIND_BY_KEY_HQL).
         setParameter("key", key).
         list();

      return (list.isEmpty() ? null : (ConfigSetting) list.get(0));
   }
}
