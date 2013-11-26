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


/**
 * -----------------------------------------------------------------------------
 * function BuildEditor
 *
 * Just a very simple wrapper class that encompasses the three distinct
 * build editors (ini editor, registry editor, and file system editor).
 * -----------------------------------------------------------------------------
 */
function BuildEditor(buildId, buildSettings)
{
   /* Create & populate package.ini editor */
   this.iniEditor = new IniEditor(
         buildSettings.iniDataRequest,
         $('#packageini-editor'),
         $('#packageini-change-marker'));

   /* Passing the iniDataRequest will set the runtime & addhorizon flag */
   this.iniEditor.Populate(buildSettings.iniDataRequest);
   this.iniEditor.PopulateSection(null); // start with the first section

   /* Create & populate registry editor */
   this.regEditor = new RegistryEditor(
         buildId,
         buildSettings.registryRoot,
         $('#registry-editor'),
         $('#registry-change-marker'));

   this.regEditor.Populate();

   /* Create & populate file system editor */
   this.fsEditor = new FileSystemEditor(
         buildId,
         buildSettings.dirRoot,
         buildSettings.projectDir,
         $('#filesystem-editor'),
         $('#filesystem-direct-link'),
         $('#settings-refresh-button'),
         false, // disable full UI editing for now
         $('#filesystem-change-marker'));

   this.fsEditor.Populate();
};


/**
 * -----------------------------------------------------------------------------
 * BuildEditor.Destruct
 *
 * Perform cleanup.
 * -----------------------------------------------------------------------------
 */
BuildEditor.prototype.
Destruct = function _destruct()
{
   this.iniEditor = null;
   this.regEditor = null;
   this.fsEditor = null;
   return null;
};


/**
 * -----------------------------------------------------------------------------
 * BuildEditor.HasChanges
 *
 * Return true if any of the three editors has pending changes.
 * -----------------------------------------------------------------------------
 */
BuildEditor.prototype.
HasChanges = function _hasChanges()
{
   return (
         this.iniEditor.HasChanges() ||
         this.regEditor.HasChanges() ||
         this.fsEditor.HasChanges());
};
