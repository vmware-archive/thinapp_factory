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

package com.vmware.appfactory.common.base;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.base.Equivalence;
import com.vmware.appfactory.common.AfIcon;
import com.vmware.appfactory.common.AfText;
import com.vmware.appfactory.common.MutableApplicationKey;
import com.vmware.thinapp.common.util.AfUtil;

/**
 * This superclass is used by both AfApplication and AfPackage, since these
 * classes have much in common.
 *
 * This base class provides the functionality for the application name,
 * version, vendor, categories, and icons. Although packages are derived
 * from applications, they can exist after the application has been deleted,
 * so data must be duplicated to a certain extent. This is why a package
 * does not reference the application from which it was generated.
 */
@MappedSuperclass
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public abstract class AbstractApp
   extends AbstractRecord
   implements MutableApplicationKey
{
   /**
    * Where did this application originate from?
    */
   public static enum Architecture {
      /** a 32-bit app */
      x86,
      /** a 64-bit app */
      x64,
      /** works with any architecture */
      any
   }

   @NotNull
   private String _name = "";

   @NotNull
   private String _version = "";

   @NotNull
   private String _locale = "";

   @NotNull
   private String _installerRev = "";

   @NotNull
   private String _vendor = "";

   /** default choice is 'any' */
   @NotNull
   private Architecture _architecture = Architecture.any;

   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_contentType", column=@Column(name="_desc_content_type")),
      @AttributeOverride(name="_content", column=@Column(name="_desc_content"))
   } )
   private AfText _description = new AfText();

   @NotNull
   @Column(length=1024)
   private String _categoriesEncoded = "";


   /**
    * Create a new AbstractApp instance.
    */
   public AbstractApp()
   {
      /* Nothing to do */
   }

   /**
    * Create a new AbstractApp instance with params passed.
    *
    * @param name
    * @param version
    * @param locale
    * @param revision
    * @param vendor
    */
   public AbstractApp(
         String name,
         String version,
         String locale,
         String revision,
         String vendor) {

      // Do not set empty strings if passed, leave them null.
      if (StringUtils.isNotEmpty(name)) {
         this._name = name;
      }
      if (StringUtils.isNotEmpty(version)) {
         this._version = version;
      }
      if (StringUtils.isNotEmpty(locale)) {
         this._locale = locale;
      }
      if (StringUtils.isNotEmpty(revision)) {
         this._installerRev = revision;
      }
      if (StringUtils.isNotEmpty(vendor)) {
         this._vendor = vendor;
      }
   }

   /**
    * Get the icons for this application.
    *
    * Each subclass must implement this, since the persistence model must be
    * managed by the subclass.
    *
    * @return
    */
   public abstract List<? extends AfIcon> getIcons();

   /**
    * Add an icon to this application.
    *
    * Each subclass must implement this, since the persistence model must be
    * managed by the subclass.
    *
    * @param icon
    */
   public abstract void addIcon(AfIcon icon);


   /**
    * Set the icons for this application.
    *
    * Each subclass must implement this, since the persistence model must be
    * managed by the subclass.
    * @param icons List of icons for this app.
    */
   public abstract void setIcons(List<? extends AfIcon> icons);


   /**
    * Get the icon that best matches the given pixel size.
    *
    * This returns the icon whose size is nearest to the specified
    * size. It will always return one of the icons. If there are no icons,
    * the default AfIcon.DEFAULT_APPLICATION_ICON is returned.
    *
    * @param size
    * @return
    */
   @Nonnull
   public AfIcon getBestIconForSize(int size)
   {
      AfIcon best = null;
      int bestDiff = 0;

      if (CollectionUtils.isEmpty(getIcons())) {
         return AfIcon.DEFAULT_APPLICATION_ICON;
      }

      for (AfIcon icon : getIcons()) {
         if (best == null) {
            best = icon;
            bestDiff = Math.abs(best.getSize() - size);
         }
         else {
            int diff = Math.abs(icon.getSize() - size);
            if (diff < bestDiff) {
               best = icon;
               bestDiff = diff;
            }
         }
      }

      return best;
   }


   /**
    * Perform a deep copy of values from another AfAbstractApp instance
    * to this one.
    *
    * @param record Instance to copy values from.
    * @return Number of actual changes made.
    */
   public int deepCopyApp(AbstractRecord record)
   {
      AbstractApp other = (AbstractApp) record;
      int numChanges = 0;

      if (other == null) {
         throw new IllegalArgumentException("Can't copy AfAbstractApp from null!");
      }

      /* Change name */
      if (!StringUtils.equals(_name, other._name)) {
         _name = other._name;
         numChanges++;
      }

      /* Change version */
      if (!StringUtils.equals(_version, other._version)) {
         _version = other._version;
         numChanges++;
      }

      /* Change locale */
      if (!StringUtils.equals(_locale, other._locale)) {
         _locale = other._locale;
         numChanges++;
      }

      /* Change installer revision */
      if (!StringUtils.equals(_installerRev, other._installerRev)) {
         _installerRev = other._installerRev;
         numChanges++;
      }

      /* Change vendor */
      if (!StringUtils.equals(_vendor, other._vendor)) {
         _vendor = other._vendor;
         numChanges++;
      }

      /* Change categories */
      if (!StringUtils.equals(_categoriesEncoded, other._categoriesEncoded)) {
         _categoriesEncoded = other._categoriesEncoded;
         numChanges++;
      }

      /* Change description */
      if (!_description.equals(other._description)) {
         _description = other._description.clone();
         numChanges++;
      }

      /* Change icons */
      if (!listsAreEqual(getIcons(), other.getIcons())) {
         setIcons(null);
         for (AfIcon icon : other.getIcons()) {
            addIcon(icon.clone());
         }
         numChanges++;
      }

      return numChanges;
   }


   /**
    * Set the application's name.
    *
    * @param name New name for application.
    */
   @Override
   public void setName(String name)
   {
      _name = name;
   }


   /**
    * Get the application's name.
    *
    * @return Name of application.
    */
   @Override
   public String getName()
   {
      return _name;
   }


   /**
    * Set application version.
    *
    * @param version
    */
   @Override
   public void setVersion(String version)
   {
      _version = version;
   }


   /**
    * Get application version.
    *
    * @return
    */
   @Override
   public String getVersion()
   {
      return _version;
   }


   /**
    * Set the application's vendor name.
    *
    * @param vendor
    */
   public void setVendor(String vendor)
   {
      _vendor = vendor;
   }


   /**
    * Get the application's vendor name.
    * This is optional, so might be null.
    *
    * @return
    */
   public String getVendor()
   {
      return _vendor;
   }


   /**
    * Set the application description.
    *
    * @param description
    */
   public void setDescription(AfText description)
   {
      _description = description;
   }


   /**
    * Get the application description.
    * This is optional, so might be null.
    *
    * @return
    */
   public AfText getDescription()
   {
      return _description;
   }


   /**
    * Set the categories that this application belongs to.
    * This is a formatted string containing individual names separated
    * by ":".
    * @param categories Names separated by ":"
    */
   @JsonIgnore
   public void setCategoriesEncoded(String categories)
   {
      _categoriesEncoded = categories;
   }


   /**
    * Set the categories that this application belongs to.
    * The set of names is converted into a formatted string containing
    * individual names separated by ":".
    *
    * @param categories Collection of names.
    */
   public void setCategories(Set<String> categories)
   {
      if (categories != null) {
         StringBuffer sb = new StringBuffer();

         for (String cat : categories) {
            sb.append(cat).append(":");
         }
         _categoriesEncoded = sb.toString();
      }
   }


   /**
    * Get the categories that this application belongs to.
    * This is a formatted string containing individual names separated
    * by ":".
    * @return Names separated by ":"
    */
   @JsonIgnore
   public String getCategoriesEncoded()
   {
      return _categoriesEncoded;
   }


   /**
    * Get the categories that this application belongs to.
    * The set of names is converted from a formatted string containing
    * individual names separated by ":" into a set of names.
    * @return
    */
   public Set<String> getCategories()
   {
      Set<String> set = new HashSet<String>();

      for (String cat : _categoriesEncoded.split(":")) {
         if (StringUtils.isNotEmpty(cat)) {
            set.add(cat);
         }
      }

      return set;
   }


   /**
    * Get the categories in a string suitable for display.
    * @return
    */
   @JsonIgnore
   public String getCategoriesDisplayString()
   {
      StringBuffer sb = new StringBuffer();

      for (String cat : _categoriesEncoded.split(":")) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append(cat);
      }

      return sb.toString();
   }


   /**
    * Return true if this application belongs to the given category.
    * @param catName Category name.
    * @return True if this app belongs to this category.
    */
   public boolean belongsToCategory(String catName)
   {
      return (getCategories().contains(catName));
   }


   /**
    * Set the application locale.
    * If there is no locale, pass an empty string.
    *
    * @param locale
    */
   @Override
   public void setLocale(String locale)
   {
      _locale = locale;
   }


   /**
    * Get the application locale.
    *
    * This will be a non-null string. If empty, there is no locale
    * defined, which means this application applies to all.
    * @return
    */
   @Override
   public String getLocale()
   {
      return _locale;
   }


   /**
    * Get the revision of the application's installer.
    * Applies to applications that have more than one installer for the same
    * application. If unknown or not applicable, will be empty.
    *
    * @param revision
    */
   @Override
   public void setInstallerRevision(String revision)
   {
      _installerRev = revision;
   }


   /**
    * Set the revision of the application's installer.
    * Applies to applications that have more than one installer for the same
    * application.
    * @return
    */
   @Override
   public String getInstallerRevision()
   {
      return _installerRev;
   }

   /**
    * @return the architecture
    */
   public Architecture getArchitecture()
   {
      return _architecture;
   }


   /**
    * @param architecture the architecture to set
    */
   public void setArchitecture(Architecture architecture)
   {
      _architecture = architecture;
   }



   /**
    * Compares this instance to another.
    *
    * Since this is an abstract base class, Comparable is not declared as being
    * implemented since each subclass needs to compare with it's own class.
    *
    * @param other
    * @return Comparison
    */
   public int compareTo(AbstractApp other)
   {
      int cmp = _name.compareToIgnoreCase(other._name);

      if (cmp == 0) {
         cmp = AfUtil.alnumCompare(_version, other._version);
      }
      if (cmp == 0) {
         cmp = _locale.compareToIgnoreCase(other._locale);
      }
      if (cmp == 0) {
         cmp = AfUtil.alnumCompare(_installerRev, other._installerRev);
      }
      if (cmp == 0) {
         cmp = _architecture.compareTo(other._architecture);
      }

      return cmp;
   }


   /**
    * Get a handy display name for this application.
    * TODO: Config option to format this, such as "%n %v (%l) %r"
    *
    * @return Something like "AppName (1.0) [en] R4".
    */
   @JsonIgnore
   public String getDisplayName()
   {
      String str = _name + " (" + _version + ")";

      if (StringUtils.isNotBlank(_locale)) {
         str += " [" + _locale + "]";
      }

      if (StringUtils.isNotBlank(_installerRev)) {
         str += " " + _installerRev;
      }

      return str;
   }

   public static class AppIdentity extends AbstractApp {

      private List<? extends AfIcon> appIcons;
      private final Equivalence<AppIdentity> equilvalence;

      public AppIdentity(AbstractApp app) {

         setArchitecture(app.getArchitecture());
         setInstallerRevision(app.getInstallerRevision());
         setLocale(app.getLocale());
         setName(app.getName());
         setVersion(app.getVersion());
         setVendor(app.getVendor());

         appIcons = app.getIcons();
         equilvalence = new AppEquivalence<AppIdentity>();
      }

      @Override
      public List<? extends AfIcon> getIcons() {
         return appIcons;
      }

      @Override
      @Deprecated
      public void addIcon(AfIcon icon) {
         throw new UnsupportedOperationException();
      }

      @Override
      @Deprecated
      public void setIcons(List<? extends AfIcon> icons) {
         throw new UnsupportedOperationException();
      }

      @Override
      @Deprecated
      public int deepCopy(AbstractRecord other) {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (!(o instanceof AppIdentity)) {
            return false;
         }

         return equilvalence.equivalent(this,(AppIdentity)o);
      }

      @Override
      public int hashCode() {
         return equilvalence.hash(this);
      }
   }

   public static class AppEquivalence<T extends AbstractApp>
      extends Equivalence<T> {

      /**
       * See if this application is the same as another.
       *
       * To be the same, the applications must have the same name, version,
       * locale, and installer revision (ignoring case variations).
       *
       * @return
       */
      @SuppressWarnings("ObjectEquality")
      @Override
      protected boolean doEquivalent(T a, T b) {
         if (a == b) {
            return true;
         }

         if (a._architecture != b._architecture) {
            return false;
         }
         if (!a._installerRev.equals(b._installerRev)) {
            return false;
         }
         if (!a._locale.equals(b._locale)) {
            return false;
         }
         if (!a._name.equals(b._name)) {
            return false;
         }
         if (!a._version.equals(b._version)) {
            return false;
         }
         if (!a._vendor.equals(b._vendor)) {
            return false;
         }

         return true;
      }

      @Override
      protected int doHash(T t) {
         int result = t._name.hashCode();
         result = 31 * result + t._version.hashCode();
         result = 31 * result + t._locale.hashCode();
         result = 31 * result + t._installerRev.hashCode();
         result = 31 * result + t._architecture.hashCode();
         result = 31 * result + t._vendor.hashCode();
         return result;
      }
   }
}
