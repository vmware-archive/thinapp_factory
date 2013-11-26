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
 * function NotificationManager
 *
 * A "class" that encapsulates all the methods for dealing with the Notification.
 * -----------------------------------------------------------------------------
 */
function NotificationManager(table)
{
   this.taskProgressHolder = $('#task-progress-holder');
   this.actionAlertHolder = $('#action-alert-holder');
   this.isInit = true;
   this.maxEvents = 50;

   /* When "notification-show" is clicked, show the notification table. */
   $("#notification-show").click(function() {
      /* Don't use toggle to properly work in IE8 */
      $('#notification-status').addClass('no-show');
      $('#notification-tab').removeClass('no-show');
      $('#notification').animate({'height': '14em'}, 'fast');

      /* Rebuild the notification table header column width. This is needed when sScrollY is set */
      VmTAF.notification.tableWrapper.dataTable.fnAdjustColumnSizing();
   });

   /* When "notification-hide" is clicked, hide the notification table. */
   $("#notification-hide").click(function() {
      /* Don't use toggle to properly work in IE8 */
      $('#notification-status').removeClass('no-show');
      $('#notification-tab').addClass('no-show');
      $('#notification').animate({'height': '1.4em'}, 'fast');
   });

   // Initialize datatable for the notification events.
   if (table) {
      this.CreateTableWrapper(table, {
         bJQueryUI: true,
         bFilter: false,
         bPaginate: false,
         bLengthChange: false,
         firstColumnSortable : true,
         aaSorting: [[2, 'asc']], // sort by 'When' column
         aoColumns: [
            null,
            null,
            { sType: "title-numeric" }
         ],
         bAutoWidth: true,
         sScrollY: '123px',
         //sScrollXInner: "100%",
         bScrollCollapse: true,
         bAutoWidth: true,
         sDom: 't<"clear">'
      });
      // Poll events in every 5 sec
      this.poll(5000);
   }

   this.icons = {
      'info': this.createIcon('info'),
      'warn': this.createIcon('warn'),
      'error': this.createIcon('error')
   };

   // Initiate the task progress display from here.
   this.PollAndPaintTaskProgress(3000);

   // update relative timestamps on the table and popup area
   setInterval(jQuery.proxy(AfRefreshTimestamps,$('#notification')), 60000);
}

NotificationManager.prototype = new AbstractManager('Notification');

/**
 * -----------------------------------------------------------------------------
 * NotificationManager.poll
 *
 * Poll new events every 'refreshInterval'.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
poll = function _poll(refreshInterval)
{
   this.UpdateDataTable({
      url: '/api/notification',
      dataHandler: this.updateEvents,
      repeatMs: refreshInterval,
      showRefreshFlag: false,
      /*
       * if the cache is true (default), the first 304 response
       * will have the same content from browser cache, which is
       * the same response from previous 200 response.
       */
      cache: false,
      ifModified: true // Enable 304 not modified status check
   });
};

/**
 * -----------------------------------------------------------------------------
 * NotificationManager.updateEvents
 *
 * Add new events to the events array and pop up alerts for new events.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
updateEvents = function _updateEvents(self, data, options)
{
   if (!data) {
      return;
   }
   /* Loading first time during page refresh. */
   if (self.isInit) {
      self.createEventTable(data);
      self.isInit = false;
      return;
   }

   var event = null;
   for (var i = 0; i < data.length; i++) {
      event = data[i];

      var title = 'Alert';
      if (event.type == 'warn') title = 'Warning';
      else if (event.type == 'error') title = 'Error';

      AfNotify(title, event.description, event.type);
      self.AddEvent(event);
   } // end for loop

   /* Set the last event as the status alert */
   self.createStatusAlert(event);

   /* check the table size, delete row(s) if needed. */
   while (self.tableWrapper.GetNumRows() > self.maxEvents) {
      // Delete first row from the table.
      self.tableWrapper.DeleteRow(0);
   }

};

