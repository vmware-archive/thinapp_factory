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

package com.vmware.thinapp.manualmode.tests;

import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.thinapp.common.converter.dto.Status.State;
import com.vmware.thinapp.common.converter.dto.Ticket;
import com.vmware.thinapp.manualmode.model.ConversionJobModel;
import com.vmware.thinapp.manualmode.server.ManualMode;
import com.vmware.thinapp.manualmode.server.Status;
import com.vmware.thinapp.manualmode.server.UnknownTicketException;

@ContextConfiguration("classpath:ManualModeTest-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class ManualModeTest {
   @Autowired
   private ManualMode manualMode;

   @Test
   public void testRequest() throws Exception {
      ConversionJobModel job = new ConversionJobModel();
      job.setId(Math.abs(new Random().nextLong()));
      Ticket t = manualMode.create(null,job);

      try {
         Status status = manualMode.redeem(t);

         while (status.getCurrentState().compareTo(State.preCaptureWait) != 0) {
            Thread.sleep(1000);
            status = manualMode.redeem(t);
         }

         manualMode.next(t, State.preCaptureWait);

         while (status.getCurrentState().compareTo(State.installationWait) != 0) {
            Thread.sleep(1000);
            status = manualMode.redeem(t);
         }

         manualMode.next(t, State.installationWait);


         while (status.getCurrentState().compareTo(State.finished) != 0) {
            Thread.sleep(1000);
            status = manualMode.redeem(t);
         }
      } finally {
         // Removed this method as it was only used in this test
         //manualMode.waitFor(t);
      }
   }

   @Test
   @ExpectedException(UnknownTicketException.class)
   public void testNoSuchRequestOnClaim() throws Exception {
      Ticket t = new Ticket(Long.toString(Math.abs(new Random().nextLong())));
      manualMode.redeem(t);
   }

   /**
    * Special care has to be taken that exceptions aren't dropped on the floor
    * inside various Futures.
    *
    * @throws Exception
    */
   @Test
   @ExpectedException(Exception.class)
   public void testThatExceptionsBubbleUp() throws Exception {
      ConversionJobModel job = new ConversionJobModel();
      job.setId(Math.abs(new Random().nextLong()));
      Ticket t = manualMode.create(null, job);
      while (true) {
         manualMode.redeem(t);
         Thread.sleep(100);
      }
   }
}
