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

package com.vmware.appfactory.common;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * Description of an icon.
 *
 * This class represents an application icon, stored on a remote server.
 * This class forms the basis for both AfAppIcon and AfPackageIcon, which have
 * identical functionality but are stored separately.
 *
 * Applications and packages can have multiple icons in order to provide the
 * best image for a given size.
 */
@MappedSuperclass
public abstract class AfIcon
   extends AbstractRecord
{
   /**
    * Default icon that is returned when an application has no
    * icon of it's own.
    */
   public static final AfIcon DEFAULT_APPLICATION_ICON = new AfIcon() {
      @Override
      public int getSize() {
         return 32;
      }
      @Override
      public String getUrl() {
         /* Just the name: Javascript will add the rest */
         return "icon-installer_windows.png";
      }
      @Override
      public AfIcon clone() {
         // Not needed
         return null;
      }
   };

   @NotNull
   private String _url = "";

   @NotNull
   private String _contentType = "";

   @NotNull
   private int _size = 0;

   private String _localUrl = "";

   private byte[] _iconBytes = null;

   private String _iconHash = "";


   /**
    * Create a new AfIcon by cloning another.
    * @param other Icon to clone.
    */
   public AfIcon(AfIcon other)
   {
      _url = other._url;
      _contentType = other._contentType;
      _size = other._size;
   }


   /**
    * Create a new AfIcon instance.
    */
   public AfIcon()
   {
      /* Nothing to do */
   }


   /**
    * Set the URL where the icon resides.
    * Must not be null.
    * @param url
    */
   public void setUrl(String url)
   {
      if (url == null) {
         throw new IllegalArgumentException();
      }

      _url = url;
   }


   /**
    * Get the URL where the icon resides.
    * Will not be null.
    *
    * @return
    */
   public String getUrl()
   {
      return _url;
   }


   /**
    * Set the icon MIME content type.
    * This is optional; AppFactory will try to determine from the HTTP
    * server if this is missing.
    *
    * @param contentType
    */
   public void setContentType(String contentType)
   {
      if (contentType == null) {
         throw new IllegalArgumentException();
      }

      _contentType = contentType;
   }


   /**
    * Get the icon MIME content type.
    * This is the value specified from the feed. It might be null.
    * @return Image content type.
    */
   public String getContentType()
   {
      return _contentType;
   }


   /**
    * Set the icon size (pixels).
    * Icons are assumed to be square.
    * @param size Image size (pixels across).
    */
   public void setSize(int size)
   {
      _size = size;
   }


   /**
    * Get the icon size (pixels).
    * Icons are assumed to be square.
    * @return
    */
   public int getSize()
   {
      return _size;
   }


   /**
    * Set the internal/local url of the icon.  This is a url that can be used
    * to access the icon if it has been cached locally.
    * @param localUrl
    */
   public void setLocalUrl(String localUrl)
   {
      _localUrl = localUrl;
   }


   /**
    * Get the URL for accessing the locally cached icon.
    *
    * @return
    */
   public String getLocalUrl()
   {
      return _localUrl;
   }


   /**
    * Set the icon bytes.  May not be set if icon is not cached locally.
    * @param iconBytes icon bytes to set
    */
   @JsonIgnore
   public void setIconBytes(byte[] iconBytes) {
      _iconBytes = iconBytes;
   }


   /**
    * Get the icon bytes.  May not be set if icon is not cached locally.
    * @return the icon bytes
    */
   @JsonIgnore
   public byte[] getIconBytes() {
      return _iconBytes;
   }


   /**
    * Get the icon hash string.  May not be set if icon is not cached locally.
    * @return the icon hash
    */
   @JsonIgnore
   public String getIconHash() {
      return _iconHash;
   }


   /**
    * Set the icon hash string.  May not be set if icon is not cached locally.
    * @param iconHash icon hash to set
    */
   @JsonIgnore
   public void setIconHash(String iconHash) {
      _iconHash = iconHash;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (obj == null) {
         return false;
      }
      else if (obj == this) {
         return true;
      }
      else if (obj instanceof AfIcon) {
         AfIcon other = (AfIcon) obj;
         return (_url.equals(other._url) &&
                 _size == other._size &&
                 _contentType.equals(other._contentType));
      } else {
         return false;
      }
   }


   @Override
   public int hashCode()
   {
      return (_url + _contentType).hashCode() + _size;
   }


   @Override
   public abstract AfIcon clone();


   @Override
   public int deepCopy(AbstractRecord record)
   {
      AfIcon other = (AfIcon) record;
      int numChanges = 0;

      if (!StringUtils.equals(getUrl(), other.getUrl())) {
         setUrl(other.getUrl());
         numChanges++;
      }

      if (!StringUtils.equals(getContentType(), other.getContentType())) {
         setContentType(other.getContentType());
         numChanges++;
      }

      if (getSize() != other.getSize()) {
         setSize(other.getSize());
         numChanges++;
      }

      // Don't include localUrl in the count of changes
      setLocalUrl(other.getLocalUrl());

      // Don't include icon bytes in the count of changes
      setIconBytes(other.getIconBytes().clone());

      // Don't include icon hash in the count of changes
      setIconHash(other.getIconHash());

      return numChanges;
   }
}
