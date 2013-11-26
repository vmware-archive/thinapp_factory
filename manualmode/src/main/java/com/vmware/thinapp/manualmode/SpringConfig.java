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

package com.vmware.thinapp.manualmode;

import java.io.File;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import com.vmware.thinapp.manualmode.server.RuntimeManager;

@Configuration
public class SpringConfig implements ApplicationContextAware {
   private final String converterDbName = "converter";
   private ApplicationContext appCtxt;

   @Value("#{manualModeProperties.runtimesPath}")
   String runtimesPath;

   @Value("#{databaseProperties['my.datasource.username']}")
   private String dbUsername;

   @Value("#{databaseProperties['my.datasource.password']}")
   private String dbPassword;

   @Override
   @Required
   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      appCtxt = applicationContext;
   }

   @Bean
   public DataSource converterDataSource() {
      return new DriverManagerDataSource(String.format("jdbc:postgresql:%s", converterDbName), dbUsername, dbPassword);
   }

   @Bean
   public AnnotationSessionFactoryBean converterSessionFactoryBean() {
      AnnotationSessionFactoryBean factory = new AnnotationSessionFactoryBean();
      factory.setPackagesToScan(new String[]{"com.vmware.thinapp.manualmode"});
      factory.setDataSource(converterDataSource());
      Properties hibProps = new Properties();
      hibProps.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      // Don't automatically update database schema but validate that it's correct.
      hibProps.setProperty("hibernate.hbm2ddl.auto", "validate");
      factory.setHibernateProperties(hibProps);
      return factory;
   }

   @Bean
   public SessionFactory converterSessionFactory() {
      return converterSessionFactoryBean().getObject();
   }

   @Bean
   public HibernateTransactionManager converterTransactionManager() {
      HibernateTransactionManager manager = new HibernateTransactionManager();
      manager.setSessionFactory(converterSessionFactory());
      return manager;
   }

   @Bean
   public RuntimeManager runtimeManager() {
      RuntimeManager runtimeManager = new RuntimeManager(new File(runtimesPath));
      appCtxt.getAutowireCapableBeanFactory().autowireBean(runtimeManager);
      return runtimeManager;
   }
}
