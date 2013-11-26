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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.StringUtils;

import com.vmware.appfactory.application.model.AppInstall;
import com.vmware.appfactory.application.model.Application;
import com.vmware.thinapp.common.util.AfCalendar;

/**
 * A DTO to hold application info delta from a file share add request.
 *
 * @author saung
 * @since M7 7/22/2011
 */
public class ApplicationInfoDelta
{
   private String _key;
   private String _name;
   private String _version;
   private String _vendor;
   private String _revision;
   private String _lang;
   private String _installCommand;

   /**
    * Default constructor
    */
   public ApplicationInfoDelta()
   {
      // do nothing
   }

   /**
    * @return the key
    */
   public String getKey() {
      return _key;
   }
   /**
    * @return the name
    */
   public String getName() {
      return _name;
   }
   /**
    * @return the version
    */
   public String getVersion() {
      return _version;
   }
   /**
    * @return the vendor
    */
   public String getVendor() {
      return _vendor;
   }
   /**
    * @return the revision
    */
   public String getRevision() {
      return _revision;
   }
   /**
    * @return the lang
    */
   public String getLang() {
      return _lang;
   }
   /**
    * @param key the key to set
    */
   public void setKey(String key) {
      _key = key;
   }
   /**
    * @param name the name to set
    */
   public void setName(String name) {
      _name = name;
   }
   /**
    * @param version the version to set
    */
   public void setVersion(String version) {
      _version = version;
   }
   /**
    * @param vendor the vendor to set
    */
   public void setVendor(String vendor) {
      _vendor = vendor;
   }
   /**
    * @param revision the revision to set
    */
   public void setRevision(String revision) {
      _revision = revision;
   }
   /**
    * @param lang the lang to set
    */
   public void setLang(String lang) {
      _lang = lang;
   }

   /**
    * @return the installCommand
    */
   public String getInstallCommand() {
      return _installCommand;
   }

   /**
    * @param installCommand the installCommand to set
    */
   public void setInstallCommand(String installCommand) {
      _installCommand = installCommand;
   }

   /**
    * Copy non-empty instance variables to a
    * given application instance.
    * @param app - an application instance.
    */
   public void copyToApplication(Application app) {
      if (app == null) {
         return;
      }
      if (StringUtils.hasLength(_name)) {
         app.setName(_name);
      }
      if (StringUtils.hasLength(_version)) {
         app.setVersion(_version);
      }
      if (StringUtils.hasLength(_vendor)) {
         app.setVendor(_vendor);
      }
      if (StringUtils.hasLength(_revision)) {
         app.setInstallerRevision(_revision);
      }
      if (StringUtils.hasLength(_lang)) {
         app.setLocale(_lang);
      }
      if (StringUtils.hasLength(_installCommand)) {
         app.setInstalls(new AppInstall(_installCommand));
      }
      // note: if we don't set lastModified, then the value
      //       will be 0 for all Applications in the table.
      //       If the user later adds a second feed without making
      //       any other changes, the second feed will also have
      //       timestamps of 0.  The timestamp here didn't change.
      //       Since the frontend uses the most recent modified timestamp
      //       to tell when it has more AJAX data to load, this means
      //       the user won't see it.
      //
      app.setModified(AfCalendar.Now());
   }

   /**
    * Copy fields of given application to this instance
    * variables.
    * @param app - an application instance.
    */
   public void copyFromApplication(Application app) {
      if (app == null) {
         return;
      }
      if (StringUtils.hasLength(app.getName())) {
         _name = app.getName();
      }
      if (StringUtils.hasLength(app.getVersion())) {
         _version = app.getVersion();
      }
      if (StringUtils.hasLength(app.getVendor())) {
         _vendor = app.getVendor();
      }
      if (StringUtils.hasLength(app.getInstallerRevision())) {
         _revision = app.getInstallerRevision();
      }
      if (StringUtils.hasLength(app.getLocale())) {
         _lang = app.getLocale();
      }
   }

    /**
    * Generate a hash code using just _key field.
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return new HashCodeBuilder(17, 31).
         append(_key).
         toHashCode();
   }


   /**
    * Equals is based on _key value.
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (this == obj) {
         return true;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final ApplicationInfoDelta other = (ApplicationInfoDelta) obj;
      return new EqualsBuilder().
         append(_key, other._key).
         isEquals();
   }

}
