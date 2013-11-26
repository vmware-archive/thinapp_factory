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

package com.vmware.thinapp.workpool;

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
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.vmware.thinapp.workpool.model.VmImageModel;
import com.vmware.thinapp.workpool.model.VmPatternModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

import akka.actor.TypedActor;
import akka.actor.TypedActorFactory;
import akka.dispatch.Dispatchers;
import akka.dispatch.MessageDispatcher;

@Configuration
public class Config implements ApplicationContextAware {
   private final String workpoolDbName = "workpool";

   @Value("#{databaseProperties['my.datasource.username']}")
   private String dbUsername;

   @Value("#{databaseProperties['my.datasource.password']}")
   private String dbPassword;

   private ApplicationContext appCtxt;

   // XXX: Had to do such a huge pool size because of all the actors waiting on VCManager.getConnection() because
   // they block waiting on it.  They should probably be made non-blocking?  Or give VCManager its own dispatcher so
   // it can continue to do work.
   private final MessageDispatcher defaultDispatcher = Dispatchers.newExecutorBasedEventDrivenDispatcher("default")
           .setMaxPoolSize(1024)
           .build();

   @Override
   @Required
   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      appCtxt = applicationContext;
   }

   @Bean
   public VmImageManager vmImageManager() {
      return TypedActor.newInstance(VmImageManager.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            VmImageManagerImpl i = new VmImageManagerImpl();
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   @Scope("prototype")
   public TransactionTemplate workpoolTransactionTemplate() {
      return new TransactionTemplate(workpoolTransactionManager());
   }

   @Bean
   public VCManager vcManager() {
      return TypedActor.newInstance(VCManager.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            VCManagerImpl i = new VCManagerImpl();
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   public WorkpoolManager workpoolManager() {
      return TypedActor.newInstance(WorkpoolManager.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            WorkpoolManagerImpl i = new WorkpoolManagerImpl();
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   @Scope("prototype")
   public Instancer cloneInstancer(final VmImageModel vmImage) {
      return TypedActor.newInstance(Instancer.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            CloneInstancer i = new CloneInstancer(vmImage);
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   @Scope("prototype")
   public Instancer installInstancer(final VmPatternModel vmPattern, final String templateName) {
      return TypedActor.newInstance(Instancer.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            InstallInstancer i = new InstallInstancer(vmPattern, templateName);
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   @Scope("prototype")
   public WorkpoolInstance workpoolInstance(final WorkpoolModel workpool, final Instancer instancer) {
      return TypedActor.newInstance(WorkpoolInstance.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            WorkpoolInstanceImpl i = new WorkpoolInstanceImpl(workpool, instancer);
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   @Scope("prototype")
   public VmImageInstance vmImageInstance(final VmImageModel newVm) {
      return TypedActor.newInstance(VmImageInstance.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            VmImageInstanceImpl i = new VmImageInstanceImpl(newVm);
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }

   @Bean
   public DataSource workpoolDataSource() {
      return new DriverManagerDataSource(String.format("jdbc:postgresql:%s", workpoolDbName), dbUsername, dbPassword);
   }

   @Bean
   public SessionFactory workpoolSessionFactory() {
      return workpoolSessionFactoryBean().getObject();
   }

   @Bean
   public AnnotationSessionFactoryBean workpoolSessionFactoryBean() {
      AnnotationSessionFactoryBean factory = new AnnotationSessionFactoryBean();
      factory.setPackagesToScan(new String[]{"com.vmware.thinapp.workpool"});
      factory.setDataSource(workpoolDataSource());
      Properties hibProps = new Properties();
      hibProps.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      // Don't automatically update database schema but validate that it's correct.
      hibProps.setProperty("hibernate.hbm2ddl.auto", "validate");
      factory.setHibernateProperties(hibProps);
      return factory;
   }

   @Bean
   public HibernateTransactionManager workpoolTransactionManager() {
      HibernateTransactionManager manager = new HibernateTransactionManager();
      manager.setSessionFactory(workpoolSessionFactory());
      return manager;
   }

   @Bean
   @Scope("prototype")
   public Instancer nullInstancer() {
      return TypedActor.newInstance(Instancer.class, new TypedActorFactory() {
         @Override
         public TypedActor create() {
            NullInstancer i = new NullInstancer();
            i.getContext().actorRef().setDispatcher(defaultDispatcher);
            appCtxt.getAutowireCapableBeanFactory().autowireBean(i);
            return i;
         }
      }, Long.MAX_VALUE);
   }
}
