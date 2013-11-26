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

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * List of commands to run in a given conversion job command phase.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class CommandList
{
   private List<Command> commands;

   public CommandList()
   {
      /* Do nothing */
   }

   public CommandList(List<Command> commands)
   {
      this.commands = commands;
   }

   public void setCommands(List<Command> commands)
   {
      this.commands = commands;
   }

   public List<Command> getCommands()
   {
      return commands;
   }

   /**
    * Swap all variables in the commands with values.
    *
    * @param variables
    */
   public void swapVariables(Map<String,String> variables)
   {
      for (Command cmd : commands) {
         cmd.swapVariables(variables);
      }
   }

   /**
    * Swap all variables in the commands with values.
    *
    * @param name
    * @param value
    */
   public void swapVariable(String name, String value)
   {
      for (Command cmd : commands) {
         cmd.swapVariable(name, value);
      }
   }
}
