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

package com.vmware.thinapp.vi.web.controllers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.workpool.exception.WpException;
import com.vmware.thinapp.vi.InventoryBrowser;

/**
 * This controller exposes virtual infrastructure's vm inventory as REST api.
 *
 * @author Keerthi Singri
 * @since M8, August 22, 2011
 */
@Controller
@RequestMapping("/vi")
public class InventoryController {
   private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

   @Autowired
   private InventoryBrowser inventoryBrowser;

   /**
    * This method provides the virtual infrastructure's inventory by type.
    * Given a VINode.nodeType, it fetches all the corresponding inventory
    * and returns the root of a canonical form result.
    *
    * @param invType - One of datastore, datacenter, network, virtualmachine
    * @return VINode
    * @throws WpException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(value="/inventory/{viType}", method=RequestMethod.GET)
   public VINode getVIInventoryByType(@PathVariable String viType) {
      if (VINode.Type.Datastore.toString().equalsIgnoreCase(viType)) {
         return inventoryBrowser.loadVITreeFromComputeResource(VINode.Type.Datastore);
      }
      else if (VINode.Type.Network.toString().equalsIgnoreCase(viType)) {
         return inventoryBrowser.loadVITreeFromComputeResource(VINode.Type.Network);
      }
      else if (VINode.Type.VirtualMachine.toString().equalsIgnoreCase(viType)) {
         return inventoryBrowser.loadVITreeFromComputeResource(VINode.Type.VirtualMachine);
      }
      throw new BaseRuntimeException("Invalid invType: " + viType);
   }

   /**
    * This method returns folders / iso files for the input datastore node
    * under the path specified in the node.
    *
    * @param node
    * @return
    * @throws WpException - If input is invalid
    */
   @ResponseBody
   @RequestMapping(value="/inventory/datastore/browseIso", method=RequestMethod.POST)
   public FileNode[] browseVIDatastoreForISO(@RequestBody VINode node) {
      if (node.getNodeType() != VINode.Type.Datastore) {
         log.error("Invalid inputm nodeType: " + node.getNodeType());
         throw new BaseRuntimeException("Invalid input");
      }
      List<FileNode> nodeList = inventoryBrowser.browseDatastoreForISO(node);
      return nodeList.toArray(new FileNode[nodeList.size()]);
   }

}
