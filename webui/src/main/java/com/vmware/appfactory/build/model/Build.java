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

package com.vmware.appfactory.build.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.util.CollectionUtils;

import com.vmware.appfactory.application.model.Application;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.base.AbstractApp;
import com.vmware.appfactory.common.base.AbstractRecord;
import com.vmware.appfactory.datastore.DatastoreClientService;
import com.vmware.appfactory.datastore.DsDatastore;
import com.vmware.appfactory.datastore.DsDatastoreCifs;
import com.vmware.appfactory.datastore.exception.DsException;
import com.vmware.thinapp.common.converter.dto.Project;
import com.vmware.thinapp.common.converter.dto.ProjectFile;
import com.vmware.thinapp.common.util.AfCalendar;


/**
 * Data model class for an AppFactory "build".
 *
 * A build is the result from converting an application. Any given application
 * can be converted multiple times, each one creates a new AfBuild instance.
 * A build has a soft reference to the source application.
 *
 * By default, a new build is STAGED, meaning it cannot be downloaded from the
 * store. Only PUBLISHED builds are available in the store.
 *
 * Each build contains one or more ThinApp executables files.
 *
 * Note that builds are not linked directly to the source application, since the
 * source application might be deleted. The build should still be available
 * even if the source application is not.
 */
@Entity
public class Build
   extends AbstractApp
   implements Comparable<Build>
{
   /**
    * Every build has a single status value.
    */
   public enum Status {
      STAGED,     // Build is ready but not available in the store.
      PUBLISHED,  // Build is ready and available in the store.
      REBUILDING  // Build is rebuilding.
   }

   /**
    * Every build is created or imported and that source is stored here.
    */
   public enum Source {
      AUTO_CAPTURE,     // Denotes project created by auto-capture.
      MANUAL_CAPTURE,   // Denotes project created by manual mode.
      IMPORT            // Denotes project was imported.
   }

   /**
    * Default build name for all manual-mode built applications.
    */
   public static final String DEFAULT_MM_BUILD_NAME = "Manual mode build";

   @NotNull
   private String _buildName;

   private Long _converterProjectId;

   private String _osType;

   private String _osVariant;

   @NotNull
   private String _runtime;

   /** The new runtime value to be applied upon rebuild */
   private String _newRuntime;

   /** Flag indicating that horizon settings are appled to the build */
   @NotNull
   private boolean _hzSupported;

   @NotNull
   @Enumerated(EnumType.STRING)
   private Source _source;

   @NotNull
   @Enumerated(EnumType.STRING)
   private Status _status;

   @NotNull
   private long _published = AfCalendar.NEVER;

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_build__id")
   private final List<BuildFile> _buildFiles = new ArrayList<BuildFile>();

   @OneToMany(
         fetch=FetchType.EAGER,
         orphanRemoval=true)
   @Cascade(CascadeType.ALL)
   @Fetch(FetchMode.SUBSELECT)
   @JoinColumn(name="_build__id")
   private final List<BuildIcon> _icons = new ArrayList<BuildIcon>();

   private Long _datastoreId;

   @NotNull
   private long _settingsEdited = AfCalendar.NEVER;

   @NotNull
   private long _built = AfCalendar.NEVER;

   private String _subdir;

   private String _horizonApplicationId;

   /**
    * Create a new build.
    */
   public Build()
   {
      /* Nothing to do */
   }


   /**
    * Create a build from a completed CWS project.
    *
    * @param app an application instance.
    * @param project a project instance.
    * @param buildName a build name.
    * @param runtime a current runtime.
    * @param newRuntime a new runtime.
    * @param source a project source.
    * @return A new build instance
    */
   public static Build newFromProject(Application app, Project project, String buildName,
         String runtime, String newRuntime, boolean hzSupported, Source source) {
      Build build = new Build();

      build.deepCopyApp(app);
      build.setStatus(Build.Status.STAGED);
      build.setConverterProjectId(project.getId());
      build.setDatastoreId(project.getDatastoreId());
      build.setBuildName(buildName);
      build.setBuilt(AfCalendar.Now());
      build.setSubdir(project.getSubdir());
      build.setRuntime(runtime);
      build.setNewRuntime(newRuntime);
      build.setHzSupported(hzSupported);
      build.setSource(source);

      build.extractAndReplaceBuildFiles(project);
      return build;
   }

   /**
    * If param project has files, convert them to BuildFiles and replace the old ones with it.
    *
    * @param project
    */
   public void extractAndReplaceBuildFiles(Project project) {
      if (!CollectionUtils.isEmpty(project.getFiles())) {
         List<BuildFile> buildFileList = new ArrayList<BuildFile>(project.getFiles().size());
         for (ProjectFile projectFile : project.getFiles()) {
            BuildFile buildFile = new BuildFile();

            buildFile.setUrl(projectFile.getUrl());
            buildFile.setSize(projectFile.getSize());
            buildFile.setExeName(projectFile.getFilename());
            buildFileList.add(buildFile);
         }
         // This method clears the old build file list and add a new list.
         this.setBuildFiles(buildFileList);
      }
   }


   @Override
   public void setIcons(List<? extends AfIcon> icons)
   {
      _icons.clear();

      if (icons != null) {
         for (AfIcon icon : icons) {
            BuildIcon buildIcon = new BuildIcon(icon);
            _icons.add(buildIcon);
         }
      }
   }


   @Override
   public List<? extends AfIcon> getIcons()
   {
      return _icons;
   }


   @Override
   public void addIcon(AfIcon icon)
   {
      BuildIcon buildIcon = new BuildIcon();
      buildIcon.deepCopy(icon);
      _icons.add(buildIcon);
   }


   /**
    * TODO: Consider serializing a hash map that the bootstrapper likes,
    * not the class itself.
    *
    * @return
    */
   @JsonProperty("package_id")
   public Long getBuildId()
   {
      return getId();
   }


   /**
    * @return the osType
    */
   public String getOsType()
   {
      return _osType;
   }


   /**
    * @param osType the osType to set
    */
   public void setOsType(String osType)
   {
      _osType = osType;
   }


   /**
    * @return the osVariant
    */
   public String getOsVariant()
   {
      return _osVariant;
   }


   /**
    * @param osVariant the osVariant to set
    */
   public void setOsVariant(String osVariant)
   {
      _osVariant = osVariant;
   }


   /**
    * @return the runtime
    */
   public String getRuntime()
   {
      return _runtime;
   }


   /**
    * @param runtime the runtime to set
    */
   public void setRuntime(String runtime)
   {
      _runtime = runtime;
   }


   /**
    * @return the newRuntime
    */
   public String getNewRuntime()
   {
      return _newRuntime;
   }


   /**
    * @param newRuntime the newRuntime to set
    */
   public void setNewRuntime(String newRuntime)
   {
      _newRuntime = newRuntime;
   }


   /**
    * @return the hzSupported
    */
   public boolean isHzSupported()
   {
      return _hzSupported;
   }


   /**
    * @param hzSupported the hzSupported to set
    */
   public void setHzSupported(boolean hzSupported)
   {
      _hzSupported = hzSupported;
   }


   /**
    * @return the source
    */
   public Source getSource()
   {
      return _source;
   }


   /**
    * @param source the source to set
    */
   public void setSource(Source source)
   {
      _source = source;
   }


   /**
    * Set build status. Only PUBLISHED builds appear in the store.
    * @param status
    *
    * @see #setPublished(long)
    */
   public void setStatus(Status status)
   {
      _status = status;
   }


   /**
    * Get build status. Only PUBLISHED builds appear in the store.
    * @return Current status.
    */
   public Status getStatus()
   {
      return _status;
   }


   /**
    * Change this build's status to PUBLISHED and also set the publish time to
    * now.
    * @param published
    */
   public void setPublished(long published)
   {
      _published = published;
   }


   /**
    * Get the time when this build was last published. Will be null if not
    * published.
    * @return When this build was published, 0 if never.
    */
   @JsonIgnore
   public long getPublished()
   {
      return _published;
   }


   /**
    * Get all the files (ThinApp executables) inside this build.
    * @return All the files in this build.
    */
   @JsonProperty("thinapps")
   public List<BuildFile> getBuildFiles()
   {
      return _buildFiles;
   }


   /**
    * Set all the files (ThinApp executables) for this build.
    * @param files
    */
   public void setBuildFiles(List<BuildFile> files)
   {
      _buildFiles.clear();

      if (files != null) {
         _buildFiles.addAll(files);
      }

      return;
   }


   /**
    * Set the user-defined arbitrary name of this build.
    * @param buildName
    */
   public void setBuildName(String buildName)
   {
      _buildName = buildName;
   }


   /**
    * Get the user-defined arbitrary name of this build.
    * @return The user-defined name for this build.
    */
   public String getBuildName()
   {
      return _buildName;
   }


   /**
    * Get the number of ThinApp executables in this build.
    * @return Number of files in the build.
    */
   @JsonIgnore
   public int getNumBuildFiles()
   {
      return _buildFiles.size();
   }


   /**
    * Set the CWS project ID of this build.
    * @param converterId
    */
   public void setConverterProjectId(Long converterId)
   {
      _converterProjectId = converterId;
   }


   /**
    * Get the CWS project ID of this build.
    * @return The CWS project ID of this build.
    */
   public Long getConverterProjectId()
   {
      return _converterProjectId;
   }


   /**
    * Set the id of the datastore where this build resides.
    * @param datastoreId
    */
   public void setDatastoreId(Long datastoreId)
   {
      _datastoreId = datastoreId;
   }


   /**
    * Get the id of the datastore where this build resides.
    * @return The id of the datastore where this build resides.
    */
   public Long getDatastoreId()
   {
      return _datastoreId;
   }


   /**
    * See if this build is stored on the given datastore.
    * This just checks the "datastoreId" property, it does look at the files
    * on the datastore.
    * @param dsId
    * @return
    */
   public boolean isStoredOn(Long dsId)
   {
      if (dsId == null) {
         throw new IllegalArgumentException("dsId is null!");
      }

      return _datastoreId.equals(dsId);
   }


   /**
    * Get a "file" URL that points to this build's location on its datastore.
    * If this build has missing or invalid datastore information, this
    * method returns null.
    *
    * This is used so a client browser can display a link for editing the
    * project content directly. Therefore, it must be a "file://" URL.
    *
    * @param dsClient
    * @param embedAuth If true, user name and password (if any) are embedded
    *                  into the URL.
    * @return
    * @throws DsException
    */
   public String getLocationUrl(DatastoreClientService dsClient, boolean embedAuth)
      throws DsException
   {
      if (getDatastoreId() != null) {
         DsDatastore ds = dsClient.findDatastore(getDatastoreId(), true);
         if (ds != null && ds instanceof DsDatastoreCifs) {
            DsDatastoreCifs cifsDs = (DsDatastoreCifs) ds;
            String base = cifsDs.getBaseUrl("file", embedAuth);
            if (base != null) {
               return base + "/" + _subdir;
            }
         }
      }

      return null;
   }


   /**
    * The time when the settings for this build were last edited in some way.
    * @param settingsEdited
    */
   public void setSettingsEdited(long settingsEdited)
   {
      _settingsEdited = settingsEdited;
   }


   /**
    * The time when the settings for this build were last edited in some way.
    * @return
    */
   public long getSettingsEdited()
   {
      return _settingsEdited;
   }


   /**
    * The most recent build completion time.
    * Its tracked separately from _created in the superclass since the record
    * creation time can differ from when the build completed. Rebuild
    * completion updates this.
    * @param built
    */
   public void setBuilt(long built)
   {
      _built = built;
   }


   /**
    * The most recent build completion time.
    * Its tracked separately from _created in the superclass since the record
    * creation time can differ from when the build completed. Rebuild
    * completion updates this.
    *
    * @return
    */
   public long getBuilt()
   {
      return _built;
   }


   @Override
   public int compareTo(Build other)
   {
      /* Compare on application attributes first */
      int cmp = super.compareTo(other);

      /* Then try build name... */
      if (cmp == 0) {
         cmp = _buildName.compareTo(other._buildName);
      }

      /* Then try build time... */
      if (cmp == 0) {
         cmp = (int) (_built - other._built);
      }

      return cmp;
   }


   /**
    * Set the location where this build lives on the server, relative to the
    * datastore it resides on.
    * @param subdir
    */
   public void setSubdir(String subdir)
   {
      _subdir = subdir;
   }


   /**
    * Get the location where this build lives on the server, relative to the
    * datastore it resides on.
    * @return The project subdirectory on the datastore
    */
   public String getSubdir()
   {
      return _subdir;
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      /* Copy base class first */
      Build build = (Build) record;
      int numChanges = deepCopyApp(build);

      /* Then the extra stuff */
      // TODO: deep copy application fields:
      // _buildName
      // _converterProjectId
      // _status
      // _published
      // _buildFiles
      // _icons
      // _datastoreId
      // _settingsEdited
      // _built
      // _subdir

      return numChanges;
   }


   /**
    * Convert into a string for debugging, logging, etc.
    */
   @Override
   public String toString()
   {
      return "AfBuild[" +
            super.toString() +
            ",status=" + _status +
            ",files=" + getNumBuildFiles() +
            "]";
   }

   /**
    * Get the application id of the build when it was published to Horizon.
    * @return an id if published, otherwise null
    */
   public String getHorizonApplicationId() {
      return _horizonApplicationId;
   }

   /**
    * Set the application id of a build that was published to Horizon.
    * @param horizonApplicationId
    */
   public void setHorizonApplicationId(String horizonApplicationId) {
      this._horizonApplicationId = horizonApplicationId;
   }
}
