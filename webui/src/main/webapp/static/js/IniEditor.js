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


/** Constants required by iniEditor */
IniEditor.BUILD_OPTIONS = "BuildOptions";
IniEditor.HORIZON_KEYS = ['AppID', 'HorizonOrgUrl', 'NotificationDLLs'];

/**
 * -----------------------------------------------------------------------------
 * function IniEditor
 *
 * A "class" that performs all the tasks for editing package INI data.
 * This data is simply a Javascript object containing properties for each
 * INI section, and each property value is another object whose properties
 * are INI keys.
 *
 * Example:
 * iniDataRequest : {
 *    hzSupported : boolValue - indicates hz configuration managed by TAF.
 *    runtimeid : appliedRuntimeId - Runtime associated with the build.
 *    packageIni : {
 *       section1 : {
 *           key1 : value,
 *           key2 : value
 *       },
 *       section2 : {
 *           key1 : value,
 *           key2 : value
 *       }
 *    }
 * }
 * -----------------------------------------------------------------------------
 */
function IniEditor(iniDataRequest, editorDiv, changeMarker)
{
   this.iniData = iniDataRequest.packageIni;
   this.hzSupported = iniDataRequest.hzSupported;

   /* Store the HTML elements that we require */
   // TODO: If any are missing, throw an error
   this.sectionSelector = editorDiv.find('.ini-sections select');
   this.sectionForm = editorDiv.find('.ini-data form');
   this.rtSelect = $('#thinappRuntime');
   this.hzCheckbox = this.rtSelect.parent().find('#addHorizon');
   /* Flag indicating hz settings is enabled on TAF. */
   this.hzEnabledOnTAF = (this.rtSelect.find('#runtimeGroupHorizon').length > 0);
   this.sectionForm = editorDiv.find('.ini-data form');
   this.changeMarker = changeMarker;
}

IniEditor.prototype = new AbstractSettingsEditor();


/**
 * -----------------------------------------------------------------------------
 * IniEditor.GetData()
 *
 * Get the data we have been editing in this editor instance.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
GetIniData = function _getIniData()
{
   return {
      packageIni: this.iniData,
      runtimeId: this.rtSelect.val(),
      hzSupported: this.hzCheckbox.prop('checked')
   };
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.Populate()
 *
 * Fills the package editor with the current packageIni data (or replaces the
 * data if new data is supplied). This sets all the INI sections but does not
 * select one: use PopulateSection() to select a section.

 * @param iniDataRequest Optional: new iniData with package INI settings
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
Populate = function _populate(iniDataRequest)
{
   var self = this;

   if (iniDataRequest) {
      AfLog('Populating package ini form with new data and setting runtime & addHorizon');
      self.iniData = iniDataRequest.packageIni;
      self.hzSupported = iniDataRequest.hzSupported;

      /* Now set the runtime dropdown value and addHorizon flag if they exist. */
      this.hzCheckbox.prop('checked', iniDataRequest.hzSupported);

      if(iniDataRequest.runtimeId) {
         this.rtSelect.val(iniDataRequest.runtimeId).show();
         if (VmTAF.isHzCapable(iniDataRequest.runtimeId) && this.hzEnabledOnTAF) {
            this.hzCheckbox.parent().show();
         }
      }
   }
   else {
      AfLog('Re populate package ini form with existing data (retain runtime, addHorizon selection).');
   }

   /* Remove existing sections */
   self.sectionSelector.find('option').remove();

   /* Add sorted INI section names to the select list */
   var sectionNames = self.extractAllSectionNames();
   for (var i = 0; i < sectionNames.length; i++) {
      var name = sectionNames[i];

      var opt = $('<option>');
      opt.text(name);
      self.sectionSelector.append(opt);
   }

   /* Size select list to show all sections, no scrolling */
   self.sectionSelector.attr('size', sectionNames.length);

   /* When selection changes, populate section values */
   self.sectionSelector.unbind('change').change(function(event) {
      var value = $(this).val();
      self.PopulateSection(value, false);
   });

   AfLog('package-ini editor populated');
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.CreateNewSection()
 *
 * Adds a new section to the current packageIni data. The new section is
 * empty, and is selected once created.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
