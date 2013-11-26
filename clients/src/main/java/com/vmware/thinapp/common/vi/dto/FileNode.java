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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;


/**
 * This is a POJO used to store the file and folder information. The structure is
 * generic enough so that the files or folders can be distinguished.
 *
 * @author Keerthi Singri
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(@JsonSubTypes.Type(value = FolderNode.class))
public class FileNode implements Comparable<FileNode>
{
   private String name;
   private String path;
   private Long size;

   public FileNode(){
      /* Empty for default constructor */
   }

   public FileNode(String name, String path) {
      this.name = name;
      this.path = path;
   }

   public FileNode(String name, String path, Long size) {
      this.name = name;
      this.path = path;
      this.size = size;
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
    * @return the size
    */
   public Long getSize() {
      return size;
   }

   /**
    * @param size the size to set
    */
   public void setSize(Long size) {
      this.size = size;
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
    * This is a generic function with level
    */
   public String constructTree(int level) {
      StringBuilder sb = new StringBuilder();
      sb.append("\n");
      for(int i=0; i < level; i++) {
         sb.append("   ");
      }
      sb.append("|--> ").append(this.toString());
      return sb.toString();
   }

   /**
    * Returns a hash code for (_path * 100) + hashcode for _name.
    */
   @Override
   public int hashCode()
   {
      return (this.getPath().hashCode() * 100)
         + this.getName().hashCode();
   }

   /**
    * Checks for equality of the object passed as input
    *
    * @param other
    * @return
    */
   @Override
   public boolean equals(Object other) {
      if (other == null) {
         return false;
      }
      if (this == other) {
         return true;
      }
      if (other instanceof FileNode) {
         FileNode o = (FileNode)other;
         return this.name.equals(o.getName())
            && this.path.equals(o.getPath());
      }
      return false;
   }

   /**
    * Compares 2 FileNode objects, and sorts them by name.
    * FolderNode has precedence over FileNode.
    *
    * @param other
    * @return
    */
   @Override
   public int compareTo(FileNode other) {
      if (this == other) {
         return 0;
      }
      if (other instanceof FolderNode) {
         return 5;
      }
      int pathDiff = this.getPath().compareTo(other.getPath());
      return (pathDiff == 0)?
            this.getName().compareTo(other.getName()) : pathDiff;
   }

   /**
    * Generates key=value,... pairs for all fields in a line.
    */
   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }
}
