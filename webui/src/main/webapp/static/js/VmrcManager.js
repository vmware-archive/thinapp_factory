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
 * This script is a modified copy from Jay's ManualMode web module. It serves as
 * a proxy to VMRC client via vmrc.js. Although the script name ends with
 * 'Manager'; unlike other classes, it does not inherit from the AbstractManager
 * class.
 * @returns {VmrcManager}
 */
function VmrcManager() {
   // VM State Map
   this.vmStateMap = {
      acquiringVm : { message : "Waiting for VM to become available...", progress : -1 },
      vmAcquired : { message : "VM acquired.", progress : -1, hideIcon : true },
      poweringOnVm : { message : "Powering on the VM...", progress : -1 },
      waitingForTools : { message : "Waiting for tools...", progress : -1 },
      installingThinApp : { message : "Installing ThinApp...", progress : 15 },
      mountingFileSharesToGuest : { message : "Mounting fileshares...", progress : 18 },
      preCaptureWait : { message : "You can now customize the VM, then please click 'Next' to continue.", progress : 20, hideIcon : true },
      preCaptureDone : { message : "VM customization completed.", progress : -1, hideIcon : true },
      takingPreCaptureSnapshot : { message : "Taking a pre-capture snapshot...", progress : 50 },
      preInstallationWait : {},  // TODO: Empty states here are not yet used during manualmode, but may be later if we add these steps to the process
      preInstallationDone : {},
      installationWait : { message : "Run the application installer and click 'Next' when finished.", progress : 60, hideIcon : true },
      installationDone : { message : "The application installation is done.", progress : 80, hideIcon : true },
      postInstallationWait : {},
      postInstallationDone : {},
      takingPostCaptureSnapshot : { message : "Taking post-capture snapshot...", progress : -1 },
      generatingProject : { message : "Starting a project build...", progress : -1 },
      preProjectBuildWait : {},
      preProjectBuildDone : {},
      buildingProject : { message : "Building the project...", progress : -1 },
      refreshingProject : { message : "Refreshing project...", progress : -1 },
      refreshingProjectDone : { message : "Creating a new build in ThinApp Factory...", progress : 90 },
      cancelling : { message : "The build is being cancelled.", progress : -1, hideIcon : true },
      cancelled : { message : "The build is being cancelled.", progress : -1, hideIcon : true },
      needsLoginWait : { message : "Please login to the VM and then click 'Next'.", progress : -1, hideIcon : true },
      needsLoginDone : { message : "Logged on to the VM.", progress : 15, hideIcon : true }
   };
   this.lease = null;
   this.ticket = null;
   this.setStatus("Waiting.", 0);
   return this;
}

/**
 * Connect to a VM.
 * @param host -
 *           a hostname of the vCenter.
 * @param username -
 *           an username to vCenter.
 * @param password -
 *           a password to vCenter.
 * @param datacenterMoid -
 *           a data center MO id.
 * @param vmPath -
 *           a VM data store path.
 * @returns {VmrcManager}
 */
VmrcManager.prototype.Connect = function _Connect(host, username, password,
      datacenterMoid, vmPath) {
   this.setStatus("Connecting to a VM...", 6);
   AfLog("Connection requested for: " + host + "," + username + "," + password
         + "," + datacenterMoid + "," + vmPath);

   // We have to either give it a (datacenter moid and datastore path) or (a VM
   // moid).
   var res = mks.connect(host, "", username, password, "", datacenterMoid, vmPath);
   AfLog("Connection result: " + res);
   return this;
};

/**
 * Startup VMRC browser plug-in.
 * @returns {VmrcManager}
 */
VmrcManager.prototype.Startup = function _Startup() {
   // 1 means embedded and event based logs, respectively.
   AfLog("Starting VmrcManager...");
   this.setStatus("Loading the VMware Remote Console...", 2);
   var res = mks.startup(1, 1, false, "");
   AfLog("Result: " + res);
   this.setStatus("VMware Remote Console is loaded.", 4, true);
   if (res) {
      AfLog('Acquiring VM...');
      this.run();
   } else {
      this.setStatus("Failed to load VMware remote client!", null, true);
   }
   return this;
};

/**
 * Shutdown VMRC client.
 * @returns {VmrcManager}
 */
