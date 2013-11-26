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

package com.vmware.thinapp.common.converter.dto;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * This class gives the result for a completed conversion job.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class ConversionResult {

   public static enum Disposition {
      succeeded,
      failed
   }

   private Disposition disposition;
   private Status.State lastRunningState;
   private Command lastCommand;
   private String lastError;

   /**
    * Default constructor
    */
   public ConversionResult() {
      /* Empty */
   }

   /**
    * Constructor to set the disposition, last running state, and last command
    *
    * @param disposition
    */
   public ConversionResult(
         Disposition disposition,
         Status.State lastRunningState,
         Command lastCommand,
         String lastError) {
      this.disposition = disposition;
      this.lastRunningState = lastRunningState;
      this.lastCommand = lastCommand;
      this.lastError = lastError;
   }

   // used by ui test
   public ConversionResult(
         Disposition disposition,
         Status.State lastRunningState,
         Command lastCommand) {
      this(disposition, lastRunningState, lastCommand, "unknown");
   }

   public void setDisposition(Disposition disposition) {
      this.disposition = disposition;
   }

   public Disposition getDisposition() {
      return disposition;
   }

   public void setLastRunningState(Status.State lastRunningState) {
      this.lastRunningState = lastRunningState;
   }

   public Status.State getLastRunningState() {
      return lastRunningState;
   }

   public void setLastCommand(Command lastCommand) {
      this.lastCommand = lastCommand;
   }

   public Command getLastCommand() {
      return lastCommand;
   }

   public void setLastError(String lastError) {
      this.lastError = lastError;
   }

   public String getLastError() {
      return lastError;
   }

   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
