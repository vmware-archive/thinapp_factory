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
 * AppManager
 *
 * A "class" that encapsulates all the methods for dealing with applications.
 * -----------------------------------------------------------------------------
 */
function AppManager(table, category, appId) {
   "use strict";

   var tableOpts = new Object();
   if (appId) {
      // sortable column 1, desc by Timestamp column for buildRequests.
      this.appsRefreshUrl = '/api/apps/' + appId + '/buildRequests';
      tableOpts.firstColumnSortable = true;
      tableOpts.aaSorting = [ [ 1, 'asc' ] ];
      tableOpts.aoColumns = [
         null,
         { sType: "title-numeric" },
         null,
         null,
         null,
         null
      ];
   }
   else {
      // Sort asc by 'Installer' column
      tableOpts.aaSorting = [ [ 1, 'asc' ] ];
      tableOpts.aoColumns = [ null,
                              null,
                              { sType: "taf-version" },
                              null,
                              null,
                              null,
                              null
                            ];
      this.appsRefreshUrl = '/api/apps?sort=true';
      if (category) {
         this.appsRefreshUrl += '&category=' + category;
      }
   }

   if (table) {
      this.CreateTableWrapper(table, tableOpts);
   }
}

(function() {
    "use strict";

    AppManager.prototype = new AbstractManager('Apps');


    /**
     * -----------------------------------------------------------------------------
     * AppManager.UpdateApps()
     *
     * Public function to load data from the server into the applications table.
     *
     * @param refreshInterval If greater that zero, schedules automatic
     *                        updates every 'refreshInterval' milliseconds.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        UpdateApps = function (refreshInterval) {

        this.UpdateDataTable({
            table: this.tableWrapper.dataTable,
            url: this.appsRefreshUrl,
            dataHandler: this.createRows,
            repeatMs: refreshInterval,
            firstTimeWaitMessage: AfTranslate('M.APPS.FETCHING_DATA'),
            showRefreshFlag: true
        });
    };


   /**
    * -----------------------------------------------------------------------------
    * AppManager.updateAppBuildRequest()
    *
    * Public function to load data from the server into the build requests table.
    *
    * @param refreshInterval If greater that zero, schedules automatic
    *                        updates every 'refreshInterval' milliseconds.
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   updateAppBuildRequest = function (refreshInterval)
   {
      this.UpdateDataTable({
         table: this.tableWrapper.dataTable,
         url: this.appsRefreshUrl,
         dataHandler: this.createBuildRequestRows,
         repeatMs: refreshInterval,
         firstTimeWaitMessage: AfTranslate('M.APPS.FETCHING_DATA'),
         showRefreshFlag: true
      });
   };


   /**
    * -----------------------------------------------------------------------------
    * AppManager.LoadBuildHistoryAndButton()
    *
    * This is the starting method that gets invoked after the page is loaded.
    * 1. Attach event handlers for the auto and manual capture buttons.
    * 2. Invoke the repeated refresher to load app build request history.
    * 3. If there is any build history, then enable that section display.
    * @param refreshInterval
    * @param appId
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   LoadBuildHistoryAndButton = function (
         refreshInterval,
         form)
   {
      var self = this;

      // Set event handlers for the 2 buttons.
      if (window.VmTAF.newUI) {
         $('#auto-capture').button();
         $('#manual-capture').button();
      }
      $('#auto-capture').bind('click',  function() {
         self.buildValidator.OnSubmit(self.requestAutoCapture,
               self.massageCaptureRequestData(form));
      });

      // Check browser compatibility for the VMRC plugin.
      var isSupportedBrowser = CheckVmrcSupportedBrowser();
      if (isSupportedBrowser) {
         $('#manual-capture').bind('click', function() {
            if(HasVMRCPlugin()) {
               self.requestManualModeCapture(
                     self.massageCaptureRequestData(form));
            } else {
               AfErrorT('T.APPS.MM.MISSING_PLUGIN', 'M.APPS.MM.MISSING_VMRC_PLUGIN');
            }
         });
      }
      else {
         var title = AfTranslate('M.APPS.MM.BROWSER_NOT_SUP');

         $('#manual-capture')
            .attr('title', title)
            .attr('disabled', 'disabled');
         if (window.VmTAF.newUI) {
            $('#manual-capture').button("option","disabled",true);
         }
      }
      // Initiate application build request history display
      self.updateAppBuildRequest(refreshInterval);
   };

   /**
    * -----------------------------------------------------------------------------
    * Render the application's icon.
    *
    * @param icons
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   RenderIcon = function (icons)
   {
      var args = {};
      args.iconSize = window.VmTAF.iconSizeLarge;
      args.iconUrl = AfGetIconUrl(icons, window.VmTAF.iconSizeLarge);
      $('#appInfoIcon').append($('#appInfoIconTemplate').render(args));
   };

   /**
    * -----------------------------------------------------------------------------
    * Finds a set or URI elements on the page and inspects their title
    * element, which presumably contains a URI.  For each one found, replace
    * it with the results of AfShortUri.
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
      RenderUris = function (uriExpression, uriClass)
   {
      $(uriExpression).each(function(x,y) {
         var title = this.title;
         if (title) {
            $(this).replaceWith(
               AfShortUri(title)
            );
         }
      });
   };

   /**
    * -----------------------------------------------------------------------------
    * AppManager.PrepareDataAndNavigate()
    *
    * Compute the user selected values for workpool, datastore, runtime addHz and
    * recipeId that was applicable earlier and pass it to the invoking url.
    *
    * @param appId
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   PrepareDataAndNavigate = function(url, removeRecipe)
   {
      $(this); // event.preventDefault();
      var form = $('#app-build-form'),
          wpId = form.find('[name="workpoolId"]').val(),
          dsId = form.find('[name="datastoreId"]').val(),
          rtId = form.find('[name="runtimeId"]').val(),
          hzFlag = form.find('[name="addHorizonIntegration"]').is(':checked'),
          recipeId = form.find('[name="recipeId"]').val();

      url += (url.indexOf('?') == -1)? '?' : '&';

      /* construct url param selection value as: <wpId>.<dsId>.<rtId>.hzFlag */
      url += 'selectionData=' + wpId + '.' + dsId + '.' + rtId + (hzFlag? '.true' : '.false');

      // Only apply if the recipe is not already applied, or is not requested to be removed and is recipeId exists.
      if (recipeId != '' && !removeRecipe && (url.indexOf('recipeId=') == -1)) {
         url += '&recipeId=' + recipeId;
      }

      // Now load the url.
      VmTAF.contentNavigator.LoadPage(url);
   };

   /**
    * -----------------------------------------------------------------------------
    * AppManager.requestAutoCapture()
    *
    * User requests an auto capture.
    *
    * @param captureRequest - the request object containing capture parameters
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   requestAutoCapture = function (captureRequest)
   {
      /* Set the addHorizonIntegration flag appropriately. Hiding checkbox does not necessarily uncheck it. */
      captureRequest.addHorizonIntegration = VmTAF.isHzCapable(captureRequest.runtimeId)
         && captureRequest.addHorizonIntegration;

      /* Put this captureRequest object into an array */
      var json = [captureRequest];

      /* Post the data */
      AfAjax({
         method: 'POST',
         contentType: 'application/json',
         url: '/api/builds',
         data: json,

         success: function() {
            AfNotifyT(
               'T.BUILDS.REQUESTS_SUBMITTED',
               'M.BUILDS.REQUESTS_SUBMITTED');
         },

         error: function(jqXHR, textStatus, errorThrown) {
            AfError('Error', 'Build Request failed: ' + errorThrown);
         }
      });
   };


   /**
    * -----------------------------------------------------------------------------
    * AppManager.requestManualModeCapture
    *
    * Request manual mode app conversion with the captureRequest.
    *
    * @param captureRequest - the request object containing capture parameters
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   requestManualModeCapture = function (captureRequest)
   {
      // Send manual mode app capture request
      AfAjax({
         method: 'POST',
         url: '/api/manualMode',
         data: captureRequest,
         contentType: 'application/json',

         success: function(data, textStatus, jqXHR) {
            var mmUrl = window.VmTAF.contextPath + '/manualMode/index?appId='
               + captureRequest.applicationId + '&ticketId=' + data;
            AfOpenPopupWindow(mmUrl, 'Manual_Capture');
         },

         error: function(jqXHR, textStatus, errorThrown) {
            // Failed
            AfError('Error', 'Manual mode request failed: ' + textStatus + ': ' + jqXHR.responseText);
         }
      });
   };


   /**
    * -----------------------------------------------------------------------------
    * AppManager.massageCaptureRequestData
    *
    * This method makes sure the mandatory request parameters are set. If not,
    * some of the defaults are set here.
    *
    * @param form - the form object containing the build parameters
    * @return captureRequest
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   massageCaptureRequestData = function (form)
   {
      var captureRequest = AfSerializeForm(form);
      if (!captureRequest.recipeVariableValues) {
         captureRequest.recipeVariableValues = {};
      }
      return captureRequest;
   };


    /**
     * -----------------------------------------------------------------------------
     * AppManager.BuildSelectedApps()
     *
     * If the user selects more than one application take the user to the screen
     * which lets them define the options for a new set of builds. If a single
     * application is selected, take the user to a application detail page and if
     * there are no applications selected, show an error.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        BuildSelectedApps = function () {
        var apps = this.tableWrapper.GetSelectedRowData();

        // Make sure at least one application was selected.
        if (apps.length === 0) {
            AfError(
                'No Selection',
                'No applications selected. Please select the applications '
                    + 'you want to capture, and try again.');
            return;
        }

        /* If only one app is selected, take the user to the app detail page
         * Else, take the user to the builddefine page.
         */
        if (apps.length == 1 && window.VmTAF.newUI) {
            window.VmTAF.contentNavigator.LoadPage(
                "/apps/detail/" + apps[0].id,
                AfTranslate('M.APPS.DETAIL_PAGE', apps[0].name));
            return;
        }

        /*
         * Construct the URL for the build definition view for multiple apps.
         * Should be '/builds/define?appId=1&appId=2&appId=3...'
         */
        var url = '/builds/define?';
        for (var i = 0; i < apps.length; i++) {
            if (i > 0) {
                url += '&';
            }
            url += 'appId=' + apps[i].id;
        }

        // Load the new view.
        AfLog(apps.length + ' rows selected, url = ' + url);
        window.VmTAF.contentNavigator.LoadPage(
            url,
            AfTranslate('M.APPS.PREPARING_SETTINGS'));
    };


    /**
     * -----------------------------------------------------------------------------
     * AppManager.IgnoreSelectedApps()
     *
     * Mark the selected applications as being "ignored", i.e. the user doesn't
     * want to see them again. This updates the flag in the feed; if the user
     * wants to get the apps back again, they can edit the list in the feed.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        IgnoreSelectedApps = function () {
        var self = this;
        var apps = this.tableWrapper.GetSelectedRowData();

        // Make sure at least one application was selected.
        if (!apps || apps.length === 0) {
            AfError(
                'No Selection',
                'No applications selected. Please select the applications ' +
                'you want to ignore, and try again.');
            return;
        }

        for (var i = 0; i < apps.length; i++) {
            AfAjax({
                       method: 'PUT',
                       contentType: 'application/json',
                       url: '/api/apps/' + apps[i].id + '/skip/true',

                       beforeSend: function() {
                           AfStartPleaseWaitT('M.COMMON.PLEASE_WAIT', { total: apps.length });
                       },

                       complete: function() {
                           if (AfEndPleaseWait()) {
                               self.PopulateDataTableNow({
                                  url: self.appsRefreshUrl,
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
     * AppManager.SubmitNewApplicationForm(form)
     *
     * Submit a new application form. This will tell the server to validate the
     * request, and if OK, to attach the request to the user's session so we can
     * create an application later on (once the file upload is complete).
     *
     * @param args
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        SubmitNewApplicationForm = function (args) {
        var holder = args.holder;
        var validated = false;

        // jQuery validation only works with the first form on a page?
        // Anyway, the validation for the file is not performed, so do it now:
        var file = $("#uploadFile").val();
        if (!file) {
            AfError('No File Selected', 'No file selected for upload.');
            return;
        }

        // Set the upload tracker unique Id
        var uploadId = $.now();
        holder.find('input:hidden[name="uploadId"]').val(uploadId);

        // Convert form data into JSON
        var json = AfSerializeForm(holder);
        AfLog(json);

        // Send app creation request to AppFactory. This will make sure all the
        // fields are good, and then save the request into the user's session. It
        // will be used to create an application once the upload is complete.
        //
        // This is called synchronously, so we are sure that the validation is complete
        // and the session data is saved, before we continue with the file upload.
        AfAjax({
                   method: 'POST',
                   url: '/api/apps/create',
                   data: json,
                   contentType: 'application/json',
                   async: false,
                   beforeSend: function() { $('#upload-error-holder').empty(); },
                   success: function(/*data, textStatus, jqXHR*/) {
                       validated = true;
                   },
                   error: function(jqXHR, textStatus /*, errorThrown*/) {
                      AfError('Error', 'Validation failed: ' + textStatus + ': ' + jqXHR.responseText);
                   }
               });

        if (validated) {
            // Request was OK and was attached to user session, continue with the file upload.
           args.self.SubmitAppAndUpload(uploadId, args.uploadForm);
        }
    };

    /**
     * -----------------------------------------------------------------------------
     * AppManager.SubmitAppAndUpload(form, uploadForm)
     *
     * Assumes that an application create request has been successful sent, and
     * therefore the request is still stored in the user's current session. Now,
     * we upload the file.
     *
     * @param uploadId
     * @param uploadForm
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        SubmitAppAndUpload = function (uploadId, uploadForm) {

       var self = this;
       /*
        * Convert embedded upload form into an AJAX form. This will always return success
        * however the data is json and data.success determines if the call was successful.
        */
        uploadForm.ajaxForm({
           url: VmTAF.contextPath + "/api/apps/upload",
           dataType: "json",
           type: "POST",

           beforeSend: function() {
              //  Disable further uploads clicks while processing.
              AfDialogBeginPleaseWait();
           },

           //  Server-side always returns a success call.
           success : self.HandleUploadResponse,

           complete : function() {
              //  Enable the button that was disabled earlier.
              AfDialogEndPleaseWait();
           }
        });

        // Trigger the file upload mini-form after the ajaxForm initialization.
        uploadForm.submit();

        // Create and show the progress bar.
        var progressDiv = AfProgressBar({
           label: 'Uploading installer ...',
           widthPercent: 1,
           stage: { widthArray: [0, 100]} });
        $('#progressHolder').html(progressDiv).parents('.stack-row').slideDown(400);

        // Invoke the upload progress poller after a second. Serverside takes about a second to start.
        setTimeout(function() {
           AfPollProgressAndUpdate(progressDiv, uploadId);
        }, 1000);
    };

    /**
     * -----------------------------------------------------------------------------
     * AppManager.handleUploadResponse
     *
     * Display the upload success status here.
     *
     * To support IE8, IE9 compatibility view, ensure the
     * response.contextType = text/plain.
     *
     * @param data - Response object from server containing 2 elements
     *            .success - Flag indicating success or failure.
     *            .message - Message to be displayed for the user.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.HandleUploadResponse = function(data) {
       // Hide the save icon, coz the server responded.
       $('#saveImgLoading').hide();

       if (data.success) {
          AfNotify('Installer upload', data.message);
          VmTAF.dialogHolder.dialog("close");

          // Remove the progress holder to avoid making ajax calls to get progress.
          $('#progressHolder').remove();
       } else if (data.message) {
             AfError('Upload installer error', data.message);
       } else {
          //   Couldnt respond with json structure.
          AfError('Upload installer error', data);
       }
    };

    /**
     * -----------------------------------------------------------------------------
     * AppManager.SubmitEditApplicationForm(form)
     *
     * Submit an updated application info form.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.SubmitEditApplicationForm = function (dataHolder, hiddenHolder) {
       AfLog('submitting edit application info form');

       // Convert form data into JSON
       var json = AfSerializeForm(dataHolder);
       var self = this;
       AfLog(json);

       // Grab the hidden app Id from the hidden form
       var appId = hiddenHolder.find('[name="appId"]').val();

       // Send create request to AppFactory
       AfAjax({
          method: 'PUT',
          url: '/api/apps/' + appId,
          data: json,
          contentType: 'application/json',

          success: function(/*data, textStatus, jqXHR*/) {
              AfNotifyT('T.APPS.SAVED', 'M.APPS.SAVED');
              self.LoadAppDetailWithFormData(hiddenHolder);
          },

          error: function(jqXHR, textStatus /*, errorThrown*/) {
              AfError('Application Save Error', 'Edit failed: ' + textStatus + ': ' + jqXHR.responseText);
          }
      });
   };

   /**
    * -----------------------------------------------------------------------------
    * AppManager.LoadAppDetailWithFormData()
    *
    * Submit an updated application info form.
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.LoadAppDetailWithFormData = function (hiddenHolder) {
      // Grab the hidden app Id, recipeId and selectionData
      var appId = hiddenHolder.find('[name="appId"]').val(),
          recipeId = hiddenHolder.find('[name="recipeId"]').val(),
          selectionData = hiddenHolder.find('[name="selectionData"]').val();

      var url = '/apps/detail/' + appId + '?selectionData=' + selectionData;
      if (recipeId && recipeId != '') {
         url += '&recipeId=' + recipeId;
      }
      window.VmTAF.contentNavigator.LoadPage(url);
   };

    /**
     * -----------------------------------------------------------------------------
     * AppManager.ResetFailCounter()
     *
     * Reset the selected applications failCount to 0. This will enable them to
     * get processed everytime the feed's app conversion kicks in.
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        ResetFailCounter = function () {
        var self = this;
        var apps = this.tableWrapper.GetSelectedRowData();

        // Make sure at least one application was selected.
        if (!apps || apps.length === 0) {
            AfErrorT('T.APPS.NO_SELECTION.RESET_FAIL_COUNT',
                     'M.APPS.NO_SELECTION.RESET_FAIL_COUNT');
            return;
        }

        for (var i = 0; i < apps.length; i++) {
            AfAjax({
                       method: 'POST',
                       contentType: 'application/json',
                       url: '/api/apps/' + apps[i].id + '/resetFailCount',

                       beforeSend: function() {
                           AfStartPleaseWaitT('M.COMMON.PLEASE_WAIT', { total: apps.length });
                       },

                       complete: function() {
                           if (AfEndPleaseWait()) {
                               self.PopulateDataTableNow({
                                                             url: self.appsRefreshUrl,
                                                             table: self.dataTable,
                                                             dataHandler: self.createRows
                                                         });
                           }
                       }
                   });
        }
    };


    /**
     * -----------------------------------------------------------------------------
     * AppManager.createRows()
     *
     * Function to update the rows of the application table using
     * the specified JSON data.
     *
     * Replaces anything already in the table.
     *
     * @param self This AppManager instance
     * @param apps Data retrieved from server
     * @param options Options passed in to UpdateDataTable()
     * -----------------------------------------------------------------------------
     */
    AppManager.prototype.
        createRows = function (self, apps, options) {
        var rows = new Array(apps.length);

        /* Add a row per application. */
        for (var i = 0; i < apps.length; i++) {

            var app = apps[i],
                editLink = [
                    'VmTAF.contentNavigator.LoadPage("/apps/detail/',
                    app.id,
                    '");'
                ].join('');

            if (window.VmTAF.newUI) {
               var iconUrl = AfGetIconUrl(app.icons);

               rows[i] = {
                   0: ['<input type="checkbox">'],
                   1: ['<a href="',
                       window.VmTAF.contextPath,
                       '/apps/detail/',
                       app.id,
                       '">',
                       '<img src="',
                       iconUrl,
                       '" width="16" height="16"/>&nbsp;',
                       app.name,
                       '</a>'].join(''),
                   2: app.version,
                   3: app.vendor,
                   4: (app.locale || ''),
                   5: (app.installerRevision ? app.installerRevision : ''),
                   6: AfCreateDataSourceSpan(app.dataSourceType, app.dataSourceName)
               };
            } else {
               rows[i] = {
                   0: '<input type="checkbox">',
                   1: AfCreateApplicationBlock(app, window.VmTAF.iconSizeLarge, editLink),
                   2: app.version,
                   3: app.vendor,
                   4: (app.locale || ''),
                   5: (app.installerRevision ? app.installerRevision : ''),
                   6: app.failCount,
                   7: AfCreateDataSourceSpan(app.dataSourceType, app.dataSourceName)
               };
            }
        }

        self.updateSidebar(apps);

        self.tableWrapper.ReplaceData(rows, apps);
    };

   AppManager.prototype.
      updateSidebar = function (apps) {

      var tableId = this.tableWrapper.dataTable.selector;

      this.replaceHistogramList({
         ulElementId: '#idVendors',
         tableId: tableId,
         data: this.buildHistogram(apps, "vendor")
      });

      this.replaceHistogramList({
         ulElementId: '#idSources',
         tableId: tableId,
         data: this.buildHistogram(apps, "dataSourceName")
      });
   };

   /**
    * Delete old application build request from the history table.
    *
    * @param id an AppBuildRequest id.
    * @param rowNum a current row number of the history datatable.
    */
   AppManager.prototype.
   deleteBuildRequest = function _deleteBuildRequest(id, rowNum) {
      var self = this;
      AfAjax({
         method: 'DELETE',
         url: '/api/apps/buildRequests/' + id,
         success: function() {
            self.tableWrapper.DeleteRow(rowNum);
         },
         error: function(jqHXR) {
            AfError('Error', 'Failed to delete the capture history');
         }
      });
   };

   /**
    * -----------------------------------------------------------------------------
    * AppManager.createBuildRequestRows()
    *
    * Function to update the rows of the application build history table using
    * the specified JSON data.
    *
    * @param self This AppManager instance
    * @param captures Data retrieved from server
    * @param options Options passed in to UpdateDataTable()
    * -----------------------------------------------------------------------------
    */
   AppManager.prototype.
   createBuildRequestRows = function _createBuildRequestRows(self, captures, options)
   {
      var rows = new Array(captures.length);

      /* Add a row per build request. */
      for (var i = 0; i < captures.length; i++) {
         var req = captures[i].appBuildRequest;

         var status = (captures[i].buildStatus)?
               AfTranslate('T.BUILDS.STATUS.' + captures[i].buildStatus) :
               AfTranslate('T.APPS.BUILD.REQ_STAGE.' + req.requestStage);

         /* Build request timestamp, OS, runtime, recipe info. */
         var os = AfTranslate('M.OSTYPE.' + req.osType);
         if(req.osVariant && req.osVariant != '') {
            os += ' ' + (AfTranslate('M.OSVARIANT.' + req.osType + '.' + req.osVariant));
         }

         var rt = req.runtime;
         var time = captures[i].lastUpdated;
         /* Create a link if recipe exists and can be applied to the app. */
         var recipeName = captures[i].recipeName || '';
         if (req.recipeId != null && recipeName != '') {
            var url = "VmTAF.appMgr.PrepareDataAndNavigate('" + window.VmTAF.contextPath
               + "/apps/detail/" + $("input[name=applicationId]").val()
               + "?recipeId=" + req.recipeId + "');";
            recipeName = $('<div onclick="' + url + '">')
               .text(recipeName)
               .addClass('button-link')
               .attr('title', 'Select this recipe');
         }

         var deleteLink = '';
         // Deleting capture request is not allowed while running task.
         if (captures[i].actionType != 'TASK') {
            var deleteScript = "VmTAF.appMgr.deleteBuildRequest(" + req.id + "," + i + ");";
            deleteLink = $('<span onclick="' + deleteScript + '">')
               .addClass('button-link')
               .text(AfTranslate('M.APPS.CAPTURE_HISTORY.DELETE'));
         }

         var allLinks = '';
         /* Create links to build or tasks page */
         if (captures[i].actionType == 'TASK' || captures[i].actionType == 'BUILD') {
            var script = "VmTAF.contentNavigator.LoadPage('" + captures[i].actionUrl + "');";
            var viewLink = $('<span onclick="' + script + '">')
               .addClass('button-link')
               .text(AfTranslate('M.APPS.CAPTURE_HISTORY.' + captures[i].actionType));
            allLinks = AfHtml(viewLink);
            if (deleteLink != '') {
               allLinks += ' | ' + AfHtml(deleteLink);
            }
         } else {
            allLinks = AfHtml(deleteLink);
         }

         rows[i] = {
            0: status,
            1: AfTimeago(time),
            2: os,
            3: rt,
            4: AfHtml(recipeName),
            5: allLinks
         };
      }
      self.tableWrapper.ReplaceData(rows, captures);

      // ALWAYS display the capture history data.
      $('#app-history').removeClass('no-show');
   };

}()); // end "use strict"