VmrcManager.prototype.Shutdown = function _Shutdown() {
   this.setStatus("Starting VMRC client shutdown...");

   AfLog("Disconnecting VMRC client.");
   var res = mks.disconnect();
   AfLog("Result: " + res);

   AfLog("Shutting down VMRC client.");
   var res = mks.shutdown();
   AfLog("Result: " + res);

   this.setStatus("VMRC shut down.", null, true);
   return this;
};

/**
 * Move to a next step of the manual mode flow.
 * @returns {VmrcManager}
 */
VmrcManager.prototype.Next = function _Next() {
   var next = $("#next")[0];
   var nextStatus = $('#nextStatus').val();
   var ticketId = $('#ticketId').val();
   var url = '/api/manualMode/' + ticketId + '/next/' + nextStatus;

   if (nextStatus == 'preCaptureWait') {
      this.setStatus("VM customization completed.", 40, true);
   }

   if (nextStatus == 'installationWait') {
      /* Confirm with the user to ensure all installation and configuration are done. */
      if (!AfConfirm(
            "Confirm Application Install",
            "Before you continue, have you fully installed and configured your application? " +
            "These cannot be performed after ThinApp conversion has completed.")) {
         return;
      }
      this.setStatus('Finished.', 100, true);
   }
   // Check status of the ticket.
   AfAjaxPostJson(url, null, function success(data, textStatus, jqXHR) {
      AfLog('Successfully moved to the next step.');
      $(next).attr("disabled", "disabled");
   }, function error(jqXHR, textStatus, errorThrown) {
      AfLog('Failed to move to the next step!');
   });
   return this;
};

/**
 * Run manual mode conversion process. It runs forever until the vm reaches
 * 'INSTALLATION_DONE', 'REFRESHING_PORJECT_DONE', 'CANCELLING' or 'CANCELLED' status.
 */
VmrcManager.prototype.run = function _run() {
   var self = this;

   // VM is available.
   var VM_ACQUIRED = "vmAcquired";

   // Waiting for user to signal pre-capture customization is complete.
   var PRECAPTURE_WAIT = "preCaptureWait";

   // Waiting for user to signal installation/configuration is complete.
   var INSTALLATION_WAIT = "installationWait";

   // Installation phase finished.
   var INSTALLATION_DONE = "installationDone";

   // Generating build artifacts are done.
   var REFRESHING_PORJECT_DONE = "refreshingProjectDone";

   // Job finished (could have succeeded or failed).
   var FINISHED = "finished";

   var CANCELLING = "cancelling";

   var CANCELLED = "cancelled";

   var NEEDS_LOGIN_WAIT = "needsLoginWait";

   var NEEDS_LOGIN_DONE = "needsLoginDone";

   function checkStatus() {
      var ticketId = $('#ticketId').val();
      var appId = $('#appId').val();
      AfLog("TICKET_ID=" + ticketId + ", APP_ID=" + appId);
      var redeemUrl = '/api/manualMode/' + ticketId + "?appId=" + appId;

      AfAjax(
         {
            url : redeemUrl,
            method : 'GET',
            success : redeemDone,
            error : function() {
               AfLog("Ticket redeem failed, so sad.");
               self.setStatus("Failed to redeem the ticket.", null, true);
            }
         });
   }

   function disableNextButton() {
      $('button:#next').attr('disabled','disabled');
   }

   function enableNextButton() {
      $('button:#next').removeAttr('disabled');
   }

   function redeemDone(status) {
      AfLog(status);

      // If the VM is available and we haven't yet used it.
      if ($.inArray(VM_ACQUIRED, status.states) >= 0 && !self.lease) {
         var vc = status.lease.vc;
         var vm = status.lease.vm;

         self.lease = status.lease;
         self.Connect(vc.host, vc.username, vc.password, vc.datacenterMoid,
               vm.vmxPath);
         AfLog(vc);
         AfLog(vm);
         self.setStatus("Connected to the VM", 10, true);
      } else if (status.currentState == NEEDS_LOGIN_WAIT) {
         self.setStatus(status.currentState);

         $('#nextStatus').val(NEEDS_LOGIN_WAIT);
         enableNextButton();
      } else if (status.currentState == PRECAPTURE_WAIT) {
         self.setStatus(status.currentState);

         $('#nextStatus').val(PRECAPTURE_WAIT);
         enableNextButton();
      } else if (status.currentState == INSTALLATION_WAIT) {
         self.setStatus(status.currentState);

         $('#nextStatus').val(INSTALLATION_WAIT);
         enableNextButton();
      } else if (status.currentState == CANCELLING || status.currentState == CANCELLED) {
         self.setStatus(status.currentState);
         // Hide 'Next' button
         disableNextButton();
         $('#nextStatus').val('cancelled');
         $('#progress-bar').addClass('red');
         $('#close').html('Close');
         AfError("Alert", "The build has been cancelled. Closing the window now...");
         self.Close();
         return; // Stop retry loop.
      } else if ($.inArray(INSTALLATION_DONE, status.states) >= 0 ||
            $.inArray(REFRESHING_PORJECT_DONE, status.states) >= 0) {
         // Hide 'Next' button
         disableNextButton();
         $('#nextStatus').val(FINISHED);
         $('#close').html('Done');
         AfError("Done", "The manual capture build is completed. You can now close the window.");
         self.Close();
         return; // Stop checkStatus loop
      } else {
         self.setStatus(status.currentState);
      }

      // Re-do the ticket status check.
      AfLog("Retrying...");
      window.setTimeout(function() {
         checkStatus();
      }, 3000);
   }

   AfLog("Waiting for VM to become available...");
   this.setStatus("Waiting for VM to become available...");
   checkStatus();
};

