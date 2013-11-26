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

import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.vmware.thinapp.common.workpool.dto.VCConfig.ApiType;
import com.vmware.thinapp.workpool.dao.VCConfigRepository;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;

import akka.actor.Scheduler;
import akka.actor.TypedActor;
import akka.dispatch.DefaultCompletableFuture;
import akka.dispatch.Future;
import akka.dispatch.Futures;
import akka.japi.Procedure;

public class VCManagerImpl extends TypedActor implements VCManager {
   private static enum State {
      connected,
      disconnected,
      connecting
   }

   private static final Logger log = LoggerFactory.getLogger(VCManagerImpl.class);

   @Autowired
   private VCConfigRepository vcConfigDao;
   @Resource(name="workpoolTransactionTemplate")
   private TransactionTemplate txn;

   private State state = State.disconnected;
   private VCManager self;

   private DefaultCompletableFuture<ServiceInstance> conn = null;

   @Override
   public boolean isCloningSupported() {
      if (state != State.connected) {
         throw new NotConnectedException();
      }

      ServiceInstance si = conn.get();
      String computeResource = getConfig().getVmLocation().getComputeResource();
      log.debug("Determining if compute resource {} supports delta backed disks.", computeResource);
      ComputeResource res;
      try {
         res = (ComputeResource) new InventoryNavigator(si.getRootFolder())
                 .searchManagedEntity("ComputeResource", computeResource);
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }

      if (res == null) {
         throw new RuntimeException("Unable to locate configured compute resource: " + computeResource + ".");
      }

      boolean cloningSupported = false;

      try {
         cloningSupported = res.getEnvironmentBrowser()
                 .queryTargetCapabilities(null /* intersection of all hosts in the cluster */)
                 .deltaDiskBackingsSupported;
         log.debug("Compute resource {} {} support delta backed disks.", computeResource,
                 cloningSupported ? "does" : "does not");
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }
      return cloningSupported;
   }

   /* ... */
   @Override
   public ApiType getApiType() {
      if (state != State.connected) {
         throw new NotConnectedException();
      }

      ServiceInstance si = conn.get();
      String apiTypeStr = null;
      apiTypeStr = si.getServiceContent().getAbout().getApiType();
      for (ApiType t : ApiType.values()) {
         if (apiTypeStr.equalsIgnoreCase(t.name())) {
            return t;
         }
      }

      String msg = "Encountered an unknown API type: " + apiTypeStr;
      log.error(msg);
      throw new RuntimeException(msg);
   }

   @Override
   public void preStart() {
      self = getContext().getSelfAs();
      connect();

      // Schedule a server ping for every 15 minutes.
      Scheduler.schedule(new Runnable() {
         @Override
         public void run() {
            self.refresh();
         }
      }, 0, 15, TimeUnit.MINUTES);
   }

   /**
    * Initiates a background connection to VC
    */
   @Override
   public void connect() {
      if (state != State.disconnected) {
         return;
      }

      final VCConfigModel config = getConfig();

      if (config == null) {
         log.debug("No VC configuration is currently set.  Skipping connect.");
         return;
      }

      log.info("Beginning connection in background...");

      // Not sure what this timeout should be.
      conn = new DefaultCompletableFuture<ServiceInstance>(Long.MAX_VALUE);
      state = State.connecting;
      final boolean ignoreCert = true;

      Futures.future(new Callable<ServiceInstance>() {
         @Override
         public ServiceInstance call() throws Exception {
            log.info("Connecting to: {}.", config);
            ServiceInstance si = new ServiceInstance(new URL(String.format("https://%s/sdk", config.getHost())),
                    config.getUsername(), config.getPassword(), ignoreCert);
            log.info("Created service instance: {}.", si);
            return si;
         }
      }, Long.MAX_VALUE).onComplete(new Procedure<Future<ServiceInstance>>() {
         @Override
         public void apply(Future<ServiceInstance> param) {
            self.connectDone(param);
         }
      });
   }

   /**
    * Callback once the connection to VC is finished
    *
    * @param result: the VC connection
    */
   @Override
   public void connectDone(Future<ServiceInstance> result) {
      try {
         ServiceInstance si = result.get();
         log.info("Successfully connected to: {}.", si);
         state = State.connected;
      } catch (Throwable t) {
         log.error("Failed to connect to VC.", t);
         state = State.disconnected;
         // XXX: Try to reconnect and reset connection future?
      }

      // Signal to callers that want a connection that it's ready (or that it failed so that they can try again.)
      conn.completeWith(result);
   }

   /**
    * Refresh the server connection to keep it active
    * <p/>
    * VC sessions are deleted on the server after 30 minutes by default.  This keeps the session
    * alive on the server.  However, it doesn't do anything about reconnecting if the session
    * on the server is closed (either by an administrator, reboot, etc.)
    */
   @Override
   public void refresh() {
      if (state == State.disconnected) {
         log.info("Connecting to VC.");
         connect();
         return;
      }

      if (conn.isCompleted()) {
         log.debug("Pinging VC to test and keep connection alive.");
         ServiceInstance si = conn.get();
         try {
            si.currentTime();
         } catch (RemoteException e) {
            log.info("Reconnecting to VC.");
            state = State.disconnected;
            connect();
         }
      } else {
         log.debug("No connection is available to refresh.");
      }
   }

   /**
    * Retrieves a VC connection
    *
    * @return This actually returns a Future<ServiceInstance> but the typed proxy doesn't let you return a
    *         Future indirectly (you must call this.future(value) which is stupid.)
    */
   @Override
   public FutureWrapper<ServiceInstance> getConnection() {
      refresh();
      return new FutureWrapper<ServiceInstance>(conn);
   }

   /**
    * Update the details for connecting to VC
    *
    * @param vcConfig
    */
   @Override
   public void update(final VCConfigModel vcConfig) {
      txn.execute(new TransactionCallbackWithoutResult() {
         @Override
         protected void doInTransactionWithoutResult(TransactionStatus status) {
            vcConfigDao.update(vcConfig);
         }
      });

      state = State.disconnected;
      connect();
   }

   @Override
   public VCConfigModel getConfig() {
      return txn.execute(new TransactionCallback<VCConfigModel>() {
         @Override
         public VCConfigModel doInTransaction(TransactionStatus status) {
            return vcConfigDao.getConfig();
         }
      });
   }
}
