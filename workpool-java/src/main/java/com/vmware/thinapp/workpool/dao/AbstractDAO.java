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

import java.io.Serializable;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public abstract class AbstractDAO<T> {
   protected final Logger log = LoggerFactory.getLogger(getClass());

   @Resource(name="workpoolSessionFactory")
   protected SessionFactory sessionFactory;

   Class<T> clazz;

   protected AbstractDAO(Class<T> clazz) {
      this.clazz = clazz;
   }

   public Session getCurrentSession() {
      return sessionFactory.getCurrentSession();
   }

   public void update(T object) {
      Session session = getCurrentSession();
      session.update(object);
   }

   public List<T> findAll() {
      Session session = getCurrentSession();
      @SuppressWarnings("unchecked")
      List<T> res = session.createCriteria(clazz).list();
      return res;
   }

   public T findByField(String field, Object value) {
      Session session = getCurrentSession();
      @SuppressWarnings("unchecked")
      T res = (T) session.createCriteria(clazz).add(Restrictions.eq(field, value)).uniqueResult();
      return res;
   }

   public List<T> findAllByField(String field, Object value) {
      Session session = getCurrentSession();
      @SuppressWarnings("unchecked")
      List<T> res = session.createCriteria(clazz).add(Restrictions.eq(field, value)).list();
      return res;
   }

   public void save(T object) {
      Session session = getCurrentSession();
      session.save(object);
   }

   public void saveOrUpdate(T object) {
      Session session = getCurrentSession();
      session.saveOrUpdate(object);
   }

   public void delete(T object) {
      Session session = getCurrentSession();
      session.delete(object);
   }

   public T get(Serializable id) {
      Session session = getCurrentSession();
      @SuppressWarnings("unchecked")
      T res = (T) session.get(clazz, id);
      return res;
   }

   public void saveAll(List<T> objects) {
      Session session = getCurrentSession();
      for (T o : objects) {
         session.save(o);
      }
   }

   public void refresh(T object) {
      Session session = getCurrentSession();
      session.refresh(object);
   }
}
