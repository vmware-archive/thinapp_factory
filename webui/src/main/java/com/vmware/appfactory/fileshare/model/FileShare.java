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

package com.vmware.appfactory.fileshare.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.datasource.model.DataSource;

/**
 * Data model class for File Share application source which can also be added as
 * JSON feed to the app factory.
 *
 * @author saung
 * @since v1.0 4/27/2011
 */
@Entity
@DiscriminatorValue("fileshare")
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonIgnoreProperties()
public class FileShare extends DataSource {
   /**
    * file share status
    */
   public static enum Status
   {
      /** File share has been created but never scanned */
      NEW(false),
      /** Last File share scanning was successful. */
      SCANNED(false),
      /** Last File share scanning failed. */
      SCANNED_ERROR(true);

      private boolean _error;

      Status(boolean error) {
         _error = error;
      }

      public boolean isError() {
         return _error;
      }
   }

   /**
    * UNC format server path.
    */
   @NotNull
   @Column(length=LONG_LEN)
   private String _serverPath;

   @NotNull
   private String _description = "";

   @NotNull
   @Enumerated(EnumType.STRING)
   private Status _status = Status.NEW;

   @NotNull
   private String _datastoreName;

   @NotNull
   private Long _datastoreId;

   /**
    * Default constructor
    */
   public FileShare()
   {
      super(Type.fileshare);
   }


   /**
    * @return the server
    */
   public String getServerPath() {
      return _serverPath;
   }

   /**
    * @param serverPath the server path to set
    */
   public void setServerPath(String serverPath) {
      _serverPath = serverPath;
   }

   /**
    * Get share path from the UNC path.
    * hostname/share/path
    * @return the path (share/path) if the serverPath is not empty. Otherwise, return a empty string.
    */
   public String getPath() {
      if (StringUtils.hasLength(_serverPath)) {
         int firstBackslashPosition = _serverPath.indexOf('/');
         return _serverPath.substring(firstBackslashPosition + 1);
      }

      return "";
   }

   /**
    * Get the server/hostname from UNC format serverPath
    * @return a server/hostname if serverPath is not empty; return false otherwise.
    */
   public String getServer() {
      if (StringUtils.hasLength(_serverPath)) {
         int firstBackslashPosition = _serverPath.indexOf('/');
         return _serverPath.substring(0, firstBackslashPosition);
      }

      return "";
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return _description;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      _description = description;
   }

   /**
    * @return the status
    */
   public Status getStatus() {
      return _status;
   }

   /**
    * @param status the status to set
    */
   public void setStatus(Status status) {
      _status = status;
   }

   /**
    * @return the datastoreName
    */
   public String getDatastoreName() {
      return _datastoreName;
   }

   /**
    * @param datastoreName the datastoreName to set
    */
   public void setDatastoreName(String datastoreName) {
      _datastoreName = datastoreName;
   }

   /**
    *
    */
   @Override
   public String toString() {
      return "AfFileShare[name=" + super.getName() + "]";
   }


   /**
    * @return the datastoreId
    */
   public Long getDatastoreId()
   {
      return _datastoreId;
   }


   /**
    * @param datastoreId the datastoreId to set
    */
   public void setDatastoreId(Long datastoreId)
   {
      _datastoreId = datastoreId;
   }


}
