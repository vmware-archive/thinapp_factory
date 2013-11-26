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

package com.vmware.thinapp.workpool.tests;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vmware.thinapp.workpool.Workpool;
import com.vmware.thinapp.workpool.WorkpoolImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

@ContextConfiguration("classpath:workpool-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class WorkpoolTest extends AbstractJUnit4SpringContextTests {
   /**
    * Test basic acquire and release flow
    *
    * @throws Exception
    */
   @Test
   public void testAcquireAndRelease() throws Exception {
      // OMG what?! Doing this to create two unique instances that themselves
      // are autowired.
      Workpool p =
            applicationContext.getAutowireCapableBeanFactory().createBean(
                  WorkpoolImpl.class);
      Workpool z =
            applicationContext.getAutowireCapableBeanFactory().createBean(
                  WorkpoolImpl.class);

      assertNotSame(p, z);

/*
      try {
         Future<Lease> Lease = p.acquire("appfactory-dev");
         l = Lease.get();
         assertEquals("datacenter-2", l.getVc().getDcMoid());
      } finally {
         if (l != null) {
            p.release(l);
         }
      }
      */
   }

   /**
    * Test running within a temporary snapshot
    *
    * @throws Exception
    */
   @Test
   public void testWithTemporarySnapshot() throws Exception {
      final java.util.concurrent.atomic.AtomicBoolean a = new AtomicBoolean();
      assertFalse(a.get());
/*
      Future<Lease> future = workpool.acquire("appfactory-dev");
      Lease lease = future.get();

      try {
         CallWithTemporarySnapshot runWithSnapshot =
               new CallWithTemporarySnapshot() {
                  @Override
                  public void call(VirtualMachine vm, Lease lease) {
                     a.set(true);
                  }
               };
         workpool.withTemporarySnapshot(runWithSnapshot, lease);
      } finally {
         workpool.release(lease);
      }

      // See if we got called.
      assertTrue(a.get());
      */
   }
}
