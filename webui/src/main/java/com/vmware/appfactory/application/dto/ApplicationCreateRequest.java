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

package com.vmware.appfactory.application.dto;

import java.io.Serializable;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.exceptions.InvalidDataException;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This object is the data transfer object used to pass on user request data
 * to the controller. This class contains details of the object
 *
 * This file is used for creating some of the most commonly used code.
 *
 * @author Keerthi Singri
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ApplicationCreateRequest implements Serializable {
   private static final long serialVersionUID = 123456789L;

   /**
    * The application specific details such as:
    * application name, version, vendor, locale, revision, and
    * install command (file share apps only).
    */
   private String _appName;
   private String _appVersion;
   private String _appVendor;
   private String _appLocale;
   private String _appRevision;
   private String _appInstallCommandOption;
   /**
    * The file that will be uploaded.
    */
   private String _displayFile;

   /**
    * The datastore to be used
    */
   private Long _dsId;

   /**
    * Unique value representing an upload. Used for tracking progress.
    */
   private String _uploadId;


   /**
    * @return the _appName
    */
   public String getAppName()
   {
      return _appName;
   }


   /**
    * @param appName the appName to set
    */
   public void setAppName(String appName)
   {
      this._appName = appName;
   }


   /**
    * @return the _appVersion
    */
   public String getAppVersion()
   {
      return _appVersion;
   }


   /**
    * @param appVersion the appVersion to set
    */
   public void setAppVersion(String appVersion)
   {
      this._appVersion = appVersion;
   }


   /**
    * @return the _appVendor
    */
   public String getAppVendor()
   {
      return _appVendor;
   }


   /**
    * @param appVendor the appVendor to set
    */
   public void setAppVendor(String appVendor)
   {
      this._appVendor = appVendor;
   }


   /**
    * @return the _appLocale
    */
   public String getAppLocale()
   {
      return _appLocale;
   }


   /**
    * @param appLocale the appLocale to set
    */
   public void setAppLocale(String appLocale)
   {
      this._appLocale = appLocale;
   }


   /**
    * @return the _appRevision
    */
   public String getAppRevision()
   {
      return _appRevision;
   }


   /**
    * @param appRevision the appRevision to set
    */
   public void setAppRevision(String appRevision)
   {
      this._appRevision = appRevision;
   }


   /**
    * @return the _dsId
    */
   public Long getDsId()
   {
      return _dsId;
   }


   /**
    * @param dsId the datastore id to set
    */
   public void setDsId(Long dsId)
   {
      this._dsId = dsId;
   }


   /**
    * @return the _uploadId
    */
   public String getUploadId()
   {
      return _uploadId;
   }


   /**
    * @return the _displayFile
    */
   public String getDisplayFile()
   {
      return _displayFile;
   }


   /**
    * @param displayFile the displayFile to set
    */
   public void setDisplayFile(String displayFile)
   {
      this._displayFile = displayFile;
   }


   /**
    * @param uploadId the uploadId to set
    */
   public void setUploadId(String uploadId)
   {
      this._uploadId = uploadId;
   }


   /**
    * @return the appInstallCommandOption
    */
   public String getAppInstallCommandOption()
   {
      return _appInstallCommandOption;
   }


   /**
    * @param appInstallCommandOption the appInstallCommandOption to set
    */
   public void setAppInstallCommandOption(String appInstallCommandOption)
   {
      _appInstallCommandOption = appInstallCommandOption;
   }


   /**
    * Validation routine for missing or invalid fields.
    *
    * @throws InvalidDataException
    */
   public void validate()
      throws InvalidDataException
   {
      if (AfUtil.anyEmpty(_appName, _appVersion, _appRevision) || _dsId == null) {
         throw new InvalidDataException("Required field(s) missing.");
      }
   }

   /**
    * Validation routine for missing name and version fields.
    *
    * @throws Exception
    */
   public void validateNameAndVersion()
   throws Exception
   {
      if (AfUtil.anyEmpty(_appName, _appVersion)) {
         throw new Exception("Both 'Name' and 'Version' fields are required.");
      }
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format(Application.KEY_META_DATA_FORMAT,
            getAppName(),
            getAppVersion(),
            getAppVendor(),
            getAppRevision(),
            getAppLocale());
   }

}
