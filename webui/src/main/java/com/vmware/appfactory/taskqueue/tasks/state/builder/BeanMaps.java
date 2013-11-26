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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vmware.thinapp.common.util.Closure;

/**
 *  A more useful implementation of the Apache BeanMap class: a bean is wrapped inside a <tt>Map</tt> to look like a <tt>Map</tt>.
 *
 *  The bean-to-map is a wrapper over the bean and does not provide any concurrent modification monitoring of the
 *  underlying bean.  The contract for Map.Entry.setValue is fragile if external bean modifications are made.
 *
 * The following maps types are available:
 * <ol>
 * <li>Map&lt String, Method&gt - for property-to-getter and property-to-setter methods</li>
 * <li>Map&lt String, Object&gt - readonly bean access and read/write bean access </li>
 * </ol>
 *
 */
@SuppressWarnings({"unchecked","overrides"})
final class BeanMaps {

   /**
    *  Caches of getter and setter maps presumably for performance
    */
   static ConcurrentMap<Class, ImmutableMap<String,Method>> getters = new MapMaker().softValues().makeMap();
   static ConcurrentMap<Class, ImmutableMap<String,Method>> setters = new MapMaker().softValues().makeMap();

   /**
    *  Convenience function for calling property get via a <tt>Method</tt>.  The general purpose checked exceptions thrown by
    *  <tt>Method.invoke()</tt> are either compilation errors, test errors or validation exceptions which are treated as
    *  unchecked in the general case of property manipulations.
    *
    * @param method
    * @param target
    * @throws IllegalArgumentException for checked exceptions
    * @throws Error if wrapped inside InvocationTargetException
    * @throws RuntimeException if wrapped inside InvocationTargetException
    * @return the property
    */
   public static Object optimisticGet(Method method, Object target) {
      Preconditions.checkNotNull(method);
      Preconditions.checkNotNull(target);
      try {
         return method.invoke(target);
      } catch (IllegalAccessException e) {
         throw new IllegalArgumentException ("While accessing: "+method + " on " + target.getClass(), e);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof RuntimeException) throw (RuntimeException)e.getCause();
         if (e.getCause() instanceof Error) throw (Error)e.getCause();
         throw new IllegalArgumentException ("While accessing: "+method,e.getCause());
      }
   }

   /**
    *  Convenience function for calling property set via a <tt>Method</tt>.  The general purpose checked exceptions thrown by
    *  <tt>Method.invoke</tt> are either compilation errors, test errors or validation exceptions which are treated as
    *  unchecked in the general case of property manipulations.
    *
    * @param method
    * @param target
    * @param value
    * @throws IllegalArgumentException for checked exceptions
    * @throws Error if wrapped inside InvocationTargetException
    * @throws RuntimeException if wrapped inside InvocationTargetException
    */
   public static void optimisticSet(Method method, Object target, Object value) {
      Preconditions.checkNotNull(method);
      Preconditions.checkNotNull(target);
      try {
         method.invoke(target,value);
      } catch (IllegalArgumentException e) {
         throw new IllegalArgumentException ("Method: " + method.getName() + " Value: " + value + " Value Class: " + value.getClass(), e.getCause());
      } catch (IllegalAccessException e) {
         throw new IllegalArgumentException (e);
      } catch (InvocationTargetException e) {
         if (e.getCause() instanceof RuntimeException) throw (RuntimeException)e.getCause();
         if (e.getCause() instanceof Error) throw (Error)e.getCause();
         throw new IllegalArgumentException ("Method: " + method.getName() + " Value: " + value + " Value Class: " + value.getClass(), e.getCause());
      }
   }

   private BeanMaps () {/* Empty */}

   /**
    *  This implementation uses two method maps, getters and setters, to decide at runtime
    *  if a Map.put (that is, a bean property set) is an UnsupportedOperationException or not.
    *
    * @param <K> A future refactoring may turn property keys into <b>Enum</b>'s
    */
   public static class BeanMapViaIntrospection<K> extends AbstractMap<K,Object> implements Supplier<Object> {
      private Object target;
      private Map<K, Method> schemaGets;
      private Map<K, Method> schemaSets;

      protected BeanMapViaIntrospection(Object target, Map<K, Method> schemaGets) {
         Preconditions.checkNotNull(target);
         Preconditions.checkNotNull(schemaGets);
         this.schemaGets = schemaGets;
         this.target = target;
      }

      private BeanMapViaIntrospection(Object target, Map<K, Method> schemaGets, Map<K, Method> schemaSets) {
         this(target, schemaGets);
         Preconditions.checkNotNull(schemaSets);
         this.schemaSets = schemaSets;
      }

      /**
       * The underlying bean.
       *
       * @return the bean
       */
      @Override
      public Object get() {
         return target;
      }

      @Override
      public int size() {
         return schemaGets.size();
      }

      @Override
      public boolean isEmpty() {
         return schemaGets.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return schemaGets.containsKey(key);
      }

      @Override
      public boolean containsValue(Object value) {
         for(Entry<K,Method> entry : schemaGets.entrySet()) {
            Object v = optimisticGet(entry.getValue(), target);
            if (value==null ? v==null : value.equals(v)) return true;
         }
         return false;
      }

      public boolean isReadOnly(Object key) {
         return schemaSets.get(key) == null;
      }

      @Override
      public Object get(Object key) {
         Method m = schemaGets.get(key);
         if (m == null) return null;
         return optimisticGet(m, target);
      }

      @Override
      public Object put(K key, Object value) {
         Method m = schemaSets.get(key);
         if (m == null) throw new UnsupportedOperationException ();
         Object result = optimisticGet(schemaGets.get(key), target);
         optimisticSet(m, target, value);
         return result;
      }

      @Override
      public Object remove(Object key) {
         throw new UnsupportedOperationException ();
      }

      @Override
      public void putAll(Map<? extends K, ? extends Object> map) {
         if (map == null) return;
         if (! schemaSets.keySet().containsAll(map.keySet()))
            throw new UnsupportedOperationException ();

         for(Entry<? extends K, ? extends Object> entry : map.entrySet()) {
            put(entry.getKey(),entry.getValue());
         }
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException ();
      }

      @Override
      public Set<K> keySet() {
         return schemaGets.keySet();
      }

      @Override
      public Collection<Object> values() {
         List<Object> result = Lists.newArrayListWithCapacity(size());
         for(Entry<K,Method> entry : schemaGets.entrySet()) {
            result.add(optimisticGet(entry.getValue(), target));
         }
         return result;
      }

      @Override
      public int hashCode() {
         int result = 0;
         for(Entry<K,Method> entry : schemaGets.entrySet()) {
            Object v = optimisticGet(entry.getValue(), target);
            if (v == null) {
               result += entry.getKey().hashCode();
            } else {
               result += entry.getKey().hashCode() ^ v.hashCode();
            }
         }
         return result;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder ();
         sb.append('{');
         for(Entry<K,Object> entry : entrySet()) {
            if (sb.length() > 1)
               sb.append(", ");
            K key = entry.getKey();
            Object value = entry.getValue();
            sb.append(key   == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
         }
         sb.append('}');
         return sb.toString();
      }

      @Override
      public Set<Entry<K, Object>> entrySet() {
         Set<Entry<K, Object>> result = Sets.newHashSetWithExpectedSize(size());
         for(Entry<K,Method> entry : schemaGets.entrySet()) {
            K key = entry.getKey();
            Method m = entry.getValue();
            Object value = optimisticGet(m, target);
            if (schemaSets != null && schemaSets.containsKey(key)) {
               Map.Entry e = new AbstractMap.SimpleEntry<K, Object> (key, value) {
                  @Override
                  public Object setValue(Object value) {
                     optimisticSet(schemaSets.get(this.getKey()), target, value);
                     return super.setValue(value);
                  }
               };
               result.add(e);
            } else {
               result.add(Maps.<K, Object>immutableEntry(key, value));
            }
         }
         return result;
      }
   }

   /**
    * Returns the get <tt>Method</tt>'s for the bean
    *
    * @param bean
    * @return property-to-Method getters map
    */
   public static ImmutableMap<String, Method> newBeanInstanceGetters(Object bean) {
      Preconditions.checkNotNull(bean);
      return newBeanGetters(bean.getClass());
   }

   /**
    * Returns the set <tt>Method</tt>'s for the bean
    *
    * @param bean
    * @return property-to-Method setters map
    */
   public static ImmutableMap<String, Method> newBeanInstanceSetters(Object bean) {
      Preconditions.checkNotNull(bean);
      return newBeanSetters(bean.getClass());
   }

   /**
    * Returns the read-only <tt>Map</tt> for the bean
    *
    * @param bean
    * @return property-to-Object map
    */
   public static BeanMapViaIntrospection<String> newBeanMap(Object bean) {
      return BeanMaps.<String>newBeanMapInternal(bean);
   }

   /**
    * Returns the read/write <tt>Map</tt> for the bean
    *
    * @param bean
    * @return property-to-Object map
    */
   public static Map<String, Object> newReadOnlyBeanMap(Object bean) {
      return BeanMaps.<String>newReadOnlyBeanMapInternal(bean);
   }

   static <K> BeanMapViaIntrospection<K> newBeanMapInternal(Object bean) {
      Preconditions.checkNotNull(bean);
      final ImmutableMap<String, Method> getters = newBeanGetters(bean.getClass());
      final ImmutableMap<String, Method> setters = newBeanSetters(bean.getClass());
      return new BeanMapViaIntrospection(bean, getters, setters);
   }

   static <K> BeanMapViaIntrospection<K> newReadOnlyBeanMapInternal(Object bean) {
      Preconditions.checkNotNull(bean);
      final ImmutableMap<String, Method> getters = newBeanGetters(bean.getClass());
      return new BeanMapViaIntrospection(bean, getters);
   }

   /**
    * Returns the get <tt>Method</tt>'s for the class
    *
    * @param aClass
    * @return property-to-Method getters map
    */
   public static ImmutableMap<String, Method> newBeanGetters(Class aClass)  {
      Preconditions.checkNotNull(aClass);
      ImmutableMap<String, Method> result = getters.get(aClass);
      if (result == null) {
         final ImmutableMap.Builder builder = new ImmutableMap.Builder ();
         final Set set = Sets.newHashSet();
         walkIntrospector(aClass, aClass, new Closure<Object[]>() {
            @Override
            public void apply(@Nonnull Object[] args) {
               Method m = (Method)args[1];
               if (m != null) {
                  if (! set.contains(args[0])) {
                     builder.put(args[0], m);
                     set.add(args[0]);
                  }
               }
            }
         });
         result = builder.build();
         getters.put(aClass, result);
      }
      return result;
   }

   /**
    * Returns the set <tt>Method</tt>'s for the class
    *
    * @param aClass
    * @return property-to-Method setters map
    */
   public static ImmutableMap<String, Method> newBeanSetters(Class aClass)  {
      Preconditions.checkNotNull(aClass);
      ImmutableMap<String, Method> result = setters.get(aClass);
      if (result == null) {
         final ImmutableMap.Builder builder = new ImmutableMap.Builder ();
         final Set set = Sets.newHashSet();
         walkIntrospector(aClass, aClass, new Closure<Object[]>() {
            @Override
            public void apply(@Nonnull Object[] args) {
               if (args.length >= 3 && args[2] != null) {
                  if (! set.contains(args[0])) {
                     builder.put(args[0], args[2]);
                  }
                  set.add(args[0]);
               }
            }
         });
         result = builder.build();
         setters.put(aClass, result);
      }
      return result;
   }

   private static void walkIntrospector(Class actualClass, Class aClass, Closure function) {
      /*
      * The introspector only follows the superclass chain.  It is not designed to be handed interfaces.
      *
      * For this, we wish to bias the search for the interface methods to avoid reflection returning
      * what is a public method in Java but considered a private method by Method.invoke
      * because the class was not declared "public".
      */
      for(Class c : aClass.getInterfaces())
         walkIntrospector(actualClass, c, function);

      if (aClass.getSuperclass() != null) {
         walkIntrospector(actualClass, aClass.getSuperclass(), function);
      }

      if (actualClass == aClass || aClass.isInterface()) {
         try {
            PropertyDescriptor[] pis = Introspector.getBeanInfo(aClass).getPropertyDescriptors();
            for(PropertyDescriptor pi : pis) {
               if (pi.getReadMethod() != null)

                  if ((pi.getReadMethod().getModifiers() & Method.PUBLIC) == Method.PUBLIC)
                     function.apply(new Object [] {pi.getName(), pi.getReadMethod(), pi.getWriteMethod()});
            }
         } catch (IntrospectionException e) {
            throw new IllegalArgumentException(e);
         }
      }
   }
}