/**
 * -----------------------------------------------------------------------------
 * NotificationManager.createRows
 *
 * Create rows of the datatable with the events recieved by the server
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
createEventTable = function _createEventTable(events)
{
   if (!events) {
      return;
   }
   // Remove all data rows from the table.
   this.tableWrapper.ClearTable();

   var event = null;
   // Add a row per event
   for (var i = 0; i < events.length; i++) {
      event = events[i];
      this.tableWrapper.AddRow(this.createRow(event), event);
   }

   // Set the last event as the status alert
   this.createStatusAlert(event);

   this.tableWrapper.DrawTable();
};

/**
 * -----------------------------------------------------------------------------
 * NotificationManager.AddEvent
 *
 * Add a given event to the datatable.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
AddEvent = function _addEvent(event)
{
   var row = this.createRow(event);
   this.tableWrapper.AddRow(row, event);
   this.tableWrapper.DrawTable();
};

/**
 * -----------------------------------------------------------------------------
 * NotificationManager.createRow
 *
 * Create an event row for the event datatable.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
createRow = function _createRow(event)
{
   var row = new Array();
   var icon = this.icons[event.type];
   var component = null;

   switch (event.component) {
      case 'feeds': component = 'Feeds'; break;
      case 'fileshares': component = 'File Shares'; break;
      case 'autoCapture': component = 'Auto Conversion'; break;
      case 'manualCapture': component = 'Manual Conversion'; break;
      case 'builds': component = 'Builds'; break;
      case 'publishingApps': component = 'Publishing Applications'; break;
      case 'recipes': component = 'Recipes'; break;
      case 'workpool': component = 'Workpool'; break;
      case 'config': component = 'Configuration'; break;
      default: component = 'Other';
   }

   row.push(AfHtml(icon) + event.description);
   row.push(component);
   row.push(AfTimeago(event.timeStamp));

   return row;
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.createIcon
 *
 * Private method to create an event icon based on a given event type.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
createIcon = function _createIcon(type)
{
   // Add generic icon class, and the type if passed, info by default.
   return $('<div></div>').addClass(type).addClass('icon');
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.createStatusAlert
 *
 * Create a status alert text with an icon, description and created time stamp.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
createStatusAlert = function _createStatusAlert(event)
{
   if (event) {
      var time = '  - ' + AfTimeago(event.timeStamp);

      $('#recent-event')
         .empty()
         .append(this.icons[event.type])
         .append( $("<div class='recent-event-msg'></div>").append(event.description.substring(0, 140)+time));
   }
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.PollAndPaintTaskProgress
 *
 * This function performs the repeated call to fetch and paint the task status
 * every pollInterval times.
 *
 * @param pollInterval - Frequency between each poll & Paint.
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
PollAndPaintTaskProgress = function _pollAndPaintTaskProgress(pollInterval)
{
   var self = this;
   var frequency = (pollInterval)? pollInterval : 3000;

   AfAjax({
      method: 'GET',
      url: '/api/notify/alertAndProgress',

      success: function(data) {
         if (data) {
            self.updateActionAlertWidget(data);
            self.updateTaskProgressWidget(data);
         }
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfLog('Progress fetch failed: ' + textStatus + errorThrown);
      },

      complete: function() {
         // Poll for task progress again after pollInterval.
         setTimeout(function() {
            self.PollAndPaintTaskProgress(frequency);
         }, frequency);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.updateTaskProgressWidget
 *
 * Shows / hides an overall task progress widget to the bottom left. If there
 * are no running tasks, the progress is hidden, if not, then its shown.
 *
 * It shows the following:
 *   1. Number of running tasks.
 *   2. Avg progress percent of running task.
 *   3. Number of waiting tasks.
 *
 * @param data
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
updateTaskProgressWidget = function _updateTaskProgressWidget(data)
{
   if (this.taskProgressHolder) {
      // If runTotal exists, then display, else hide task progress.
      if (data.runTotal) {
         var newSummary = $('<div></div>').addClass('summary');

         // Compute the top desc and add it to the task summary.
         var temp = $('<div></div>').addClass('top-desc')
            .append(this.getPluralizedMsg(
                  'T.TASKS.CAPTURE_TOP_MSG',
                  data.runTotal));

         var button = $('<button>').addClass('button-link')
            .text(AfTranslate('T.TASKS.CAPTURE_VIEW'));
         button.bind(
            'click',
            function() {
               VmTAF.contentNavigator.LoadPage("/tasks/index?metaStatus=RUNNING");
         });

         // Link to running tasks
         temp.append(button);
         newSummary.append(temp);

         // Create a new progress bar for the progress percent.
         temp = AfProgressBar({
            widthPercent: data.runTotalProgress,
            styleClass: 'green'
         });
         temp = $('<div>').addClass('pbar').append(temp);
         // Displays an icon to the right of the progress bar.
         //var icon = $('<div>').addClass('pause').attr('title', 'Not Implemented');

         newSummary.append($('<div>').append(temp));

         // Append waiting task count if there are any.
         temp = $('<div></div>').addClass('bottom-desc')
            .append(this.getPluralizedMsg(
                  'T.TASKS.CAPTURE_BOTTOM_MSG',
                  data.waitTotal || '0'));
         newSummary.append(temp);

         this.taskProgressHolder.empty().append(newSummary).show();
      }
      else {
         this.taskProgressHolder.empty();
         this.taskProgressHolder.hide();
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.updateActionAlertWidget
 *
 * Shows / hides task progress widget to the bottom left. If there
 * are no running tasks, the progress is hidden, if not, then its shown.
 *
 * Stack up alerts in the order received.
 *
 * @param data
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
updateActionAlertWidget = function _updateActionAlertWidget(data)
{
   var groupUri = {
         "datastore" : "/admin/settings#1",
         "capture" : "/tasks/index?metaStatus=FINISHED",
         "build" : "/builds/index?metaStatus=FINISHED",
         "workpool" : "/settings#0",
         "image" : "/workpools/image",
         "feed" : "/settings#5"
   };

   if (this.actionAlertHolder && data.actionList && data.actionList.length > 0) {
      // If runTotal exists, then display, else hide task progress.
      var tempHolder = $('<div></div>');
      for (var i=0; i < data.actionList.length; i++) {
         var action = data.actionList[i];
         var alertDiv = $('<div></div>').addClass('action-alert').addClass(action.type);

         // Get translated alert string by passing count.
         var anchor = $('<a>');

         if (action.group == 'manualCapture') {
            anchor.addClass(action.group);
            // note: a HREF attribute is always required on an anchor tag
            anchor.attr('href', '#');
            anchor.text(AfTranslate(
                  'T.ALERT.GROUP.' + action.group, action.info[1]));
            // Attach event handler for this anchor.
            anchor.click(function(event) {
               event.preventDefault();
               AfOpenManualCaptureWindow(VmTAF.contextPath + action.info[0], "Manual_Capture");
            });
         }
         else {
            anchor.attr('href', VmTAF.contextPath + groupUri[action.group]);
            anchor.text(this.getPluralizedMsg(
                  'T.ALERT.GROUP.' + action.group,
                  action.count));
         }

         // Append the icon and the anchor to alertDiv.
         alertDiv.append($('<div></div>').addClass('icon')).append(anchor);
         tempHolder.append(alertDiv);
      }
      this.actionAlertHolder.empty().append(tempHolder).show();
   }
   else {
      this.actionAlertHolder.empty();
      this.actionAlertHolder.hide();
   }
};


/**
 * -----------------------------------------------------------------------------
 * NotificationManager.getPluralizedAlert
 *
 * Get pluralized alert message based on count > 1 or not.
 *
 * @param group
 * @param count
 * -----------------------------------------------------------------------------
 */
NotificationManager.prototype.
getPluralizedMsg = function _getPluralizedMsg(group, count)
{
   if (count == 0) {
      return AfTranslate(group + '.ZERO');
   }
   if (count == 1) {
      return AfTranslate(group + '.ONE');
   }
   return AfTranslate(group, count);
};
