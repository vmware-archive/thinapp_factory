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
 * function TaskManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with tasks.
 * -----------------------------------------------------------------------------
 */
function TaskManager(table, metaStatus)
{
   this.updateUrl = '/api/tasks?sort=true';
   if (metaStatus) {
      this.updateUrl += '&metaStatus=' + metaStatus;
   }

   if (table) {
      this.CreateTableWrapper(table, {
         iDisplayLength: 25,
         "aaSorting": [[1, 'desc']], // sort by task id
         aLengthMenu: [5, 25, 50, 100]
      });
   }
}

TaskManager.prototype = new AbstractManager('Tasks');


/**
 * -----------------------------------------------------------------------------
 * TaskManager.UpdateTasks
 *
 * Public function to load data from the server into the tasks table.
 *
 * @param table HTML "table" element that displays all the tasks.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
TaskManager.prototype.
UpdateTasks = function _updateTasks(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.updateUrl,
      dataHandler: this.createRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.FEEDS.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * TaskManager.Cleanup
 *
 * Sends a request to the server to clean up all jobs that can be cleaned.
 * This is defined by the server, but typically includes all jobs that
 * are not running (i.e. those that finished and those with an error).
 * -----------------------------------------------------------------------------
 */
TaskManager.prototype.
Cleanup = function _cleanup()
{
   var self = this;

   AfAjax({
      method: 'POST',
      url: '/api/tasks/cleanup',

      complete: function() {
         self.PopulateDataTableNow({
            url: self.updateUrl,
            table: self.tableWrapper.dataTable,
            dataHandler: self.createRows
         });
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * TaskManager.AbortSelectedTasks
 *
 * Send an abort request for each selected job in the table.
 * -----------------------------------------------------------------------------
 */
TaskManager.prototype.
AbortSelectedTasks = function _abortSelectedTasks()
{
   var self = this;
   var tasks = self.tableWrapper.GetSelectedRowData();

   for (var i = 0; i < tasks.length; i++) {
      var taskId = tasks[i].id;

      AfLog('cancelling task ' + taskId);
      AfAjax({
         url: '/api/tasks/' + taskId + '/abort',
         method: 'POST',

         beforeSend: function() {
            AfStartPleaseWaitT('M.TASKS.CANCELLING_WAIT');
         },

         complete: function() {
            if (AfEndPleaseWait()) {
               self.PopulateDataTableNow({
                  url: self.updateUrl,
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
 * TaskManager.createRows
 *
 * Public function to update the rows of the task table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the feeds.
 * @param jsonData JSON-formatted feed summary data.
 * @param args Arguments passed to UpdateDataTable.
 * -----------------------------------------------------------------------------
 */
TaskManager.prototype.
createRows = function _createRows(self, jsonData, options)
{
   var tasks = jsonData;
   var popupData = {};

   // To avoid memory leaks, unbind prior event handlers.
   $('.mm-link').unbind('click');
   $('.action-link').unbind('click');
   $('.popup-link').unbind('click');

   // Remove all data rows from the table.
   self.tableWrapper.ClearTable();

   // Add a row per task.
   for (var i = 0; i < tasks.length; i++) {
      var task = tasks[i];

      // Check-box to select this task.
      var cb = $('<input type="checkbox">');

      var status;
      if (task.progress >= 0) {
         // Show progress bar
         var color = null;
         if ('FINISHED' == task.queueStatus) {
            color = "#dddddd";
         }
         status = AfCreateProgressBarDiv(
               AfTranslate("T.TASKS.STATUS." + task.status) + ' (' + task.progress + '%)',
               task.progress,
               color);
         status = AfHtml(status);
      }
      else {
         // Show text only for non-running tasks
         status = AfTranslate("T.TASKS.STATUS." + task.status);
         if (task.statusLinkInfo) {
            /* Convert status text into a link */
            var link = $("<a/>")
               .text(status)
               .attr('link', task.statusLinkInfo.link)
               .attr('title', task.statusLinkInfo.tooltip);
            /* Require popup window for manual mode */
            if (task.type === 'MANUAL_MODE_BUILD' && task.status === 'WAITING_FOR_USER') {
               link.addClass('mm-link');
            } else {
               link.addClass('action-link');
            }
            status = AfHtml(link);
         } else if (task.statusPopupInfo) {
            var popupInfo;

            if (task.status === 'stalled') {
                popupInfo = {
                     title: 'Stalled Conversion Job',
                     message: AfTranslate(task.statusPopupInfo.message),
                     labels: task.statusPopupInfo.labels,
                     ajaxUrls: task.statusPopupInfo.ajaxUrls,
                     waitMessage: 'M.TASKS.IGNORE_STALL_WAIT'
               };
            } else {
               var message = '';
               var title = 'Task Error';

               if (task.status === 'cancelled') {
                  title = 'Cancelled Conversion Job';
               }

               if (task.statusPopupInfo.message) {
                  message = message + task.statusPopupInfo.message;
               } else if (task.status === 'cancelled') {
                  message = message + 'Cancelled Conversion Job';
               }

               /* Check for last running state */
               if (task.lastRunningState) {
                  message = message + '<br><br>' +
                     '<b>Last running state</b>: ' +
                     AfTranslate('T.TASKS.STATUS.FAILED.' + task.lastRunningState) +
                     '<br>';
               }

               /* Check for last command */
               if (task.lastCommand) {
                  message = message + '<br>' +
                     '<b>Last command label</b>: ' + task.lastCommand.label +
                     '<br><br><b>Last comand</b>: ' + task.lastCommand.command +
                     '<br>';
               }

               /* Check for last error */
               if (task.lastError) {
                  message = message + '<br>' +
                     '<b>Last error</b>: ' +
                     task.lastError.replace(/\n/g, '<br>') +
                     '<br>';
               }

               popupInfo = {
                     title: title,
                     message: message,
                     labels: task.statusPopupInfo.labels,
                     ajaxUrls: task.statusPopupInfo.ajaxUrls,
                     windowUrls: task.statusPopupInfo.windowUrls
               };
            }

            var link = $('<a/>')
               .text(status)
               .attr('popupId', task.id)
               .addClass('popup-link');
            status = AfHtml(link);
            popupData[task.id] = popupInfo;
         }
      }

      var row = new Array();
      row.push(AfHtml(cb));
      row.push(task.id || "unknown ID");
      row.push(AfTranslate("T.TASKS.TYPE."+task.type));
      row.push(self.makeDescription(task));
      row.push(status);
      row.push($.timeago(new Date(task.queued)));
      row.push((-1 == task.started) ? "n/a" : $.timeago(new Date(task.started)));
      row.push((-1 == task.finished) ? "n/a" :$.timeago(new Date(task.finished)));

      // Add row to the table.
      self.tableWrapper.AddRow(row, task);
   }

   self.tableWrapper.DrawTable();

   /* Add listeners to all manual mode 'Waiting of user' status links */
   /* Only manual mode needs to be opened in a pop-up window. */
   $('.mm-link').click(function() {
      var link = $(this).attr('link');
      var url = VmTAF.contextPath + link;
      AfOpenPopupWindow(url, 'Manual_Capture');
   });

   /* Add listeners to all completed manual mode 'Available' status links */
   $('.action-link').click(function() {
      var link = $(this).attr('link');
      VmTAF.contentNavigator.LoadPage(link);
   });

   /* Add listeners to all tasks that need a status popup to be shown */
   $('.popup-link').click(function() {
      var popupInfo = popupData[$(this).attr('popupId')];
      var dialogArgs = {
            title: popupInfo.title,
            message: popupInfo.message,
            buttons: popupInfo.labels
      };

      if (popupInfo.ajaxUrls) {
         dialogArgs.callback = function(pos) {
            var url = popupInfo.ajaxUrls[pos];
            if (url) {
               AfAjax({
                  url: url,
                  method: 'POST',
                  beforeSend: function() {
                     AfStartPleaseWaitT(popupInfo.waitMessage);
                  },
                  complete: function() {
                     if (AfEndPleaseWait()) {
                        self.PopulateDataTableNow({
                           url: self.updateUrl,
                           table: self.tableWrapper.dataTable,
                           dataHandler: self.createRows
                        });
                     }
                  }
               });
            }
            return true;
         };
      } else if (popupInfo.windowUrls) {
         dialogArgs.callback = function(pos) {
            var url = popupInfo.windowUrls[pos];
            if (url) {
               window.location = VmTAF.contextPath + url;
               return false;
            }
            return true;
         };
      }
      AfShowMessageDialog(dialogArgs);
   });
};


TaskManager.prototype.
   makeDescription = function(task) {
   if ("WAITING" == task.queueStatus) {
      var moveToTopLink = '<a href="#" onclick="window.tmgr.moveToTop(' + task.id + ');">move to top</a>';
      var moveToBottomLink = '<a href="#" onclick="window.tmgr.moveToBottom(' + task.id + ');">move to bottom</a>';
      return task.description + "<br>" + moveToTopLink + " " + moveToBottomLink;
   }
   return task.description;
};

TaskManager.prototype.
   moveToTop = function(id) {
   var self = this;
   AfAjax({
      method: 'POST',
      url: '/api/tasks/' + id + '/moveToTop',
      success: function() {
         self.PopulateDataTableNow({
            table: self.tableWrapper.dataTable,
            url: self.updateUrl,
            dataHandler: self.createRows
         });
      }
   });
};

TaskManager.prototype.
   moveToBottom = function(id) {
   var self = this;
   AfAjax({
      method: 'POST',
      url: '/api/tasks/' + id + '/moveToBottom',
      success: function() {
         self.PopulateDataTableNow({
            table: self.tableWrapper.dataTable,
            url: self.updateUrl,
            dataHandler: self.createRows
         });
      }
   });
};
