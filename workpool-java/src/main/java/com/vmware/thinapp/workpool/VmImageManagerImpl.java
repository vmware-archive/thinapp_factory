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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.vmware.thinapp.common.workpool.dto.DeleteMethod;
import com.vmware.thinapp.workpool.dao.VmImageRepository;
import com.vmware.thinapp.workpool.model.VmImageModel;

import akka.actor.TypedActor;
import akka.dispatch.Future;
import scala.Option;

public class VmImageManagerImpl extends TypedActor implements VmImageManager {
   private static final Logger log = LoggerFactory.getLogger(VmImageManagerImpl.class);

   @Autowired
   private VmImageRepository vmImageDao;

   @Autowired
   private Util util;

   @Resource(name="workpoolTransactionTemplate")
   private TransactionTemplate txn;

   private final HashMap<Long, VmImageInstance> images = new HashMap<Long, VmImageInstance>();
   private VmImageManager self;

   @Override
   public void preStart() {
      self = getContext().getSelfAs();
      initialize();
   }

   private void initialize() {
      log.info("Reconstructing actors from storage.");

      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         public void doInTransactionWithoutResult(TransactionStatus status) {
            List<VmImageModel> insts = vmImageDao.findAll();

            for (final VmImageModel vmImage : insts) {
               VmImageInstance i = createVmInstance(vmImage);
               images.put(vmImage.getId(), i);
            }
         }
      });

      log.info("Reconstruction complete.");
   }

   private VmImageInstance createVmInstance(final VmImageModel newVm) {
      VmImageInstance instance = (VmImageInstance) util.appCtxt.getAutowireCapableBeanFactory()
              .getBean("vmImageInstance", newVm);
      // Subscribe to instance updates so that when it goes into state deleted
      // we can stop the actor and remove it from our lookup table.
      instance.subscribe(self);
      return instance;
   }

   @Override
   public Future<Void> delete(final long id, DeleteMethod deleteMethod) {
      final VmImageInstance instance = get(id);
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
            throw new IllegalArgumentException("Invalid delete method.");
      }

      future.get();
      return future(null);
   }

   @Override
   public VmImageInstance create(final VmImageModel vmImage) {
      log.debug("Creating new VM image: {}", vmImage);

      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         public void doInTransactionWithoutResult(TransactionStatus status) {
            VmImageModel vm = vmImageDao.findByField("name", vmImage.getName());
            if (vm != null) {
               throw new IllegalArgumentException("The given name is already in use.");
            }

            vmImageDao.save(vmImage);
         }
      });

      VmImageInstance inst = createVmInstance(vmImage);
      images.put(vmImage.getId(), inst);
      return inst;
   }

   @Override
   public VmImageInstance get(long id) {
      return images.get(id);
   }

   @Override
   public Option<VmImageInstance> findByName(final String name) {
      VmImageModel image = txn.execute(new TransactionCallback<VmImageModel>() {
         @Override
         public VmImageModel doInTransaction(TransactionStatus transactionStatus) {
            return vmImageDao.findByField("name", name);
         }
      });

      if (image != null) {
         return Option.apply(get(image.getId()));
      } else {
         return Option.empty();
      }
   }

   @Override
   public Collection<VmImageInstance> list() {
      return images.values();
   }

   @Override
   public void update(VmImageStateChange arg) {
      VmImageStateChange change = arg;
      VmImageInstance instance = change.getSender();

      if (change.getNewState() == VmImageModel.State.deleted) {
         log.info("Stopping instance: {}.", instance);
         long vmImageId = instance.getId();
         instance.unsubscribe(self);
         TypedActor.stop(instance);
         images.remove(vmImageId);
      }
   }
}
