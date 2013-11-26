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

package com.vmware.appfactory.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

/**
 * The only purpose of this class is to get around a limitation in Spring 3
 * whereby one cannot set a customArgumentResolver when using
 * <mvc:annotation-driven /> controllers.
 *
 * In spring 3.1, one can set this much more easily by the following:
 *     <mvc:annotation-driven>
 *       <mvc:argument-resolvers>
 *         <beans:bean class="com.vmware.appfactory.common.AtmosphereResourceArgumentResolver" />
 *       </mvc:argument-resolvers>
 *     </mvc:annotation-driven>
 *
 * So when we move to this version of spring, we can remove this class,
 * subtituting the above XML in mvc-config.xml.
 *
 * For more info, see:
 * http://scottfrederick.blogspot.com/2011/03/customizing-spring-3-mvcannotation.html
 */
public class AtmosphereResourceBeanPostProcessor implements BeanPostProcessor {

   @Autowired
   AtmosphereResourceArgumentResolver resolver;

   @Override
   public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof AnnotationMethodHandlerAdapter) {
         ((AnnotationMethodHandlerAdapter) bean).setCustomArgumentResolver(resolver);
      }
      return bean;
   }

   @Override
   public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }
}
