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
 * FeedManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with feeds.
 * -----------------------------------------------------------------------------
 */
function FeedManager(table, feedId)
{
   if (table) {
      if (feedId) {
         // The datatable used to display the feed applications on edit feed page.
         var tableOpts = {
               sScrollY : '350px',
               bScrollCollapse: true,
               aoColumns: [
                  null,
                  null,
                  null,
                  null,
                  null,
                  { sType: "title-numeric" },
                  null
               ]
            };
      }
      else {
         this.feedsRefreshUrl = '/api/feeds?sort=true';
         // this tells Datatables that columns 5 and 6 are special strings encased in a <span>.
         // The 'title' attribute of the span has a numeric value, which can easily be sorted.
         // e.g. <span title="12">12 / 42 apps published</span>
         var tableOpts = {
            aoColumns: [
               null,
               null,
               null,
               null,
               { sType: "title-numeric" },
               { sType: "title-numeric" },
               null
            ]
         };
      }
      this.CreateTableWrapper(table, tableOpts);
   }
}

FeedManager.prototype = new AbstractManager('Feeds');


/**
 * -----------------------------------------------------------------------------
 * FeedManager.UpdateFeeds
 *
 * Public function to load data from the server into the feeds table.
 *
 * @param table HTML "table" element that displays all the feeds.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
UpdateFeeds = function _UpdateFeeds(refreshInterval)
{
   this.UpdateDataTable({
      dataHandler: this.createRows,
      table: this.tableWrapper.dataTable,
      url: this.feedsRefreshUrl,
      repeatMs : refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.FEEDS.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * FeedManager.GetFeedAndPopulateForm
 *
 * Fetch the specified feed from the server, and display it in the feed
 * edit form.
 *
 * @param feedId ID of feed to be edited.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
GetFeedAndPopulateForm = function(feedId)
{
   var self = this;

   AfAjax({
      method: 'GET',
      url: '/api/feeds/' + feedId + '?sort=true',
      success: function(data) {
         self.PopulateEditForm(data);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FeedManager.PopulateEditForm
 *
 * Public function to fill out the feed edit view with the given
 * feed (JSON format).
 *
 * @param feed Feed (JSON) to be displayed.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
PopulateEditForm = function _PopulateEditForm(feed)
{
   $('[name="name"]').val(feed.name);
   $('[name="url"]').val(feed.url);
   $('[name="description"]').val(feed.description.content);
   $('[name="okToScan"]').attr('checked', feed.okToScan);
   $('[name="okToConvert"]').attr('checked', feed.okToConvert);
   $('[name="authRequired"]').attr('checked', feed.httpAuthRequired);
   $('[name="authUsername"]').val(feed.httpUser);
   $('[name="authPassword"]').val(feed.httpPassword);

   // Remove any old rows.
   this.tableWrapper.ClearTable();

   var rows = new Array(feed.applications.length);
   for (var i = 0; i < feed.applications.length; i++) {
      var app = feed.applications[i];

      // Check-box html to select applications.
      var cbHtml = '<input type="checkbox" subproperty="appIncludes" name="' + app.id + '"';
      if (!app.skipped) {
         cbHtml += ' CHECKED ';
      }
      cbHtml += '></input>';

      var cats = '';
      for (var ci = 0; ci < app.categories.length; ci++) {
         if (ci > 0) cats += ', ';
         cats += app.categories[ci];
      }

      rows[i] = {
            0: AfHtml(cbHtml),
            1: AfCreateApplicationBlock(app),
            2: AfHtml(app.vendor),
            3: AfHtml(cats),
            4: AfHtml(app.locale),
            5: app.installerRevision,
            6: app.failCount
      };
   }
   // Now draw the table.
   this.tableWrapper.ReplaceData(rows, feed.applications);

   // Apply additional checkbox change handlers for these check-boxes.
   // The triggers for this is when something changes on row check-box or master check-box.
   this.tableWrapper.dataTable.find('tbody td:first-child input:checkbox')
      .bind(TableWrapper.ROW_CHECKBOX_CHANGE, function(event) {
         if ($(this).is(':checked')) {
            $(this).parents('tr').children('td:gt(0)').removeClass('excluded');
         } else {
            $(this).parents('tr').children('td:gt(0)').addClass('excluded');
         }
    });

   // Trigger the above event to apply css changes for unchecked rows.
   this.tableWrapper.dataTable.find('tbody td input:checkbox').not(':checked')
      .trigger(TableWrapper.ROW_CHECKBOX_CHANGE);
};

/**
 * -----------------------------------------------------------------------------
 * FeedManager.DeleteSelectedFeeds
 *
 * Public function to delete all feeds selected in the given
 * feed table.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
DeleteSelectedFeeds = function _DeleteSelectedFeeds()
{
   var self = this;

   /* Get the feed IDs that are selected. */
   var feeds = self.tableWrapper.GetSelectedRowData();
   if (feeds.length == 0) {
      AfErrorT(
         'T.FEEDS.NO_SELECTION',
         'M.FEEDS.NO_SELECTION');
      return;
   }

   /* Check with the user. */
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete ' +
         AfThisOrThese(feeds, 'feed') + '?')) {
      return;
   }

   /* Delete each feed */
   for (var i = 0, counter = 0; i < feeds.length; i++) {
      AfAjax({
         method: 'DELETE',
         url: '/api/feeds/' + feeds[i].id,

         error: function _onError(jqXHR, textStatus, errorThrown) {
            /**
             * FIXME: Relying on HTTP 403 forbidden for a specific error message is ugly.
             */
            if (errorThrown === 'Forbidden') {
               AfError('Not Allowed', 'Failed to delete the feed.\n\n'
                     + 'NOTE: Deleting an internal feed is prohibited. Please delete its file share instead.');
            } else {
               AfError('Error', 'Failed to delete the feed: ' + errorThrown);
            }
         },

         /* When complete, end the blocker for the last feed */
         complete: function() {
            counter++;
            if (counter == feeds.length) {
               /* Update immediately */
               self.PopulateDataTableNow({
                  dataHandler: self.createRows,
                  url: self.feedsRefreshUrl,
                  table: self.tableWrapper.dataTable
               });
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * FeedManager.ResetSelectedFeeds
 *
 * Clears any error state from a feed. This will make sure it gets scanned
 * again at the next opportunity.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
ResetSelectedFeeds = function _ResetSelectedFeeds()
{
   var self = this;

   /* Get the feed IDs that are selected. */
   var feeds = self.tableWrapper.GetSelectedRowData();
   if (feeds.length == 0) {
      AfErrorT(
         'T.FEEDS.NO_SELECTION',
         'M.FEEDS.NO_SELECTION');
      return;
   }

   /* Reset each feed */
   for (var i = 0, counter = 0; i < feeds.length; i++) {
      AfAjax({
         method: 'PUT',
         url: '/api/feeds/' + feeds[i].id + '/reset',

         /* When complete, end the blocker for the last feed */
         complete: function() {
            counter++;
            if (counter == feeds.length) {
               /* Now the resets are done, force a refresh the table. */
               self.PopulateDataTableNow({
                     url: self.feedsRefreshUrl,
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
 * FeedManager.ScanSelectedFeeds
 *
 * Creates a new task for each selected feed.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
ScanSelectedFeeds = function _ScanSelectedFeeds()
{
   var self = this;

   /* Get the feed IDs that are selected. */
   var feeds = self.tableWrapper.GetSelectedRowData();
   if (feeds.length == 0) {
      AfErrorT(
         'T.FEEDS.NO_SELECTION',
         'M.FEEDS.NO_SELECTION');
      return;
   }

   for (var i = 0, counter = 0; i < feeds.length; i++) {
      AfAjax({
         method: 'PUT',
         url: '/api/feeds/' + feeds[i].id + '/scan',

         /* Display success notification */
         success: function() {
            AfNotifyT('T.FEEDS.SCAN_SUCCESS', 'M.FEEDS.SCAN_SUCCESS');
         },

         /* Only catch & notify failure case when feed is already scanned */
         error: function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 409 && 'FEED_TASK_EXISTS' == jqXHR.responseText) {
               AfNotifyT('T.FEEDS.SCAN_FAILURE_TASK_EXIST',
                         'M.FEEDS.SCAN_FAILURE_TASK_EXIST',
                         'warn');
            }
            else {
               // Use default handler in all other cases
               AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown);
            }
         },

         /* When complete, end the blocker for the last feed and uncheck rows */
         complete: function() {
            counter++;
            if (counter == feeds.length) {
               self.tableWrapper.MarkAllRowCheckboxes(false);
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * FeedManager.SubmitFeedForm
 *
 * Submit the feed edit form for creating new feeds, or updating existing ones.
 * @param form The feed edit form.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
SubmitFeedForm = function _submitFeedForm(form)
{
   var json = AfSerializeForm(form);
   if (!json.name) {
      json.name = json.url;
   }

   /* Grab the hidden feed ID from the form and delete it */
   var feedId = json['feedId'];
   delete json['feedId'];

   var method = (feedId ? 'PUT' : 'POST');
   var url = (feedId ? '/api/feeds/' + feedId : '/api/feeds');

   AfAjax({
      method: method,
      contentType: 'application/json',
      url: url,
      data: json,

      error: function _onError(jqXHR, textStatus, errorThrown) {
         AfError('Error', 'Failed to save the feed: ' + jqXHR.responseText);
      },

      success: function() {
         AfNotifyT('T.COMMON.SUCCESS', 'M.FEEDS.SAVED');
         if (window.VmTAF.dialogHolder) {
            window.VmTAF.dialogHolder.dialog("close");
         } else {
            VmTAF.contentNavigator.GoBack();
         }
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * FeedManager.createRows
 *
 * Function to update the rows of the feed table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the feeds.
 * @param jsonData JSON-formatted feed summary data.
 * @param args Arguments passed to UpdateDataTable.
 * -----------------------------------------------------------------------------
 */
FeedManager.prototype.
createRows = function _createRows(self, jsonData, args)
{
   var feeds = jsonData;

   // To avoid memory leaks, unbind prior event handlers before clearing table
   $('.edit-link').unbind('click');
   $('.error-link').unbind('click');

   /* Remove all data rows from the table. */
   self.tableWrapper.ClearTable();

   // Add a row per feed.
   for (var i = 0; i < feeds.length; i++) {
      var feed = feeds[i];

      // Check-box to select this feed: when clicked, possibly
      // pause table updates.
      var cb = $('<input>');
      cb.attr('type', 'checkbox');

      /* Link for the feed name: opens the feed editor. */
      var link = self.createRecordLink(
            feed.name,
            feed.id,
            '/feeds/edit/' + feed.id);

      /* Text for the 'applications' column */
      var appCount = '<span title="' + feed.numIncludedApplications + '">' +
                     + feed.numIncludedApplications + ' of ' + feed.applications.length
                     + '</span>';

      var DISABLED_SPAN = '<span title="-1">Disabled</span>';

      /* Values for 'last scan' and 'last convert' */
      var lastScan = (feed.okToScan ? AfTimeago(feed.lastScanMillis) : DISABLED_SPAN);
      var lastConvert = (feed.okToConvert ? AfTimeago(feed.lastConversion) : DISABLED_SPAN);

      var status = 'OK';
      if (feed.failure) {
         if (feed.failure.summary == "Scanning") {
            status = feed.failure.summary;
         } else {
            status = self.createAlertLink(
               'Error',
               "Feed Scan Error",
               feed.failure.summary);
         }
      }

      /* Create a row */
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(link));
      row.push(appCount);
      row.push(feed.description.content);
      row.push(lastScan);
      row.push(lastConvert);
      row.push(AfHtml(status));

      /* Add row to the table. */
      self.tableWrapper.AddRow(row, feed);
   }

   self.tableWrapper.DrawTable();
};
