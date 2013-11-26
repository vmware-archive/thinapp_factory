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

package com.vmware.appfactory.cws;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;


/**
 * Defines CWS project settings related to directory attributes.
 *
 * This is based on the CWS API specification: do not edit unless it remains
 * compatible.
 *
 * @author levans
 *
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class CwsSettingsDir
   implements Comparable<CwsSettingsDir>
{
   private String _path;

   private final Map<String,String> _files = new TreeMap<String,String>();

   private final Map<String,String> _directories = new TreeMap<String,String>();

   private CwsSettingsIni _attributes;

   private Long _id;


   public void setPath(String path) {
      _path = path;
   }


   public String getPath() {
      return _path;
   }


   public Map<String,String> getFiles() {
      return _files;
   }


   public void addFile(String name, String uri) {
      _files.put(name, uri);
   }


   public Map<String, String> getDirectories() {
      return _directories;
   }


   public void addDirectory(String name, String uri) {
      _directories.put(name, uri);
   }


   public void setAttributes(CwsSettingsIni attrs) {
      _attributes = attrs;
   }


   public CwsSettingsIni getAttributes()
   {
      return _attributes;
   }


   public void setId(Long directoryId)
   {
      _id = directoryId;
   }


   public Long getId()
   {
      return _id;
   }


   @Override
   public int compareTo(CwsSettingsDir o) {
      return new CompareToBuilder()
         .append(this._path, o._path)
         .append(this._files, o._files)
         .append(this._directories, o._directories)
         .append(this._attributes, o._attributes)
         .toComparison();
   }
}
