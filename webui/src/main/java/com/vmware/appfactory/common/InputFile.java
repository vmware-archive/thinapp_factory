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

import java.net.URI;
import java.net.URISyntaxException;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.vmware.appfactory.common.base.AbstractRecord;

/**
 * A feed includes input files for various purposes. There are basically two
 * types of file: URL files, which have a URL (relative or absolute) pointing
 * to the file content, or reference files, which refer to an application that
 * must already be in the TAF database.
 */
@MappedSuperclass
public abstract class InputFile
   extends AbstractRecord
{
   private String _name;

   // Original location relative to where the file was found
   private String _path;

   // Full URI of the file (Stored as a string for easy database debugging)
   @Column(length=LONG_LEN)
   private String _uriStr;

   // For URL files only:
   @Embedded
   @AttributeOverrides( {
      @AttributeOverride(name="_function", column=@Column(name="_hash_function")),
      @AttributeOverride(name="_value", column=@Column(name="_hash_value"))
   } )
   private AfHash _hash;

   @Column(length=4096)
   private String _description;


   /**
    * Set the name of the file.
    * If not specified, TAF will attempt to deduce a name from the HTTP
    * headers when downloading it. Not applicable to reference files.
    *
    * @param name
    */
   public void setName(String name)
   {
      if (name != null) {
         _name = name.trim();
      } else {
         _name = null;
      }
   }


   /**
    * Get the name of the file.
    * If not specified, TAF will attempt to deduce a name from the HTTP
    * headers when downloading it. Not applicable to reference files.
    * @return the name
    */
   public String getName()
   {
      return _name;
   }


   /**
    * Get the hash function for verifying this file. Not applicable to
    * reference files.
    * @return
    */
   public AfHash getHash()
   {
      return _hash;
   }


   /**
    * Set the hash function for verifying this file. Not applicable to
    * reference files.
    * @param hash
    */
   public void setHash(AfHash hash)
   {
      _hash = hash;
   }


   /**
    * Set the path where this file is located relative to its parent.
    * @param path
    */
   public void setPath(String path)
   {
      if (path != null) {
         _path = path.trim();
      } else {
         _path = null;
      }
   }


   /**
    * Get the path where this file is located relative to its parent.
    * @return
    */
   public String getPath()
   {
      return _path;
   }


   /**
    * Get the full URI to this file.
    * @return
    */
   public URI getURI()
   {
      try {
         return (_uriStr == null ? null : new URI(_uriStr));
      }
      catch (URISyntaxException x) {
         throw new RuntimeException(String.format(
            "Invalid URL %s: corrupt data?",
            _uriStr));
      }
   }


   /**
    * Set the full URI to this file.
    * @param uri
    */
   public void setURI(URI uri)
   {
      if (uri != null) {
         _uriStr = uri.toString().trim();
      } else {
         _uriStr = null;
      }
   }


   /**
    * Set the description of this file.
    * @param description
    */
   public void setDescription(String description)
   {
      _description = description;
   }


   /**
    * Get the description of this file.
    * @return
    */
   public String getDescription()
   {
      return _description;
   }


   /**
    * Multi-purpose function to set either the full URI where this file is
    * located (if 'location' can be parsed as a URI), or its path relative
    * to its source.
    *
    * @param location
    */
   public void setLocation(String location)
   {
      try {
         /* First see if this is an absolute location */
         URI uri = new URI(location);
         if (uri.isAbsolute()) {
            setURI(uri);
            return;
         }
      } catch (URISyntaxException e) { /* Not a URI? */ }

      /* Must be a relative path */
      setPath(location);
   }


   @Override
   public int deepCopy(AbstractRecord record)
   {
      InputFile other = (InputFile) record;
      int numChanges = 0;

      if (!StringUtils.equals(getDescription(), other.getDescription())) {
         setDescription(other.getDescription());
         numChanges++;
      }

      if (!AfHash.equals(getHash(), other.getHash())) {
         setHash(other.getHash());
         numChanges++;
      }

      if (!StringUtils.equals(getName(), other.getName())) {
         setName(other.getName());
         numChanges++;
      }

      if (!ObjectUtils.equals(getURI(), other.getURI())) {
         setURI(other.getURI());
         numChanges++;
      }

      if (!StringUtils.equals(getPath(), other.getPath())) {
         setPath(other.getPath());
         numChanges++;
      }

      return numChanges;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (!(obj instanceof InputFile)) {
         return false;
      }

      InputFile other = (InputFile) obj;
      if (other == this) {
         return true;
      }

      return new EqualsBuilder()
         .append(_name, other._name)
         .append(_path, other._path)
         .append(_hash, other._hash)
         .append(_description, other._description)
         .isEquals();
   }


   @Override
   public int hashCode()
   {
      return new HashCodeBuilder(11, 69)
         .append(_name)
         .append(_path)
         .append(_hash)
         .append(_description)
         .toHashCode();
   }
}
