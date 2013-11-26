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

package com.vmware.thinapp.vi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.vi.dto.FileNode;
import com.vmware.thinapp.common.vi.dto.FolderNode;
import com.vmware.thinapp.common.vi.dto.VINode;
import com.vmware.thinapp.common.vi.util.VIConstants;
import com.vmware.thinapp.workpool.VCManager;
import com.vmware.thinapp.workpool.model.VCConfigModel;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.FileQuery;
import com.vmware.vim25.FileQueryFlags;
import com.vmware.vim25.FolderFileInfo;
import com.vmware.vim25.FolderFileQuery;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.IsoImageFileQuery;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostDatastoreBrowser;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.Network;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * The inventory browser performs specific operations related to navigating
 * the virtual infrastructure.
 *
 * The supported operations are to browse the datastore for ISO files, list managed
 * entities for Folder, Datacenter, Datastore, VirtualMachine, etc.
 *
 * The getServiceInstance() takes care of the VC connection using the VCManager.
 * The getComputeResource() provides the right ComputeResource to use.
 *
 * @see VCManager
 * @author Keerthi Singri
 */
@Component
public class InventoryBrowser {
   private static final Logger log = LoggerFactory.getLogger(InventoryBrowser.class);

   @Autowired
   private VCManager vcManager;

   /**
    * The tafVMs and othersVMs lists are used to reduce the overhead of VM
    * filtering. When a VM should be filtered (either b/c it is a Linux one
    * or b/c its machine.id indicates it is a TAF VM), its moid is added
    * to the tafVMs list. Otherwise, its moid is added the othersVMs list.
    * Next time, we are able to decide whether a VM should be filtered by
    * looking at these two local lists without going back to VC (which could
    * be expensive).
    *
    * To avoid we cached a wrong moid due to moid reuse, these two lists
    * are refreshed every 7200 seconds.
    */

   /*
    *  A list that contains moids of all TAF VMs we have seen so far.
    */
   private Set<String> tafVMs = new HashSet<String>();

   /*
    * A list that contains moids of all non-TAF VMs we have seen so far.
    */
   private Set<String> othersVMs = new HashSet<String>();

   private long lastGCTime = 0;
   private static final long CACHE_REFRESH_INTERVAL = 7200 * 1000;
   private static final String TAF_VM_KEY = "com.vmware.thinappfactory";

   /**
    * Returns the ServieInstance handle, used to browse the VI inventory.
    *
    * @return
    */
   public ServiceInstance getServiceInstance() {
      ServiceInstance si = vcManager.getConnection().get().get();
      return si;
   }

   /**
    * Get the computeResource ManagedEntity from vcManager.
    * The name of the compute resource is stored in VCConfig.vmLocation
    *
    * @return
    */
   public ComputeResource getComputeResource() {
      VCConfigModel config = vcManager.getConfig();
      String name = config.getVmLocation().getComputeResource();
      return (ComputeResource) getManagedEntityByName(VIConstants.COMPUTE_RESOURCE, name);
   }

   /**
    * Get a tree construct containing compute resources as level 0.
    * Based on type, level 2 is loaded as children nodes.
    *
    * If type=Network        Level 2 nodes are network nodes.
    * If type=Datastore      Level 2 nodes are datastore nodes.
    * If type=VirtualMachine Level 2+ nodes are virtual machine/resource pool nodes.
    *
    * @param type
    * @param name
    * @return
    */
   public VINode loadVITreeFromComputeResource(VINode.Type type) {
      ComputeResource cr = getComputeResource();
      // Create the root node and set its children.
      VINode crVINode = convertManagedEntityToVINode(cr);
      List<VINode> crList = new ArrayList<VINode>();
      crVINode.setName(cr.getName() + extractVIHostName(cr));
      crVINode.setChildren(crList);

      if (type == VINode.Type.Datastore) {
         // If datastore, load the all datastores that are browsable.
         Datastore[] dsArray = cr.getDatastores();
         List<VINode> dsVINodeList = convertManagedEntityArrayToVINode(dsArray, true);
         crVINode.setChildren(dsVINodeList);
      } else if (type == VINode.Type.Network) {
         Network[] netArray = cr.getNetworks();
         List<VINode> networkVINodeList = convertManagedEntityArrayToVINode(netArray, false);
         crVINode.setChildren(networkVINodeList);
      } else if (type == VINode.Type.VirtualMachine) {
         try {
            List<VINode> vmPoolVINodeList = loadVMSubTreeForResourcePool(cr.getResourcePool());
            crVINode.setChildren(vmPoolVINodeList);
         } catch (RemoteException e) {
            throw new BaseRuntimeException(e);
         }
      } else {
         // Flag ComputeResource as having children.
         crVINode.setHasChild(true);
      }
      return crVINode;
   }

