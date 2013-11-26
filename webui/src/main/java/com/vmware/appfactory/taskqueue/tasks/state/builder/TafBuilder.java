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

package com.vmware.appfactory.taskqueue.tasks.state.builder;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vmware.appfactory.taskqueue.tasks.state.HasBuilderChanges;

abstract class TafBuilder<API> {

   private final Map<String, HasBuilderChanges.BuilderChange> changes = Maps.newLinkedHashMap();
   private final Collection<HasBuilderChanges.BuilderChange> changeEvents;

   private final ImmutableMap<String, Method> resultClassGetters;
   private final ImmutableMap<String, Method> resultClassSetters;

   final Class<? extends API> resultClass;

   API builtResult;

   protected TafBuilder(Class<? extends API> resultClass) {
      this.resultClass = resultClass;
      this.resultClassGetters = BeanMaps.newBeanGetters(resultClass);
      this.resultClassSetters = BeanMaps.newBeanSetters(resultClass);
      this.changeEvents = Sets.newHashSetWithExpectedSize(resultClassGetters.size());
   }

   public abstract TafBuilder<API> withOriginal(API original);

   public API build() {
      if (null != this.builtResult) {
         return builtResult;
      }

      API localResult;
      try {
         localResult = this.resultClass.newInstance();
      } catch (InstantiationException e) {
         throw new Error(e);
      } catch (IllegalAccessException e) {
         throw new Error(e);
      }

      for(HasBuilderChanges.BuilderChange execute : changeValues()) {
         perRegularChange(execute, localResult);
      }

      if (localResult instanceof HasBuilderChanges) {
         perRegularChange(
               new HasBuilderChanges.BuilderChange("changes", changeEvents),
               localResult);
      }

      this.builtResult = localResult;
      return localResult;
   }

   public Collection<HasBuilderChanges.BuilderChange> changeValues() {
      return changes.values();
   }

   protected void perRegularChange(HasBuilderChanges.BuilderChange execute, API result) {
      Method setter = resultClassSetters.get(execute.getProperty());
      Object item;
      try {
         if (setter == null) {
            throw new NullPointerException("Readonly property [" + this.resultClass +
                                           '.' + execute.getProperty() + "] missing setter");
         }
         item = execute.getValue();
         BeanMaps.optimisticSet(setter, result, item);
      } catch (Exception e) {
         if (e instanceof RuntimeException) throw (RuntimeException) e;
         throw new Error (e);
      }
   }

   protected void addChange(String property, Object object) {
      HasBuilderChanges.BuilderChange change = new HasBuilderChanges.BuilderChange(property, object);

      HasBuilderChanges.BuilderChange oldChange = changes.get(property);
      if (null != oldChange && !Objects.equal(oldChange.getValue(), object)) {
         // are we changing the value from the original?
         changeEvents.add(change);
      }

      changes.put(property, change);
   }

   protected void addChanges(@Nonnull API source) {
      Preconditions.checkNotNull(source);
      BeanMaps.BeanMapViaIntrospection<String> bm = BeanMaps.newBeanMap(source);
      for(Map.Entry<String, Object> entry : bm.entrySet()) {
         Object value = entry.getValue();
         String key = entry.getKey();
         if (! resultClassSetters.containsKey(entry.getKey())) {
            continue;
         }
         addChange(key,value);
      }
   }
}