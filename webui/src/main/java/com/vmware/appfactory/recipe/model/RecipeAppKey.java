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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.vmware.appfactory.common.ApplicationKey;
import com.vmware.appfactory.common.MutableApplicationKey;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.recipe.RecipeMatch;

/**
 * Recipes have one or more "application keys" which identify which applications
 * they can be used with. These keys can act as wild-card matches, since any of
 * the application key fields are optional.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
public class RecipeAppKey
   extends AbstractRecord
   implements MutableApplicationKey
{
   private String _name;

   private String _version;

   private String _locale;

   private String _installerRevision;

   /**
    * Set the name of the application to match.
    * Pass null or empty to match any name.
    * @param name
    */
   @Override
   public void setName(String name)
   {
      _name = name;
   }

   /**
    * Get the name of the application to match.
    * Null or empty means match any name.
    * @return
    */
   @Override
   public String getName()
   {
      return _name;
   }

   /**
    * Set the version of the application to match.
    * Pass null or empty to match any version.
    * @param version
    */
   @Override
   public void setVersion(String version)
   {
      _version = version;
   }

   /**
    * Get the version of the application to match.
    * Null or empty means match any version.
    * @return
    */
   @Override
   public String getVersion()
   {
      return _version;
   }

   /**
    * Set the locale of the application to match.
    * Pass null or empty to match any locale.
    * @param locale
    */
   @Override
   public void setLocale(String locale)
   {
      _locale = locale;
   }

   /**
    * Get the locale of the application to match.
    * Null or empty means match any locale.
    * @return
    */
   @Override
   public String getLocale()
   {
      return _locale;
   }

   /**
    * Set the installer revision of the application to match.
    * Pass null or empty to match any installer revision.
    * @param installerRevision
    */
   @Override
   public void setInstallerRevision(String installerRevision)
   {
      _installerRevision = installerRevision;
   }

   /**
    * Get the installer revision of the application to match.
    * Null or empty means match any installer revision.
    * @return
    */
   @Override
   public String getInstallerRevision()
   {
      return _installerRevision;
   }

   @Override
   public int deepCopy(AbstractRecord record)
   {
      RecipeAppKey other = (RecipeAppKey) record;
      int numChanges = 0;

      if (!StringUtils.equals(getName(), other.getName())) {
         setName(other.getName());
         numChanges++;
      }

      if (!StringUtils.equals(getVersion(), other.getVersion())) {
         setVersion(other.getVersion());
         numChanges++;
      }

      if (!StringUtils.equals(getLocale(), other.getLocale())) {
         setLocale(other.getLocale());
         numChanges++;
      }

      if (!StringUtils.equals(getInstallerRevision(), other.getInstallerRevision())) {
         setInstallerRevision(other.getInstallerRevision());
         numChanges++;
      }

      return numChanges;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }

      if (obj == this) {
         return true;
      }

      if (obj.getClass() != this.getClass()) {
         return false;
      }

      RecipeAppKey other = (RecipeAppKey) obj;
      return new EqualsBuilder()
         .append(_name, other._name)
         .append(_version, other._version)
         .append(_locale, other._locale)
         .append(_installerRevision, other._installerRevision)
         .isEquals();
   }

   @Override
   public int hashCode()
   {
      return new HashCodeBuilder()
         .append(_name)
         .append(_version)
         .append(_locale)
         .append(_installerRevision)
         .toHashCode();
   }


   @Override
   public RecipeAppKey clone()
   {
      RecipeAppKey clone = new RecipeAppKey();
      clone.deepCopy(this);
      return clone;
   }


   /**
    * Compare this key to an application, and return what kind of a match
    * that is.
    *
    * @param application
    * @return
    */
   public RecipeMatch matchToApplication(ApplicationKey application)
   {
      RecipeMatch match = RecipeMatch.precise;

      if (StringUtils.isBlank(getName()) &&
          StringUtils.isBlank(getVersion()) &&
          StringUtils.isBlank(getLocale()) &&
          StringUtils.isBlank(getInstallerRevision())) {
         /* All keys are blank, so this is a 'wild' match that matches anything */
         return RecipeMatch.wild;
      }

      match = RecipeMatch.worstOf(match, RecipeMatch.match(getName(), application.getName()));
      match = RecipeMatch.worstOf(match, RecipeMatch.match(getVersion(), application.getVersion()));
      match = RecipeMatch.worstOf(match, RecipeMatch.match(getLocale(), application.getLocale()));
      match = RecipeMatch.worstOf(match, RecipeMatch.match(getInstallerRevision(), application.getInstallerRevision()));

      return match;
   }
}
