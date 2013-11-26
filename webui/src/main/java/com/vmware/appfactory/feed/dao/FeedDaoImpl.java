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

package com.vmware.appfactory.feed.dao;

import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.common.base.AbstractDaoImpl;
import com.vmware.appfactory.feed.model.Feed;

/**
 * Default implementation of the AfFeedDao interface.
 */
@Service
@Transactional
class FeedDaoImpl
	extends AbstractDaoImpl<Feed>
	implements FeedDao
{

   /**
    * Search for feeds by name.
    * This should return no matches (null) or one match, since feed names
    * are supposed to be unique.
    */
   @Override
   public Feed findByName(String name)
   {
      Criterion c = Restrictions.eq("_name", name);
      List<Feed> list = findByCriterion(c);
      return (list.isEmpty() ? null : list.get(0));
   }

   @Override
   public long countFailed() {
      Criterion criterion =
            Restrictions.and(
                  Restrictions.isNotNull("_failure._summary"),
                  Restrictions.ne("_failure._summary", Feed.SCANNING)
            );
      return countByCriterion(criterion);
   }
}
