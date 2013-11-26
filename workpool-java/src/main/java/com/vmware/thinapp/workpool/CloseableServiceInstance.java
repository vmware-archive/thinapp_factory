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

import com.vmware.vim25.mo.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

public class CloseableServiceInstance extends ServiceInstance {
   private static final Logger log = LoggerFactory
         .getLogger(CloseableServiceInstance.class);
   // 15 minutes.
   private static final int KEEP_ALIVE_PERIOD_IN_SECONDS = 60 * 15;
   private boolean connected = false;

   /**
    * We have to implement this to make Spring happy since it's the constructor
    * we use. Not really sure why.
    *
    * @param url
    * @param username
    * @param password
    * @param ignoreCert
    * @throws MalformedURLException
    * @throws RemoteException
    */
   public CloseableServiceInstance(URL url, String username, String password,
         boolean ignoreCert) throws MalformedURLException, RemoteException {
      super(url, username, password, ignoreCert);
      connected = true;
   }

   @PreDestroy
   public synchronized void close() throws InterruptedException {
      log.debug("Logging out ServiceInstance session.");
      connected = false;
      this.getServerConnection().logout();
   }

   @SuppressWarnings("unused")
   @Scheduled(fixedDelay = KEEP_ALIVE_PERIOD_IN_SECONDS * 1000)
   private synchronized void refresh() {
      // Sessions expire on the server after 30 minutes of inactivity so we
      // have to poll every so often to keep the session fresh.
      if (!connected) {
         return;
      }

      try {
         // TODO: We might fail if the VC server reboots and the server-side session is lost.
         // Probably need to be able to reconnect.
         log.debug("Pinging server to keep session alive.");
         currentTime();
      } catch (Exception e) {
         log.error("Error while refreshing the vSphere session.", e);
      }
   }
}