   /**
    * Return a list of VINodes containing VM or Folder VINode types for
    * the input Folder, and each Folder recursively containing VINodes for
    * its children.
    *
    * @param folder
    * @return - null if no children exist.
    * @throws RemoteException
    */
   public synchronized List<VINode> loadVMSubTreeForResourcePool(ResourcePool pool) throws RemoteException {
      long currentTime = (new Date()).getTime();
      if (currentTime - lastGCTime > CACHE_REFRESH_INTERVAL) {
         // clear all local caches to ensure we don't
         // cache a reused moid (take long time to happen).
         tafVMs.clear();
         othersVMs.clear();
         lastGCTime = currentTime;
      }
      HashSet<String> allVmIds = new HashSet<String>();
      List<VINode> vmList = loadVMSubTreeForResourcePool(pool, allVmIds);

      // GC on tafVMs
      Iterator<String> itr = tafVMs.iterator();
      while(itr.hasNext()) {
         String moid = itr.next();
         if (!allVmIds.contains(moid)) {
            itr.remove();
            log.trace("Removed moid {} from known TAF VM list.", moid);
         }
      }

      // GC on othersVMs
      itr = othersVMs.iterator();
      while (itr.hasNext()) {
         String moid = itr.next();
         if (!allVmIds.contains(moid)) {
            itr.remove();
            log.trace("Removed moid {} from known others VM list.", moid);
         }
      }

      return vmList;
   }

   private List<VINode> loadVMSubTreeForResourcePool(
                     ResourcePool pool,
                     Collection<String> allVmIds) throws RemoteException {
      List<VINode> childVINodeList = new ArrayList<VINode>();
      ResourcePool[] poolArray = pool.getResourcePools();
      for (int j = 0; poolArray != null && j < poolArray.length; j++) {
         List<VINode> childList = loadVMSubTreeForResourcePool(
                                  poolArray[j], allVmIds);
         // Add only if this folder has vms or child folders
         if (!CollectionUtils.isEmpty(childList)) {
            VINode poolVINode = convertManagedEntityToVINode(poolArray[j]);
            poolVINode.setChildren(childList);
            childVINodeList.add(poolVINode);
         }
      }

      VirtualMachine vms[] = pool.getVMs();
      for (VirtualMachine vm : vms) {
         allVmIds.add(vm.getMOR().get_value());
      }
      List<VINode> vmNodeList = convertManagedEntityArrayToVINode(
                                           filterOutTAFVms(vms), false);
      childVINodeList.addAll(vmNodeList);
      return childVINodeList;
   }

