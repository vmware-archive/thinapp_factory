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

package com.vmware.appfactory.taskqueue.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.appfactory.taskqueue.tasks.TaskHelperFactory;
import com.vmware.thinapp.common.converter.dto.ThinAppRuntime;
import com.vmware.thinapp.common.exception.BaseRuntimeException;
import com.vmware.thinapp.common.workpool.dto.Workpool;

/**
 * This class contains the list of build options that are selected and
 * used for capturing a ThinApp. This is consumed by AbstractCaptureTask.
 *
 * @see com.vmware.appfactory.taskqueue.tasks.state.tasks.AbstractCaptureTask
 */
public class CaptureRequestImpl
      implements CaptureRequest {

   /** ID of the application to convert */
   private Long _applicationId;

   /** List of icons for the application to convert */
   private List<? extends AfIcon> _icons;

   /** Display name of the application to build */
   private String _displayName;

   /** Desired/suggested name of the build */
   private String _buildName;

   /** Id of workpool to use for this build */
   private Long _workpoolId;

   /** Id of the recipe to apply to this build */
   private Long _recipeId;

   /** Id of the datastore to use for this build */
   private Long _datastoreId;

   /** Id of the ThinApp runtime to build with */
   private Long _runtimeId;

   private boolean _addHorizonIntegration;


   /** Values for variables in the chosen recipe */
   private Map<String, String> _recipeVariableValues = Collections.emptyMap();

   private TaskHelperFactory _taskHelperFactory;

   private Long _buildRequestId;

   /**
    * Default constructor.
    */
   public CaptureRequestImpl()
   {
      // empty constructor
   }


   /**
    * Constructor setting applicationId, displayName, buildName
    *
    * @param applicationId
    * @param displayName
    * @param buildName
    *
    * @deprecated
    * use {@link CaptureRequestImpl#CaptureRequestImpl(Long, String, String, Long, Long, Long, TaskHelperFactory)}
    * instead, in order to supply a runtime ID.
    */
   @Deprecated
   public CaptureRequestImpl(
         Long applicationId,
         List<? extends AfIcon> icons,
         String displayName,
         String buildName)
   {
      this._applicationId = applicationId;
      this._icons = icons;
      this._displayName = displayName;
      this._buildName = buildName;
   }

   /**
    * Constructor setting some fields on this object.
    *
    * @param applicationId
    * @param displayName
    * @param buildName
    * @param workpoolId
    * @param datastoreId
    * @param runtimeId
    */
   public CaptureRequestImpl(
         @Nonnull Long applicationId,
         @Nullable List<? extends AfIcon> icons,
         @Nonnull String displayName,
         @Nonnull String buildName,
         @Nonnull Long workpoolId,
         @Nonnull Long datastoreId,
         @Nonnull Long runtimeId,
         boolean addHorizonIntegration,
         @Nonnull TaskHelperFactory taskHelperFactory)
   {
      this._applicationId = applicationId;
      this._icons = icons;
      this._displayName = displayName;
      this._buildName = buildName;
      this._workpoolId = workpoolId;
      this._datastoreId = datastoreId;
      this._runtimeId = runtimeId;
      this._addHorizonIntegration = addHorizonIntegration;
      this._taskHelperFactory = taskHelperFactory;
   }

   /**
    * @param applicationId the applicationId to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setApplicationId(Long applicationId)
   {
      _applicationId = applicationId;
   }


   /**
    * @param icons the icons to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setIcons(List<? extends AfIcon> icons)
   {
      _icons = icons;
   }


   /**
    * @param displayName the displayName to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setDisplayName(String displayName)
   {
      _displayName = displayName;
   }


   /**
    * @param buildName the buildName to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setBuildName(String buildName)
   {
      _buildName = buildName;
   }


   /**
    * @param workpoolId the workpoolId to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setWorkpoolId(Long workpoolId)
   {
      _workpoolId = workpoolId;
   }


   /**
    * @param recipeId the recipeId to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setRecipeId(Long recipeId)
   {
      _recipeId = recipeId;
   }


   /**
    * @param datastoreId the datastoreId to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setDatastoreId(Long datastoreId)
   {
      _datastoreId = datastoreId;
   }


   /**
    * @param runtimeId the runtimeId to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setRuntimeId(Long runtimeId)
   {
      _runtimeId = runtimeId;
   }


   /**
    * @param recipeVariableValues the recipeVariableValues to set
    */
   @SuppressWarnings({"UnusedDeclaration", "PublicMethodNotExposedInInterface"})
   public void setRecipeVariableValues(Map<String, String> recipeVariableValues)
   {
      if (null == recipeVariableValues) {
         _recipeVariableValues = Collections.emptyMap();
      } else {
         _recipeVariableValues = ImmutableMap.copyOf(recipeVariableValues);
      }
   }

   /**
    * @param buildRequestId the request ID given to us by the CWS,
    *                       if running
    */
   public void setBuildRequestId(Long buildRequestId) {
      this._buildRequestId = buildRequestId;
   }

   public Long getBuildRequestId() {
      return _buildRequestId;
   }

   @Nonnull
   @Override
   public Long getApplicationId()
   {
      return _applicationId;
   }

   @Override
   public List<? extends AfIcon> getIcons()
   {
      return _icons;
   }

   @Nonnull
   @Override
   public String getDisplayName()
   {
      return _displayName;
   }

   @Nonnull
   @Override
   public String getBuildName()
   {
      return _buildName;
   }

   @Override
   public Long getWorkpoolId()
   {
      return _workpoolId;
   }

   @Override
   @Nullable
   public Workpool getWorkpool() {
      if (null == _taskHelperFactory) {
         return null;
      }
      return _taskHelperFactory.getCachedWorkpool(_workpoolId);
   }

   @JsonIgnore
   @Deprecated
   @SuppressWarnings({"MethodMayBeStatic", "PublicMethodNotExposedInInterface", "UnusedDeclaration"})
   public void setWorkpool(@SuppressWarnings("unused") Workpool workpool) {
      // no-op
      throw new UnsupportedOperationException();
   }

   @Override
   public Long getRecipeId()
   {
      return _recipeId;
   }

   @Override
   @Nullable
   public Recipe getRecipe() {
      if (null == _taskHelperFactory) {
         return null;
      }
      return _taskHelperFactory.getDaoFactory().getRecipeDao().find(_recipeId);
   }

   @JsonIgnore
   @Deprecated
   @SuppressWarnings({"MethodMayBeStatic", "PublicMethodNotExposedInInterface", "UnusedDeclaration"})
   public void setRecipe(@SuppressWarnings("unused") Recipe recipe) {
      // no-op
      throw new UnsupportedOperationException();
   }

   @Override
   public Long getDatastoreId()
   {
      return _datastoreId;
   }

   @Override
   @Nullable
   public DsDatastore getDatastore() {
      if (null == _taskHelperFactory) {
         return null;
      }
      return _taskHelperFactory.getCachedDatastore(_datastoreId);
   }

   @JsonIgnore
   @Deprecated
   @SuppressWarnings({"MethodMayBeStatic", "PublicMethodNotExposedInInterface", "UnusedDeclaration"})
   public void setDatastore(@SuppressWarnings("unused") DsDatastore datastore) {
      // no-op
      throw new UnsupportedOperationException();
   }

   /**
    * @return the integrateHorizon
    */
   @Override
   public boolean isAddHorizonIntegration() {
      return _addHorizonIntegration;
   }


   public void setAddHorizonIntegration(boolean addHorizonIntegration) {
      this._addHorizonIntegration = addHorizonIntegration;
   }

   @Override
   public Long getRuntimeId()
   {
      return _runtimeId;
   }

   @JsonIgnore
   @Deprecated
   @SuppressWarnings({"MethodMayBeStatic", "PublicMethodNotExposedInInterface", "UnusedDeclaration"})
   public void setRuntime(@SuppressWarnings("unused") ThinAppRuntime runtime) {
      // no-op
      throw new UnsupportedOperationException();
   }

   @Override
   @Nullable
   public ThinAppRuntime getRuntime() {
      if (null == _taskHelperFactory || null == _runtimeId) {
         return null;
      }
      try {
         return _taskHelperFactory.getCachedRuntime(_runtimeId);
      } catch (UncheckedExecutionException e) {
         // this is the wrapper exception used by
         // the guava cache code
         return null;
      } catch (BaseRuntimeException e) {
         return null;
      }
   }

   @Override
   public Map<String, String> getRecipeVariableValues()
   {
      return _recipeVariableValues;
   }

   @SuppressWarnings("PublicMethodNotExposedInInterface")
   @JsonIgnore
   public TaskHelperFactory getTaskHelperFactory() {
      return _taskHelperFactory;
   }

   @JsonIgnore
   @SuppressWarnings({"PublicMethodNotExposedInInterface", "UnusedDeclaration"})
   public void setTaskHelperFactory(TaskHelperFactory taskHelperFactory) {
      _taskHelperFactory = taskHelperFactory;
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }


   /**
    * @see com.vmware.appfactory.taskqueue.dto.CaptureRequest#validateRequiredFields()
    */
   @Override
   public void validateRequiredFields() {
      if (null == getRuntimeId() || null == getDatastoreId() || null == getWorkpoolId()) {
         throw new IllegalStateException("Unexpected: runtimeId, datastoreId or workpoolId is null");
      }

   }
}
