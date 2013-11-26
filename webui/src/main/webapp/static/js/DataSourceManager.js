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
 * DataSourceManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with data sources.
 *
 * TODO: remove this once we remove the old UI
 * -----------------------------------------------------------------------------
 */
function DataSourceManager(table)
{
   this.sourcesRefreshUrl = '/api/sources?sort=true';
   if (table) {
      var tableOpts = {
         aoColumns: [
            null,
            null,
            null,
            { sType: "title-numeric" },
            null,
            null,
            { sType: "title-numeric" },
            { sType: "title-numeric" },
            null
         ]
      };
      this.CreateTableWrapper(table, tableOpts);
   }
}

DataSourceManager.prototype = new AbstractManager('DataSources');


/**
 * -----------------------------------------------------------------------------
 * DataSourceManager.UpdateDataSources
 *
 * Public function to load data from the server into the sources table.
 *
 * @param table HTML "table" element that displays all the sources.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
DataSourceManager.prototype.
UpdateDataSources = function _UpdateDataSources(refreshInterval)
{
   this.UpdateDataTable({
      dataHandler: this.createRows,
      table: this.tableWrapper.dataTable,
      url: this.sourcesRefreshUrl,
      repeatMs : refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.COMMON.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * DataSourceManager.DeleteSelectedDataSources
 *
 * Public function to delete all sources selected in the given
 * source table.
 * -----------------------------------------------------------------------------
 */
