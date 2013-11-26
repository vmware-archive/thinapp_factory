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

package com.vmware.appfactory.workpool.simulator;

import java.util.ArrayList;
import java.util.List;

import com.vmware.thinapp.common.vi.dto.VINode;

/**
 * This class contains static methods that return dummy virtual infrastructure data.
 */
public class VISimulatorData
{
   /**
    * Creates a dummy VC inventory for datastores.
    * This returns a root VINode, and its children contains the datastores.
    *
    * @TODO convert all this into a static loadable form and fetch data from there
    * instead of creating it from scratch again and again.
    *
    * @param vmNode
    * @return The root VINode
    */
   protected static VINode createDummyDatastoresTree(VINode vmNode)
   {
      List<VINode> nodeList = new ArrayList<VINode>();
      // First compute resource
      VINode cr = new VINode("VMwareCluster", "domain-c668", VINode.Type.ComputeResource, true);
      cr.setChildren(nodeList);

      VINode n1 = new VINode("thinApp ds-1", "datastore-1", VINode.Type.Datastore, "[thinApp ds-1]");
      n1.setHasChild(true);
      nodeList.add(n1);

      n1 = new VINode("thinApp ds-2", "datastore-2", VINode.Type.Datastore, "[thinApp ds-2]");
      n1.setHasChild(true);
      nodeList.add(n1);

      n1 = new VINode("thinApp ds-3", "datastore-3", VINode.Type.Datastore, "[thinApp ds-3]");
      n1.setHasChild(true);
      nodeList.add(n1);

      return cr;
   }

   /**
    * Creates a dummy VC inventory for network.
    * This returns a root VINode, and its children contains the datastores.
    *
    * @TODO convert all this into a static loadable form and fetch data from there
    * instead of creating it from scratch again and again.
    *
    * @return VINode - the root VINode
    */
   protected static VINode createDummyNetworkTree(VINode dsNode)
   {
      VINode root = dsNode.clone();
      List<VINode> nodeList = new ArrayList<VINode>();
      root.setChildren(nodeList);

      nodeList.add(new VINode("wdc-thinapp-br-4LM8", "network-11", VINode.Type.Network));
      nodeList.add(new VINode("wdc-thinapp-prime-vm", "network-219", VINode.Type.Network));
      nodeList.add(new VINode("ThinApp-network-3", "network-3", VINode.Type.Network));

      return root;
   }


   /**
    * Creates a dummy VC inventory. This returns a root ComputeResource, which recursively contains
    * children VINode elements containing ResourcePools and VirtualMachines.
    *
    * @return VINode - the root VINode
    */
   protected static VINode createDummyVMTree()
   {
      List<VINode> nodeList = new ArrayList<VINode>();
      // First compute resource
      VINode root = new VINode("VMwareCluster [vcenter.mycompany.com]", "domain-c668", VINode.Type.ComputeResource, true);
      root.setChildren(nodeList);

      VINode n1 = new VINode("Thinapp resourcepool", "pool-a1z", VINode.Type.ResourcePool);
      nodeList.add(n1);
      List<VINode> nodeList1 = new ArrayList<VINode>();
      n1.setChildren(nodeList1);

      nodeList1.add(new VINode("Test Suite XP Template", "vm-4452", VINode.Type.VirtualMachine));
      nodeList1.add(new VINode("VMware vCenter Server Appliance", "vm-4700", VINode.Type.VirtualMachine));
      nodeList1.add(new VINode("appfactory-capture2-dev Template Instance 8907e", "vm-4793", VINode.Type.VirtualMachine));
      nodeList1.add(new VINode("appfactory-capture2-dev Template", "vm-4787", VINode.Type.VirtualMachine));
      nodeList1.add(new VINode("appfactory-capture2-dev", "vm-4785", VINode.Type.VirtualMachine));
      nodeList1.add(new VINode("test-workpool", "vm-4721", VINode.Type.VirtualMachine));

      VINode n11 = new VINode("TAF Demo pool", "pool-v215", VINode.Type.ResourcePool);
      nodeList1.add(n11);
      List<VINode> nodeList11 = new ArrayList<VINode>();
      n11.setChildren(nodeList11);

      nodeList11.add(new VINode("appfactory-capture1-dev (OLD)", "vm-29", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("appfactory-dev", "vm-40", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("capture-1-new", "vm-50", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("AppFactory Windows 7 Template", "vm-3538", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("Test Suite Vista Template", "vm-4046", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("Test Suite XP Template", "vm-4452", VINode.Type.VirtualMachine));
      nodeList11.add(new VINode("VMware vCenter Server Appliance", "vm-4700", VINode.Type.VirtualMachine));

      VINode n2 = new VINode("Thinapp prime pool", "pool-2", VINode.Type.ResourcePool);
      nodeList.add(n2);
      List<VINode> nodeList2 = new ArrayList<VINode>();
      n2.setChildren(nodeList2);

      nodeList2.add(new VINode("appfactory-capture1-dev (OLD)", "vm-29", VINode.Type.VirtualMachine));
      nodeList2.add(new VINode("appfactory-dev", "vm-40", VINode.Type.VirtualMachine));
      nodeList2.add(new VINode("capture-1-new", "vm-43", VINode.Type.VirtualMachine));
      nodeList2.add(new VINode("capture-10-new", "vm-53", VINode.Type.VirtualMachine));

      return root;
   }

}
