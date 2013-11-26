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

import java.rmi.RemoteException;
import java.util.Random;
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
import com.vmware.thinapp.workpool.model.OsInfoModel;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.thinapp.workpool.model.VmPatternModel;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

import akka.dispatch.Future;
import akka.dispatch.Futures;

/**
 * Instancer that instantiates VMs by doing a full OS installation.
 */
public class InstallInstancer extends InstancerBase implements Instancer {
   private static final Logger log = LoggerFactory.getLogger(InstallInstancer.class);

   @Autowired
   private Util util;
   @Autowired
   private VCManager vcManager;
   @Resource(name="workpoolTransactionTemplate")
   private TransactionTemplate txn;
   @Autowired
   private InstanceRepository instanceDao;

   private final VmPatternModel vmPattern;
   private final String templateName;

   public InstallInstancer(VmPatternModel vmPattern, String templateName) {
      this.vmPattern = vmPattern;
      this.templateName = templateName;

      // Install instancer is always available.
      setState(State.available);
   }

   @Override
   public FutureWrapper<CloneRunner.Result> addInstance(final InstanceModel instance) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
            InstanceModel inst = instanceDao.get(instance.getId());

            if (vmPattern.getOsInfo().getOsType() == OsInfoModel.OsType.winXPPro) {
               inst.setGuestUsername(Constants.GUEST_USERNAME);
            } else {
               // For the Vista+ case we can't use the Administrator account so we
               // use the account that the OS is installed with.
               inst.setGuestUsername(vmPattern.getOsRegistration().getUserName());
            }

            inst.setGuestPassword(Constants.GUEST_PASSWORD);
            // All Easy provisioned VMs also have autologon enabled.
            inst.setAutologon(true);
         }
      });
      return new FutureWrapper<CloneRunner.Result>(install());
   }

   @Override
   public void update(StateChange<Instancer.State, Instancer.State> stateChange) {
      // Needed to satisfy interface but unused for this implementation.
   }

   protected InstallRunner getInstallerRunner(InstallRequest request) {
      return (InstallRunner) util.appCtxt.getAutowireCapableBeanFactory().getBean("installRunner", request);
   }

   public Future<CloneRunner.Result> install() {
      log.info("Requesting an installation.");

      // Note that this actor can't service any requests while we're waiting for the
      // ServiceInstance to be returned.  Not sure if that matters or not.  It also eats
      // up a thread.
      final ServiceInstance conn = vcManager.getConnection().get().get();
      VCConfigModel config = vcManager.getConfig();
      String vmName = templateName + String.format(" Instance %05x", new Integer(new Random().nextInt(0xfffff)));

      final InstallRequest request = new InstallRequest(vmName, config, vmPattern,
              HardwareConfiguration.DEFAULT, config.getVmLocation(), Constants.GUEST_PASSWORD);
      final InstallRunner runner = getInstallerRunner(request);

      // Note that we currently have no way to receive notification that a future reached
      // its timeout so we just use max for now.  See:
      // https://www.assembla.com/spaces/akka/tickets/904-add--ontimeout--callback-to-futures
      return Futures.future(new Callable<CloneRunner.Result>() {
         @Override
         public CloneRunner.Result call() throws Exception {
            try {
               InstallRunner.Result result = runner.run();

               if (result.isSuccess()) {
                  createCleanSnapshot(result.getMoid(), conn);
               }

               CloneRunner.Result cloneRes = new CloneRunner.Result(result.getMoid(), result.isSuccess(), result.getError());
               return cloneRes;
            } catch (Exception e) {
               CloneRunner.Result cloneRes = new CloneRunner.Result("", false, e.getMessage());
               log.error("Installation was unsuccessful.", e);
               return cloneRes;
            }
         }
      }, Long.MAX_VALUE);
   }

   /**
    * Create a clean base snapshot.
    *
    * @param moid VM managed object id
    * @param conn VC connection
    */
   private void createCleanSnapshot(String moid, ServiceInstance conn) {
      VirtualMachine vm = VirtualMachineHelper.createFromMoid(conn, moid);
      log.info("Creating clean snapshot for: {}", vm);
      try {
         Task task = vm.createSnapshot_Task(Constants.THINAPP_CLEAN_SNAPSHOT, Constants.THINAPP_CLEAN_SNAPSHOT_DESCRIPTION, false, false);
         task.waitForTask();
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}
