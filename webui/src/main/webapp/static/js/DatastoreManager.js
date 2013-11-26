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
 * DatastoreManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with datastores.
 * -----------------------------------------------------------------------------
 */
function DatastoreManager(table, status)
{
   this.refreshUrl = '/api/datastores';
   if (status) {
      this.refreshUrl += '?status=' + status;
   }

   if (table) {
      this.CreateTableWrapper(table);
   }
}

DatastoreManager.prototype = new AbstractManager('Datastores');


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.UpdateDatastores
 *
 * Public function to load data from the server into the datastores table.
 *
 * @param table HTML "table" element that displays all the datastores.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
UpdateDatastores = function _updateDatastores(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.COMMON.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.GetDatastoreAndPopulateEditForm
 *
 * Fetch the named datastore from the server, and if successful load that
 * into the datastore edit form.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
GetDatastoreAndPopulateEditForm = function _PopulateEditForm(dsId)
{
   var self = this;

   AfAjax({
      method: 'GET',
      url: '/api/datastores/' + dsId,
      success: function(data) {
         self.PopulateEditForm(data);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.PopulateEditForm
 *
 * Public function to fill out the datastore edit view with the given
 * datastore (JSON format).
 *
 * @param datastore Datastore (JSON) to be displayed.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
PopulateEditForm = function _PopulateEditForm(datastore)
{
   $('[name="name"]').val(datastore.name);
   $('[name="type"]').val(datastore.type);
   $('[name="serverShare"]').val(this.joinServerShare(datastore));
   $('[name="username"]').val(datastore.username);
   $('[name="password"]').val(datastore.password);
   $('[name="mountAtBoot"]').attr('checked', datastore.mountAtBoot);
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.DeleteSelectedDatastores
 *
 * Public function to delete all datastores selected in the given
 * datastore table.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
DeleteSelectedDatastores = function _DeleteSelectedDatastores()
{
   var self = this;
   var datastores = this.tableWrapper.GetSelectedRowData();

   if (datastores.length == 0) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.FILE_SHARES.NO_SELECTION');
      return;
   }

   /* Check with the user. */
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete ' +
         AfThisOrThese(datastores, 'datastore') + '?')) {
      return;
   }

   /* Delete each datastore */
   var numOk = 0;
   var numErr = 0;
   var errStr = '';
   for (var i = 0; i < datastores.length; i++) {
      /* Don't bother requesting a delete if not offline */
      if (datastores[i].status != "offline") {
         AfErrorT(
            "T.COMMON.ERROR",
            "M.STORAGE.CANT_DELETE_IF_NOT_OFFLINE");
         continue;
      }

      /* Check if there are any projects or uploaded apps */
      if(!self.CheckUsage(datastores[i])) {
         continue;
      }

      AfAjax({
         method: 'DELETE',
         url: '/api/datastores/' + datastores[i].id,

         beforeSend: function() {
            AfStartPleaseWait("Deleting datastores ... please wait", { total: datastores.length });
         },

         success: function() {
            numOk++;
         },

         error: function(xhr) {
            numErr++;
            errStr = errStr + '<p><hr>'+ xhr.responseText + '</p>';
         },

         complete: function() {
            if (AfEndPleaseWait()) {
               if (numErr > 0) {
                  AfError('Error', 'Error while deleting datastore.<p><b>Details:</b></p>' + errStr);
               } else if (numOk > 0) {
                  AfNotifyT('T.COMMON.SUCCESS', 'M.FILE_SHARES.DELETED');
                  self.tableWrapper.MarkAllRowCheckboxes(false);
                  /* Now the deletes are done, force a refresh the table. */
                  self.PopulateDataTableNow({
                     url: self.refreshUrl,
                     table: self.tableWrapper.dataTable,
                     dataHandler: self.createRows
                  });
               }
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.SetSelectedDatastoresStatus
 *
 * Public function to delete all datastores selected in the given
 * datastore table.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
SetSelectedDatastoresStatus = function _SetSelectedDatastoresStatus(status)
{
   var self = this;
   var datastores = this.tableWrapper.GetSelectedRowData();

   if (datastores.length == 0) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.STORAGE.NO_SELECTION');
      return;
   }

   var numOK = 0;
   var numUnknownErr = 0;
   /* Update each datastore */
   for (var i = 0; i < datastores.length; i++) {
      AfAjax({
         method: 'PUT',
         url: '/api/datastores/' + datastores[i].id + '/' + status,

         success: function() {
            numOK++;
         },

         error: function(xhr) {
            if (xhr && xhr.responseText != '') {
               AfError(xhr.responseText);
            } else {
               numUnknownErr++;
            }
         },

         complete: function() {
            if (numOK > 0) {
               /* Now the updates are done, force a refresh the table. */
               self.PopulateDataTableNow({
                  url: self.refreshUrl,
                  table: self.tableWrapper.dataTable,
                  dataHandler: self.createRows
               });
            }
            if (numUnknownErr > 0) {
               AfErrorT('T.COMMON.ERROR', 'M.STORAGE.STATUS_CHANGED_FAILED');
            }
         }
      });
   }
   this.tableWrapper.MarkAllRowCheckboxes(false);
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.SubmitEditForm
 *
 * Submit the datastore edit form for updating an existing datastore.
 * @param form
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
SubmitEditForm = function _SubmitEditForm(form)
{
   var json = AfSerializeForm(form);

   /* Change 'serverShare' into 'server' and 'share' */
   AfSplitServerShare(json);

   var datastoreId = json['datastoreId'];
   delete json['datastoreId'];

   AfLog('submitting edit form for datastore ' + datastoreId);
   AfLog(json);

   AfAjax({
      method: 'PUT',
      contentType: 'application/json',
      url: '/api/datastores/' + datastoreId,
      data: json,

      success: function() {
         AfNotifyT('T.STORAGE.SAVED', 'M.STORAGE.SAVED');
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
 * DatastoreManager.CheckUsage
 *
 * Check datastore usage: number of projects and applcations stored on the
 * datastore.
 * @param id a datastore id.
 * @return true if the lookup found any projects or applications and the user
 * confirmed to delete the datastore. Otherwise, it will returns false
 * if the user declined the confirmation.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
CheckUsage = function _checkUsage(ds)
{
   var response = null;
   AfAjax({
      method: 'GET',
      contentType: 'application/json',
      url: '/api/datastores/' + ds.id + '/usage',
      async: false,
      success: function(data) {
         response = data;
      },
      error: function() {
         AfLog('Failed to get the datastore usage');
      }
   });

   if (!response) {
      // Ignore and continue if the call failed.
      return true;
   }

   var msg = null;
   if (response.numProjects > 0) {
      msg = response.numProjects + AfPluralOf(' Project', response.numProjects);
   }

   if (response.numApplications > 0) {
      if (msg) {
         msg += ' and ';
      } else {
         msg = '';
      }
      msg += response.numApplications + AfPluralOf(' Application', response.numApplications);
   }

   if (msg) {
      if (!AfConfirm(
            'Confirm Delete',
            'Found ' + msg + ' on ' + ds.name + ' datastore! ' +
            'Do you still want to delete it?')) {
         return false;
      }
   }
   return true;
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.SubmitNewDatastoreForm
 *
 * Invoked when the add new Datastore is saved.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
SubmitNewDatastoreForm = function _submitNewDatastoreForm()
{
   var self = this;
   AfLog('submitting new datastore');

   // Convert form data into JSON
   var json = AfSerializeForm($('#datastore-form'));
   AfSplitServerShare(json);

   AfLog('built json from form:');
   AfLog(json);

   // Send create request to AppFactory
   AfAjax({
      method: 'POST',
      url: '/api/datastores',
      data: json,
      contentType: 'application/json',

      success: function() {
         AfNotifyT('T.STORAGE.SAVED', 'M.STORAGE.SAVED');
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
 * DatastoreManager.createRows
 *
 * Function to update the rows of the datastore table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the datastores.
 * @param jsonData JSON-formatted datastore summary data.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
createRows = function _createRows(self, jsonData, options)
{
   var datastores = jsonData.datastores;
   var defaultId = jsonData['defaultId'];

   // Remove all click handlers if they were applied earlier.
   $('.default-button').unbind("click");

   // Remove all data rows from the table
   self.tableWrapper.ClearTable();

   // Add a row per datastore.
   for (var i = 0; i < datastores.length; i++) {
      var datastore = datastores[i];

      // Check-box to select this datastore: when clicked, possibly
      // pause table updates. Only modifiable datastores can be
      // set offline or deleted.
      var cb = '';
      if (datastore.modifiable) {
         cb = $('<input type="checkbox">');
      }

      // Link to the datastore editor. Only online modifiable datastores
      // can be edited.
      var link = self.createRecordLink(
            datastore.name,
            datastore.id,
            (datastore.status == "offline" && datastore.modifiable) ? '/datastores/edit/' + datastore.id : null);

      // Status: in a <span> so we can pick them out
      var status = $('<span>').text('--');
      if (datastore.status) {
         status.text(datastore.status);
         if (datastore.status != 'online') {
            status.addClass('inactive');
         }
      }

      // Construct datastore path
      // This will be empty for datastores that have no path.
      var path = self.joinServerShare(datastore);

      // Free space (online only)
      var freeBar = 'unavailable';
      if (datastore.status == 'online') {
         freeBar = self.computeAndCreateFreeSpaceBar(datastore.used, datastore.size);
      }

      // Radio button for the 'default' datastore. Only online, writable
      // datastores can be selected for output.
      var rb = '';
      if (datastore.status == 'online' && datastore.writable) {
         // Thanks to datatable, we have to build the object with one string
         var rbStr = '<input type="radio" name="default" value="' + datastore.id + '"';
         if (datastore.id == defaultId) {
            rbStr += ' checked';
         }
         rbStr += ' class="default-button">';
         rb = rbStr;
      }

      // Create a row
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(link));
      row.push(AfHtml(status));
      row.push(path);
      row.push(AfHtml(freeBar));
      row.push(AfHtml(rb));

      // Add row to the table.
      self.tableWrapper.AddRow(row, datastore);
   }

   /* Redraw the table with all its rows */
   self.tableWrapper.DrawTable();

   /* Remove old ones and add click event listeners to default links */
   // Note: Must be done AFTER the table has been drawn
   self.tableWrapper.dataTable.find('.default-button').click(function() {
      var dsId = $(this).attr('value');
      self.setDefaultDatastore(dsId, true);
   });

   // (bug 848929): Apply 'inactive' class to all but the first td whenever a td with inactive class exists on a row
   // (bug 838782): only look in the current table, as the Workpool Manager uses the same CSS class
   self.tableWrapper.dataTable.find('.inactive').parents('tr').children('td:not(:first-child)').addClass('inactive');
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.computeAndCreateFreeSpaceBar
 *
 * Use the datastore data (used space, total size) in bytes to compute a
 * "free space" bar. Converts the values into the most convenient unit
 * (kb, Mb, etc).
 *
 * @param used Disk space used (bytes)
 * @param size Disk space total (bytes)
 * @returns A progress bar element.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
computeAndCreateFreeSpaceBar = function _computeAndCreateFreeSpaceBar(used, size)
{
   // Free space percentage.
   var free = size - used;
   var freePc = (size == 0) ? 0 : Math.round(100 * free / size);

   var freeBar = AfCreateProgressBarDivColor(
         AfSimplifyDiskSize(free) + ' (' + freePc + '%)',
         freePc, 'red', 5, 'orange', 20, 'green');

   return freeBar;
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.setDefaultDatastore
 *
 * Send an Ajax request to the server to change the default datastore to the
 * one specified.
 *
 * @returns nothing
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
setDefaultDatastore = function(dsId, refresh)
{
   var self = this;

   AfLog('Setting default datastore to ' + dsId);

   AfAjax({
      method: 'PUT',
      url: '/api/datastores/' + dsId + '/default',

      success: function() {
         if (refresh) {
            self.PopulateDataTableNow({
               url: self.refreshUrl,
               table: self.tableWrapper.dataTable,
               dataHandler: self.createRows
            });
         }
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * DatastoreManager.joinServerShare
 *
 * Combine a datastore server and share path into a single string for display
 * in the datastore table/editor.
 * -----------------------------------------------------------------------------
 */
DatastoreManager.prototype.
joinServerShare = function(datastore)
{
   var join = datastore.server || "";

   if (datastore.server) {
      join += ":";
   }

   if (datastore.share) {
      if (datastore.share.length && datastore.share[0] != '/') {
         join += "/";
      }
      join += datastore.share;
   }

   return join;
};
