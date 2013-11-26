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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.datasource.DataSourceObject;
import com.vmware.appfactory.datasource.model.DataSource;
import com.vmware.appfactory.datasource.model.DataSource.Type;
import com.vmware.thinapp.common.util.AfCalendar;
import com.vmware.thinapp.common.util.AfJson;


/**
 * Data model class for an AppFactory application.
 *
 * An application is the source application, which is defined in a data source.
 * An application is used as input to Converter, to create a ThinApp package
 * (Build).
 *
 * TODO: add unique constraint: feedId + name + version
 */
@Entity
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Application
   extends AbstractApp
   implements Comparable<Application>, DataSourceObject
{
   /**
    * Key meta-data name-value pair format
    */
   public static final String KEY_META_DATA_FORMAT = "[name:%s,version:%s,vendor:%s,revision:%s,locale:%s]";

   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_contentType", column=@Column(name="_eula_content_type")),
      @AttributeOverride(name="_content", column=@Column(name="_eula_content"))
   } )
   private AfText _eula = new AfText();

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_application__id")
   private final List<AppDownload> _downloads = new ArrayList<AppDownload>();

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_application__id")
   private final List<AppInstall> _installs = new ArrayList<AppInstall>();

   @OneToMany(
         fetch=FetchType.LAZY,
         orphanRemoval=true)
   @Cascade(CascadeType.DELETE)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_application__id")
   private final List<AppBuildRequest> _buildRequest = new ArrayList<AppBuildRequest>();

   @NotNull
   private long _lastRemoteUpdate = AfCalendar.NEVER;

   @NotNull
   private boolean _skipped = false;

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_application__id")
   private final List<AppIcon> _icons = new ArrayList<AppIcon>();

   @ManyToOne
   private DataSource _dataSource;

   /**
    * A boolean flag to set if user overrides any metadata that is
    * obtained by feed or file share scan.
    */
   private boolean _overrideMetadata = false;

   /**
    * The number of failed conversion attempts.
    */
   private long _failCount = 0;

   /**
    * Total number of build requests ever made.
    */
   private long _buildRequestsTotal = 0L;


   /**
    * Create a new Application.
    */
   public Application()
   {
      /* Nothing to do */
   }


   /**
    * Create a new Application with the params passed.
    *
    * @param name
    * @param version
    * @param locale
    * @param revision
    * @param vendor
    */
   public Application(
         String name,
         String version,
         String locale,
         String revision,
         String vendor) {
      super(name, version, locale, revision, vendor);
   }


   @Override
   @JsonDeserialize(contentAs= AppIcon.class)
   public void setIcons(@Nullable List<? extends AfIcon> icons)
   {
      _icons.clear();

      if (icons != null) {
         for (AfIcon icon : icons) {
            AppIcon appIcon = new AppIcon(icon);
            _icons.add(appIcon);
         }
      }
   }


   @Override
   @Nonnull
   public List<? extends AfIcon> getIcons()
   {
      return _icons;
   }


   @Override
   public void addIcon(@Nonnull AfIcon icon)
   {
      AppIcon appIcon = (AppIcon) icon;
      _icons.add(appIcon);
   }


   /**
    * Set the item to be downloaded.
    * Note: although the persistent model allows more than one download file
    * per application, we only support one. If an application needs other files
    * in order to install properly, it should come with a built-in recipe.
    *
    * @see #getDownload
    * @param download
    */
   public void setDownload(@Nullable AppDownload download)
   {
      _downloads.clear();

      if (download != null) {
         _downloads.add(download);
      }
   }


   /**
    * Get the item to be downloaded.
    * Note: although the persistent model allows more than one download file
    * per application, we only support one. If an application needs other files
    * in order to install properly, it should come with a built-in recipe.
    *
    * @see #setDownload(AppDownload)
    * @return The download item
    */
   @Nullable
   public AppDownload getDownload()
   {
      if (_downloads.isEmpty()) {
         return null;
      }
      return _downloads.get(0);
   }


   /**
    * Set application installer commands.
    * @see #setInstalls
    * @param installs
    */
   public void setInstalls(@Nullable List<AppInstall> installs)
   {
      _installs.clear();

      if (installs != null) {
         _installs.addAll(installs);
      }
   }


   /**
    * Add application installer commands.
    * @see #setInstalls
    * @param install
    */
   public void addInstall(@Nonnull AppInstall install)
   {
      _installs.add(install);
   }


   /**
    * Set application installer commands.
    * @see #setInstalls(List)
    * @param install
    */
   @JsonIgnore
   public void setInstalls(@Nullable AppInstall install)
   {
      _installs.clear();

      if (install != null) {
         _installs.add(install);
      }
   }


   /**
    * Get application installer commands.
    * @return List of all install commands.
    */
   @Nonnull
   @JsonProperty("install")
   public List<AppInstall> getInstalls()
   {
      return _installs;
   }


   /**
    * Set the time when the application was updated in the feed.
    * @param lastUpdate
    */
   @JsonDeserialize(using=AfJson.CalendarDeserializer.class)
   public void setLastRemoteUpdate(long lastUpdate)
   {
      _lastRemoteUpdate = lastUpdate;
   }


   /**
    * Get the time when the application was updated in the feed.
    * @return When this app was last updated in the feed.
    */
   @JsonSerialize(using=AfJson.CalendarSerializer.class)
   public long getLastRemoteUpdate()
   {
      return _lastRemoteUpdate;
   }


   /**
    * Set the EULA text.
    * @param eula
    */
   public void setEula(@Nonnull AfText eula)
   {
      _eula = Preconditions.checkNotNull(eula);
   }

   /**
    * Get the EULA text.
    * @return The EULA text, which might be null.
    */
   @Nonnull
   public AfText getEula()
   {
      return _eula;
   }


   /**
    * Check if application should be included when scanning a feed.
    * @return True if user doesn't want this app converted automatically.
    */
   public boolean isSkipped()
   {
      return _skipped;
   }


   /**
    * Set if application should be included when scanning a feed.
    * @param skipped
    */
   public void setSkipped(boolean skipped)
   {
      _skipped = skipped;
   }


   /**
    * Returns the number of failed attempts since conversion started.
    * @return failCount
    */
   public long getFailCount()
   {
      return _failCount;
   }


   /**
    * Set the number of failed attempts.
    * @param failCount
    */
   public void setFailCount(long failCount)
   {
      this._failCount = failCount;
   }


   /**
    * Set the data source that this application came from.
    * @param dataSource
    */
   public void setDataSource(@Nullable DataSource dataSource)
   {
      _dataSource = dataSource;
   }


   /**
    * This exists so that we can round-trip an Application through
    * JSON
    * @param dataSourceName   The name of the data source.  This
    *                         hopefully corresponds to a currently-existing
    *                         Datasource object.
    */
   @JsonIgnore
   @Deprecated
   public void setDataSourceName(String dataSourceName)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * Get the data source that this application came from.
    * @return The data source that this app belongs to, or null if none.
    */
   @JsonIgnore
   @Nullable
   public DataSource getDataSource()
   {
      return _dataSource;
   }


   /**
    * Get the name of the data source that this application came from.
    * @return The name of the data source that this app belongs to.
    */
   public String getDataSourceName()
   {
      return (_dataSource == null) ?
            DataSource.UPLOAD_SOURCE.getName() :
            _dataSource.getName();
   }


   /**
    * Return a value to indicate which data source Type this application
    * originated from.
    *
    * @return - the type of the data source this application belongs to.
    */
   @Nonnull
   public Type getDataSourceType()
   {
      return (_dataSource == null) ?
            DataSource.UPLOAD_SOURCE.getType() :
            _dataSource.getType();
   }

   /**
    * This exists so that we can round-trip an Application through
    * JSON
    * @param dataSourceType   The type of the data source.  This
    *                         hopefully corresponds to a currently-existing
    *                         Datasource object.
    */
   @JsonIgnore
   @Deprecated
   public void setDataSourceType(String dataSourceType)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * Set the total number of build requests ever made for this application.
    * @param buildRequestsTotal
    */
   public void setBuildRequestsTotal(long buildRequestsTotal)
   {
      _buildRequestsTotal = buildRequestsTotal;
   }


   /**
    * Increment the total number of build requests ever made for this
    * application by one, and return it.
    */
   public void incrementBuildRequestsTotal()
   {
      _buildRequestsTotal++;
   }


   /**
    * Get the total number of build requests ever made for this application.
    * @return
    */
   public long getBuildRequestsTotal()
   {
      return _buildRequestsTotal;
   }


   /**
    * @return the overrideMetadata
    */
   public boolean isOverrideMetadata()
   {
      return _overrideMetadata;
   }


   /**
    * @param overrideMetadata the overrideMetadata to set
    */
   public void setOverrideMetadata(boolean overrideMetadata)
   {
      _overrideMetadata = overrideMetadata;
   }


   /**
    * @return the _buildRequest
    */
   @JsonIgnore
   public List<AppBuildRequest> getBuildRequest()
   {
      return _buildRequest;
   }


   /**
    * Get a suggested build name for this application.
    *
    * @return
    */
   @JsonIgnore
   @Nonnull
   public String getSuggestedBuildName()
   {
      return this.getName() + "-#" + this._buildRequestsTotal;
   }


   /**
    * TODO: Store all meta-data into a map.
    * @param fieldName
    * @param value
    * @return
    */
   @Nonnull
   public Application setAppInfoField(String fieldName, String value)
   {
      /* Do nothing if either argument is empty */
      if (StringUtils.isBlank(fieldName) || StringUtils.isBlank(value)) {
         return this;
      }

      if (fieldName.equalsIgnoreCase("vendor")) {
         setVendor(value);
      } else if (fieldName.equalsIgnoreCase("name")) {
         setName(value);
      } else if (fieldName.equalsIgnoreCase("version")) {
         setVersion(value);
      } else if (fieldName.equalsIgnoreCase("locale")) {
         setLocale(value);
      } else if (fieldName.equalsIgnoreCase("revision")) {
         setInstallerRevision(value);
      } else if (fieldName.equalsIgnoreCase("arch")) {
         setArchitecture(Architecture.valueOf(value));
      }

      return this;
   }

   /**
    * Create application meta-data map.
    *
    * @param metanames - an array of metanames.
    * @param valuesInOrder - one or more var-arg values to be assigned to the metanames.
    * @return a meta-data map.
    */
   @Nullable
   public static Map<String, String> createMetadataMap(
         String[] metanames,
         String... valuesInOrder)
   {
      if (metanames == null || metanames.length < 1 ||
            valuesInOrder == null || valuesInOrder.length < 1 ||
            valuesInOrder.length != metanames.length) {
         return null;
      }

      final Map<String, String> map = new HashMap<String, String>();
      for (int i = 0; i < valuesInOrder.length; i++) {
         String name = metanames[i];
         String value = valuesInOrder[i];
         // TODO: validate name?
         if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)) {
            map.put(name, value);
         }
      }

      return map;
   }

   /**
    * Get first install instance from the list.
    * @return an install instance if the list is not empty.
    *    Otherwise, it will return null.
    */
   @JsonIgnore
   @Nullable
   public AppInstall getInstall()
   {
      if (CollectionUtils.isNotEmpty(_installs)) {
         return _installs.get(0);
      }
      return null;
   }

   @Override
   public int compareTo(Application other)
   {
      return super.compareTo(other);
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      /* Copy base class first */
      Application other = (Application) record;
      int numChanges = deepCopyApp(other);

      /* Downloads */
      if (!listsAreEqual(_downloads, other._downloads)) {
         setDownload(other.getDownload());
         numChanges++;
      }

      /* Installs */
      if (!listsAreEqual(_installs, other._installs)) {
         setInstalls((AppInstall) null);
         for (AppInstall i : other.getInstalls()) {
            addInstall(i.clone());
         }
         numChanges++;
      }

      /*
       * Then all the other stuff.
       */

      if (!_eula.equals(other._eula)) {
         setEula(other._eula);
         numChanges++;
      }

      if (_lastRemoteUpdate != other._lastRemoteUpdate) {
         _lastRemoteUpdate = other._lastRemoteUpdate;
         numChanges++;
      }

      // note: do NOT copy the _skipped field, as the whole
      // point of the skipped field is to persistently ignore
      // apps.

      if (_overrideMetadata != other._overrideMetadata) {
         _overrideMetadata = other._overrideMetadata;
         numChanges++;
      }

      if (!listsAreEqual(_icons, other._icons)) {
         setIcons(null);
         for (AfIcon i : other.getIcons()) {
            addIcon(i.clone());
         }
         numChanges++;
      }

      /*
       * These we don't need to copy. I think. The problem is that we use the
       * same class for apps we know about and apps we find in feeds, etc.
       *
       * _dataSource
       * _failCount
       * _buildRequestsTotal
       */

      return numChanges;
   }

   @Override
   public boolean equals(Object o) {
      return EqualsBuilder.reflectionEquals(this,
                                            o,
                                            ImmutableList.of("_recipes","_dataSource"));
   }

   @Override
   public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
   }
}
