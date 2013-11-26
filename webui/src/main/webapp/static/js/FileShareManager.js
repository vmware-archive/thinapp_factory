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
 * FileShareManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with fileshare.
 * -----------------------------------------------------------------------------
 */
function FileShareManager(table)
{
   if (table) {
      var tableOpts = {
         aoColumns: [
            null,
            null,
            null,
            { sType: "title-numeric" },
            null,
            null
         ]
      };
      this.CreateTableWrapper(table, tableOpts);
   }
}
FileShareManager.prototype = new AbstractManager('FileShare');

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.UpdateFileShareList
 *
 * Public function to load data from the server into the fileshare table.
 *
 * @param table HTML "table" element that displays all the fileshare.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
UpdateFileShareList = function _UpdateFileShareList(refreshInterval)
{
   this.UpdateDataTable({
      dataHandler: this.createRows,
      table: this.tableWrapper.dataTable,
      url: '/api/fileshare?orderBy=name',
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.FILE_SHARES.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.GetFileshareAndPopulateForm
 *
 * Fetch the specified fileshare from the server, and display it in the file
 * share edit form.
 *
 * @param fileshareId ID of fileshare to be edited.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
GetFileshareAndPopulateForm = function(fileshareId)
{
   var self = this;

   AfAjax({
      method: 'GET',
      url: '/api/fileshare/' + fileshareId,
      success: function(data) {
         self.PopulateEditForm(data);
         self.ToggleOptionalFields();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.PopulateEditForm
 *
 * Public function to fill out the fileshare edit view with the given
 * fileshare (JSON format).
 *
 * @param fs Fileshare (JSON) to be displayed.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
PopulateEditForm = function _PopulateEditForm(fs)
{
   $('[name="name"]').val(fs.name);
   $('[name="serverPath"]').val(fs.serverPath);
   $('[name="description"]').val(fs.description);
   $('[name="okToConvert"]').attr('checked', fs.okToConvert);
   $('[name="authRequired"]').attr('checked', fs.authRequired);
   $('[name="authUsername"]').val(fs.username);
   $('[name="authPassword"]').val(fs.password);
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.DeleteSelectedFileShares
 *
 * Public function to delete all fileshare selected in the given
 * file share list table.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
DeleteSelectedFileShares = function _DeleteSelectedFileShares()
{
   var self = this;

   /* Get the fileshare IDs that are selected. */
   var shares = this.tableWrapper.GetSelectedRowData();
   if (!shares || shares.length == 0) {
      AfErrorT(
         'T.FILE_SHARES.NO_SELECTION',
         'M.FILE_SHARES.SELECT_TO_DELETE');
      return;
   }

   /* Check with the user. */
   if (!AfConfirm(
         AfTranslate('T.FILE_SHARES.CONFIRMATION'),
         AfTranslate('M.FILE_SHARES.CONFIRMATION_TO_DELETE') + ' ' +
         AfThisOrThese(shares, 'file share') + '?')) {
      return;
   }

   /* Delete each fileshare */
   for (var i = 0; i < shares.length; i++) {
      AfAjax({
         method: 'DELETE',
         url: '/api/fileshare/' + shares[i].id,

         beforeSend: function() {
            AfStartPleaseWaitT('M.FILE_SHARES.DELETING');
         },

         error: function(client, status, error) {
            AfError('Error',
                  'Unable to delete fileshare: ' + error);
         },

         complete: function() {
            if (AfEndPleaseWait()) {
               /* Force a refresh the table. */
               self.PopulateDataTableNow({
                  url: '/api/fileshare?orderBy=name',
                  table: self.tableWrapper.dataTable,
                  dataHandler: self.createRows
               });
            }
         }
      });
   }
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.SubmitEditForm
 *
 * Submit the file share edit form for updating an existing fileshare.
 * @param form
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
SubmitEditForm = function _SubmitEditForm(form)
{
   var self = this;

   var json = AfSerializeForm(form, '^(fileshare-import-table.*|app-*|id-*)');
   // Will add delta array to json if a user updated meta-data.
   json = this.checkDelta(self, json);
   json = this.getSkippedApps(self, json);
   AfLog(json);

   var fileShareId = json['fileShareId'];

   AfLog('submitting edit form for file share ' + fileShareId);
   AfLog(json);

   AfAjax({
      method: 'PUT',
      contentType: 'application/json',
      url: '/api/fileshare/' + fileShareId,
      data: json,

      error: function _onError(jqXHR, textStatus, errorThrown) {
         // Failed to save the form
         AfError('Error', 'Save failed: ' + jqXHR.responseText);
      },

      success: function() {
         AfNotifyT('T.FILE_SHARES.SAVED', 'M.FILE_SHARES.SAVED');
         if (VmTAF.dialogHolder) {
            VmTAF.dialogHolder.dialog("close");
         } else {
            VmTAF.contentNavigator.GoBack();
         }
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.SubmitNewFileShareForm
 *
 * Callback invoked when the user saves data from the New File Share dialog.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
SubmitNewFileShareForm = function _SubmitNewFileShareForm(holder)
{
   var self = this;

   // Convert form data into JSON
   var json = AfSerializeForm(holder, '^(fileshare-import-table.*|app-*|id-*)');
   AfLog('built json from file share form:');
   // Will add delta array to json if a user updated meta-data.
   json = this.checkDelta(self, json);
   json = this.getSkippedApps(self, json);
   AfLog(json);

   // Send create request to AppFactory
   AfAjax({
      method: 'POST',
      url: '/api/fileshare/create',
      data: json,
      contentType: 'application/json',

      success: function(data, textStatus, jqXHR) {
         AfNotifyT('T.FILE_SHARES.SAVED', 'M.FILE_SHARES.SAVED');
         if (VmTAF.dialogHolder) {
            VmTAF.dialogHolder.dialog("close");
         } else {
            VmTAF.contentNavigator.GoBack();
         }
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Error', 'Save failed: ' + textStatus + ': ' + jqXHR.responseText);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.checkDelta
 *
 * Check user changes on application info fields from unselected rows in the
 * datatable.
 *
 * @param self
 * @param jsonData
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.checkDelta = function _checkDelta(self, json)
{
   var rows = self.tableWrapper.GetUnSelectedRowData(true);
   var appDeltas = new Array();
   var metanames = VmTAF.metaNames;

   for (var i = 0; i < rows.length; i++) {
      var path = rows[i].app.download.uri;
      var dataRow = rows[i].element;
      // Change data (jQuery data) stores in the last column.
      var dataElement = $(dataRow).find('td:last-child');
      var data = $(dataElement).data();

      if (!jQuery.isEmptyObject(data)) {
         // Metadata is full path to installer name, so chop off the
         // URI scheme and host
         var installerName = unescape(path.replace(/datastore:\/\/[0-9]+/, ''));
         // add 'key' attribute to the app delta.
         data.key = installerName;
         appDeltas.push(data);
      }
   }
   json.appDeltas = appDeltas;

   return json;
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.getSkippedApps
 *
 * Check if there is any apps to be skipped in the import. It adds a new json
 * element ('appsToSkip') to the given json object.
 *
 * @param self
 * @param jsonData - an updated json.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.getSkippedApps = function _getSkippedApps(self, json)
{
   var apps = self.tableWrapper.GetSelectedRowData();
   var skip_array = new Array();

   for(var i = 0; i < apps.length; i++) {
      var fullpath = apps[i].download.uri;
      fullpath = unescape(fullpath.replace(/datastore:\/\/[0-9]+/, ''));
      skip_array.push(fullpath);
   }
   json.appsToSkip = skip_array;

   return json;
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.addApplicationRows
 *
 * Add a row per application to the table.
 *
 * @param apps - an array of applications
 * @param installerSuffix - an optional installer suffix to be added.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
addApplicationRows = function _addApplicationRows(self, apps, installerSuffix)
{
   var suffix = (installerSuffix) ? installerSuffix + '-' : '';

   /* Add a row per application. */
   for (var i = 0; i < apps.length; i++) {
      var app = apps[i];
      var id = suffix + i;

      /* Check-box to select this application */
      var installer = app.download.uri;
      var tempArray = installer.split("/");
      if (tempArray && tempArray.length > 1) {
         installer = tempArray[ tempArray.length - 1 ];
      }

      // Unique id is required because validation rules depend on checkbox using id.
      var cb = AfCreateInputCheckbox({ name: "id-" + id, id: "id-" + id });
      // Validation rule when the above chcekbox is unchecked.
      var whenUnchecked = "{required:'#id-" + id + ":unchecked'}";
      var label = AfOverflowToTooltip(installer);

      /* Install command (required) */
      var appCmdOpt = ((app.install && app.install.length && app.install[0].command) || '');
      var installCommandInput = AfCreateInputText({
         name: "app-installcommand-" + id, /* unique 'name' to make validation worked */
         value: appCmdOpt,
         size: 33,
         cssClass: "required-text-field",
         extra: {
            name: "dKey",
            value: "installCommand"
         }
      });

      /* Application Name (required) */
      var appName = (app.name || '');
      var nameInput = AfCreateInputText({
         name: "app-name-" + id, /* unique 'name' to make validation worked */
         value: appName,
         size: 15,
         cssClass: "required-text-field",
         placeholder: "(Required)",
         validate: whenUnchecked,
         extra: {
            name: "dKey",
            value: "name"
         }
      });

      /* Version (required) */
      var version = (app.version || '');
      var versionInput = AfCreateInputText({
         name: "app-version-" + id, /* unique 'name' to make validation worked */
         value: version,
         size: 15,
         cssClass: "required-text-field",
         placeholder: "(Required)",
         validate: whenUnchecked,
         extra: {
            name: "dKey",
            value: "version"
         }
      });

      /* Optional fields - Vendor, Language and Revision */
      var vendor = (app.vendor || '');
      var vendorInput = AfCreateInputText({
         name: "app-vendor",
         value: vendor,
         size: 15,
         extra: {
            name: "dKey",
            value: "vendor"
         }
      });

      var rev = (app.installerRevision || '');
      var revInput = AfCreateInputText({
         name: "app-revision",
         value: rev,
         size: 5,
         extra: {
            name: "dKey",
            value: "revision"
         }
      });

      var lang = (app.locale || '');
      var langInput = AfCreateInputText({
         name: "app-lang",
         value: lang,
         size: 5,
         extra: {
            name: "dKey",
            value: "lang"
         }
      });

      /* Create a table row */
      var row = new Array();
      if (installerSuffix) {
         var suffix_label = $('<em>  ' + installerSuffix + '</em>');
         row.push(AfHtml(cb) + AfHtml(label) + AfHtml(suffix_label));
      } else {
         row.push(AfHtml(cb) + AfHtml(label));
      }
      row.push(AfHtml(installCommandInput));
      row.push(AfHtml(nameInput));
      row.push(AfHtml(versionInput));
      row.push(AfHtml(vendorInput));
      row.push(AfHtml(revInput));
      row.push(AfHtml(langInput));

      self.tableWrapper.AddRow(row, app);
   } // end for-loop

};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.createEditableRows
 *
 * Create editable rows for the given applications (jsonData).
 *
 * @param self
 * @param jsonData
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
createEditableRows = function _createEditableRows(self, jsonData)
{
   /* Remove all rows data from the table. */
   self.tableWrapper.ClearTable();
   self.addApplicationRows(self, jsonData.applications);
   self.tableWrapper.DrawTable();
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.createTabs
 *
 * Create different tabs with datatable for the given applications (jsonData).
 *
 * @param self
 * @param jsonData
 * @param installerSuffix - an optional installer suffix to be added.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
createTabs = function _createTabs(self, jsonData, installerSuffix)
{
   /* Remove all rows data from the table. */
   self.tableWrapper.ClearTable();
   self.addApplicationRows(self, jsonData.appSyncResponse.newItems, 'NEW');
   self.addApplicationRows(self, jsonData.appSyncResponse.updatedItems);
   self.tableWrapper.DrawTable();

   // Show 'Deleted' tab if it has any deleted apps.
   if (jsonData.appSyncResponse.numDeletedItems > 0) {
      $('#deleted-tab').removeClass('no-show');
      self.createRemovedAppsTable(jsonData.appSyncResponse.deletedItems);
   }
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.createRemovedAppsTable
 *
 * Create a new datatable for the deleted applications.
 *
 * @param apps - a list of deleted applications.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
createRemovedAppsTable = function _createRemovedAppsTable(apps)
{
   var table = new TableWrapper($('#fileshare-import-table-removed'));

   table.ClearTable();

   /* Add a row per application. */
   for (var i = 0; i < apps.length; i++) {
      var app = apps[i];

      var installer = app.download.uri;
      var tempArray = installer.split("/");
      if (tempArray && tempArray.length > 1) {
         installer = tempArray[ tempArray.length - 1 ];
      }

      /* Create a table row */
      var row = new Array();
      row.push(installer);
      row.push(app.name);
      row.push(app.version);
      row.push(app.vendor);
      row.push(app.installerRevision);
      row.push(app.locale);

      table.AddRow(row, app);
   }

   table.DrawTable();
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.changeHandler
 *
 * Create a change handler for app info input field. It compares the original
 * value from the initial rendering against user update and stores the difference
 * in the last key column via jQuery data.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.changeHandler = function _changeHandler()
{
   /* Save the original value */
   var original_value = $(this).val();

   $(this).unbind('change');

   $(this).change(function() {
      /* Get current value */
      var value = $(this).val();
      var key = $(this).attr('dKey');
      var keyColumn = $(this).parent().parent().find('td:last-child');
      var keyColumn = keyColumn[0];
      if (original_value === value) {
         $.removeData(keyColumn, key);
      } else {
         $.data(keyColumn, key, value);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.Scan
 *
 * Scan the given file share and render the application info in the table
 * for editing.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.Scan = function _Scan(holder)
{
   var self = this;

   // Convert form data into JSON except name starts with app-* and
   // fileshare-import-table.*
   var json = AfSerializeForm(holder, '^(fileshare-import-table.*|app-.*)');

   AfLog('built json from file share form:');
   AfLog(json);

   // Send create request to AppFactory
   AfAjax({
      method: 'POST',
      url: '/api/fileshare/scan',
      data: json,
      contentType: 'application/json',

      beforeSend: function() {
         AfStartPleaseWaitT('M.FILE_SHARES.SCANNING');
      },

      success: function(data) {
         self.disableBasicInfoFields();
         $('label[for="basic-information"]').click();
         $('#edit-meta-data-stack-group').removeClass('no-show');
         self.createEditableRows(self, data);
         $('#scan-btn').hide();
         $('#save-btn').show();
         $('[name^="app-"]').each(self.changeHandler);
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Error', 'Scan failed: ' + textStatus + ': ' + jqXHR.responseText);
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.Rescan
 *
 * Re-scan the given file share and render the application info in the table
 * for editing.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.Rescan = function _Rescan(holder)
{
   var self = this;

   // Convert form data into JSON except name starts with app-* and
   // fileshare-import-table.*
   var json = AfSerializeForm(holder, '^(fileshare-import-table.*|app-.*)');

   AfLog('built json from file share form:');
   AfLog(json);

   // Send create request to AppFactory
   AfAjax({
      method: 'POST',
      url: '/api/fileshare/sync',
      data: json,
      contentType: 'application/json',

      beforeSend: function() {
         AfStartPleaseWaitT('M.FILE_SHARES.SCANNING');
      },

      success: function(data) {
         self.displaySyncResult(self, data);
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Error', 'Rescan failed: ' + textStatus + ': ' + jqXHR.responseText);
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.Rescan
 *
 * Re-scan the given file share and render the application info in the table
 * for editing.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.displaySyncResult = function _displaySyncResult(self, data)
{
   self.disableBasicInfoFields();
   $('label[for="basic-information"]').click();
   $('#edit-meta-data-stack-group').removeClass('no-show');
   self.createTabs(self, data);
   $('#scan-btn').toggle();
   $('#save-btn').toggle();
   $('[name^="app-"]').each(self.changeHandler);
};

/**
 * -----------------------------------------------------------------------------
 * FileShareManager.createRows
 *
 * Function to update the rows of the file share table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the fileshare.
 * @param jsonData JSON-formatted fileshare summary data.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
createRows = function _createRows(self, jsonData, options)
{
   var fileshare = jsonData;

   // To avoid memory leaks, unbind prior event handlers before clearing table
   $('.edit-link').unbind('click');

   // Remove all data rows
   self.tableWrapper.ClearTable();

   // Sort fileshare by name.
   fileshare.sort(function(fs1, fs2) {
      return fs1.name.localeCompare(fs2.name);
   });

   // Add a row per file share.
   for (var i = 0; i < fileshare.length; i++) {
      var fs = fileshare[i];

      // Check-box to select this file share: when clicked, possibly
      // pause table updates.
      var cb = $('<input type="checkbox">');

      // Link for the file share name: opens the file share editor.
      var link = self.createRecordLink(
            fs.name,
            fs.id,
            '/fileshare/edit/' + fs.id);

      // Create a row: attach file share id as data
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(link));
      row.push(fs.description);
      row.push(AfTimeago(fs.lastScanMillis));
      row.push(fs.datastoreName);
      row.push(AfTranslate("T.FILE_SHARES.STATUS."+fs.status));

      // Add row to the table.
      self.tableWrapper.AddRow(row, fs);
   }

   /* Redraw the table */
   self.tableWrapper.DrawTable();
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.ToggleOptionalFields
 *
 * Toggle 'login' div based on Authentication Required checkbox.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
ToggleOptionalFields = function _ToggleOptionalFields()
{
   if ($('[name="authRequired"]').is(':checked')) {
      $('#login').show();
   }
   else {
      $('#login').hide();
   }
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.ToDataTable
 *
 * Bind the table for application info editing to the table wrapper object.
 * It needs to disable datatable's paginating since jQuery validator
 * validates rows from the current page only.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
ToDataTable = function _ToDataTable(table)
{
   var tableOpts = {
         "bPaginate": false,
         "bProcessing": true,
         "bJQueryUI": true,
         "bScrollCollapse": false
      };
   this.CreateTableWrapper(table, tableOpts);
};


/**
 * -----------------------------------------------------------------------------
 * FileShareManager.disableBasicInfoFields
 *
 * Disable all basic info fields except name and 'Ok to convert' checkbox.
 * -----------------------------------------------------------------------------
 */
FileShareManager.prototype.
disableBasicInfoFields = function _disableBasicInfoFields()
{
   $('[name="serverPath"]').attr("disabled", true);
   $('[name="authRequired"]').attr("disabled", true);
   $('[name="authUsername"]').attr("disabled", true);
   $('[name="authPassword"]').attr("disabled", true);
};
