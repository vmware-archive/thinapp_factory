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

import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * This is a POJO used to store the file and folder information. The structure is
 * generic enough so that the files or folders can be distinguished.
 *
 * @author Keerthi Singri
 */
public class FolderNode extends FileNode implements Comparable<FileNode> {
   private boolean hasChild;
   private List<FileNode> children;

   public FolderNode(){
      super();
   }

   public FolderNode(String name, String path, Long size) {
      super(name, path, size);
   }

   public FolderNode(String name, String path, Long size, boolean hasChild) {
      super(name, path, size);
      this.hasChild = hasChild;
   }

   /**
    * @return the children
    */
   public List<FileNode> getChildren() {
      return children;
   }

   /**
    * @param children the children to set
    */
   public void setChildren(List<FileNode> children) {
      this.children = children;
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
    * Compares 2 FolderNode objects, and checks precedence by path, name.
    *
    * @param other
    * @return
    */
   @Override
   public int compareTo(FileNode other) {
      if (this == other) {
         return 0;
      }
      if (!(other instanceof FolderNode)) {
         return -1;
      }
      int pathDiff = this.getPath().compareTo(other.getPath());
      return (pathDiff == 0)?
            this.getName().compareTo(other.getName()) : pathDiff;
   }

   /**
    * Creates a string representation of a tree structure for this node.
    * NOTE: Use wisely, not performant
    *
    * @return
    */
   @Override
   public String constructTree(int level){
      StringBuilder sb = new StringBuilder(super.constructTree(level));
      if(!CollectionUtils.isEmpty(children)) {
         for(FileNode node : children) {
            sb.append(node.constructTree(level+1));
         }
      }
      return sb.toString();
   }

   /**
    * Generates a string representation of this node and its children.
    */
   @Override
   public String toString() {
      return constructTree(0);
   }


}
