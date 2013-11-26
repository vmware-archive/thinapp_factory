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

package com.vmware.appfactory.manualmode;

import org.springframework.stereotype.Service;

import com.vmware.appfactory.common.base.AbstractRestClient;
import com.vmware.appfactory.config.ConfigRegistryConstants;
import com.vmware.thinapp.common.converter.dto.Status;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.common.converter.dto.TicketRequest;

/**
 * A client for the manual capture process.
 */
@Service("manualModeClient")
public class ManualModeClientService
   extends AbstractRestClient
{
   public ManualModeClientService()
   {
      super(ConfigRegistryConstants.CWS_CONVERSIONS_URL);
   }

   @Override
   protected String baseUrl() {
      return super.baseUrl() + "/manual";
   }

   public Ticket create(TicketRequest ticketRequest)
   {
      return _rest.postForObject(baseUrl(), ticketRequest, Ticket.class);
   }

   public Status redeem(Ticket ticket)
   {
      return _rest.postForObject(baseUrl() + "/redeem", ticket, Status.class);
   }

   public void next(Ticket ticket, String stage)
   {
      _rest.postForObject(baseUrl() + String.format("/next/%s", stage), ticket, String.class);
   }

   public void cancel(Ticket ticket) {
      _rest.postForObject(baseUrl() + "/cancel", ticket, String.class);
   }
}