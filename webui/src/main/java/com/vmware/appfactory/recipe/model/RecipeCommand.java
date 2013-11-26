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

package com.vmware.appfactory.recipe.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * During conversion, custom commands can be executed as part of a recipe
 * at various phases of conversion. Those commands are represented by this
 * class.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
public class RecipeCommand
   extends AbstractRecord
{
   /** Length of the 'command' database column */
   public static final int COMMAND_LEN = 4096;

   @NotNull
   private String _label;

   @NotNull
   @Column(length=COMMAND_LEN)
   private String _command;


   /**
    * Create a new empty instance.
    */
   public RecipeCommand()
   {
      /* Empty */
   }

   /**
    * Create a new initialized instance.
    *
    * @param label
    * @param command
    */
   public RecipeCommand(String label, String command)
   {
      _label = label;
      _command = truncate(command, COMMAND_LEN);
   }


   /**
    * Set the command to execute.
    * @param command the command to set
    */
   public void setCommand(String command)
   {
      _command = truncate(command, COMMAND_LEN);
   }

   /**
    * Get the command to execute.
    * @return the command
    */
   public String getCommand()
   {
      return _command;
   }

   /**
    * Set the descriptive label for this command.
    * @param label the label to set
    */
   public void setLabel(String label)
   {
      _label = label;
   }

   /**
    * Get the descriptive label for this command.
    * @return the label
    */
   public String getLabel()
   {
      return _label;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      RecipeCommand other = (RecipeCommand) record;
      int numChanges = 0;

      if (!StringUtils.equals(getCommand(), other.getCommand())) {
         setCommand(other.getCommand());
         numChanges++;
      }

      if (!StringUtils.equals(getLabel(), other.getLabel())) {
         setLabel(other.getLabel());
         numChanges++;
      }

      return numChanges;
   }


   @Override
   public RecipeCommand clone()
   {
      RecipeCommand clone = new RecipeCommand();
      clone.deepCopy(this);
      return clone;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || obj.getClass() != this.getClass()) {
         return false;
      }

      if (obj == this) {
         return true;
      }

      RecipeCommand other = (RecipeCommand) obj;
      return new EqualsBuilder()
         .append(_label, other._label)
         .append(_command, other._command)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(_label)
         .append(_command)
         .toHashCode();
   }
}
