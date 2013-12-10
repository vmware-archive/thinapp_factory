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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.workpool.dao.WorkpoolRepository;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmLocationModel;
import com.vmware.thinapp.workpool.model.WorkpoolModel;

import akka.actor.TypedActor;
import akka.dispatch.Future;
import scala.Option;

public class WorkpoolManagerImpl extends TypedActor implements WorkpoolManager {
   private static final Logger log = LoggerFactory.getLogger(WorkpoolManagerImpl.class);

   @Value("#{workpoolProperties['iniPath']}")
   private String INI_PATH;

   @Autowired
   private WorkpoolRepository workpoolDao;
   @Autowired
   private VCManager vcManager;
   @Autowired
   private Util util;
   @Resource(name="workpoolTransactionTemplate")
   private TransactionTemplate txn;

   private final HashMap<Long, WorkpoolInstance> workpools = new HashMap<Long, WorkpoolInstance>();
   private WorkpoolManager self;

   @Override
   public void preStart() {
      self = getContext().getSelfAs();
      loadVcConfig();
      initialize();
   }

   @Override
   public Future<Void> delete(final long id, DeleteMethod deleteMethod) {
      Option<WorkpoolInstance> maybeWorkpool = get(id);

      if (maybeWorkpool.isDefined()) {
         WorkpoolInstance instance = maybeWorkpool.get();
         final Future<Void> future;
         switch (deleteMethod) {
            case deleteFromDisk:
               future = instance.delete();
               break;
            case removeFromInventory:
               future = instance.forget();
               break;
            default:
               // Have to have this to ensure future is assigned a value.
               throw new IllegalArgumentException();
         }

         future.get();
      } else {
         throw new IllegalArgumentException("No workpool instance found with id: " + id);
      }

      return future(null);
   }

   /**
    * Initialize workpool manager at startup by reanimating workpools persisted in the database.
    */
   private void initialize() {
      // Should probably do other cleanup actions like look for dead clones (ie clones with a moid), etc.
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            log.info("Reconstructing actors from storage.");
            List<WorkpoolModel> wps = workpoolDao.findAll();

            for (final WorkpoolModel workpool : wps) {
               createInstance(workpool);
            }
         }
      });
   }

   /**
    * Load VC configuration from OVF-based settings.
    */
   private void loadVcConfig() {
      Ini ini = null;
      try {
         ini = new Ini(new File(INI_PATH));
      } catch (IOException e) {
         log.info("Skipping VC configuration because " + INI_PATH + " was not found.");
         return;
      }

      log.info("Loading configuration from: {}.", ini.getFile());
      Profile.Section vcSection = ini.get("VCEnvironment");
      VCConfigModel vcConfig = createVcConfig(vcSection);
      log.info("Updating VC.");
      vcManager.update(vcConfig);
   }

   /**
    * Populate VC configuration from the legacy ini file
    *
    * @param vcSection vc ini section
    * @return VC configuration
    */
   private VCConfigModel createVcConfig(Profile.Section vcSection) {
      VCConfigModel vcConfig = new VCConfigModel();
      vcConfig.setHost(vcSection.get("VirtualMachineHost"));
      vcConfig.setDatacenter(vcSection.get("Datacenter"));
      vcConfig.setUsername(vcSection.get("HostLoginUserName"));
      vcConfig.setPassword(vcSection.get("HostLoginPassword"));

      VmLocationModel vmLocation = new VmLocationModel();
      vmLocation.setComputeResource(vcSection.get("ComputeResource"));
      vmLocation.setDatastoreName(vcSection.get("Datastore"));
      vmLocation.setResourcePool(vcSection.get("ResourcePool"));
      vcConfig.setVmLocation(vmLocation);

      return vcConfig;
   }

   @Override
   public WorkpoolInstance create(final WorkpoolModel workpool) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            workpoolDao.saveOrUpdate(workpool);
         }
      });
      return createInstance(workpool);
   }

   /**
    * Create a new workpool instance.
    *
    * @param workpool a workpool model that must return a valid id
    * @return
    */
   private WorkpoolInstance createInstance(WorkpoolModel workpool) {
      WorkpoolInstance instance = (WorkpoolInstance) util.appCtxt.getAutowireCapableBeanFactory().
              getBean("workpoolInstance", workpool, workpool.createInstancer(util.appCtxt));
      workpools.put(workpool.getId(), instance);
      instance.subscribe(self);
      return instance;
   }

   @Override
   public Collection<WorkpoolInstance> list() {
      return workpools.values();
   }

   @Override
   public void update(WorkpoolStateChange stateChange) {
      if (stateChange.getNewState() == WorkpoolModel.State.deleted) {
         WorkpoolInstance workpool = stateChange.getSender();
         workpool.unsubscribe(self);
         log.debug("Deleting workpool instance: {}.", workpool);
         workpools.remove(workpool.getId());
         TypedActor.stop(workpool);
      }
   }

   @Override
   public Option<WorkpoolInstance> get(long id) {
      return Option.apply(workpools.get(id));
   }

   @Override
   public Option<WorkpoolInstance> findByName(final String name) {
      WorkpoolModel wp = txn.execute(new TransactionCallback<WorkpoolModel>() {
         @Override
         public WorkpoolModel doInTransaction(TransactionStatus transactionStatus) {
            return workpoolDao.findByField("name", name);
         }
      });

      if (wp != null) {
         return get(wp.getId());
      } else {
         return Option.empty();
      }
   }

   /**
    * Reset all workpools and images by deleting any current leases and
    * attempting to shut down all running VMs.
    */
   @Override
   public void reset() {
      for (WorkpoolInstance instance : workpools.values()) {
         instance.reset().get();
      }
   }
}