   /**
    * Method to filter out TAF VMs (appliances, templates, and capture
    * instances managed by this appliances as well as others) from the
    * input parameter 'vms'. Returns only VMs that does not belong to
    * any TAF server. This method also update the tafVMs and othersVMs
    * lists on seeing new VMs.
    *
    * @param vms all VMs to be checked.
    *
    * @return VMs that not belong to any TAF server.
    */
   private VirtualMachine[] filterOutTAFVms(VirtualMachine vms[]) throws RemoteException {
      HashMap<String, VirtualMachine> vmMap = new HashMap<String, VirtualMachine>();

      for (VirtualMachine vm : vms) {
         String moid = vm.getMOR().get_value();
         vmMap.put(moid, vm);
      }

      // Check the tafVMs list.
      for (String moid : tafVMs) {
         if (vmMap.remove(moid) != null) {
            log.trace("VM {} in known TAF VM list is filtered out.", moid);
         }
      }

      // Check the othersVMs list, os type, machine.id etc.
      Iterator<VirtualMachine> itr = vmMap.values().iterator();
      while (itr.hasNext()) {
         VirtualMachine vm = itr.next();
         String moid = vm.getMOR().get_value();

         if (othersVMs.contains(moid)) {
            continue;
         }

         // We haven't seen this VM previously. check its guest id against
         // support guest id list.
         VirtualMachineConfigInfo info = vm.getConfig();
         String guestId = info.getGuestId();
         if (!VIConstants.GUEST_ID_SUPPOTRED.contains(guestId)) {
            tafVMs.add(moid);
            itr.remove();
            log.trace("VM {} of {} guest is filtered out.", moid, guestId);
            continue;
         }

         // It is not a Linux VM, we check its machine.id
         OptionValue[] extraConfig = info.getExtraConfig();
         if (extraConfig == null) {
            // No extra config, it is not a TAF VM. Hence, add it to the
            // othersVMs
            othersVMs.add(moid);
            continue;
         }

         boolean isTAFVm = false;

         for (OptionValue optval : extraConfig) {
            if (optval.getKey().equals(TAF_VM_KEY)) {
               isTAFVm = true;
               break;
            }
         }

         if (isTAFVm) {
            // It is a TAF VM, add to the known TAF VM list and remove from
            // display VM list.
            tafVMs.add(moid);
            itr.remove();
            log.trace("VM {} is managed by a TAF and is filtered out.", vm.getName());
         } else {
            // It is not a TAF VM, add to the others VM list to reduce further
            // VC call.
            othersVMs.add(moid);
            log.trace("Added VM {} to othersVMs.", vm.getName());
         }
      }

      // What left in the vmMap are all non-TAF VMs.
      return vmMap.values().toArray(new VirtualMachine[]{});
   }

   /**
    * This method browses a given datastore starting at a certain path, and gets
    * all the children iso files and folders under that.
    *
    * @param node
    * @return
    */
   public List<FileNode> browseDatastoreForISO(VINode node) {
      if (node == null || VINode.Type.Datastore != node.getNodeType()) {
         throw new BaseRuntimeException("Invalid input for datastore browsing");
      }
      Datastore ds = (Datastore)getManagedEntityFromVINode(node);
      HostDatastoreBrowser hdb = ds.getBrowser();

      List<FileNode> nodeList = new ArrayList<FileNode>();
      try {
         Task task = hdb.searchDatastore_Task(node.getPath(), generateHDBSSForISO());
         if (task.waitForTask() == Task.SUCCESS) {
            Object object = task.getTaskInfo().getResult();
            if (object instanceof HostDatastoreBrowserSearchResults) {
               HostDatastoreBrowserSearchResults result = (HostDatastoreBrowserSearchResults)object;
               // log.debug("Folder: " + result.getFolderPath());
               FileInfo[] fis = result.getFile();
               for (int j = 0; fis != null && j < fis.length; j++) {
                  // log.debug("Path: " + fis[j].getPath() + ", size: " + fis[j].getFileSize());
                  String newPath = result.getFolderPath();

                  // If search path is the datastore root append a whitespace else file browsing fails.
                  if (newPath.charAt(newPath.length()- 1) == ']') {
                     newPath += " ";
                  } else if (!newPath.endsWith("/")) {
                     // Append / to the folder path if not ending with 1 (only in WS case)
                     newPath += "/";
                  }
                  newPath += fis[j].getPath();
                  FileNode file = null;
                  if (fis[j] instanceof FolderFileInfo) {
                     file = new FolderNode(fis[j].getPath(), newPath, fis[j].getFileSize(), true);
                  } else {
                     file = new FileNode(fis[j].getPath(), newPath, fis[j].getFileSize());
                  }
                  nodeList.add(file);
               }
            }
         }
      } catch (RemoteException e) {
         throw new BaseRuntimeException(e);
      } catch (InterruptedException e) {
         throw new BaseRuntimeException(e);
      }
      return nodeList;
   }

   /**
    * Helper method to load a ManagedEntity by type and name.
    *
    * @param name
    * @return null if not found or input is null
    */
   private ManagedEntity getManagedEntityByName(String type, String name) {
      if (StringUtils.isEmpty(name) || StringUtils.isEmpty(type)) {
         return null;
      }
      try {
         InventoryNavigator i = new InventoryNavigator(getServiceInstance().getRootFolder());
         return i.searchManagedEntity(type, name);
      } catch (RemoteException e) {
         throw new BaseRuntimeException(e);
      }
   }

