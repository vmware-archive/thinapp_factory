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

package com.vmware.thinapp.workpool.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.vmware.thinapp.workpool.Instancer;

/**
 * A growable workpool that creates new instances by installing
 * VMs from scratch each time.
 */
@Entity
@Table(name = "fullworkpool")
public class FullWorkpoolModel extends GrowableWorkpoolModel<VmPatternModel> {
   private static final long serialVersionUID = 4076004578967776668L;

   @NotNull
   private VmPatternModel instancer;

   @ManyToOne(cascade = CascadeType.ALL)
   @JoinColumn(name = "instance_id")
   @Override
   public VmPatternModel getInstancer() {
      return instancer;
   }

   @Override
   public void setInstancer(VmPatternModel instancer) {
      this.instancer = instancer;
   }

   @Override
   public Instancer createInstancer(ApplicationContext appCtxt) {
      AutowireCapableBeanFactory beanFactory = appCtxt.getAutowireCapableBeanFactory();
      return (Instancer) beanFactory.getBean("installInstancer", getInstancer(), getName());
   }
}
