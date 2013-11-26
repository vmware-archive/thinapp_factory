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

import java.util.concurrent.Callable;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.vmware.thinapp.workpool.dao.InstanceRepository;
import com.vmware.thinapp.workpool.model.InstanceModel;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmImageModel;

import akka.dispatch.Futures;

/** Clone strategy for instantiating a VM. */
public class CloneInstancer extends InstancerBase implements Instancer {
   private static final Logger log = LoggerFactory.getLogger(CloneInstancer.class);

   @Resource(name="workpoolTransactionTemplate")
   private TransactionTemplate txn;
   @Autowired
   private Util util;
   @Autowired
   private VmImageManager vmImageManager;
   @Autowired
   private InstanceRepository instanceDao;
   @Autowired
   private VCManager vcManager;

   private VmImageModel.State imageState = VmImageModel.State.unknown;

   private final long vmImageId;
   private VmImageInstance vmImageInstance;

   public CloneInstancer(VmImageModel vmImage) {
      this.vmImageId = vmImage.getId();
   }

   @Override
   public void preStart() {
      // InstancerBase actually has a preStart implementation, not TypedActor.
      super.preStart();

      this.vmImageInstance = vmImageManager.get(vmImageId);
      vmImageInstance.subscribe(self);
   }

   @Override
   public void postStop() {
      // Unsubscribe to avoid a dangling reference that would prevent our being GCed.
      vmImageInstance.unsubscribe(self);
   }

   @Override
   public void update(@SuppressWarnings("rawtypes") StateChange stateChange) {
      if (stateChange instanceof VmImageStateChange) {
         VmImageStateChange vmImageStateChange = (VmImageStateChange) stateChange;
         log.debug("Received notification of state change: {}.", vmImageStateChange);
         imageState = vmImageStateChange.getNewState();

         if (imageState.isFailure()) {
            setState(State.failed);
         } else if (imageState.isReady()) {
            setState(State.available);
         }
      }
   }

   /**
    * Initiates creation of a new clone.
    *
    * @param instance a non-owned instance that has the id set
    */
   @Override
   public FutureWrapper<CloneRunner.Result> addInstance(final InstanceModel instance) {
      VmImageModel vmImage = vmImageInstance.getVmImage();

      // XXX: If this code throws an exception then onInstancingComplete never runs
      // and a useless instance lingers around that will never actually be created.
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
            instanceDao.refresh(instance);
            // For XP and Vista+ we are able to reset the Administrator account during sysprep.
            instance.setGuestUsername(Constants.GUEST_USERNAME);
            instance.setGuestPassword(Constants.GUEST_PASSWORD);
            // All cloned instances are created with autologon enabled.
            instance.setAutologon(true);
            instanceDao.update(instance);
         }
      });

      if (imageState == VmImageModel.State.available) {
         VCConfigModel vcConfig = vcManager.getConfig();
         final CloneRequest cloneRequest = new CloneRequest(
                 vcConfig.getVmLocation(),
                 vcConfig,
                 vmImage,
                 instance.getWorkpool().getName(),
                 Constants.GUEST_PASSWORD);
         final CloneRunner runner = getCloneRunner(cloneRequest);
         return new FutureWrapper<CloneRunner.Result>(Futures.future(new Callable<CloneRunner.Result>() {
            @Override
            public CloneRunner.Result call() throws Exception {
               return runner.run();
            }
         }, Long.MAX_VALUE));
      } else {
         throw new RuntimeException("Image is not yet in state available.");
      }
   }

   protected CloneRunner getCloneRunner(CloneRequest cloneRequest) {
      return (CloneRunner) util.appCtxt.getAutowireCapableBeanFactory().getBean("cloneRunner", cloneRequest);
   }
}
