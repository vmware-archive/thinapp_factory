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

/**
 * A conglomeration of all the various aspects of a CWS project's settings.
 * This data structure is not officially part of the CWS API (the API deals
 * with packageIni, directory, and registry separately) but we use this
 * with CwsClient to make life easier.
 *
 * @author levans
 *
 */
public class CwsSettings
{
   private CwsSettingsIni _packageIni;

   private CwsSettingsRegKey _registryRoot;

   private CwsSettingsDir _dirRoot;


   /**
    * Sets the new package INI settings values.
    *
    * @param packageIni
    */
   public void setPackageIni(CwsSettingsIni packageIni) {
      _packageIni = packageIni;
   }


   /**
    * Sets the new package INI settings values if they are different.
    *
    * @param packageIni
    * @return true if the settings were changed, false otherwise
    */
   public boolean setPackageIniIfChanged(CwsSettingsIni packageIni) {
      if(_packageIni.equals(packageIni)) {
         return false;
      }

      _packageIni = packageIni;
      return true;
   }


   public CwsSettingsIni getPackageIni() {
      return _packageIni;
   }


   /**
    * Sets the registry root.
    *
    * @param registryRoot
    */
   public void setRegistryRoot(CwsSettingsRegKey registryRoot) {
      _registryRoot = registryRoot;
   }


   public CwsSettingsRegKey getRegistryRoot() {
      return _registryRoot;
   }


   public void setDirRoot(CwsSettingsDir dirRoot) {
      _dirRoot = dirRoot;
   }


   public CwsSettingsDir getDirRoot() {
      return _dirRoot;
   }
}
