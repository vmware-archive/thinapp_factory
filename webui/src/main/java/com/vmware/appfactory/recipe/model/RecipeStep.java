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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * A recipe is a sequence of recipe steps. Each step is associated with a
 * given phase of the conversion process, and contains a list of commands
 * to be executed.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
public class RecipeStep
   extends AbstractRecord
{
   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_recipestep__id")
   private final List<RecipeCommand> _commands = new ArrayList<RecipeCommand>();


   /**
    * Create a new instance.
    */
   public RecipeStep()
   {
      /* Empty */
   }

   /**
    * Create a new initialized instance.
    * @param commands
    */
   public RecipeStep(List<RecipeCommand> commands)
   {
      _commands.addAll(commands);
   }

   /**
    * Create a new initialized instance.
    * @param commands
    */
   public RecipeStep(RecipeCommand... commands)
   {
      for (RecipeCommand cmd : commands) {
         _commands.add(cmd);
      }
   }

   /**
    * Add a new command to this step.
    * @param cmd
    */
   public void addCommand(RecipeCommand cmd)
   {
      _commands.add(cmd);
   }


   /**
    * Get all the commands for this step.
    * @return
    */
   public List<RecipeCommand> getCommands()
   {
      return _commands;
   }

   /**
    * Set all the commands in this step, replacing all existing commands.
    * @param commands
    */
   public void setCommands(List<RecipeCommand> commands)
   {
      _commands.clear();

      if (commands != null) {
         _commands.addAll(commands);
      }
   }


   @Override
   public RecipeStep clone()
   {
      RecipeStep clone = new RecipeStep();
      clone.deepCopy(this);
      return clone;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || obj.getClass() != getClass()) {
         return false;
      }

      RecipeStep other = (RecipeStep) obj;
      return listsAreEqual(_commands, other._commands);
   }


   @Override
   public int hashCode()
   {
      return _commands.hashCode();
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      RecipeStep other = (RecipeStep) record;
      int numChanges = 0;

      if (!listsAreEqual(_commands, other._commands)) {
         _commands.clear();
         for (RecipeCommand cmd : other.getCommands()) {
            addCommand(cmd.clone());
         }
         numChanges++;
      }

      return numChanges;
   }
}