   /**
    * This is a helper method that loads the managed entity from VINode.
    * It uses the VINode.Type and the VINode.morValue to determine the
    * ManagedEntity.
    *
    * @param node
    * @return
    */
   private ManagedEntity getManagedEntityFromVINode(VINode node) {
      ManagedObjectReference vmRef = new ManagedObjectReference();
      vmRef.setVal(node.getMorValue());

      if (VINode.Type.Datastore == node.getNodeType()) {
         vmRef.setType(VIConstants.DATASTORE);
         return new Datastore(getServiceInstance().getServerConnection(), vmRef);
      } else if (VINode.Type.ComputeResource == node.getNodeType()) {
         vmRef.setType(VIConstants.COMPUTE_RESOURCE);
         return new ComputeResource(getServiceInstance().getServerConnection(), vmRef);
      }
      // The type didnt match as expected.
      return null;
   }

   /**
    * Convert array of ManagedEntity objects into VINode list.
    *
    * @param meArray
    * @param hasChild
    * @return
    */
   private List<VINode> convertManagedEntityArrayToVINode(ManagedEntity[] meArray, boolean hasChild) {
      List<VINode> viNodeList = new ArrayList<VINode>();
      for (int j = 0; meArray != null && j < meArray.length; j++) {
         VINode node = convertManagedEntityToVINode(meArray[j]);
         node.setHasChild(hasChild);
         viNodeList.add(node);
      }
      return viNodeList;
   }

   /**
    * Creates a VINode for a ManagedEntity of type:
    * Datacenter, Folder, Datastore, VirtualMachine only
    *
    * Depending on the type, the corresponding flexible fields are added.
    *
    * For any other type return null.
    *
    * @param managedEntity
    * @return
    */
   private VINode convertManagedEntityToVINode(ManagedEntity managedEntity) {
      VINode viNode = null;
      if (managedEntity instanceof ComputeResource) {
         // ComputeResource is the root in our case.
         viNode = new VINode(managedEntity.getName(), managedEntity.getMOR().getVal(),
               VINode.Type.ComputeResource, true);
      } else if (managedEntity instanceof VirtualMachine) {
         VirtualMachine vm = (VirtualMachine) managedEntity;
         viNode = new VINode(vm.getName(), vm.getMOR().getVal(),
               VINode.Type.VirtualMachine);
         if (vm.getConfig() != null) {
            viNode.addProperty(VIConstants.GUEST_ID, vm.getConfig().getGuestId());
         }
      } else if (managedEntity instanceof Datastore) {
         viNode = new VINode(managedEntity.getName(), managedEntity.getMOR().getVal(),
               VINode.Type.Datastore, "[" + managedEntity.getName() + "]");
      } else if (managedEntity instanceof Network) {
         viNode = new VINode(managedEntity.getName(), managedEntity.getMOR().getVal(),
               VINode.Type.Network);
      } else if (managedEntity instanceof ResourcePool) {
         viNode = new VINode(managedEntity.getName(), managedEntity.getMOR().getVal(),
               VINode.Type.ResourcePool);
      } else if (managedEntity instanceof Folder) {
         viNode = new VINode(managedEntity.getName(), managedEntity.getMOR().getVal(),
               VINode.Type.Folder);
      }
      return viNode;
   }

   /**
    * Get the VI hostname from any ManagedEntity.
    *
    * @param managedEntity
    * @return
    */
   private String extractVIHostName(ManagedEntity managedEntity) {
      return " [" + managedEntity.getServerConnection().getUrl().getHost() + "]";
   }

   /**
    * This is the HostDatastoreBrowserSearchSpec customized to search for
    * ISOs and folders.
    *
    * @return tailored instance of HostDatastoreBrowserSearchSpec
    */
   private HostDatastoreBrowserSearchSpec generateHDBSSForISO() {
      HostDatastoreBrowserSearchSpec hdbss = new HostDatastoreBrowserSearchSpec();
      FileQueryFlags fqf = new FileQueryFlags();

      // All these fields are mandatory
      fqf.setFileSize(true);
      fqf.setModification(false);
      fqf.setFileOwner(false);
      fqf.setFileType(true);

      // Not need to match *.iso pattern, use IsoImageFileQuery, as its faster.
      hdbss.setQuery(new FileQuery[] { new IsoImageFileQuery(), new FolderFileQuery() });
      hdbss.setDetails(fqf);
      hdbss.setSortFoldersFirst(true);
      hdbss.setSearchCaseInsensitive(true);

      return hdbss;
   }

}
