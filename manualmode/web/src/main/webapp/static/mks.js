function Mks() {
   var self = this;

   this.setStatus("Waiting.");
   this.lease = null;
   this.ticket = null;

   var next = $("#next").click(function() {
      self.next();
   });

   mks.onConnectionStateChange = function(cs, host, vmId, userRequested, reason) {
      self.onConnectionStateChange(cs, host, vmId, userRequested, reason);
   };

   this.connected = false;
}

Mks.prototype.onConnectionStateChange = function(cs, host, vmId, userRequested, reason) {
   log.debug("Received onConnectionStateChange", cs,
           host,
           vmId,
           userRequested,
           reason);
   /* XXX: There may a race-condition here in that when we disconnect we will
    try to reconnect sometime when polling but it's not clear when the VM is in a
    state to be reconnected.
     */
   if (!cs && this.connected) {
      log.info("Disconnecting...");
      this.disconnect();
   }
};

Mks.prototype.setStatus = function(status) {
   log.info("Status changed to: ", status, ".");
   $("#status")[0].innerHTML = status;
};

Mks.prototype.cancel = function() {
   if (!this.ticket) {
      log.error("No request in progress, unable to cancel.");
      return;
   }

   log.info("Canceling...");
   var cancel = $("#cancel");
   var url = "/manualmode-web/manual/cancel";

   function cancelDone() {
      cancel.attr("disabled", "true");
   }

   function cancelFail() {
      log.error("Cancel failed.");
   }

   $.postJSON(url, this.ticket).success(cancelDone).error(cancelFail);
};

Mks.prototype.connect = function(host, username, password, dcMoid, vmPath) {
   this.setStatus("Connecting to server...");

   log.debug("Connection requested for: ", host, username, password, dcMoid,
           vmPath);

   // We have to either give it a (datacenter moid and datastore path) or (a VM
   // moid).
   var res = mks.connect(host, "", username, password, "", dcMoid, vmPath);
   log.info("Connect result: ", res, ".");
   this.connected = true;
};

Mks.prototype.startup = function() {
   // 1 means embedded and event based logs, respectively.
   log.info("Starting mks.");
   this.setStatus("Loading the VMware Remote Console...");
   var res = mks.startup(1, 1, false, "");
   log.info("Result: ", res, ".");
   this.setStatus("VMware Remote Console is loaded.");
   $("#acquire").attr("disabled", "");
};

Mks.prototype.disconnect = function() {
   var res = mks.disconnect();
   log.info("Result: ", res, ".");
   this.connected = false;
   this.lease = undefined;
};

Mks.prototype.shutdown = function() {
   this.setStatus("Shutting down VMRC...");

   log.info("Disconnecting VMRC.");
   var res = mks.disconnect();
   log.info("Result: ", res, ".");

   log.info("Shutting down VMRC.");
   var res = mks.shutdown();
   log.info("Result: ", res, ".");

   this.setStatus("VMRC shut down.");
};

Mks.prototype.next = function() {
   var next = $("#next")[0];
   var url = "/manualmode-web/manual/next/" + next.status;

   function nextDone() {
      // XXX: How do you prevent people from doing this multiple times?
      $(next).attr("disabled", "true");
   }

   function nextFail() {
      log.error("Next failed.");
   }

   $.postJSON(url, this.ticket).success(nextDone).error(nextFail);
};

Mks.prototype.acquire = function() {
   var self = this;

   // User needs to login manually.
   var NEEDS_LOGIN_WAIT = "needsLoginWait";

   var NEEDS_LOGIN_DONE = "needsLoginDone";

   // Waiting for user to signal pre-capture customization is complete.
   var PRECAPTURE_WAIT = "preCaptureWait";

   // Waiting for user to signal installation/configuration is complete.
   var INSTALLATION_WAIT = "installationWait";

   // VM is available.
   var VM_ACQUIRED = "vmAcquired";

   // VM is no longer available.
   var VM_RELEASED = "vmReleased";

   // Installation phase finished.
   var INSTALLATION_DONE = "installationDone";

   // Job finished (could have succeeded or failed).
   var FINISHED = "finished";

   function ticketDone(data) {
      var ticket = data;
      self.ticket = ticket;
      log.info("Received ticket: ", ticket, ".");
      var url = "/manualmode-web/manual/redeem";

      $("#cancel").attr("disabled", "");

      $.postJSON(url, ticket).success(redeemDone).error(redeemFail);
   }

   function redeemDone(status) {
      log.info("Received redeem status: ", status);
      var currentState = status.states[status.states.length - 1];

      // If the VM is available and we haven't yet used it.
      if ($.inArray(VM_ACQUIRED, status.states) >= 0 && !self.lease) {
         var vc = status.lease.vc;
         var vm = status.lease.vm;

         self.lease = status.lease;
         self.connect(vc.vcHost, vc.vcUsername, vc.vcPassword, vc.dcMoid,
                 vm.vmxPath);
      } else if (currentState == NEEDS_LOGIN_WAIT) {
         self.setStatus("Login to the VM and hit next.");

         var next = $("#next")[0];
         next.status = NEEDS_LOGIN_WAIT;
         $(next).attr("disabled", "");
      } else if (currentState == PRECAPTURE_WAIT) {
         self.setStatus("Configure the VM and hit next.");

         var next = $("#next")[0];
         next.status = PRECAPTURE_WAIT;
         $(next).attr("disabled", "");
      } else if (currentState == INSTALLATION_WAIT) {
         self.setStatus("Install your stuff and hit next.");

         var next = $("#next")[0];
         next.status = INSTALLATION_WAIT;
         $(next).attr("disabled", "");
      } else if (currentState == FINISHED) {
         self.setStatus("Finished.");
         return; // Don't retry.
      } else {
         self.setStatus(currentState);
      }

      // Redo!
      log.info("Retrying...");
      window.setTimeout(function() {
         ticketDone(self.ticket);
      }, 3000);
   }

   function redeemFail() {
      log.error("Ticket redeem failed, so sad.");
      self.setStatus("Failed to redeem ticket.");
   }

   function ticketFail() {
      log.error("Ticket request failed, so sad.");
      self.setStatus("Failed to request ticket.");
   }

   var req = {};
   req.inputUri = $("#inputUri").val();
   req.outputDatastore = $("#outputDatastore").val();
   req.commandLine = $("#commandLine").val();

   var url = "/manualmode-web/manual";
   log.info("Requesting a VM from: ", url, "with contents ", req, ".");
   this.setStatus("Requesting a virtual machine...");
   $.postJSON(url, req).success(ticketDone).error(ticketFail);
};