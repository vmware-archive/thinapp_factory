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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.vmware.thinapp.manualmode.server.Mounter;
import com.vmware.thinapp.workpool.GuestCommands;

public class MounterTest {
   GuestCommands gc;

   @Before
   public void setup() {
      //XXX: This is not compiling for me, commenting out so the build is no
      //     longer broken
      //gc = new GuestCommands(WorkpoolStub.vcInfo, WorkpoolStub.vmInfo);
   }

   @Test
   public void testMount() throws Exception {
      Mounter m =
            new Mounter("\\\\taf.your.company.com\\myshare", "taf-user",
               "taf-password", "F:");
      m.mount(gc);
   }

   @Test
   public void testUnmount() throws IOException {
      Mounter m =
            new Mounter("\\\\taf.your.company.com\\myshare", "taf-user",
               "taf-password", "F:");
      m.mount(gc);
      m.unmount(gc);
   }
}
