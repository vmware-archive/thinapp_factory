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

package com.vmware.thinapp.manualmode.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.converter.dto.TicketRequest;
import com.vmware.thinapp.manualmode.Util;
import com.vmware.thinapp.manualmode.model.ConversionJobModel;

/**
 * <p>
 * 1. Acquire a VM given a template name.
 * <p>
 * 2. Power down the VM if on (?).
 * <p>
 * 3. Create a temporary snapshot.
 * <p>
 * 4. Power the VM on.
 * <p>
 * 5. Wait for Tools to respond.
 * <p>
 * 6. Upload ThinApp installer.
 * <p>
 * 7. Install ThinApp silently.
 * <p>
 * 8. Upload vmw.lic.
 * <p>
 * ?. Start the remote console in the user's browser.
 * <p>
 * ?. User does their thing.
 * <p>
 * ?. User says they're done with it (or timeout expires). ?. Delete snapshot.
 * <p>
 * ?. Release the VM (after some timeout?).
 * <p>
 * ?. How to prevent a previous user from clobbering another user's VM?
 */

@Component
public class ManualMode {
   private final Map<Ticket, Request> requests =
         new ConcurrentHashMap<Ticket, Request>();

   @Autowired
   Util util;

   @Autowired
   RequestFactory requestFactory;

   public Ticket create(TicketRequest req, ConversionJobModel job) {
      Ticket ticket = job.createTicket();
      Request request = requestFactory.create(req);
      requests.put(ticket, request);

      // Start processing the request asynchronously.
      request.start();

      return ticket;
   }

   public Status redeem(Ticket ticket) {
      Request request = getRequestOrThrow(ticket);
      return request.getStatus();
   }

   public void next(Ticket ticket, State state) {
      Request request = getRequestOrThrow(ticket);
      request.next(state);
   }

   public void cancel(Ticket ticket) {
      Request request = getRequestOrThrow(ticket);
      request.cancel();
   }

   // XXX: Need to do this somewhere... look at ExecutorCompletionService.
   //      // Need to do this after job is done.
   //      requests.remove(ticket);

   private Request getRequestOrThrow(Ticket ticket) {
      Request request = requests.get(ticket);

      if (request == null) {
         throw new UnknownTicketException();
      }
      return request;
   }
}
