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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.vmware.thinapp.common.workpool.dto.InstanceInfo;
import com.vmware.thinapp.common.workpool.dto.VCConfig;
import com.vmware.vim25.ManagedObjectNotFound;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedObject;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Component(value = "workpool.util")
public class Util {
   @Autowired
   ApplicationContext appCtxt;

   /**
    * Autowires the given object
    *
    * Spring doesn't make it easy to autowire objects created directly (i.e.
    * with new)
    *
    * @param <T>
    * @param bean
    *           instance to be autowired
    * @return the autowired instance
    */
   public <T> T autowire(T bean) {
      appCtxt.getAutowireCapableBeanFactory().autowireBean(bean);
      return bean;
   }

   /**
    * Find a managed virtual machine object.  The datacenter specified in the
    * the given VC info must exist.
    *
    * @param vc source VC
    * @param vm source VM
    * @param svc connected instance
    * @return managed object on success, not sure on failure
    */
   public static com.vmware.vim25.mo.VirtualMachine findVm(VCConfig vc,
         InstanceInfo vm, ServiceInstance svc) {
      // Call just to ensure the datacenter exists.
      findDatacenter(vc.getDatacenter(), svc);
      return findVm(vm.getMoid(), svc);
   }

   /**
    * Find a managed virtual machine object.
    *
    * @param vmMoid the moid of the VM to locate
    * @param svc connected instance
    * @return managed object on success, not sure on failure
    */
   public static com.vmware.vim25.mo.VirtualMachine findVm(String vmMoid,
         ServiceInstance svc) {
      ManagedObjectReference ref = new ManagedObjectReference();
      ref.setType("VirtualMachine");
      ref.setVal(vmMoid);
      VirtualMachine m = new VirtualMachine(svc.getServerConnection(), ref);
      // Make sure it's valid.
      m.getName();
      return m;
   }

   /**
    * Determines if the given VM is actually present on the server.
    *
    * @param vm
    * @return
    */
   public static boolean vmExists(VirtualMachine vm) {
      try {
         vm.getName();
         return true;
      } catch (Exception e) {
         // Can't do catch (ManagedObjectNotFound) because it doesn't extend
         // Exception.
         if (!(e instanceof ManagedObjectNotFound)) {
            throw new RuntimeException(e);
         }
      }

      return false;
   }

   /**
    * Find a managed datacenter object.  If not found, a WorkpoolException will be thrown.
    *
    * @param name
    *           the common name as seen in vSphere Client
    * @param svc
    *           a connected instance
    * @return datacenter object or null if not found
    */
   static Datacenter findDatacenter(String name, ServiceInstance svc) {
      Folder root = svc.getRootFolder();
      ManagedObject[] children;

      try {
         children =
               new InventoryNavigator(root).searchManagedEntities("Datacenter");
      } catch (RemoteException e) {
         throw new RuntimeException(e);
      }

      for (ManagedObject o : children) {
         Datacenter dc = (Datacenter) o;

         if (dc.getName().equals(name)) {
            return dc;
         }
      }

      throw new WorkpoolException(String.format(
            "Unable to locate the datacenter %s.", name));
   }
}