CreateNewSection = function _createNewSection()
{
   var self = this;

   var newName = AfPrompt(
      'New Section',
      'Enter a name for the new section:');
   if (!newName) {
      return;
   }

   if (self.iniData[newName]) {
      AfError(
         'Name In Use',
         'There is already a section named "' + newName + "'");
      return;
   }

   /* Insert a new empty section and refresh the editor */
   self.iniData[newName] = new Object();
   self.MarkAsChanged();
   self.Populate();
   self.PopulateSection(newName, true);
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.RuntimeHzSupportedChanged()
 *
 * Mark as changed, and update display.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
RuntimeHzSupportedChanged = function _runtimeHzSupportedChanged()
{
   /* Just mark changes and re populate. */
   var self = this;

   self.MarkAsChanged();
   self.Populate();
   var name = self.sectionSelector.find('option').val();
   if (name && name != '') {
      self.PopulateSection(name, true);
   }
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.RenameCurrentSection()
 *
 * Rename an existing section on the packageIni section listing area. Rename
 * will also validate for unique names to avoid name conflicts.
 *
 * TODO incorporate AfPromptForUniqueName()
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
RenameCurrentSection = function _renameCurrentSection()
{
   var self = this;

   /* Get the name of the selected INI section */
   var sectionName = self.sectionSelector.val();
   if (!sectionName) {
      /* This should not happen */
      AfError('No section selected!');
      return;
   }

   var newName = AfPrompt(
      'New Section Name',
      'Enter a new name for section: ' + sectionName);
   if (!newName) {
      return;
   }

   if (self.iniData[newName]) {
      AfError(
         'Name In Use',
         'There is already a section named "' + newName + "'");
      return;
   }

   /* Insert a new empty section and refresh the editor */
   self.iniData[newName] = self.iniData[sectionName];
   delete this.iniData[sectionName];
   self.MarkAsChanged();
   self.Populate();
   self.PopulateSection(newName, true);
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.DuplicateCurrentSection()
 *
 * This creates a copy of the selected section, and forces user to rename it.
 * It will not validate the contents of that section.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
DuplicateCurrentSection = function _duplicateCurrentSection()
{
   var self = this;

   /* Get the name of the selected INI section */
   var sectionName = this.sectionSelector.val();
   if (!sectionName) {
      /* This should not happen */
      AfError('No section selected!');
      return;
   }

   var newName = sectionName + '-copy-';
   var counter = 0;

   // Ensure the selected sectionName + '-copy-' + counter does not exist.
   do {
      counter += 1;
   } while (self.iniData[(newName + counter)]);

   // Set the new name using counter as: sectionName + '-copy-' + counter
   newName += counter;

   /* Insert a deep copy of the selected section and refresh the editor */
   self.iniData[newName] = jQuery.extend(true, {}, self.iniData[sectionName]);
   self.MarkAsChanged();
   self.Populate();

   // Select the duplicate copy, and populate with this sections data.
   self.PopulateSection(newName, true);
   self.sectionSelector.find('option').val(newName);

   // Request renaming this duplicated section.
   self.RenameCurrentSection();
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.DeleteCurrentSection()
 *
 * Deletes the currently selected INI section completely, including all its
 * values. There is currently no way to get them back.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
DeleteCurrentSection = function _deleteCurrentSection()
{
   /* Get the name of the selected INI section */
   var sectionName = this.sectionSelector.val();
   if (!sectionName) {
      /* This should not happen */
      AfError('No section selected!');
      return;
   }

   /* Check the user really wants to do this */
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete section "' + sectionName + '"? ' +
         'All its values will also be deleted. ' +
         'This cannot be undone.')) {
      return;
   }

  /* Delete the section and refresh the editor */
  delete this.iniData[sectionName];
  this.MarkAsChanged();
  this.Populate();
  this.PopulateSection(this.extractAllSectionNames()[0], true);
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.CreateNewValue()
 *
 * Creates a new value for the current INI section. Asks the user for a value
 * name, and inserts it into the section. The initial value is empty.
 *
 * TODO incorporate AfPromptForUniqueName()
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
CreateNewValue = function _createNewValue()
{
   var self = this;

   /* Get the name of the selected INI section */
   var sectionName = this.sectionSelector.val();
   if (!sectionName) {
      /* Should not happen */
      AfLog('No section selected!');
      return;
   }

   var newName = AfPrompt(
         'Enter Name',
         'Enter a name for the value:');
   if (!newName) {
      return;
   }

   var section = self.iniData[sectionName];

   if (section[newName]) {
      AfError(
         'Name In Use',
         'There is already a value named "' + newName + "'");
      return;
   }

   /* Add a new property with a default value */
   section[newName] = '';
   this.MarkAsChanged();

   /* Refresh the editor display */
   this.PopulateSection(sectionName, false);
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.DeleteValue()
 *
 * Deletes the named value from the current INI section.
 *
 * @param sectionName
 * @param valueName
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
DeleteValue = function _deleteCurrentValue(
      sectionName,
      valueName)
{
   var section = this.iniData[sectionName];
   delete section[valueName];
   this.MarkAsChanged();

   /* Refresh the editor display */
   this.PopulateSection(sectionName, false);
   return;
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.PopulateSection()
 *
 * Invoked when a package INI section is selected. This causes the list
 * if fields to be updated so that it shows the data from that INI
 * section.
 *
 * @param sectionName INI section name to show
 * @param selectInList If true, highlight the name in the select list too.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
PopulateSection = function _populateSection(
      sectionName,
      selectInList)
{
   var self = this;

   /* If not specified, use the first one */
   if (!sectionName) {
      sectionName = self.extractAllSectionNames()[0];
   }

   AfLog('Selecting ini section ' + sectionName);

   /* Set the section legend */
   this.sectionForm.find('#ini-label').text(sectionName);

   var data = self.iniData[sectionName];
   var insertPoint = this.sectionForm.find('#ini-stack-group');

   /* Remove any stackgroups insterted into the sectionForm except insertPoint */
   this.sectionForm.find('.stack-group').not(insertPoint).remove();

   /* Delete existing 'fields' */
   insertPoint.empty();

   // Compute if Hz Section can be displayed if hz is really applicable based on runtime, flag and TAF config.
   var displzyHzSection = (this.hzEnabledOnTAF && this.hzCheckbox.prop('checked')
         && VmTAF.isHzCapable(this.rtSelect.val()));

   /* Create a horizon group that could be displayed separately. */
   var horizonGroup = [];

   for (var key in data) {
      var delSpan = $('<span class="delete-icon"></span>');
      delSpan.click(key, function(event) {
         var key = event.data;
         self.DeleteValue(sectionName, key);
         self.MarkAsChanged();
      });

      var input = $('<input>');
      //input.attr('size', 40);
      input.attr('name', key);
      input.attr('type', 'text');
      input.attr('value', data[key]);
      input.attr('onblur', "this.value=jQuery.trim(this.value)");

      /* When input value changes, update the packageIni data */
      input.change(function() {
         var key = $(this).attr('name');
         var val = $(this).val();
         self.iniData[sectionName][key] = val;
         self.MarkAsChanged();
      });

      var label = $('<label>');
      label.attr('for', key);
      label.text(key);
      var labelDiv = $('<div></div>').addClass('label').append(label);

      var div = $('<div class="field text long">');
      var row = $('<div></div>').addClass('stack-row');

      // Create a non editable horizon group that is managed by TAF when hz is enabled.
      if (displzyHzSection && sectionName == IniEditor.BUILD_OPTIONS && $.inArray(key, IniEditor.HORIZON_KEYS) >= 0) {
         var fieldText = $('<span></span>').text(data[key]);
         fieldText = $('<div class="field"></div>').append(fieldText);
         row.append(labelDiv).append(fieldText);
         horizonGroup.push(row);
      }
      else if (this.hzSupported && sectionName == IniEditor.BUILD_OPTIONS && $.inArray(key, IniEditor.HORIZON_KEYS) >= 0) {
         // Do not display the horizon settings that existed when iniEditor was initially loaded.
         // Case occurs only if hzSupported=true initially, and user decided to remove it.
      }
      else {
         div.append(input).append(delSpan);
         row.append(labelDiv).append(div);
         insertPoint.append(row);
      }
   }

   // If Horizon group exists, add related setting entries into separate group.
   if (horizonGroup.length > 0) {
      var div = $('<label>').text('Horizon settings');
      div = $('<div></div>').addClass('label').append(div);
      div = $('<div></div>').addClass('stack-row-head').append(div);
      var stackGroup = $('<div></div>').addClass("stack-group ui-widget-content").append(div);

      $.each(horizonGroup, function(index, value) {
         stackGroup.append(value);
         insertPoint.after(stackGroup);
     });
   }

   if (selectInList) {
      self.sectionSelector.val(sectionName);
   }
};


/**
 * -----------------------------------------------------------------------------
 * IniEditor.extractAllSectionNames()
 *
 * Get all the section names from the current INI data and put them into a
 * sorted array.
 * -----------------------------------------------------------------------------
 */
IniEditor.prototype.
extractAllSectionNames = function _extractAllSectionNames(iniData)
{
   var sectionNames = new Array();
   for (var name in this.iniData) {
      sectionNames.push(name);
   }

   return sectionNames.sort();
};
