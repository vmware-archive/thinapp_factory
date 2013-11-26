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

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


/**
 * A conversion job command string and the command's label.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class Command
{
   private String label;

   private String command;

   public Command()
   {
      /* Do nothing */
   }

   /**
    * Constructor that initializes this object with label, command
    * @param label
    * @param command
    */
   public Command(String label, String command)
   {
      this.label = label;
      this.command = command;
   }

   public void setLabel(String label)
   {
      this.label = label;
   }

   public String getLabel()
   {
      return label;
   }

   public void setCommand(String command)
   {
      this.command = command;
   }

   public String getCommand()
   {
      return command;
   }

   /**
    * Swap all variables in the command with values. Variables names (keys from
    * the specified map) are search for, with a preceding "$", and replaced with
    * the corresponding map value.
    *
    * @param variables
    */
   public void swapVariables(Map<String,String> variables)
   {
      for (String varName : variables.keySet()) {
         swapVariable(varName, variables.get(varName));
      }
   }

   /**
    * Swap a variable in the command with a values. Variables are located by
    * searching for "$" plus the variable name.
    *
    * @param name
    * @param value
    */
   public void swapVariable(String name, String value)
   {
      command = command.replaceAll("\\$" + name, value);
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this,o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }

   @Override
   public String toString() {
      return ToStringBuilder.reflectionToString(this);
   }
}
