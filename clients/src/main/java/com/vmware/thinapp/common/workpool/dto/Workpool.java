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

package com.vmware.thinapp.common.workpool.dto;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * This is a definition of a workpool. This is a common object used across the
 * frontend and backend for all workpool related interactions.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LinkedWorkpool.class),
        @JsonSubTypes.Type(value = FullWorkpool.class),
        @JsonSubTypes.Type(value = CustomWorkpool.class)})
public abstract class Workpool implements Comparable<Workpool> {
   /**
    * Unique ID of the workpool assigned by the service
    */
   private Long id;

   /**
    * Name of the workpool
    */
   private String name;

   /**
    * Maximum number of instances that can be created
    */
   private int maximum;

   public enum State {
      created,
      available,
      unavailable,
      deleted,
      deleting,
      resetting,
      waitingForInstancer
   }

   private State state;

   private String lastError = "";

   /**
    * Set of associated instances.
    */
   private Set<InstanceInfo> instances = new HashSet<InstanceInfo>();

   /**
    * Default constructor.
    */
   public Workpool() {
      /* Empty */
   }

   /**
    * Constructor to set the id, maxInstances and name for a workpool
    *
    * @param id
    * @param maxInstances
    * @param name
    */
   public Workpool(Long id, int maxInstances, String name) {
      this.id = id;
      this.maximum = maxInstances;
      this.name = name;
   }

   /**
    * @return the id
    */
   public Long getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(Long id) {
      this.id = id;
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
    * @return the maximum
    */
   public int getMaximum() {
       return maximum;
   }

   /**
    * @param maximum the maximum to set
    */
   public void setMaximum(int maximum) {
      this.maximum = maximum;
   }

   /**
    * @return the state
    */
   public State getState() {
      return state;
   }

   /**
    * @param state the state to set
    */
   public void setState(State state) {
      this.state = state;
   }

   /**
    * @return the lastError
    */
   public String getLastError() {
      return lastError;
   }

   /**
    * @param lastError the lastError to set
    */
   public void setLastError(String lastError) {
      this.lastError = lastError;
   }

   /**
    * Determine if workpool is in a state where it can be deleted.
    * @return
    */
   public boolean isDeletable() {
      if (getState() == null) {
         return false;
      }
      switch (getState()) {
         case created:
         case available:
         case unavailable:
         case waitingForInstancer:
            return true;
         case resetting:
         case deleted:
         case deleting:
      }
      return false;
   }

   @JsonIgnore
   public void setDeletable(@SuppressWarnings("unused") boolean dummy) {
      // Adding this so jackson deserialization ignores it.
   }

   /**
    * Determine if workpool is in a processing state. This also means that
    * after processing it will reach and end state.
    *
    * @return
    */
   @JsonIgnore
   public boolean isProcessing() {
      if (getState() == null) {
         return false;
      }
      switch (getState()) {
         case available:
         case deleted:
         case unavailable:
            return false;
         case created:
         case waitingForInstancer:
         case resetting:
         case deleting:
      }
      return true;
   }

   /**
    * Return the OsType object irrespective of the implementation for different workpool types.
    * @return
    */
   abstract public OsType getOsType();

   /**
    * This method does nothing. One of the implementation handles this differently.
    * @param obj
    */
   public void setOsType(OsType obj) {
      // Do nothing. No JsonIgnore either, coz its overwritten in CustomWorkpool.
   }

   public Set<InstanceInfo> getInstances() {
      return instances;
   }

   public void setInstances(Set<InstanceInfo> instances) {
      this.instances = instances;
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (obj instanceof Workpool) {
         Workpool other = (Workpool) obj;
         return StringUtils.equals(getName(), other.getName());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return getName() == null ? 0 : getName().hashCode();
   }

   /**
    * NOTE: Name & Id are always unique.
    *
    * @param other
    * @return
    */
   @Override
   public int compareTo(Workpool other) {
      if (other == this) {
         return 0;
      }
      if (other == null) {
         return 1;
      }
      return this.getName().compareTo(other.getName());
   }
}
