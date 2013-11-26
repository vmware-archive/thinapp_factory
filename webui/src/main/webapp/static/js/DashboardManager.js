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
 * function DashboardManager
 *
 * A "class" that encapsulates all the methods for dealing with the dashboard.
 * -----------------------------------------------------------------------------
 */
function DashboardManager(showAlert)
{
   this.workpoolSetupAlert = (showAlert);
}

DashboardManager.prototype = new AbstractManager('Dashboard');

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.UpdateDashboard
 *
 * Public function to load data from the server into the dashboard.
 * But it updates 'System Data' and 'Default Datastore' section.
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.
UpdateDashboard = function _updateDashboard(refreshInterval)
{
   this.UpdateDataTable({
      url: '/api/admin/stats',
      dataHandler: this.populateDashboard,
      repeatMs: refreshInterval,
      showRefreshFlag: true
   });
};

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.populateDashboard
 *
 * Using the JSON summary of system statistics, populate the dashboard
 * view.
 *
 * @param self Reference to the DashboardManager
 * @param sysData Data received from server
 * @param args Same arguments we passed to UpdateDataTable()
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.
populateDashboard = function _populateDashboard(self, sysData, args)
{
   var stat;

   /* Feeds */
   stat = sysData.numFeedsTotal;
   $('#feed-stats').children().replaceWith($('<span>').text(stat));

   /* File Shares */
   stat = sysData.numFileSharesTotal;
   $('#fileshare-stats').children().replaceWith($('<span>').text(stat));

   /* Apps */
   stat = sysData.numAppsTotal;
   $('#app-stats').children().replaceWith($('<span>').text(stat));

   /* Tasks */
   stat = sysData.numTasksRunning;
   $('#task-stats').children().replaceWith($('<span>').text(stat));

   /* Builds */
   stat = sysData.numBuildsPublished + '/' + sysData.numBuildsTotal;
   $('#build-stats').children().replaceWith($('<span>').text(stat));

   /* Workpools size */
   stat = sysData.numAvailableWorkpools + '/' + sysData.totalWorkpools;
   stat = $('<span>').text(stat);
   if (sysData.totalWorkpools < 1) {
      stat.css('color', 'red');
      if (self.workpoolSetupAlert) {
         self.AlertWorkpoolSetup();
         self.workpoolSetupAlert = false;
      }
   }
   $('#workpool-stats').children().replaceWith(stat);

   /* Default datastore free space */
   if (sysData.defaultDatastore) {
      var ds = sysData.defaultDatastore;
      var free = (ds.size - ds.used);
      var freePc = (ds.size == 0) ? 0 : Math.round(100 * free / ds.size);
      var sb = AfCreateProgressBarDivColor(
            AfSimplifyDiskSize(free) + ' (' + freePc + '%)',
            freePc, 'red', 5, 'orange', 20, 'green');
      $('#storage-stats').children().replaceWith(sb);

      /* Default datastore name */
      var title = 'Free space on "' + ds.name + '"';
      if (ds.server && ds.share) {
         title += ' (' + ds.server + ':/' + ds.share + ')';
      }
      $('#storage-stats').prev().text(title);
   } else {
      var sb = AfCreateProgressBarDivColor(
            'Datastore information unavailable',
            0, 'red', 5, 'orange', 20, 'green');
      $('#storage-stats').children().replaceWith(sb);

      /* Default datastore name */
      $('#storage-stats').prev().text('Free space on default datastore');
   }

};

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.OpenView
 *
 * Loads the specified view in the main panel.
 * @param view The url of the view to be opened.
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.
OpenView = function _openView(view)
{
   VmTAF.contentNavigator.LoadPage(view);
};

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.ShowVideo()
 *
 * Load a given video url into jQuery dialog and play it via iframe that
 * supports both embedded Flash and HTML5.
 * @param a video url to be played.
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.ShowVideo = function _showVideo(url)
{
   var div = $('#af-confirm');
   var iframe = $('<iframe width="560" height="349" src="'+ url +'?rel=0&autoplay=1" frameborder="0" allowfullscreen></iframe>');
   div.append(iframe);
   div.dialog({
      title: AfTranslate('T.DASHBOARD.VIDEO_TITLE'),
      autoOpen: true,
      closeOnEscape: false,
      height: 420,
      width: 600,
      zIndex: 1111,
      hide: 'explode',
      show: 'scale',
      resizable: false,
      close: function(event, ui) {
         div.empty();
      }
   });
};

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.AlertWorkpoolSetup
 *
 * Show one-time workpools setup alert.
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.
AlertWorkpoolSetup = function _alertWorkpoolSetup()
{
   var setupNow = AfConfirmT('T.WORKPOOL', 'M.WORKPOOL.SETUP.REQUIRED');
   if (setupNow) {
      this.OpenView('/workpools/create');
   }
   this.DisableWorkpoolsSetupAlert();
};

/**
 * -----------------------------------------------------------------------------
 * DashboardManager.UpdateWorkpoolsSetupConfig
 *
 * Disable workpools setup alert on the page load.
 * -----------------------------------------------------------------------------
 */
DashboardManager.prototype.
DisableWorkpoolsSetupAlert = function _disableWorkpoolsSetupAlert()
{
   var data = new Object();
   data['workpool.show_setup_alert'] = false;

   AfAjax({
      method: 'POST',
      url: '/api/config',
      contentType: 'application/json',
      data: JSON.stringify(data),
      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Failed', 'Failed to update workpools setup status: ' + errorThrown);
      }
   });
};
