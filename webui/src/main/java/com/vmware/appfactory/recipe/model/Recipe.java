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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.common.ApplicationKey;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.datasource.DataSourceObject;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.appfactory.datasource.model.DataSource.Type;
import com.vmware.appfactory.recipe.RecipeMatch;
import com.vmware.thinapp.common.converter.dto.ConversionPhase;

/**
 * The class that represents a collection of recipe steps for customizing
 * the conversion of a ThinApp.
 */
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"_datasource__id","_name"}))
@JsonIgnoreProperties(ignoreUnknown=true)
public class Recipe
   extends AbstractRecord
   implements Comparable<Recipe>, DataSourceObject
{
   @NotNull
   private String _name = "";

   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_contentType", column=@Column(name="_desc_content_type")),
      @AttributeOverride(name="_content", column=@Column(name="_desc_content"))
   } )
   private AfText _description = new AfText();

   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_recipe__id")
   private final List<RecipeAppKey> _appKeys = new ArrayList<RecipeAppKey>();

   @ManyToOne
   private DataSource _dataSource;

   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_recipe__id")
   private final List<RecipeVariable> _variables = new ArrayList<RecipeVariable>();

   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_recipe__id")
   private final List<RecipeFile> _files = new ArrayList<RecipeFile>();

   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true)
   @MapKeyEnumerated(value=EnumType.STRING)
   @Cascade(CascadeType.ALL)
   private final Map<ConversionPhase,RecipeStep> _steps = new TreeMap<ConversionPhase,RecipeStep>();


   /**
    * Set the name of this recipe.
    * All recipes must have a unique name.
    * @param name the name to set
    */
   public void setName(@Nonnull String name)
   {
      _name = name;
   }


   /**
    * Get the (unique) name of this recipe.
    * @return the name
    */
   @Nonnull
   public String getName()
   {
      return _name;
   }


   /**
    * Set the data source that this recipe came from.
    * @param dataSource
    */
   public void setDataSource(DataSource dataSource)
   {
      _dataSource = dataSource;
   }


   /**
    * Get the data source that this recipe came from.
    * @return The data source that this recipe belongs to, or null if none.
    */
   @JsonIgnore
   public DataSource getDataSource()
   {
      return _dataSource;
   }


   /**
    * Get the name of the data source that this recipe came from.
    * @return The name of the data source that this recipe belongs to.
    */
   public String getDataSourceName()
   {
      return (_dataSource == null) ? "" : _dataSource.getName();
   }


   /**
    * Return a value to indicate which data source Type this recipe
    * originated from.
    *
    * @return - the type of the data source this recipe belongs to.
    */
   public Type getDataSourceType()
   {
      return (_dataSource == null) ? null : _dataSource.getType();
   }


   /**
    * Is this recipe read-only?
    * Read only recipes cannot be edited. Currently, recipes from feeds and
    * fileshares cannot be edited.
    */
   public boolean isReadOnly()
   {
      return (_dataSource != null);
   }


   /**
    * Set the complete set of recipe steps in this recipe.
    * All existing steps are replaced and/or removed.
    * @param steps
    */
   public void setSteps(Map<ConversionPhase, RecipeStep> steps)
   {
      _steps.clear();

      if (steps != null) {
         for (ConversionPhase ph : steps.keySet()) {
            _steps.put(ph, steps.get(ph));
         }
      }
   }


   /**
    * Get the complete set of recipe steps in this recipe.
    * @return the steps
    */
   public Map<ConversionPhase, RecipeStep> getSteps()
   {
      return _steps;
   }


   /**
    * Set a step in this recipe.
    * @param phase
    * @param step
    */
   public void setStep(ConversionPhase phase, RecipeStep step)
   {
      _steps.put(phase, step);
   }


   /**
    * Get a step from this recipe.
    * @param phase
    * @return
    */
   public RecipeStep getStep(ConversionPhase phase)
   {
      return _steps.get(phase);
   }


   /**
    * Check whether or not there are any commands for the
    * specified step.
    * @param phase
    * @return
    */
   public boolean hasStepCommands(ConversionPhase phase)
   {
      RecipeStep step = _steps.get(phase);
      if (step != null) {
         return CollectionUtils.isNotEmpty(step.getCommands());
      }
      return false;
   }


   /**
    * Set the recipe description.
    * @param description
    */
   public void setDescription(AfText description)
   {
      _description = description;
   }


   /**
    * Get the recipe description.
    * This is optional, so might be null.
    * @return
    */
   public AfText getDescription()
   {
      return _description;
   }


   /**
    * Get a list of all files in this recipe.
    * @return
    */
   public List<RecipeFile> getFiles()
   {
      return _files;
   }


   /**
    * Replace all files in this recipe with new files.
    * @param newFiles
    */
   public void setFiles(List<RecipeFile> newFiles)
   {
      _files.clear();

      if (newFiles != null) {
         _files.addAll(newFiles);
      }
   }


   /**
    * Add an application key to this recipe.
    * @param newKey
    */
   public void addAppKey(RecipeAppKey newKey)
   {
      _appKeys.add(newKey);
   }


   /**
    * Get a list of all application keys in this recipe.
    * @return
    */
   public List<RecipeAppKey> getAppKeys()
   {
      return _appKeys;
   }


   /**
    * Replace all application keys in this recipe with new ones.
    * @param newKeys
    */
   public void setAppKeys(List<RecipeAppKey> newKeys)
   {
      _appKeys.clear();

      if (newKeys != null) {
         _appKeys.addAll(newKeys);
      }
   }


   /**
    * Match this recipe to the specified application. Returns value indicating
    * if the recipe is "suitable" for that application, or not.
    *
    * @param application Application to check
    * @return "none" if there is no match, "wildcard" if the recipe only kind of
    * matches, or "precise" if it's a perfect match.
    */
   public RecipeMatch matchToApplication(ApplicationKey application)
   {
      RecipeMatch match = RecipeMatch.none;

      for (RecipeAppKey key : getAppKeys()) {
         match = RecipeMatch.bestOf(match, key.matchToApplication(application));
         if (match == RecipeMatch.precise) {
            /* We can't do better than that. */
            break;
         }
      }

      return match;
   }


   /**
    * Add a file to this recipe.
    * @param newFile
    */
   public void addFile(RecipeFile newFile)
   {
      _files.add(newFile);
   }


   /**
    * Get all the variables in this recipe.
    * @return
    */
   public List<RecipeVariable> getVariables()
   {
      return _variables;
   }


   /**
    * Replace all variables with new ones.
    * @param newVariables
    */
   public void setVariables(List<RecipeVariable> newVariables)
   {
      _variables.clear();

      if (newVariables != null) {
         _variables.addAll(newVariables);
      }
   }


   /**
    * Add a variable to the recipe.
    * @param newVariable
    */
   public void addVariable(RecipeVariable newVariable)
   {
      _variables.add(newVariable);
   }


   /**
    * Compare two recipes.
    * All names are unique, so comparing names is sufficient.
    */
   @Override
   public int compareTo(Recipe other)
   {
      if (other == this) {
         return 0;
      }
      return _name.compareTo(other.getName());
   }


   /**
    * Compare two recipes.
    * All names are unique, so comparing names is sufficient.
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      Recipe other = (Recipe) obj;
      return _name.equals(other.getName());
   }


   /**
    * Get a unique hash code.
    * All names are unique, so hashing the name is sufficient.
    */
   @Override
   public int hashCode()
   {
      return _name.hashCode();
   }


   @Override
   public Recipe clone()
   {
      Recipe clone = new Recipe();
      clone.deepCopy(this);
      return clone;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      Recipe other = (Recipe) record;
      int numChanges = 0;

      if (!StringUtils.equals(_name, other._name)) {
         _name = other._name;
         numChanges++;
      }

      /* Change description */
      if (!_description.equals(other._description)) {
         _description = other._description.clone();
         numChanges++;
      }

      if (!listsAreEqual(_variables, other._variables)) {
         setVariables(null);
         for (RecipeVariable v : other.getVariables()) {
            addVariable(v.clone());
         }
         numChanges++;
      }

      if (!listsAreEqual(_files, other._files)) {
         setFiles(null);
         for (RecipeFile file : other.getFiles()) {
            addFile(file.clone());
         }
         numChanges++;
      }

      if (!listsAreEqual(_appKeys, other._appKeys)) {
         setAppKeys(null);
         for (RecipeAppKey key : other.getAppKeys()) {
            addAppKey(key.clone());
         }
         numChanges++;
      }

      ConversionPhase[] thisPhases = _steps.keySet().toArray(new ConversionPhase[_steps.keySet().size()]);
      ConversionPhase[] otherPhases = other._steps.keySet().toArray(new ConversionPhase[_steps.keySet().size()]);
      Arrays.sort(thisPhases);
      Arrays.sort(otherPhases);

      if (Arrays.equals(thisPhases, otherPhases)) {
         for (ConversionPhase phase : thisPhases) {
            RecipeStep thisStep = _steps.get(phase);
            RecipeStep otherStep = other._steps.get(phase);
            numChanges += thisStep.deepCopy(otherStep);
         }
      }
      else {
         setSteps(null);
         for (ConversionPhase ph : other.getSteps().keySet()) {
            setStep(ph, other.getSteps().get(ph).clone());
         }
         numChanges++;
      }

      return numChanges;
   }
}
