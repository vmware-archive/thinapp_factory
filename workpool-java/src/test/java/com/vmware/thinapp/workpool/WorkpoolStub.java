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

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.Lease;
import com.vmware.thinapp.common.workpool.dto.VCConfig;

public class WorkpoolStub {
   private final Logger log = LoggerFactory.getLogger(WorkpoolStub.class);

   public static InstanceInfo vmInfo = new InstanceInfo() {
      {
         setGuestUsername("guest-user");
         setGuestPassword("guest-password");
         setVmxPath("[LUN1] appfactory-dev Instance/appfactory-dev Instance.vmx");
      }
   };

   public static VCConfig vcInfo = new VCConfig() {
      {
         setDatacenter("ThinApp Domain Controller");
         setHost("vc.your.company.com");
         setUsername("vc-user");
         setPassword("vc-password");
         setDatacenterMoid("ds1");
      }
   };

   Lease lease = new Lease() {
      {
         setVc(vcInfo);
         setVm(vmInfo);
      }
   };

   /*
   @Override
   public Future<Lease> acquire(final String templateName) {
      if (!templateName.equals("appfactory-dev")) {
         FutureTask<Lease> futureTask = new FutureTask<Lease>(
               new Callable<Lease>() {
                  @Override
                  public Lease call() {
                     log.error("Throwing exception!");
                     throw new RuntimeException(
                           "Template must be appfactory-dev, is: " + templateName);
                  };
               });
         futureTask.run();
         return futureTask;
      }

      FutureTask<Lease> l = new FutureTask<Lease>(Callables.returning(lease));
      log.info("Acquiring lease: {}.", l);
      l.run();
      return l;
   }
*/

   public void release(Lease l) {
      log.info("Releasing lease: {}.", l);
   }

   public void withTemporarySnapshot(CallWithTemporarySnapshot runWithSnapshot,
         Lease lease) throws Exception {
      log.info("withTemporarySnapshot unimplemented.");
      runWithSnapshot.call(new VirtualMachineStub(), lease);
   }

   @SuppressWarnings("unused")
   public void withSnapshot(PrepareSnapshot i,
         CallWithTemporarySnapshot runWithSnapshot, Lease lease)
         throws Exception {
      // TODO Auto-generated method stub

   }

   @SuppressWarnings("unused")
   public Future<Lease> acquire(
         com.vmware.thinapp.common.workpool.dto.Workpool workpool) {
      // TODO Auto-generated method stub
      return null;
   }
}
