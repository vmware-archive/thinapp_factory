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

package com.vmware.appfactory.workpool.controller;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vmware.appfactory.common.base.AbstractApiController;
import com.vmware.appfactory.common.exceptions.AfBadRequestException;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.workpool.exception.WpException;


/**
 * This controller handles all the virtual infrastructure related API calls
 * to AppFactory.
 *
 * All these API calls use JSON-formatted data where applicable.
 */
@Controller
public class VIApiController
   extends AbstractApiController
{
   /**
    * This method provides the vSphere inventory tree for the given VCConfig.
    * If no VCConfig is provided, then the default is used.
    *
    * @return
    * @throws WpException
    */
   @ResponseBody
   @RequestMapping(
         value = "/vi/inventory/networkList",
         method = RequestMethod.GET)
   public String[] getNetworkList()
   throws WpException, AfBadRequestException
   {
      VINode root = getVIInventory(VINode.Type.Network);

      // Check if no network nodes, return empty.
      if (root == null || CollectionUtils.isEmpty(root.getChildren())) {
         return null;
      }
      List<VINode> nodeList = root.getChildren();
      String[] networkNames = new String[nodeList.size()];
      for (int i = 0; i < nodeList.size(); i++) {
         networkNames[i] = nodeList.get(i).getName();
      }
      return networkNames;
   }


   /**
    * This method provides the vSphere inventory tree for the given VCConfig.
    * If no VCConfig is provided, then the default is used.
    *
    *
    * @param viType - Defines a specific type of the VI inventory.
    * @return VINode
    * @throws WpException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/vi/inventory/{viType}",
         method = RequestMethod.GET)
   public VINode getVIInventory(
         @PathVariable VINode.Type viType)
   throws WpException, AfBadRequestException
   {
      if (!(VINode.Type.Datastore == viType
            || VINode.Type.Network == viType
            || VINode.Type.VirtualMachine == viType)) {
         throw new AfBadRequestException("Invalid input");
      }
      return _viClient.getVIInventoryByType(viType);
   }


   /**
    * This method gets the iso files and folder for the VINode that contains the
    * datastore and the path to the current folder within the datastore.
    *
    * @param node - VINode
    * @return - Children files(iso) and folders, Null if empty.
    *
    * @throws WpException
    * @throws AfBadRequestException
    */
   @ResponseBody
   @RequestMapping(
         value = "/vi/inventory/datastore/browseIso",
         method = RequestMethod.POST)
   public FileNode[] browseVIDatastoreForISO (
         @RequestBody VINode node)
      throws WpException, AfBadRequestException
   {
      if (node == null
            || StringUtils.isEmpty(node.getMorValue())
            || node.getNodeType() != VINode.Type.Datastore) {
         throw new AfBadRequestException("Invalid input VINode");
      }
      FileNode[] nodeArray = _viClient.browseVIDatastoreForISO(node);
      Arrays.sort(nodeArray);
      return nodeArray;
   }

}
