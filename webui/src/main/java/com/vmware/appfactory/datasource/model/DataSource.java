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

package com.vmware.appfactory.datasource.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.AfFailure;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.recipe.model.Recipe;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfJson;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This class holds all common fields for application and recipe sources,
 * such as Feeds and FileShares.
 *
 * @author saung
 * @since M7 7/26/2011
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name="datasource", uniqueConstraints=@UniqueConstraint(columnNames={"_name"}))
@DiscriminatorColumn(name="_type", discriminatorType=DiscriminatorType.STRING)
@JsonSerialize(include=Inclusion.NON_NULL)
@JsonIgnoreProperties()
public class DataSource extends AbstractRecord
   implements Comparable<DataSource>
{
   /**
    * Where did this data source originate from?
    */
   public static enum Type {
      /** Applications/recipes from a feed (URL) */
      feed,
      /** Applications/recipes from a file share */
      fileshare,
      /** Applications/recipes uploaded by user */
      upload
   }

   /**
    * Singleton instance used for applications that were
    * uploaded by a user
    */
   public static final DataSource UPLOAD_SOURCE;
   static {
      UPLOAD_SOURCE = new DataSource(Type.upload);
      UPLOAD_SOURCE.setName("Uploaded");
   }


   @NotNull
   @Enumerated(EnumType.STRING)
   private Type _type;

   @NotNull
   private String _name = "";

   @NotNull
   private long _lastScan = AfCalendar.NEVER;

   @NotNull
   private long _lastConversion = AfCalendar.NEVER;

   @NotNull
   private boolean _okToScan = true;

   @NotNull
   private boolean _okToConvert = true;

   @OneToMany(
      fetch=FetchType.EAGER,
      orphanRemoval=true,
      cascade=CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_datasource__id")
   private final List<Application> _apps = new ArrayList<Application>();

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true,
         cascade=CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_datasource__id")
   private final List<Recipe> _recipes = new ArrayList<Recipe>();

   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_summary", column=@Column(name="_failure_summary",length=AfFailure.SUMMARY_LEN)),
      @AttributeOverride(name="_details", column=@Column(name="_failure_details",length=AfFailure.DETAILS_LEN))
   } )
   private AfFailure _failure = null;

   private String _username;

   private String _password;

   /**
    * Constructor that accept data source type.
    *
    * @param type - a type.
    */
   public DataSource(Type type)
   {
      super();
      _type = type;
   }


   /**
    * Get the data source name.
    * This is defined by the user creating the data source.
    * @return
    */
   public String getName()
   {
      return _name;
   }


   /**
    * Set the data source name.
    * This is defined by the user creating the data source.
    * @param name
    */
   public void setName(String name)
   {
      if (name == null) {
         throw new IllegalArgumentException();
      }

      _name = truncateDefault(name);
   }

   /**
    * Set the list of applications in this data source.
    * @param apps
    */
   public void setApplications(List<Application> apps)
   {
      _apps.clear();

      if (apps != null) {
         _apps.addAll(apps);

         for (Application app : _apps) {
            app.setDataSource(this);
         }
      }
   }


   /**
    * Add an application to this data source.
    *
    * @param app
    */
   public void addApplication(Application app)
   {
      app.setDataSource(this);
      _apps.add(app);
   }


   /**
    * Get the list of applications in this data source.
    * @return
    */
   public List<Application> getApplications()
   {
      return _apps;
   }


   /**
    * Get the total number of applications in this data source.
    * @return
    */
   public int getNumApplications()
   {
      return _apps.size();
   }

   /**
    * This only exists so that we can round-trip an
    * object through JSON.  It will be set through the
    * "applications" list property
    *
    * @param ignored not used
    *
    * @deprecated #{see setApplications} instead
    */
   @JsonIgnore
   @Deprecated
   public void setNumApplications(int ignored) {
      throw new UnsupportedOperationException();
   }

   /**
    * Set the list of recipes in this data source.
    * @param recipes
    */
   public void setRecipes(List<Recipe> recipes)
   {
      _recipes.clear();

      if (recipes != null) {
         _recipes.addAll(recipes);

         for (Recipe recipe : _recipes) {
            recipe.setDataSource(this);
         }
      }
   }


   /**
    * Add an application to this data source.
    *
    * @param recipe
    */
   public void addRecipe(Recipe recipe)
   {
      recipe.setDataSource(this);
      _recipes.add(recipe);
   }


   /**
    * Get the list of recipes in this data source.
    * @return
    */
   public List<Recipe> getRecipes()
   {
      return _recipes;
   }


   /**
    * Get the total number of recipes in this data source.
    * @return
    */
   public int getNumRecipes()
   {
      return _recipes.size();
   }


   /**
    * This only exists so that we can round-trip an
    * object through JSON.  It will be set indirectly
    * through the _recipes property.
    *
    * @param recipes ignored
    *
    * @deprecated #{see setRecipes} instead
    */
   @JsonIgnore
   @Deprecated
   public void setNumRecipes(int recipes)
   {
      throw new UnsupportedOperationException();
   }


   /**
    * Search for one of the applications in the data source by looking for
    * its ID. If not found, returns null.
    * @param id
    * @return
    */
   public Application findApplication(Long id)
   {
      for (Application app : _apps) {
         if (app.getId().equals(id)) {
            return app;
         }
      }

      return null;
   }

   /**
    * @return the username
    */
   @Nullable
   public String getUsername() {
      return _username;
   }


   /**
    * @param username the username to set
    */
   public void setUsername(@Nullable String username) {
      _username = username;
   }


   /**
    * @return the password
    */
   @Nullable
   public String getPassword() {
      return _password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(@Nullable String password) {
      _password = password;
   }


   /**
    * Return true if this data source required HTTP authentication.
    * @return
    */
   public boolean isAuthRequired() {
      return !AfUtil.anyEmpty(_username);
   }

   /**
    * This exists so that we can round-trip an object
    * through JSON.
    *
    * This property will be set indirectly when we set the
    * username field.
    *
    * @param ignored   not used
    *
    * @deprecated use {@see setUsername} instead
    */
   @JsonIgnore
   @Deprecated
   public void setAuthRequired(boolean ignored) {
      throw new UnsupportedOperationException();
   }

   /**
    * Get the applications selected for conversion in this data source.
    * @return
    */
   @JsonIgnore
   public List<Application> getIncludedApplications()
   {
      List<Application> actual = new ArrayList<Application>();

      for (Application app : _apps) {
         if (!app.isSkipped()) {
            actual.add(app);
         }
      }

      return actual;
   }


   /**
    * Get the number of applications selected for conversion in this data
    * source.
    * @return
    */
   public int getNumIncludedApplications()
   {
      return getIncludedApplications().size();
   }


   /**
    * This method exists so that we can round-trip a DataSource
    * through JSON.
    *
    * The list of included applications will be set indirectly
    * from the applications list itself.
    *
    * @param ignored not used
    *
    * @deprecated use {@see setApplications} instead
    */
   @JsonIgnore
   @Deprecated
   public void setNumIncludedApplications(int ignored) {
      throw new UnsupportedOperationException();
   }


   /**
    * Set the time when this data source was last scanned.
    * @param lastScan
    */
   @JsonDeserialize(using=AfJson.CalendarDeserializer.class)
   public void setLastScan(Long lastScan)
   {
      if (null == lastScan) {
         _lastScan = AfCalendar.NEVER;
      } else {
         _lastScan = lastScan;
      }
   }


   /**
    * Get the time when this data source was last scanned.
    * @return
    */
   @JsonSerialize(using=AfJson.CalendarSerializer.class)
   public Long getLastScan()
   {
      return _lastScan;
   }


   /**
    * The difference from the above is that this does NOT use the CalendarSerializer,
    * and so returns the raw long milliseconds value when converted into JSON, rather
    * than a date string.
    *
    * @return the time when this data source was last scanned.
    */
   public long getLastScanMillis()
   {
      return _lastScan;
   }

   @JsonIgnore
   @Deprecated
   public void setLastScanMillis(@SuppressWarnings("unused") long val)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * Check if this data source was last scanned more than 'rescanPeriodMins'
    * minutes ago.
    * @param rescanPeriodMins
    * @return
    */
   public boolean lastScanIsOlderThan(long rescanPeriodMins)
   {
      long now = AfCalendar.Now();
      return ((now - _lastScan) > rescanPeriodMins * 60000);
   }


   /**
    * Set when this data source was last converted.
    *
    * @param lastConversion   timestamp, in UTC, from the
    *                         state of the epoch, in ms
    */
   public void setLastConversion(Long lastConversion)
   {
      if (null == lastConversion) {
         _lastConversion = AfCalendar.NEVER;
      } else {
         _lastConversion = lastConversion;
      }
   }


   /**
    * Get the timestamp when this data source was last converted.
    *
    * @return
    */
   public Long getLastConversion()
   {
      return _lastConversion;
   }


   /**
    * Mark this data source as being OK to scan.
    * @param okToScan
    */
   public void setOkToScan(boolean okToScan)
   {
      _okToScan = okToScan;
   }


   /**
    * Check if this data source is OK to scan.
    * @return
    */
   public boolean isOkToScan()
   {
      return _okToScan;
   }


   /**
    * Mark this data source as being OK to convert.
    * @param okToConvert
    */
   public void setOkToConvert(boolean okToConvert)
   {
      _okToConvert = okToConvert;
   }


   /**
    * Check if this data source is OK to convert.
    * @return
    */
   public boolean isOkToConvert()
   {
      return _okToConvert;
   }


   /**
    * Get the reason why this data source failed to scan or convert.
    * Will be null if there is no error.
    * @return
    */
   public AfFailure getFailure()
   {
      return _failure;
   }


   /**
    * Set the reason why this data source failed to scan or convert.
    * Set to null if there is no error.
    * @param failure
    */
   public void setFailure(AfFailure failure)
   {
      _failure = failure;
   }


   /**
    * @return the type
    */
   public Type getType() {
      return _type;
   }


   /**
    * @param type the type to set
    */
   public void setType(Type type) {
      _type = type;
   }


   @Override
   public int compareTo(DataSource other)
   {
      return _name.compareTo(other._name);
   }


   /**
    * Generate a hash code using just _name field.
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      return new HashCodeBuilder(17, 31).
         append(_name).
         toHashCode();
   }


   /**
    * Equals is based on its super equals and _name.
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }
      if (this == obj) {
         return true;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final DataSource other = (DataSource) obj;
      return new EqualsBuilder().
         append(_name, other._name).
         isEquals();
   }


   @Override
   public int deepCopy(AbstractRecord other)
   {
      // TODO: deep copy datasource
      return 0;
   }
}