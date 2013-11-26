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

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.vmware.thinapp.workpool.Instancer;

/**
 * A growable workpool that creates new instances by cloning a base VM.
 */
@Entity
@Table(name = "linkedworkpool")
public class LinkedWorkpoolModel extends GrowableWorkpoolModel<VmImageModel> {
   private static final long serialVersionUID = -6327755025965080498L;

   @NotNull
   private VmImageModel instancer;

   // Need any cascades?  We don't want ALL because we don't want DELETE to cascade.
   @ManyToOne
   @JoinColumn(name = "instance_id")
   @Override
   public VmImageModel getInstancer() {
      return instancer;
   }

   @Override
   public void setInstancer(VmImageModel instancer) {
      this.instancer = instancer;
   }

   @Override
   public Instancer createInstancer(ApplicationContext appCtxt) {
      AutowireCapableBeanFactory beanFactory = appCtxt.getAutowireCapableBeanFactory();
      return (Instancer) beanFactory.getBean("cloneInstancer", getInstancer());
   }
}
