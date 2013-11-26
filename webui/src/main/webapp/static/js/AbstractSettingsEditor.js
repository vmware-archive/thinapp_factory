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
 * function AbstractSettingsEditor
 *
 * Superclass for all the build editors, containing code common to each of
 * them. See IniEditor, RegistryEditor, and FileSystemEditor.
 * -----------------------------------------------------------------------------
 */
function AbstractSettingsEditor()
{
   this.unsavedChanges = 0;
}


/**
 * -----------------------------------------------------------------------------
 * AbstractSettingsEditor.MarkAsChanged
 *
 * Mark this editor as being changed.
 * -----------------------------------------------------------------------------
 */
AbstractSettingsEditor.prototype.
MarkAsChanged = function _markedAsChanged()
{
   this.unsavedChanges++;
   if (this.changeMarker && this.unsavedChanges == 1) {
      this.changeMarker.removeClass("no-show");
   }
};


/**
 * -----------------------------------------------------------------------------
 * AbstractSettingsEditor.MarkAsUnchanged
 *
 * Mark this editor as being unchanged. Does not undo anything!
 * -----------------------------------------------------------------------------
 */
AbstractSettingsEditor.prototype.
MarkAsUnchanged = function _markedAsUnchanged()
{
   this.unsavedChanges = 0;
   if (this.changeMarker) {
      this.changeMarker.addClass("no-show");
   }
};


/**
 * -----------------------------------------------------------------------------
 * AbstractSettingsEditor.HasChanges
 *
 * Check if this editor has any changes. Only looks at the 'unsavedChanges'
 * value, does not look for actual edits!
 * -----------------------------------------------------------------------------
 */
AbstractSettingsEditor.prototype.
HasChanges = function _hasChanges()
{
   return (this.unsavedChanges > 0);
};