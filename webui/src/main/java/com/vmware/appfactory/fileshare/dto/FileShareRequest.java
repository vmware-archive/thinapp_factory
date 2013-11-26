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

package com.vmware.appfactory.fileshare.dto;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * A DTO to encapsulate all form fields of adding/editing file share request.
 *
 * @author saung
 * @since M7 7/22/2011
 */
public class FileShareRequest
{
   private Long _fileShareId;
   private String _name;
   private String _description;
   private String _serverPath;
   private boolean _okToConvert;
   private boolean _authRequired;
   private String _authUsername;
   private String _authPassword;
   private List<ApplicationInfoDelta> _appDeltas;
   private List<String> _appsToSkip;

   /**
    *
    * @throws InvalidDataException
    */
   public void validate()
      throws InvalidDataException
   {
      /* Name and URL are required */
      if (!StringUtils.hasLength(_name)) {
         throw new InvalidDataException("Required field missing.");
      }

      /* Authorization user and pass are required */
      // XXX empty password should be OK
      if (_authRequired) {
         if (AfUtil.anyEmpty(_authUsername)) {
            throw new InvalidDataException("Required field missing.");
         }
      }

      if(!StringUtils.hasLength(_serverPath)) {
         throw new InvalidDataException("Share Location field is required.");
      }
      /* Trim any leading \\ or // */
      _serverPath = StringUtils.trimLeadingCharacter(_serverPath, '\\');
      _serverPath = StringUtils.trimLeadingCharacter(_serverPath, '/');
      /* Replace "\" with "/" because Samba format uses / */
      _serverPath = _serverPath.replace("\\", "/");
   }

   /**
    * @return the fileShareId
    */
   public Long getFileShareId() {
      return _fileShareId;
   }

   /**
    * @param fileShareId the fileShareId to set
    */
   public void setFileShareId(Long fileShareId) {
      this._fileShareId = fileShareId;
   }

   /**
    * @return the name
    */
   public String getName() {
      return _name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return _description;
   }

   /**
    * @return the serverPath
    */
   public String getServerPath() {
      return _serverPath;
   }

   /**
    * @return the okToConvert
    */
   public boolean isOkToConvert() {
      return _okToConvert;
   }

   /**
    * @return the authRequired
    */
   public boolean isAuthRequired() {
      return _authRequired;
   }

   /**
    * @return the authUsername
    */
   public String getAuthUsername() {
      return _authUsername;
   }

   /**
    * @return the authPassword
    */
   public String getAuthPassword() {
      return _authPassword;
   }

   /**
    * @return the delta
    */
   public List<ApplicationInfoDelta> getAppDeltas() {
      return _appDeltas;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      _name = name;
   }

   /**
    * @param description the description to set
    */
   public void setDescription(String description) {
      _description = description;
   }

   /**
    * @param serverPath the serverPath to set
    */
   public void setServerPath(String serverPath) {
      _serverPath = serverPath;
   }

   /**
    * @param okToConvert the okToConvert to set
    */
   public void setOkToConvert(boolean okToConvert) {
      _okToConvert = okToConvert;
   }

   /**
    * @param authRequired the authRequired to set
    */
   public void setAuthRequired(boolean authRequired) {
      _authRequired = authRequired;
   }

   /**
    * @param authUsername the authUsername to set
    */
   public void setAuthUsername(String authUsername) {
      _authUsername = authUsername;
   }

   /**
    * @param authPassword the authPassword to set
    */
   public void setAuthPassword(String authPassword) {
      _authPassword = authPassword;
   }

   /**
    * @param deltas the delta to set
    */
   public void setAppDeltas(List<ApplicationInfoDelta> deltas) {
      _appDeltas = deltas;
   }

   /**
    * @return the skip
    */
   public List<String> getAppsToSkip() {
      return _appsToSkip;
   }

   /**
    * @param skip the skip to set
    */
   public void setAppsToSkip(List<String> skip) {
      _appsToSkip = skip;
   }


   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this, o);
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
