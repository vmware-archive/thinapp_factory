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

package com.vmware.appfactory.application.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * Class to manage the history of conversions for a given app.
 */
@Entity
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class AppBuildRequest
   extends AbstractRecord
   implements Comparable<AppBuildRequest>
{
   /** The buildId that represents the successful capture. */
   private Long _buildId;

   @NotNull
   private String _osType;

   @NotNull
   private String _osVariant;

   @NotNull
   private String _runtime;

   @NotNull
   private Long _datastoreId;

   /** Package format defined for future. */
   private String _packageFormat;

   /** Optional recipe that can be associated with the capture */
   private Long _recipeId;

   /** Flag to denote manualMode or auto capture. */
   private boolean _isManualMode;

   /**
    * These stages provide a high level summary of the build request.
    * The detail build state will be available under the running tasks.
    */
   public enum RequestStage
   {
      created,
      running,
      cancelled,
      failed,
      successful
   }

   @NotNull
   @Enumerated(EnumType.STRING)
   private RequestStage _requestStage;

   /** Association of this object to the Application */
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "_application__id")
   private Application _application;


   /**
    * @return the _buildId
    */
   public Long getBuildId()
   {
      return _buildId;
   }


   /**
    * @param projectId the _buildId to set
    */
   public void setBuildId(Long projectId)
   {
      this._buildId = projectId;
   }


   /**
    * @return the _recipeId
    */
   public Long getRecipeId()
   {
      return _recipeId;
   }


   /**
    * @param recipeId the _recipeId to set
    */
   public void setRecipeId(Long recipeId)
   {
      this._recipeId = recipeId;
   }


   /**
    * @return the _osType
    */
   public String getOsType()
   {
      return _osType;
   }


   /**
    * @param osType the _osType to set
    */
   public void setOsType(String osType)
   {
      this._osType = osType;
   }


   /**
    * @return the _osVariant
    */
   public String getOsVariant()
   {
      return _osVariant;
   }


   /**
    * @param osVariant the _osVariant to set
    */
   public void setOsVariant(String osVariant)
   {
      this._osVariant = osVariant;
   }


   /**
    * @return the _runtime
    */
   public String getRuntime()
   {
      return _runtime;
   }


   /**
    * @param runtime the _runtime to set
    */
   public void setRuntime(String runtime)
   {
      this._runtime = runtime;
   }


   /**
    * @return the _datastoreId
    */
   public Long getDatastoreId()
   {
      return _datastoreId;
   }


   /**
    * @param datastoreId the _datastoreId to set
    */
   public void setDatastoreId(Long datastoreId)
   {
      this._datastoreId = datastoreId;
   }


   /**
    * @return the _packageFormat
    */
   public String getPackageFormat()
   {
      return _packageFormat;
   }


   /**
    * @param packageFormat the _packageFormat to set
    */
   public void setPackageFormat(String packageFormat)
   {
      this._packageFormat = packageFormat;
   }


   /**
    * @return the _isManualMode
    */
   public boolean isManualMode()
   {
      return _isManualMode;
   }


   /**
    * @param isManualMode the _isManualMode to set
    */
   public void setManualMode(boolean isManualMode)
   {
      _isManualMode = isManualMode;
   }


   /**
    * @return the _requestStage
    */
   public RequestStage getRequestStage()
   {
      return _requestStage;
   }


   /**
    * @param requestStage the _requestStage to set
    */
   public void setRequestStage(RequestStage requestStage)
   {
      _requestStage = requestStage;
   }


   /**
    * @return the application
    */
   @JsonIgnore
   public Application getApplication()
   {
      return _application;
   }


   /**
    * @param application the application to set
    */
   @JsonIgnore
   public void setApplication(Application application)
   {
      _application = application;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      AppBuildRequest other = (AppBuildRequest) record;
      int numChanges = 0;

      if (this._buildId != null && !this._buildId.equals(other._buildId)) {
         _buildId = other._buildId;
         numChanges++;
      }

      numChanges += this._application.deepCopy(other._application);
      return numChanges;
   }


   /**
    * The comparison order is most recent first.
    */
   @Override
   public int compareTo(AppBuildRequest o)
   {
      return new CompareToBuilder()
         .append(o.getCreated(), this.getCreated())
         .toComparison();
   }
}