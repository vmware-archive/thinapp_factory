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

package com.vmware.appfactory.common.base;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.appfactory.common.dao.AfDao;
import com.vmware.thinapp.common.util.AfCalendar;


/**
 * This base DAO implementation does many of the common persistence functions
 * for records, including find() (by ID), update(), save(), etc. It is abstract,
 * so must be extended by a specific DAO for each record subclass.
 * @param <T> The model class
 */
@Transactional
public abstract class AbstractDaoImpl<T extends AbstractRecord>
   implements AfDao<T>
{
   @Resource(name = "sessionFactory")
   private SessionFactory _sessionFactory;

   protected Class<T> _class;


   /**
    * Create a new instance. Only subclasses can call this.
    */
   @SuppressWarnings("unchecked")
   protected AbstractDaoImpl()
   {
      try {
         _class = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
      }
      catch (Exception e) {
         /**
          * This does not occur when the actual derived classes
          * are instantiated (which is good), but it is thrown
          * when a (Hibernate?) wrapper class is created, such
          * as FeedDaoImpl$$EnhancerByCGLIB$$6c02440b.
          *
          * It seems safe to just ignore those cases.
          */
      }
   }


   /**
    * Get the current hibernate session.
    * @return
    */
   public Session getCurrentSession()
   {
      return _sessionFactory.getCurrentSession();
   }


   /**
    * Search for a record by ID, return null if not found.
    */
   @SuppressWarnings("unchecked")
   @Override
   public T find(Long id)
   {
      if (id == null) {
         return null;
      }
      Session session = getCurrentSession();
      T record = (T) session.get(_class, id);
      return record;
   }


   /**
    * Search for records by their IDs, return a list of results.
    */
   @Override
   public List<T> findAll(List<Long> ids, boolean includeMisses)
   {
      List<T> records = new ArrayList<T>();

      for (Long id : ids) {
         T record = find(id);
         if (record != null || includeMisses) {
            records.add(record);
         }
      }
      return records;
   }


   /**
    * Helper method that simplifies data loopups based on a criterion.
    *
    * @param criterion
    * @return
    */
   @SuppressWarnings("unchecked")
   protected List<T> findByCriterion(Criterion criterion)
   {
      Criteria criteria = getCurrentSession().createCriteria(_class);
      if (criterion != null) {
         criteria.add(criterion);
      }

      // Return distinct root entities. If not set, we can get duplicate rows.
      criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
      return criteria.list();
   }


   /**
    * Returns the number of records matching the given criterion.
    *
    * @param criterion   condition to match
    * @return     a value from 0 to the number or rows in the table
    */
   protected long countByCriterion(Criterion criterion)
   {
      Criteria criteria = getCurrentSession().createCriteria(_class);
      if (criterion != null) {
         criteria.add(criterion);
      }

      // Return distinct root entities. If not set, we can get duplicate rows.
      criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
      criteria.setProjection(Projections.rowCount());
      return (Long) criteria.uniqueResult();
   }


   /**
    * Get all records.
    */
   @SuppressWarnings("unchecked")
   @Override
   public List<T> findAll()
   {
      String hql = "from " + _class.getName();
      Query q = getCurrentSession().createQuery(hql);
      return q.list();
   }


   /**
    * Create a new record. A new ID will be assigned by the database, and the
    * 'created' time stamp field will be set.
    */
   @Override
   public Long create(T record)
   {
      Session session = getCurrentSession();
      record.setCreated(AfCalendar.Now());
      record.setModified(record.getCreated());
      session.save(record);
      session.flush();
      return record.getId();
   }


   /**
    * Update an existing record. The 'modified' time stamp field will be set.
    */
   @Override
   public void update(T record)
   {
      Session session = getCurrentSession();
      record.setModified(AfCalendar.Now());
      session.update(record);
      session.flush();
   }


   /**
    * Calls either 'create' or 'update' as appropriate.
    */
   @Override
   public void createOrUpdate(T record)
   {
      if (record.getId() == null) {
         create(record);
      }
      else {
         update(record);
      }
   }


   /**
    * Delete a record from the database.
    */
   @Override
   public void delete(T record)
   {
      Session session = getCurrentSession();
      session.delete(record);
      session.flush();
   }


   /**
    * Delete multiple records from the database.
    */
   @Override
   public void delete(List<T> records)
   {
      Session session = getCurrentSession();
      for (T record : records) {
         session.delete(record);
      }
      session.flush();
   }


   /**
    * Delete EVERY record from the table.
    */
   @Override
   public void deleteAll()
   {
      Session session = getCurrentSession();
      String hql = "delete from " + _class.getName();
      Query q = session.createQuery(hql);
      q.executeUpdate();
      session.flush();
   }


   /**
    * Count all records.
    */
   @Override
   public long countAll()
   {
      Criteria criteria = getCurrentSession().createCriteria(_class);
      criteria.setProjection(Projections.rowCount());
      return ((Long) criteria.uniqueResult()).longValue();
   }


   /**
    * Flush pending transactions to the database.
    */
   @Override
   public void flush()
   {
      Session session = getCurrentSession();
      session.flush();
   }


   /**
    * Search a column for the given value. If not found, return it.
    * Otherwise, try variations on the name until no match is found, and
    * return that.
    *
    * TODO: We create "Copy #X of value", which is not localized.
    *
    * @param column Column name (without the underscore)
    * @param currentValue
    * @return
    */
   protected String findUniqueValue(String column, String currentValue)
   {
      String hqlTemplate =
         "from " + _class.getName() +
         " where _" + column + " = :value";

      /* First check the current name */
      if (query1(hqlTemplate, "value", currentValue).isEmpty()) {
         return currentValue;
      }

      /* Now try variations until we succeed */
      String newValue = "Copy of " + currentValue;
      int index = 1;
      while (!query1(hqlTemplate, "value", newValue).isEmpty()) {
         newValue = "Copy #" + (++index) + " of " + currentValue;
      }

      return newValue;
   }


   /**
    * Utility function to execute a template HQL query with
    * one parameter.
    *
    * @param template HQL template.
    * @param placeHolder
    * @param value
    * @return
    */
   private List<?> query1(String template, String placeHolder, String value)
   {
      return getCurrentSession()
         .createQuery(template)
         .setParameter(placeHolder, value)
         .list();
   }

   @Override()
   public long lastModified()
   {
      String FIND_LAST_MODIFIED_HQL =
            "from " + _class.getName() + " order by _modified desc";

      AbstractRecord record = (AbstractRecord)getCurrentSession()
            .createQuery(FIND_LAST_MODIFIED_HQL)
            .setMaxResults(1)
            .uniqueResult();
      if (null == record) {
         return -1;
      }
      return record.getModified();
   }
}