/**
 * Set the current status of the Vmrc client.
 * @param status - a status string
 * @param progressPercent - a integer progress value between 0 and 100.
 * @param wantToHideIcon - a boolean flag to hide loading icon next to status message.
 *    If the parameter is not present or is set to false, the loading icon will be displayed.
 * @returns {VmrcManager}
 */
VmrcManager.prototype.setStatus = function _setStatus(status, progressPercent, wantToHideIcon) {
   AfLog("Status changed to: " + status + " with progress " + progressPercent);
   if (status) {
      // Spinning icon
      var icon = VmTAF.imagePath + '/loading.gif';
      var withLoadingIcon = "<img src='" + icon + "' alt='loading'/>";
      var state = this.vmStateMap[status];
      if (state && state.message) {
         if (state.hasOwnProperty('hideIcon') && state.hideIcon == true) {
            $("#status")[0].innerHTML = state.message;
         } else {
            $("#status")[0].innerHTML = state.message + withLoadingIcon;
         }
      } else {
         if (wantToHideIcon && wantToHideIcon == true) {
            $("#status")[0].innerHTML = status;
         } else {
            $("#status")[0].innerHTML = status + withLoadingIcon;
         }
      }
      if (state && state.progress && state.progress !== -1) {
         progressPercent = state.progress;
      }
   }
   if (progressPercent && progressPercent >= 0 && progressPercent <= 100) {
      $("#progress-bar").width(progressPercent + '%');
   };
   return this;
};

/**
 * Close the current manual capture window.
 */
VmrcManager.prototype.Close = function _Close() {
   // Check whether the build is in work-in-progress or not.
   var nextStatus = $('#nextStatus').val();

   if (nextStatus !== 'finished' && nextStatus !== 'cancelled') {
      /* Confirm with the user. */
      if (!AfConfirm("Confirmation","The build isn't finished yet. Are you sure you want to cancel?")) {
         return;
      }
      this.cancel();
   }
   this.Shutdown();
   self.close();
};

/**
 * Cancel the current manual mode build process.
 */
VmrcManager.prototype.cancel = function _cancel() {
   var ticketId = $('#ticketId').val();
   var cancelUrl = '/api/manualMode/cancel?ticketId=' + ticketId;

   this.setStatus('Cancelling current manual build...');
   AfAjax(
         {
            url : cancelUrl,
            method : 'PUT',
            success : function() {
               AfLog('Canceled the build.');
            },
            error : function() {
               AfLog("Failed to cancel the build");
            }
         });
};

/**
 * Destruct VmrcManager object.
 * @returns null.
 */
VmrcManager.prototype.Destruct = function _destruct()
{
   this.vmStateMap = null;
   this.lease = null;
   this.ticket = null;
   return null;
};
