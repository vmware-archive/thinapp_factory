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


/* Calls to AfStartPleaseWait() are reference counted: */
VmTAF.waitCount = 0;

/* Icon displayed in the wait blocker dialog */
VmTAF.waitIcon = VmTAF.imagePath + '/ajax-loader.gif';

/**
 * -----------------------------------------------------------------------------
 * AfStartPleaseWaitT()
 *
 * The same as AfStartPleaseWait(), but takes a translation key as it's
 * argument instead of text.
 *
 * @param key The message corresponding to this translated key to be displayed
 *            on PleaseWait progress modal.
 * @param args - Custom information related to progress etc.
 * -----------------------------------------------------------------------------
 */
function AfStartPleaseWaitT(key, args)
{
   AfStartPleaseWait(AfTranslate(key), args);
}

/**
 * -----------------------------------------------------------------------------
 * AfStartPleaseWait()
 *
 * Block the entire UI, showing the specified message (if omitted, a default
 * "Please wait" message is used). Call AfEndPleaseWait() to unblock. Calls to
 * this function are reference counted, and an equal number of calls to
 * AfEndPleaseWait are needed in order to remove the blocker.
 *
 * This call could be used in the ajax "beforeSend" callback, and
 * AfEndPleaseWait() in the ajax "complete" callback.
 *
 * @param message Optional text to display on PleaseWait progress modal.
 * @param args - Custom information related to progress etc.
 *            .total - Indicates total number of items to be processed.
 * -----------------------------------------------------------------------------
 */
function AfStartPleaseWait(message, args) {
   VmTAF.waitCount++;

   if (VmTAF.waitCount == 1) {
      if (!message) {
         message = 'Please Wait';
      }
      var msg = '<h1><img src="' + VmTAF.waitIcon + '"> ' + message + '</h1>';

      // If progress params are set, construct the progress bar display.
      if (args && args.total) {
         msg += createProgressForPleaseWait(args.total);
      }
      $.blockUI({ message: msg });
      AfDialogBeginPleaseWait();
   }
}

/**
 * Create a progress bar on the wait modal only when there are more than 1 item being processed.
 * The total param indicates the number of concurrent processing items while the progress wait is displayed.
 *
 * @param total
 * @returns {String}
 */
function createProgressForPleaseWait(total) {
   var total = (typeof total == 'number')? total : 0;
   if (total <= 1) {
      return '';
   }

   // Compute the current value, set total-1 indicating processing first entity.
   var progress = createOrUpdateProgress(null, total, (total-1));

   return ('<div id="waitProgressHolder" total="' + total + '">' + AfHtml(progress) + '</div>');
}

/**
 * Creates a progress bar when no holderDiv is passed, else it updates the progress and returns html.
 * Since the current value indicates items remaining to be process, we use total - current.
 *
 * @param holderDiv - Progress holder div.
 * @param total - Total items to be processed.
 * @param remaining - Items left to be processed.
 * @returns
 */
function createOrUpdateProgress(holderDiv, total, remaining) {
   var current = (remaining > total)? total : (total-remaining);
   var percent = (current == 0)? 0 : Math.round(100 * current / total);

   var progress = AfProgressBar({
         holderDiv: holderDiv,
         label: 'Processing: ' + percent + '% (' + current + ' / ' + total + ')',
         widthPercent : percent
      });
   return progress;
}

/**
 * Update the wair progressbar with the latest update to the waitCounter.
 *
 * @param current
 */
function updateProgressForPleaseWait(current) {
   var holderDiv = $('#waitProgressHolder').find('.progress-container');
   // Invoke only if holderDiv exists, else it will create a new one.
   if (holderDiv) {
      createOrUpdateProgress(holderDiv, $('#waitProgressHolder').attr('total'), current);
   }
}

/**
 * -----------------------------------------------------------------------------
 * AfEndPleaseWait()
 *
 * Unblock the entire UI following a prior call to AfStartPleaseWait() or
 * AfPleaseWaitAndProcess(). Calls to this function are reference counted, and
 * the blocker is only removed after this function has been called the same
 * number of times as AfStartPleaseWait().
 *
 * @return true if the blocker is actually removed, false if just the reference
 * count was decremented.
 * -----------------------------------------------------------------------------
 */
function AfEndPleaseWait()
{
   if (0 == VmTAF.waitCount) {
      return true;
   }

   VmTAF.waitCount--;

   if (VmTAF.waitCount < 0) {
      AfLog('Too many calls to AfEndPleaseWait!');
   }

   if (VmTAF.waitCount <= 0) {
      AfDialogEndPleaseWait();
      $.unblockUI();
      return true;
   } else {
      // Update the progress bar if one exists.
      updateProgressForPleaseWait(VmTAF.waitCount);
   }

   return false;
}

/**
 * -----------------------------------------------------------------------------
 * AfPleaseWaitAndProcess()
 *
 * Block the entire UI, showing the specified message (if omitted, a default
 * "Please wait" message is used). Call AfEndPleaseWait() to unblock.
 *
 * @param workFunction Function to be invoked as soon as the 'wait' message
 *        appears.
 * @param message Mandatory message to display while workFunction executes.
 * @param data The optional data that the workFunction will act upon.
 * -----------------------------------------------------------------------------
 */
function AfPleaseWaitAndProcess(message, workFunction, data) {
   if (!message) {
      message = 'Please Wait';
      return;
   }
   AfStartPleaseWait(message);
   setTimeout( function() { workFunction(data); } , 1);
}

/**
 * -----------------------------------------------------------------------------
 * AfPleaseWaitModalInit()
 *
 * Initialize the modal for #modal-overlay with default params.
 * -----------------------------------------------------------------------------
 */
function AfPleaseWaitModalInit()
{
   // Remove default styling from UI blocker
   $.blockUI.defaults.css = {};
   $.blockUI.defaults.overlayCSS = {};
   $.blockUI.defaults.fadeOut = 0;
   $.blockUI.defaults.fadeIn = 250;
   $.blockUI.defaults.messageDelay = 1000;

   // on IE, the beforeunload handler appears to ALWAYS open
   // a confirmation dialog no matter what is returned by beforeunload.
   // Fortunately, the issue that we're using the unloading flag to
   // work around (ajax connections being aborted on page navigation,
   // leading to spurious error dialogs), doesn't happen on IE,
   // so we can avoid the whole issue on that browser altogether.
   if (!$.browser.msie) {
      $(window).bind('beforeunload', function(){
         VmTAF.unloading = true;
         // beforeunload expects a return value of either null or
         // a string.  If it is a string, the browser will open a dialog
         // asking the user to confirm that they want to leave the page.
         // Since we only want to know that we're leaving the page, we
         // return null here.
         //
         return null;
      });
   }
}

function AfDialogBeginPleaseWait() {
   $("#idDialogOk").prop('disabled', true).addClass('ui-state-disabled');
}

function AfDialogEndPleaseWait() {
   $("#idDialogOk").prop('disabled', false).removeClass('ui-state-disabled');
}
