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
 * function AdminManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with administration.
 * -----------------------------------------------------------------------------
 */
function AdminManager()
{
   this.serverDateTime = null;
   this.serverUptime = null;
}

AdminManager.prototype = new AbstractManager('Admin');


/**
 * AdminManager
 * Cleans up anything this object might have instantiated.
 * @returns null
 */
AdminManager.prototype.Destruct = function _destruct()
{
   this.serverDateTime = null;
   this.serverUptime = null;
   clearInterval(this.intervalId);
   this.intervalId = null;
};

/**
 * -----------------------------------------------------------------------------
 * AdminManager.CreateAdminEditor
 *
 * Create the stack editor that shows all the admin settings.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
CreateAdminEditor = function _createAdminEditor(insertPoint)
{
   var self = this;

   var stackEditor = new StackEditor(insertPoint, "Server Administration");
   var stackGroup = stackEditor.AddGroup({});

   stackGroup.AddRow({
      label: "Server Date",
      type: StackEditor.LABEL,
      id: "server-time",
      value: "Fetching..."
   });

   stackGroup.AddRow({
      label: "Server Uptime",
      type: StackEditor.LABEL,
      id: "server-uptime",
      value: "Fetching..."
   });

   stackGroup.AddRow({
      id: "timesync-toggle-cb",
      label: "Server Time Synchronization",
      type: StackEditor.BOOLEAN,
      changeFunc: self.ToggleTimeSync
   });

   stackEditor.AddFooter([ {
         label: "Reboot Appliance",
         clickData : self,
         clickFunc: self.Reboot
      }, {
         label: "Download System Logs",
         clickData : self,
         clickFunc: self.GetLogs
      }
   ]);

   self.pwdResetStackEditor = new StackEditor(insertPoint, "Password Reset");
   stackGroup = self.pwdResetStackEditor.AddGroup({});

   stackGroup.AddRow({
      label: "Username",
      type: StackEditor.LABEL,
      id: "username",
      value: "admin"
   });

   stackGroup.AddRow({
      label: "Old Password",
      type: StackEditor.PWD_SHORT,
      id: "oldPassword",
      value: ""
   });

   stackGroup.AddRow({
      label: "New Password",
      type: StackEditor.PWD_SHORT,
      id: "newPassword",
      value: ""
   });

   stackGroup.AddRow({
      label: "Confirm Password",
      type: StackEditor.PWD_SHORT,
      id: "confirmPassword",
      value: ""
   });

   self.pwdResetStackEditor.AddFooter([ {
         label: "Save",
         clickData : self,
         clickFunc: function() { self.ResetPassword(self.pwdResetStackEditor.formElement); }
      }
   ]);

};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.Reboot()
 *
 * Sends a request to reboot the appliance. The UI will block until the
 * reboot is complete and we get a successful response from a test API
 * call.
 *
 * XXX: Need to add logic to ensure that rebooting won't interrupt anything
 * important.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
Reboot = function(self)
{
   /* Send the reboot request */
   AfAjax({
      method: 'POST',
      url: '/api/admin/reboot',

      beforeSend: function() {
         AfStartPleaseWait('Reboot in progress...please wait');
         window.VmTAF.rebooting = true;
      },

      success: function() {
         /* Reboot in progress, check if complete in a short while */
         self.rebootStart = (new Date()).getTime();
         setTimeout(
            function() { self.CheckApplianceIsUp(self); },
            VmTAF.rebootWaitSeconds * 1000);
      },

      error: function() {
         AfEndPleaseWait();
         AfError('Error', 'Reboot request failed.');
         window.VmTAF.rebooting = false;
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.CheckApplianceIsUp()
 *
 * Check if the appliance is up (responding to API calls). If not, check again
 * in a short while, otherwise remove the UI blocker.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
CheckApplianceIsUp = function _checkApplianceIsUp(self)
{
   /* Try some webui URL which hits CWS */
   AfAjax({
      method: 'GET',
      url: '/api/server-health',

      success: function(data) {
         // on success, the returned data should be the string "true"
         //
         if (data && !!data) {
            window.VmTAF.rebooting = false;
            /* We got data, so the appliance is ready */
            AfEndPleaseWait();
            location.reload();
         }
      },

      // call the error() function even when 'timeout' errors occur,
      // so that we can schedule the next check
      handleDisconnected: true,

      error: function() {
         /* Appliance is probably still rebooting; try again later */
         var now = (new Date()).getTime();
         var waited = (now - self.rebootStart) / 1000;
         var retry = true;

         if (waited > VmTAF.rebootTimeoutSeconds) {
            /* We've waited too long: ask user to wait some more */
            self.rebootStart = (new Date).getTime();
            retry = AfConfirm(
               'No Response',
               'The ' + VmTAF.productName + ' server is not responding. ' +
               'Press "OK" to continue waiting, or "Cancel" to give up.');
         }

         if (retry) {
            /* Check again shortly */
            setTimeout(
               function() { self.CheckApplianceIsUp(self); },
               VmTAF.rebootWaitSeconds * 1000);
         } else {
            /* Give up */
            AfEndPleaseWait();
            AfError(
               'No Response',
               'The ' + VmTAF.productName + ' server is not responding. ' +
               'Check the console for errors.');
         }
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.GetLogs()
 *
 * Download a zip file of the appliance system logs.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
GetLogs = function _getLogs(self)
{
   // Point the current window at the REST resource for the log file, it will
   // be downloaded by the browser
   window.location = VmTAF.contextPath + '/api/admin/logs';
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.UpdateServerInfo()
 *
 * Update various info about the server.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
UpdateServerInfo = function _updateServerInfo()
{
   var self = this;

   // Get various info about the server
   AfAjax({
      method:  'GET',
      url:     '/api/admin/info',
      success: function(info) {
         AfLog('Received json response: date=' + info.date + ' uptime=' + info.uptime);

         // Grab the date and uptime
         // NOTE: info.date contains a "iso 8601"-formatted string.
         //  This parses well with Date.parse() on most modern browsers,
         //  but unfortunately IE versions 8 and lower do not support it.
         //  So instead, we use this:
         self.serverDateTime = jQuery.timeago.parse(info.date);
         self.serverUptime = info.uptime;

         self.UpdateServerTimes();

         // Register a 1 second callback to update the date/time and uptime
         self.intervalId = setInterval(function() {self.UpdateServerTimes();}, 1000);
      },
      error: function(){
         $('#server-time').text('Failed to get server date/time.');
         $('#server-uptime').text('Failed to get server uptime.');
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.UpdateServerTimes()
 *
 * Update the local server date/time and uptime (called as callback every second)
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
UpdateServerTimes = function _updateServerTimes()
{
   if (this.serverDateTime) {
      this.serverDateTime.setSeconds(this.serverDateTime.getSeconds() + 1);
      $('#server-time').text(this.serverDateTime.toString('yyyy-MM-dd hh:mm:ss tt'));
   }

   if (this.serverUptime) {
      this.serverUptime +=1 ;
      $('#server-uptime').text(AfTimeSpan(this.serverUptime));
   }
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.ShowTimeSync()
 *
 * Show the current state of host/guest time synchronization.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
ShowTimeSync = function _showTimeSync()
{
   // Get the timesync checkbox and status
   var cb = $('#timesync-toggle-cb');
   var cbStatus = $('#timesync-status');

   // Disable the checkbox while we are waiting for a status response
   cb.attr('disabled', 'disabled');
   cbStatus.text('(Waiting for status...)');

   // Save the 'this' object to be used within closures
   var self = this;

   AfAjax({
         method:  'GET',
         url:     '/api/admin/timesync',
         success: function(state) {
            AfLog("Received json response: " + state);

            // Success, enable the checkbox
            cb.removeAttr('disabled');
            cb.attr('checked', state);
            cbStatus.text('');
         },
         error: function(){
            cbStatus.text('(Failed to get time synchronization status.)');
         }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.ToggleTimeSync()
 *
 * Toggle the current state of host/guest time synchronization.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
ToggleTimeSync = function _toggleTimeSync()
{
   // Get the timesync checkbox
   var cb = $('#timesync-toggle-cb');

   // Disable the checkbox while we are waiting for a status response
   cb.attr('disabled', 'disabled');

   // Save the 'this' object to be used within closures
   var self = this;

   AfAjax({
         method:  'GET',
         url:     '/api/admin/timesync',
         beforeSend: function() {
            AfStartPleaseWait('Waiting for status');
         },
         success: function(state) {
            AfLog("Received json response: " + state);

            // Toggle state
            setStr = (state ? 'disable' : 'enable');

            // Build the API request URL to toggle the state
            urlStr = '/api/admin/timesync/' + setStr;

            AfLog("POST to url " + urlStr);

            AfAjaxPostJson(
               urlStr,
               "",
               function(){
                  cb.removeAttr('disabled');
                  AfLog("POST successful, setting checkbox state: " + state);
                  cb.attr('checked', !state);
               },
               function() {
                  AfLog("Failed to set time synchronization status");
               }
            );
         },
         error: function(){
            AfLog("Failed to get time synchronization status");
         },
         complete: function() {
            AfEndPleaseWait();
         }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AdminManager.AcceptEula
 *
 * Tell AppFactory that the user has accepted the license agreement, and then
 * redirect to the main landing page.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
AcceptEula = function _acceptEula()
{
   /* Make sure the box is checked */
   if ($('#eula-accepted').is(':checked') == false) {
      AfError(
         'Please Confirm',
         'Please confirm you agree to the terms of this agreement by ' +
         'checking the box.');
      return;
   }

   AfAjax({
      method: 'POST',
      url: '/api/config/eula',
      contentType: 'application/json',

      success: function() {
         window.location = VmTAF.contextPath + '/';
      },
      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Failed', 'Failed to update EULA status: ' + jqXHR.responseText);
      }
   });
};

/**
 * -----------------------------------------------------------------------------
 * AdminManager.ResetPassword()
 * Reset current user password. However, it only supports single 'admin' only.
 * TODO: Replace valdiation with jquery validation.
 * -----------------------------------------------------------------------------
 */
AdminManager.prototype.
ResetPassword = function _resetPassword(form)
{
   var json = AfSerializeForm(form);
   if (json['newPassword'] != json['confirmPassword']) {
      AfError('Error', 'New password and confirm password must be the same!');
      return;
   }

   delete json['confirmPassword'];

   AfAjax({
      method: 'PUT',
      contentType: 'application/json',
      url: '/api/admin/resetpwd',
      data: json,

      success: function() {
         AfNotify('Saved', 'Your password had been changed!');
      },

      error: function() {
         AfError('Error', 'Password-reset request failed.');
      }
   });
};
