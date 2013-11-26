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

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * A recipe may need the user to provide values for things that are unknown
 * to the recipe. These are recipe variables; to use the recipe the user must
 * provide values for these.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
public class RecipeVariable
   extends AbstractRecord
{
   @NotNull
   private String _name;

   private boolean _required;

   private String _pattern;


   /**
    * Create a default instance.
    */
   public RecipeVariable()
   {
      /* Empty */
   }


   /**
    * Create a fully initialized instance.
    * @param name
    * @param required
    * @param pattern
    */
   public RecipeVariable(String name, boolean required, String pattern)
   {
      _name = name;
      _required = required;
      _pattern = pattern;
   }


   /**
    * Set the variable name.
    * Each variable has a name, which is used in the recipe.
    * @param name the name to set
    */
   public void setName(String name)
   {
      _name = name;
   }

   /**
    * Get the variable name.
    * Each variable has a name, which is used in the recipe.
    * @return the name
    */
   public String getName()
   {
      return _name;
   }

   /**
    * Mark the variable as being required or not.
    * @param required the required to set
    */
   public void setRequired(boolean required)
   {
      _required = required;
   }

   /**
    * Get whether the variable is required or not.
    * @return the required
    */
   public boolean isRequired()
   {
      return _required;
   }

   /**
    * Set the validation regular expression for the variable.
    * @param pattern the pattern to set
    */
   public void setPattern(String pattern)
   {
      _pattern = pattern;
   }

   /**
    * Get the validation regular expression for the variable.
    * @return the pattern
    */
   public String getPattern()
   {
      return _pattern;
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

      RecipeVariable other = (RecipeVariable) obj;
      return new EqualsBuilder()
         .append(_name, other._name)
         .append(_pattern, other._pattern)
         .append(_required, other._required)
         .isEquals();
   }


   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(_name)
         .append(_pattern)
         .append(_required)
         .toHashCode();
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      RecipeVariable other = (RecipeVariable) record;
      int numChanges = 0;

      if (!StringUtils.equals(getName(), other.getName())) {
         setName(other.getName());
         numChanges++;
      }

      if (!StringUtils.equals(getPattern(), other.getPattern())) {
         setPattern(other.getPattern());
         numChanges++;
      }

      if (isRequired() != other.isRequired()) {
         setRequired(other.isRequired());
         numChanges++;
      }

      return numChanges;
   }


   @Override
   public RecipeVariable clone()
   {
      RecipeVariable clone = new RecipeVariable();
      clone.deepCopy(this);
      return clone;
   }
}
