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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * This object represents a vm image thats either a clone or a new one.
 * vmSource can be either ExistingVM (representing linked clone) or VmPattern
 * (representing a full VM)
 *
 * @see VmSource
 * @see VmPattern
 * @see ExistingVm
 */
public class VmImage {
   protected Long id;
   protected String name;
   protected VmSource vmSource;

   /**
    * State transition diagram.
    *
    * created --> waitingInstall --> installing --------> installFailed
    *    |                                           |
    *    V                                           V
    * waitingImport --> creatingBaseSnapshot <-- installFinished
    *                            |
    *                            |-----> snapshotFailed
    *                            V
    *                       snapShotted
    *                            |
    *                            V
    *                     -----------------
    *                     |   available   |  --> reset?
    *                     -----------------
    *                            |
    *                            V
    *                     deleteRequested --> deleting --> deleted.
    * unknown?
    */
   public enum State {
      /** Initial state. */
      created,
      /** Image is ready to be used. */
      available,
      /** Request to delete the backing VM. */
      deleteRequested,
      /** Signal instance should be deleted. */
      deleted,
      /** Backing VM was expected to exist but does not.  Cannot continue. */
      vmDoesNotExist,
      /** Installation is in progress. */
      installing,
      /** Installation failed. */
      installFailed,
      /** Installation finished. */
      installFinished,
      /** VM is being prepared. */
      creatingBaseSnapshot,
      /** VM was successfully prepared. */
      snapshotted,
      /** VM preparation failed. */
      snapshotFailed,
      /** Existing VM was provided and needs to be imported. */
      waitingImport,
      /** Waiting to start installation for a new VM. */
      waitingInstall,
      /** Backing VM deletion is in progress. */
      deleting,
      /** Deletion failed. */
      deleteFailed,
      /** Unknown state. */
      unknown
   }

   protected State state;

   protected String lastError = "";

   /**
    * Default constructor
    */
   public VmImage() {
       /* Empty default constructor */
   }

   /**
    * Constructor setting name and vmSource fields.
    *
    * @param name
    * @param source
    */
   public VmImage(String name, VmSource source) {
       this.name = name;
       this.vmSource = source;
   }

   /**
    * Indicates if the image is still being processed and has not reached
    * an end state.
    * @return
    */
   @JsonIgnore
   public boolean isProcessing() {
      if (getState() == null) {
         return false;
      }
      switch (getState()) {
         case created:
         case deleteRequested:
         case installing:
         case installFinished:
         case creatingBaseSnapshot:
         case snapshotted:
         case waitingImport:
         case waitingInstall:
         case deleting:
            return true;
         case available:
         case deleted:
         case vmDoesNotExist:
         case installFailed:
         case snapshotFailed:
         case deleteFailed:
         case unknown:
      }
      return false;
   }

   /**
    * Indicates if the image state is in a fail state.
    * @return
    */
   public boolean isFailState() {
      if (getState() == null) {
         return false;
      }
      switch (getState()) {
         case vmDoesNotExist:
         case installFailed:
         case snapshotFailed:
         case deleteFailed:
         case unknown:
            return true;
         case created:
         case available:
         case deleteRequested:
         case installing:
         case installFinished:
         case creatingBaseSnapshot:
         case snapshotted:
         case waitingImport:
         case waitingInstall:
         case deleting:
         case deleted:
      }

      return false;
   }

   /**
    * Created dummy setters so Json deserialization does not fail.
    * Marking them protected to reduce visibility and avoid IDE warnings.
    * @param dummy
    */
   @JsonIgnore
   protected void setFailState(boolean dummy) {
      // Adding this so jackson deserialization ignores it.
   }

   /**
    * Determine if VmImage is in a state where it can be deleted.
    * @return
    */
   public boolean isDeletable() {
      if (getState() == null) {
         return false;
      }
      switch (getState()) {
         case unknown:
         case created:
         case available:
         case vmDoesNotExist:
         case installFailed:
         case installFinished:
         case snapshotted:
         case snapshotFailed:
         case waitingImport:
         case waitingInstall:
         case deleteFailed:
            return true;
         case deleteRequested:
         case deleted:
         case installing:
         case creatingBaseSnapshot:
         case deleting:
      }
      return false;
   }

   @JsonIgnore
   public void setDeletable(@SuppressWarnings("unused") boolean dummy) {
      // Adding this so jackson deserialization ignores it.
   }

   /**
    * This method gives a percentage representation of the image state.
    */
   public int getPercent() {
      int percent = -1;
      if (getState() == null) {
         return percent;
      }
      switch (getState()) {
         case created:
            percent = 0;
            break;

         case waitingImport:
         case waitingInstall:
         case deleteRequested:
            percent = 15;
            break;

         case installing:
         case deleting:
            percent = 50;
            break;

         case installFinished:
            percent = 70;
            break;

         case creatingBaseSnapshot:
            percent = 75;
            break;

         case snapshotted:
            percent = 90;
            break;

         case deleted:
         case available:
            percent = 100;
            break;

         // Failure cases, percent = 100
         case vmDoesNotExist:
         case installFailed:
         case snapshotFailed:
         case deleteFailed:
         case unknown:
            percent = 100;
            break;
      }
      return percent;
   }

   /**
    * Created dummy setters so Json deserialization does not fail.
    * Marking them protected to reduce visibility and avoid IDE warnings.
    * @param dummy
    */
   @JsonIgnore
   protected void setPercent(int dummy) {
      // Adding this so jackson deserialization ignores it.
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public VmSource getVmSource() {
      return vmSource;
   }

   public void setVmSource(VmSource vmSource) {
      this.vmSource = vmSource;
   }

   public void setState(State state) {
       this.state = state;
   }

   public State getState() {
       return this.state;
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

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
