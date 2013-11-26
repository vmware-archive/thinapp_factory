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

package com.vmware.thinapp.common.vi.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * This is a POJO used to store the VI inventory. This is a light-weight object
 * that holds very minimal info compared to Folder, Datacenter, VirtualMachine
 * objects in the vijava api.
 *
 * @author Keerthi Singri
 */
public class VINode
{
   /**
    * Various types of nodes based on vijava's ManagedEntity.
    * Used as css class for displaying icons.
    */
   public enum Type {
      Folder,
      Datacenter,
      ComputeResource,
      VirtualMachine,
      Datastore,
      Network,
      ResourcePool
   }

   /** Set for  the root node only */
   private boolean root;

   /** This can be used to construct back the ManagedEntity. */
   private String morValue;

   /** Explicitly indicate node has children, without setting children field. */
   private boolean hasChild;
   private String name;
   private String path;
   private Type nodeType;
   private List<VINode> children = new ArrayList<VINode>();
   private Map<String, String> properties;

   public VINode(){
      /* Empty for default constructor */
   }

   public VINode(String name, String morValue, Type nodeType) {
      this.name = name;
      this.morValue = morValue;
      this.nodeType = nodeType;
   }

   public VINode(String name, String morValue, Type nodeType, boolean root) {
      this.name = name;
      this.morValue = morValue;
      this.nodeType = nodeType;
      this.root = root;
   }

   public VINode(String name, String morValue, Type nodeType, String path) {
      this.name = name;
      this.morValue = morValue;
      this.nodeType = nodeType;
      this.path = path;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the root
    */
   public boolean isRoot() {
      return root;
   }

   /**
    * @param root the root to set
    */
   public void setRoot(boolean root) {
      this.root = root;
   }

   /**
    * @return the nodeType
    */
   public Type getNodeType() {
      return nodeType;
   }

   /**
    * @param nodeType the nodeType to set
    */
   public void setNodeType(Type nodeType) {
      this.nodeType = nodeType;
   }

   /**
    * @return the children
    */
   public List<VINode> getChildren() {
      return children;
   }

   /**
    * @param children the children to set
    */
   public void setChildren(List<VINode> children) {
      this.children = children;
   }

   /**
    * @return the properties
    */
   public Map<String, String> getProperties() {
      return properties;
   }

   /**
    * @param properties the properties to set
    */
   public void setProperties(Map<String, String> properties) {
      this.properties = properties;
   }

   /**
    * @return the morValue
    */
   public String getMorValue() {
      return morValue;
   }

   /**
    * @param morValue the morValue to set
    */
   public void setMorValue(String morValue) {
      this.morValue = morValue;
   }

   /**
    * If not explicitly set, & children is not empty, hasChild will return true
    *
    * At times, we need to indicate this node has children, but is not loaded,
    * and hence the hasChild field.
    *
    * @return the hasChild
    */
   public boolean isHasChild() {
       return hasChild || !CollectionUtils.isEmpty(children);
   }

   /**
    * @param hasChild the hasChildren to set
    */
   public void setHasChild(boolean hasChild) {
       this.hasChild = hasChild;
   }

   /**
    * @return the path
    */
   public String getPath() {
       return path;
   }

   /**
    * @param path the path to set
    */
   public void setPath(String path) {
       this.path = path;
   }

   /**
    * Helper method to add properties: key-value pairs to properties.
    * It overwrites the value of any exisiting key.
    *
    * @param key
    * @param value
    */
   public void addProperty(String key, String value) {
      if (this.properties == null) {
         // Initializing with minimal data of 2 with 100% load factor.
         properties = new HashMap<String, String>(2,1);
      }
      properties.put(key, value);
   }

   /**
    * Performs a shallow copy of the current VINode.
    * This does not set/clone the children nodes.
    */
   @Override
   public VINode clone() {
      VINode node = new VINode(this.name, this.morValue, this.nodeType, this.root);
      node.setPath(node.getPath());
      if(!CollectionUtils.isEmpty(this.properties)) {
         Map<String, String> newProps =
            new HashMap<String, String>(this.properties);
         node.setProperties(newProps);
      }
      return node;
   }

   /**
    * Creates a string representation of a tree structure for this node.
    * NOTE: Use wisely, not performant
    *
    * @return
    */
   public String constructTree(int level){

      // Level is used for tabbing
      int newLevel = this.root? 0: level;
      StringBuilder sb = new StringBuilder();
      sb.append("\n");
      for(int i=0; i < newLevel; i++) {
         sb.append("   ");
      }
      sb.append(this.root? "(" : "|--> (")
         .append(nodeType).append(") ")
         .append(name).append("(name) : (mor)")
         .append(morValue);

      if(!CollectionUtils.isEmpty(children)) {
         for(VINode node : children) {
            String str = node.constructTree(newLevel+1);
            if (StringUtils.isNotEmpty(str)) {
               sb.append(str);
            }
         }
      }
      return sb.toString();
   }

}