DataSourceManager.prototype.
DeleteSelectedDataSources = function _DeleteSelectedDataSources()
{
   var self = this;

   /* Get the source IDs that are selected. */
   var sources = self.tableWrapper.GetSelectedRowData();
   if (sources.length == 0) {
      AfErrorT(
         'T.DATA_SOURCES.NO_SELECTION',
         'M.DATA_SOURCES.NO_SELECTION');
      return;
   }

   /* Check with the user. */
   // XXX If/when we decouple apps/recipes from sources, this warning
   // needs to be updated.
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete ' +
         AfThisOrThese(sources, 'source') + '? ' +
         'All related applications and recipes will also be deleted.')) {
      return;
   }

   /* Delete each source */
   for (var i = 0; i < sources.length; i++) {
      var url = null;
      switch(sources[i].type) {
         case "feed":
            url = "/api/feeds/" + sources[i].id;
            break;
         case "fileshare":
            url = "/api/fileshare/" + sources[i].id;
            break;
      }

      AfAjax({
         method: 'DELETE',
         url: url,

         /* Before send, start a blocker for the first source */
         beforeSend: function() {
            AfStartPleaseWaitT('M.DATA_SOURCES.DELETING');
         },

         error: function _onError(jqXHR, textStatus, errorThrown) {
            AfError('Error', 'Failed to delete data source: ' + errorThrown);
         },

         /* When complete, end the blocker for the last source */
         complete: function() {
            if (AfEndPleaseWait()) {
               /* Update immediately */
               self.PopulateDataTableNow({
                  dataHandler: self.createRows,
                  url: self.sourcesRefreshUrl,
                  table: self.tableWrapper.dataTable
               });
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * DataSourceManager.ResetSelectedDataSources
 *
 * Clears any error state from a source. This will make sure it gets scanned
 * again at the next opportunity.
 * -----------------------------------------------------------------------------
 */
DataSourceManager.prototype.
ResetSelectedDataSources = function _ResetSelectedDataSources()
{
   var self = this;

   /* Get the source IDs that are selected. */
   var sources = self.tableWrapper.GetSelectedRowData();
   if (sources.length == 0) {
      AfErrorT(
         'T.DATA_SOURCES.NO_SELECTION',
         'M.DATA_SOURCES.NO_SELECTION');
      return;
   }

   /* Reset each source */
   for (var i = 0, counter = 0; i < sources.length; i++) {
      var url = null;
      switch(sources[i].type) {
         case "feed":
            url = "/api/feeds/" + sources[i].id + "/reset";
            break;
         case "fileshare":
            url = "/api/fileshare/" + sources[i].id + "/reset";
            break;
      }

      AfAjax({
         method: 'PUT',
         url: url,

         /* When complete, end the blocker for the last source */
         complete: function() {
            counter++;
            if (counter == sources.length) {
               /* Now the resets are done, force a refresh the table. */
               self.PopulateDataTableNow({
                     url: self.sourcesRefreshUrl,
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
 * DataSourceManager.ScanSelectedDataSources
 *
 * Creates a new task for each selected source.
 * -----------------------------------------------------------------------------
 */
DataSourceManager.prototype.
ScanSelectedDataSources = function _ScanSelectedDataSources()
{
   var self = this;

   /* Get the source IDs that are selected. */
   var sources = self.tableWrapper.GetSelectedRowData();
   if (sources.length == 0) {
      AfErrorT(
         'T.DATA_SOURCES.NO_SELECTION',
         'M.DATA_SOURCES.NO_SELECTION');
      return;
   }

   for (var i = 0, counter = 0; i < sources.length; i++) {
      var url = null;
      var method = null;

      switch(sources[i].type) {
         case "feed":
            url = "/api/feeds/" + sources[i].id + "/scan";
            method = "PUT";
            break;
         case "fileshare":
            url = "/api/fileshare/" + sources[i].id + "/sync";
            method = "PUT";
            break;
      }

      AfAjax({
         method: method,
         url: url,

         /* Display success notification */
         success: function() {
            AfNotifyT('T.DATA_SOURCES.SCAN_SUCCESS', 'M.DATA_SOURCES.SCAN_SUCCESS');
         },

         /* Only catch & notify failure case when source is already scanned */
         error: function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 409 && 'FEED_TASK_EXISTS' == jqXHR.responseText) {
               AfNotifyT('T.DATA_SOURCES.SCAN_FAILURE_TASK_EXIST',
                         'M.DATA_SOURCES.SCAN_FAILURE_TASK_EXIST',
                         'warn');
            }
            else {
               // Use default handler in all other cases
               AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown);
            }
         },

         /* When complete, end the blocker for the last source and uncheck rows */
         complete: function() {
            counter++;
            // Invoke only when the last request completes
            if (counter == sources.length) {
               self.tableWrapper.MarkAllRowCheckboxes(false);
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * DataSourceManager.createRows
 *
 * Function to update the rows of the source table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the sources.
 * @param jsonData JSON-formatted source summary data.
 * @param args Arguments passed to UpdateDataTable.
 * -----------------------------------------------------------------------------
 */
DataSourceManager.prototype.
createRows = function _createRows(self, jsonData, args)
{
   var sources = jsonData;

   // To avoid memory leaks, unbind prior event handlers before clearing table
   $('.edit-link').unbind('click');
   $('.error-link').unbind('click');

   /* Remove all data rows from the table. */
   self.tableWrapper.ClearTable();

   // Add a row per source.
   for (var i = 0; i < sources.length; i++) {
      var source = sources[i];

      // Check-box to select this source: when clicked, possibly
      // pause table updates.
      var cb = $('<input>');
      cb.attr('type', 'checkbox');

      var type = AfCreateDataSourceSpan(source.type);

      /* Link for the source name: opens the source editor. */
      var link = null;
      switch (source.type) {
         case "feed":
            link = self.createRecordLink(
                  source.name,
                  source.id,
                  '/feeds/edit/' + source.id);
            break;

         case "fileshare":
            link = self.createRecordLink(
                  source.name,
                  source.id,
                  '/fileshare/edit/' + source.id);
            break;
      }

      /* Text for the 'applications' column */
      var appCount = '<span title="' + feed.numIncludedApplications + '">' +
         + feed.numIncludedApplications + ' of ' + feed.applications.length
         + '</span>';

      /* Text for the 'recipes' column */
      var recipeCount = source.numRecipes;

      var DISABLED_SPAN = '<span title="-1">Disabled</span>';

      /* Values for 'last scan' and 'last convert' */
      var lastScan = (source.okToScan ? AfTimeago(source.lastScanMillis) : DISABLED_SPAN);
      var lastConvert = (source.okToConvert ? AfTimeago(source.lastConversion) : DISABLED_SPAN);

      var status = 'OK';
      if (source.failure) {
         status = self.createAlertLink(
            'Error',
            'DataSource Error',
            source.failure.summary);
      }

      var desc = "";
      if (source.description && source.description.content) {
         desc = source.description.content;
      }

      /* Create a row */
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(type));
      row.push(AfHtml(link));
      row.push(appCount);
      row.push(recipeCount);
      row.push(desc);
      row.push(lastScan);
      row.push(lastConvert);
      row.push(AfHtml(status));

      /* Add row to the table. */
      self.tableWrapper.AddRow(row, source);
   }

   self.tableWrapper.DrawTable();
};
