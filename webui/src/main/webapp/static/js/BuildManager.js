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
 * function BuildManager
 *
 * A "class" that encapsulates all the methods for dealing with builds.
 * -----------------------------------------------------------------------------
 */
function BuildManager(table, status, likeBuildId, groupRows)
{
   this.newRegValueDialog = null;
   this.groupRows = groupRows;

   if (likeBuildId) {
      /* URL fetches builds for the same app */
      this.refreshUrl = '/api/builds/like/' + likeBuildId;
   }
   else if (this.groupRows) {
      /* URL fetches builds grouped by apps with extra meta data. */
      this.refreshUrl = '/api/builds/group/app';
   }
   else {
      /* URL fetches all builds */
      this.refreshUrl = '/api/builds';
   }

   /* Filter fetched data on build status */
   if (status && status.length > 0) {
      this.refreshUrl += '?status=' + status;
   }

   if (table) {
      var tableOpts = {};
      if (groupRows) {
         /* Builds-by-app has no checkbox in column 0 */
         tableOpts.firstColumnSortable = true;

         // tell Datatables to use a custom sort procedure for our timestamp columns
         tableOpts.aoColumns = [
            null,
            null,
            { sType: "title-numeric" },
            { sType: "title-numeric" }
         ];
      } else {
         tableOpts.aaSorting = [ [3, 'asc'] ];
         tableOpts.aoColumns = [
            null,
            null,
            null,
            { sType: "title-numeric" },
            null,
            null,
            null,
            null,
            null,
            null
         ];
      }
      this.CreateTableWrapper(table, tableOpts);
   }
}

BuildManager.prototype = new AbstractManager('Builds');

/**
 * -----------------------------------------------------------------------------
 * BuildManager.UpdateBuildsView
 *
 * Public function to load data from the server into the builds table.
 *
 * @param table HTML "table" element that displays all the builds.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
UpdateBuildsView = function _updateBuildsView(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createBuildRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.BUILDS.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.UpdateAppsView
 *
 * Public function to load data from the server into the builds-by-application
 * table.
 *
 * @param table HTML "table" element that displays all the builds.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
UpdateAppsView = function _updateAppsView(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createAppRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.BUILDS.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.SetStatuses
 *
 * Change the status of every selected build to the new specified
 * status. When done, refresh the current view.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
SetStatuses = function _setStatuses(newStatus)
{
   var self = this;
   var builds = self.tableWrapper.GetSelectedRowData();

   if (builds.length == 0) {
      AfErrorT(
         "T.COMMON.NO_SELECTION",
         "M.BUILDS.STATUS_CHANGE_NO_SELECTION");
      return;
   }

   var numOk = 0;
   var numErr = 0;
   var errStr = '';

   for (var i = 0; i < builds.length; i++) {
      // Check if this build needs rebuilding, if so, do not allow publishing.
      if (newStatus == 'PUBLISHED' && self.needsRebuild(builds[i])) {
         AfNotify('Cannot Publish',
               'Project ' + builds[i].buildName + ' needs rebuilding before it can be published.', 'warn');
         continue;
      }
      AfAjax({
         method: 'PUT',
         contentType: 'application/json',
         url: '/api/builds/' + builds[i].id + '/status/' + newStatus,

         beforeSend: function() {
             AfStartPleaseWait("Updating build status ... please wait", { total: builds.length });
         },

         success: function() {
            numOk++;
         },

         error: function(jqXHR, textStatus, errorThrown) {
            numErr++;
            errStr = errStr + '<p><hr>'+ jqXHR.responseText + '</p>';
         },

         complete: function() {
            if (AfEndPleaseWait()) {
               AfLog('status values set: numOk=' + numOk + ' numErr=' + numErr);

               if (numErr > 0) {
                  AfError('Error', 'Error while publishing / unpublishing.<p><b>Details:</b></p>' + errStr);
               }
               else if (numOk > 0) {
                  self.PopulateDataTableNow({
                     url: self.refreshUrl,
                     table: self.tableWrapper.dataTable,
                     dataHandler: self.createBuildRows
                  });
               }
            }
         }
      });
   }
   // If some of the checkboxes were not unchecked, do it now.
   self.tableWrapper.MarkAllRowCheckboxes(false);
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.DeleteSelected
 *
 * Delete all the selected builds from the given build table.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
DeleteSelected = function _deleteSelected()
{
   var builds = this.tableWrapper.GetSelectedRowData();

   if (builds.length == 0) {
      AfErrorT(
         "T.COMMON.NO_SELECTION",
         "M.BUILDS.DELETE_NO_SELECTION");
      return;
   }

   /* Check with the user. */
   // TODO: Translation key needed
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete ' +
         AfThisOrThese(builds, 'project') + '?')) {
      return;
   }

   var self = this;
   var numOk = 0;
   var numErr = 0;

   for (var i = 0; i < builds.length; i++) {
      AfLog('deleting build ' + builds[i].id);

      AfAjax({
         method: 'DELETE',
         contentType: 'application/json',
         url: '/api/builds/' + builds[i].id,

         beforeSend: function() {
            AfStartPleaseWaitT("M.BUILDS.DELETE_IN_PROGRESS", { total: builds.length });
         },

         success: function() {
            numOk++;
         },

         error: function() {
            numErr++;
         },

         complete: function() {
            if (AfEndPleaseWait()) {
               if (numErr > 0) {
                  AfErrorT(
                     'T.COMMON.ERROR',
                     'M.BUILDS.SOME_DELETES_FAILED');
               }
               else if (numOk > 0) {
                  AfNotifyT(
                     'T.COMMON.SUCCESS',
                     'M.BUILDS.DELETED');
                  self.PopulateDataTableNow({
                     url: self.refreshUrl,
                     table: self.tableWrapper.dataTable,
                     dataHandler: self.createBuildRows
                  });
               }
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.RebuildSelected
 *
 * Rebuild all the selected builds from the given build table.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
RebuildSelected = function _rebuildSelected()
{
   var self = this;
   var localRebuilds = self.tableWrapper.GetSelectedRowData();

   if (localRebuilds.length == 0) {
      AfErrorT(
         "T.COMMON.NO_SELECTION",
         "M.BUILDS.REBUILD_NO_SELECTION");
      return;
   }

   var numOk = 0;
   var numErr = 0;

   for (var i = 0, counter = 0; i < localRebuilds.length; i++) {
      AfLog('rebuild Id ' + localRebuilds[i].id);

      AfAjax({
         method:      'POST',
         contentType: 'application/json',
         url:         '/api/builds/' + localRebuilds[i].id + '/rebuild',

         success:     function() { numOk++; },
         error:       function() { numErr++; },

         complete:    function() {
            counter++;
            if (counter == localRebuilds.length) {
               // The last of the ajax calls has completed:
               // Perform on-complete stuff.
               if (numErr > 0) {
                  AfError('Error', 'One or more errors requesting rebuilds.');
               }
               else if (numOk > 0) {
                  /*
                   * Before we perform a page refresh here, we need to clear all check boxes explicitly:
                   *
                   * This is needed as there are times that the server side does not register a change yet,
                   * but sees it after a while, and by then it returns a http status 304 (Not modified).
                   * When this happens, we have to make sure the check boxes are cleared, else all possible
                   * future page refreshes will halt.
                   *
                   * If we dont clear the checkboxes, due to a 304, future auto table refreshes are put on
                   * hold.
                   */
                  self.tableWrapper.MarkAllRowCheckboxes(false);

                  self.PopulateDataTableNow({
                        url: self.refreshUrl,
                        table: self.tableWrapper.dataTable,
                        dataHandler: self.createBuildRows
                  });
               }
               AfNotifyT('T.BUILDS.REQUESTS_SUBMITTED', 'M.BUILDS.REQUESTS_SUBMITTED');
            }
         }
     });
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.LoadBuildSettings
 *
 * Ask the server for the current build settings for a given build. This
 * returns a JSON structure containing the package INI file, registry settings,
 * and directory settings.
 *
 * These settings can be used to initialize the various editors.
 * See PopulatePackageIniEditor(), PopulateRegistryEditor(), etc.
 *
 * @param buildId Build to retrieve
 * @param what Null for a map of all settings, else 'packageini', 'directory'
 *             or 'registry'
 * @param successFunc Function invoked when the data has been fetched.
 * @param errorFunc Function invoked if the data fetch fails
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
LoadBuildSettings = function _loadBuildSettings(
      buildId,
      what,
      successFunc,
      errorFunc)
{
   AfLog('Loading settings for build id ' + buildId);
   var settings = new Object();
   var url = '/api/builds/' + buildId + '/settings';

   if (what) {
      url += '/' + what;
   }

   /* Get all settings at once */
   AfAjax({
      url: url,
      method: 'GET',

      beforeSend: function() {
         AfStartPleaseWaitT("M.BUILDS.LOADING_SETTINGS");
      },

      success: function(data) {
         AfLog('Settings loaded OK');
         successFunc(data);
      },

      error: function(jqXHR, textStatus, errorThrown) {
         errorHandlerForRebuildState(jqXHR, textStatus, errorThrown);
         if (errorFunc) {
            errorFunc();
         }
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.GetBuildDefinitionsAndPopulateForm
 *
 * Makes an Ajax call to fetch default build settings for a given set of
 * applications. When done, populate a stack editor to let the user modify
 * these as necessary, and then submit them as build requests.
 *
 * @param insertPoint Where to create the stack editor
 * @param appIds Applications to be built
 * @param matchTypes All recipe match types ordered by increasing precision.
 * @param horizonEnabled Indicates if horizon support is enabled.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
GetBuildDefinitionsAndPopulateForm = function(
      insertPoint,
      appIds,
      horizonEnabled,
      matchTypes)
{
   var self = this;

   /* URL to get default build settings for these apps */
   var url = "/api/builds/define";
   for (var ai = 0; ai < appIds.length; ai++) {
      url += (ai == 0 ? "?" : "&");
      url += "appId=" + appIds[ai];
   }

   /* Make the request */
   AfAjax({
      url: url,
      method: 'GET',
      beforeSend: function() {
         AfStartPleaseWait();
      },
      success: function(data) {
         /* Success! Now create the stack editor  */
         self.PopulateDefineForm(insertPoint, data, horizonEnabled, matchTypes);
      },
      complete: function() {
         AfEndPleaseWait();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.PopulateDefineForm
 *
 * Use the JSON data from the server to create the build request form. For the
 * contents and format of "buildDefs", see BuildDefineResponse.java.
 *
 * @param insertPoint Where to create the stack editor
 * @param buildDefs Default build settings
 * @param matchTypes All recipe match types ordered by increasing precision.
 * @param horizonEnabled Indicates if horizon support is enabled.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
PopulateDefineForm = function _populateDefineForm(
      insertPoint,
      buildDefs,
      horizonEnabled,
      matchTypes)
{
   var self = this;

   /* Create stack editor for the build request form */
   var stackEd = new StackEditor(insertPoint, AfTranslate("T.BUILDS.BUILD_SETTINGS"));

   /* Stack group for the default settings for the batch */
   var batchGroup = stackEd.AddGroup({
      titleT: "T.BUILDS.BATCH_DEFAULTS",
      collapsible: false,
      name: "batch"
   });

   // Create a link to create workpool page if workpools are not available.
   var wpUnits = $('<a id="wp-create"></a>')
      .text('Create Workpool')
      .click(function(event) {
         VmTAF.contentNavigator.LoadPage('/workpools/create');
         event.preventDefault();
      });

   // Display 'add horizon settings to build' checkbox.
   var rtChangeFunc = function(newData, dummy, self) {
      if (VmTAF.isHzCapable(newData) && self.find('#runtimeGroupHorizon').length > 0) {
         self.parent().find('#addHorizonHolder').fadeIn(500);
      } else {
         self.parent().find('#addHorizonHolder').fadeOut(500);
      }
   };

   var rtUnits = $('<input>').attr({
      name : 'addHorizonIntegration',
      id: 'addHorizon',
      type: 'checkbox'
   });
   rtUnits = $('<div id="addHorizonHolder"></div>')
      .append(rtUnits)
      .append('<label for="addHorizon"> ' + AfTranslate("T.COMMON.APPLY_HORIZON") + '</label>');

   if (!horizonEnabled) {
      rtUnits.addClass('no-show');
   }

   // Wrap with a dummy span as it will be removed while passing into addRow.
   rtUnits = $('<span id="dummy"></span>').append(rtUnits);

   /* Batch workpool */
   batchGroup.AddRow({
      label: "Workpool",
      name: "workpoolId",
      type: StackEditor.PULLDOWN,
      required: true,
      options: buildDefs.buildComponents.workpoolOptions.options,
      value: buildDefs.buildComponents.workpoolOptions.value,
      units: wpUnits
   });

   batchGroup.AddRow({
      label: "ThinApp Version",
      name: "runtimeId",
      type: StackEditor.PULLDOWN,
      options: buildDefs.buildComponents.runtimeOptions.options,
      value: buildDefs.buildComponents.runtimeOptions.value,
      changeFunc: rtChangeFunc,
      units: rtUnits.html()
   });

   batchGroup.AddRow({
      label: "Project Location",
      name: "datastoreId",
      type: StackEditor.PULLDOWN,
      required: true,
      options: buildDefs.buildComponents.datastoreOptions.options,
      value: buildDefs.buildComponents.datastoreOptions.value
   });

   /* Collect all recipe names and IDs, and a map from ID to name. */
   var recipeNames = [];
   var recipeIds = [];
   var recipeMap = {};
   var recipeOptions = [];

   for (var i = 0; i < buildDefs.recipes.length; i++) {
      var recipe = buildDefs.recipes[i];

      var displayName = recipe.name + " (" + recipe.dataSourceName + ")";
      var recipeOption = { display: displayName, key : recipe.id };
      recipeOptions.push(recipeOption);
      recipeMap[recipe.id] = recipeOption;
   }

   /* The default batch recipe selection: */
   var defaultBatchRecipe = "#NONE";
   var defaultBatch = "#BATCH";

   /* Data we need to know when a batch recipe is chosen */
   var recipeChangeData = {
      group: batchGroup,
      request: null,
      defs: buildDefs,
      numFixedFields: 4
   };

   /* Batch recipe */
   batchGroup.AddRow({
      label: "Recipe",
      name: "recipeId",
      type: StackEditor.PULLDOWN,
      options: [{display: "None", key: defaultBatchRecipe}].concat(recipeOptions),
      value: defaultBatchRecipe,
      changeData: recipeChangeData,
      changeFunc: self.showRecipeVariables
   });

   /* Trigger the recipe changeFunc now to set initial variables, etc */
   self.showRecipeVariables(defaultBatchRecipe, recipeChangeData);

   /* Default value is #BATCH, so we dont have to display the horizon checkbox, hence hide it. */
   rtUnits.find('#addHorizonHolder').addClass('no-show');

   /* Stack group for every requested application */
   for (var i = 0; i < buildDefs.requests.length; i++) {
      var request = buildDefs.requests[i];

      var group = stackEd.AddGroup({
         title: request.captureRequest.displayName,
         collapsible: true,
         collapsed: true,
         icon: AfGetIconUrl(request.icons, window.VmTAF.iconSizeSmall),
         name: "requests",
         serializeAsArray: true
      });

      /* Application ID (Hidden row) */
      group.AddRow({
         type: StackEditor.HIDDEN,
         name: "applicationId",
         value: request.captureRequest.applicationId
      });

      /* Application workpool */
      group.AddRow({
         label: "Workpool",
         type: StackEditor.PULLDOWN,
         name: "workpoolId",
         inputData: { subProperty: "requests", isArray: true },
         options: [{ display: "Use batch default", key: defaultBatch }].concat(buildDefs.buildComponents.workpoolOptions.options),
         value: defaultBatch
      });

      group.AddRow({
         label: "ThinApp Version",
         type: StackEditor.PULLDOWN,
         name: "runtimeId",
         inputData: { subProperty: "requests", isArray: true },
         options: [{ display: "Use batch default", key: defaultBatch }].concat(buildDefs.buildComponents.runtimeOptions.options),
         value: defaultBatch,
         changeFunc: rtChangeFunc,
         units: rtUnits.html()
      });

      group.AddRow({
         label: "Project Location",
         type: StackEditor.PULLDOWN,
         name: "datastoreId",
         inputData: { subProperty: "requests", isArray: true },
         options: [{ display: "Use batch default", key: defaultBatch }].concat(buildDefs.buildComponents.datastoreOptions.options),
         value: defaultBatch
      });

      /*
       * Figure out what the default recipe choice should be.
       * We pick the first 'precise' match if there is one, else the
       * first 'partial' match if there is one, else we leave it as
       * batch default. 'Wild' matches are never selected automatically.
       */
      var defaultRecipeId =
         request.recipeMatches.precise ? request.recipeMatches.precise[0] :
         request.recipeMatches.partial ? request.recipeMatches.partial[0] :
         "#BATCH";

      /* Data we need to know when a recipe is chosen */
      var recipeChangeData = {
         group: group,
         request: request,
         defs: buildDefs,
         numFixedFields: 5
      };

      /* Application recipe */
      group.AddRow({
         label: "Recipe",
         type: StackEditor.PULLDOWN,
         name: "recipeId",
         options: self.createRecipePulldownGroups(request, matchTypes, recipeMap),
         value: defaultRecipeId,
         changeData: recipeChangeData,
         changeFunc: self.showRecipeVariables
      });

      /* Trigger the recipe changeFunc now to set initial variables, etc */
      self.showRecipeVariables(defaultRecipeId, recipeChangeData);
   }

   /* Footer buttons */
   stackEd.AddFooter([{
      label: "Submit",
      clickData: stackEd,
      validate: true,
      clickFunc: function(stackEditor) {
         var json = stackEditor.Serialize();
         self.SubmitBuildRequest(json);
      }
   }, {
      label: "Cancel",
      clickFunc: function() {
         VmTAF.contentNavigator.GoBack();
      }
   }]);
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.SubmitBuildRequest
 *
 * Take the JSON from the build request form, massage it into a format that the
 * server will accept, and post it. This will generate some new build tasks
 * on the server.
 *
 * @param formData Data from the build request stack editor
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
SubmitBuildRequest = function _buildApplications(formData)
{
   // Validate the form, and if errors exist do not proceed.
   try {
      this.validate(formData);
   }
   catch(error) {
      AfError("Invalid Build Request", error);
      return;
   }

   /* Put batch variables into their own object */
   formData.batch.recipeVariableValues = {};
   for (var prop in formData.batch) {
      if (prop[0] == '$') {
         var name = prop.slice(1); // drop the leading $
         formData.batch.recipeVariableValues[name] = formData.batch[prop];
         delete formData.batch[prop];
      }
   }

   /* We need to massage the form data into a list of BuildRequest objects */
   var json = [];
   for (var r = 0; r < formData.requests.length; r++) {
      var request = formData.requests[r];
      request.recipeVariableValues = {};

      /* Swap 'Use batch' for actual batch value */
      if (request.workpoolId == "#BATCH") {
         request.workpoolId = formData.batch.workpoolId;
      }

      /* If batch runtime value, the use the horizon value as well. */
      if (request.runtimeId == "#BATCH") {
         request.runtimeId = formData.batch.runtimeId;
         request.addHorizonIntegration = formData.batch.addHorizonIntegration;
      }
      /* Validate if the addHorizon flag is set appropriately. Hiding checkbox does not necessarily uncheck it. */
      request.addHorizonIntegration = VmTAF.isHzCapable(request.runtimeId) && request.addHorizonIntegration;

      if (request.datastoreId == "#BATCH") {
         request.datastoreId = formData.batch.datastoreId;
      }
      if (request.recipeId == "#BATCH") {
         request.recipeId = formData.batch.recipeId;
         request.recipeVariableValues = formData.batch.recipeVariableValues;
      }

      /* Swap 'Use None' for nothing */
      if (request.recipeId == "#NONE") {
         request.recipeId = null;
      }

      /* Put variables into their own object */
      for (var prop in request) {
         if (prop[0] == '$') {
            var name = prop.slice(1); // drop the leading $
            request.recipeVariableValues[name] = request[prop];
            delete request[prop];
         }
      }
      json.push(request);
   }

   /* Post it */
   AfAjax({
      method: 'POST',
      contentType: 'application/json',
      url: '/api/builds',
      data: json,

      success: function() {
         AfNotifyT(
            'T.BUILDS.REQUESTS_SUBMITTED',
            'M.BUILDS.REQUESTS_SUBMITTED');
         // TODO(rude): make sure this works in the new UI
         if (window.VmTAF.dialogHolder) {
            window.VmTAF.dialogHolder.dialog("close");
         } else {
           window.VmTAF.contentNavigator.GoBack();
         }
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Error', 'Request failed: ' + errorThrown);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.validate
 *
 * Validate the given build page, making sure required fields are present before
 * sending it to the server for create or update.
 *
 * @param formData
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
validate = function _validate(formData)
{
   /* Default workpool is required */
   if (!formData.batch.workpoolId) {
      // default workpool cannot be selected as there are no available workpools
      throw AfTranslate('M.WORKPOOL.NOT.AVAIL.FOR.DEFAULT');
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.SavePackageIni
 *
 * Upload the specified packageIni data to the server for storing with the
 * given build. This data will be passed back to CWS for updating the
 * ThinApp settings.
 *
 * @param buildId
 * @param packageIni
 * @param successFunc
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
SavePackageIni = function _savePackageIni(buildId, packageIni, successFunc)
{
   var self = this;
   AfLog('Saving packageIni for build id ' + buildId);

   AfAjax({
      url: '/api/builds/' + buildId + '/packageIni',
      method: 'PUT',
      contentType: 'application/json',
      data: packageIni,

      beforeSend: function() {
         AfStartPleaseWaitT('M.BUILDS.SAVING_SETTINGS');
      },

      success: function(data) {
         AfNotifyT('T.BUILDS.INI.SAVED', 'M.BUILDS.INI.SAVED');
         successFunc(data);
      },

      error: function(jqXHR, textStatus, errorThrown) {
         if (jqXHR.status == 409 && 'NO_CRUD_DURING_REBUILD' == jqXHR.responseText) {
            AfErrorT('T.BUILDS.NO_CRUD_DURING_REBUILD',
                     'M.BUILDS.NO_CRUD_DURING_REBUILD');
            VmTAF.contentNavigator.GoBack();
         }
         else {
            AfError('Error', 'There was a problem saving your changes.');
         }
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });

};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.SaveRegistry
 *
 * Upload the specified registry data to the server for storing with its
 * build. This data will be passed back to CWS for updating the ThinApp
 * settings.
 *
 * @param registry
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
SaveRegistry = function _saveRegistry(registryEditor)
{
   var success = registryEditor.ProcessChanges(
         this.registryCreateCallback,
         this.registryEditCallback,
         this.registryDeleteCallback);

   if (success) {
      AfNotifyT('T.BUILDS.REGISTRY.SAVED', 'M.BUILDS.REGISTRY.SAVED');
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.SaveFileSystem
 *
 * Upload the specified file system data to the server for storing with its
 * build. This data will be passed back to CWS for updating the ThinApp
 * settings.
 *
 * @param filesystemEditor
 * @param buildId
 * @param forceRescan
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
SaveFileSystem = function _saveFileSystem(
      filesystemEditor,
      buildId,
      forceRescan)
{
   var self = this;

   /* First, save all changes made in the UI (e.g. isolation mode) */
   AfLog('Processing file system editor changes');
   var success = filesystemEditor.ProcessChanges(
         this.filesystemCreateCallback,
         this.filesystemEditCallback,
         this.filesystemDeleteCallback);

   /* Then, tell CWS to rescan in case the user made manual edits */
   if (forceRescan) {
      AfLog('Forcing server rescan');
      self.ForceServerRescan(buildId, function whenDone() {
         AfLog('Rescan complete, reloading data');
         self.LoadBuildSettings(buildId, null, function whenDone(data) {
            AfLog('Reloading complete, populating editor');
            filesystemEditor.Populate(data.dirRoot, data.projectDir);
         });
      });
   }

   if (success) {
      AfNotifyT('T.BUILDS.DIRECTORIES.SAVED', 'M.BUILDS.DIRECTORIES.SAVED');
   }
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.CreateDialog
 *
 * Create a new dialog element. For convenience, the returned dialog contains
 * a property "manager" which is a reference to the manager instance invoking
 * this function.
 *
 * @param parentId ID of the DIV element to hold the dialog.
 * @param title Title for the dialog.
 * @param html The HTML content for the dialog body.
 *
 * TODO: get drag mode to work
 * TODO: get modal to work
 *
 * note: moved from the base class so that we can remove the YUI
 * libraries from pages which don't need it.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
CreateDialog = function _createDialog(parentId, title, html, buttons)
{
   var top = '<div class="dialog">';
   top += '<div class="hd">' + title + '</div>';
   top += '<div class="bd">';

   var bottom = '</div></div>';

   var dialog = new YAHOO.widget.Dialog('myDialog', {
      fixedcenter: true,
      constraintoviewport: true,
      underlay: "shadow",
      close: true,
      draggable: false,
      modal: false
   });

   dialog.setBody(top+html+bottom);
   dialog.cfg.queueProperty("buttons", buttons);
   dialog.render(parentId);
   dialog.manager = this;

   return dialog;
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.ShowNewRegistryValueDialog
 *
 * Shows a dialog which lets the user define a new registry key.
 * This will fetch the view and render it as a modal dialog. When OK is pressed,
 * the specified callback is invoked, and the serialized form data is passed
 * to it. If this callback returns true, the dialog closes; if false, it
 * stays open.
 *
 * @param holderId Div needed to create a dialog.
 * @param okCallback Function invoked when OK is pressed.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
ShowNewRegistryValueDialog = function _showNewRegistryValueDialog(
      holderId,
      okCallback)
{
   var self = this;
   var title = 'New Registry Value';

   /* There is no editor dialog yet, so ask for HTML and create it */
   AfAjax({
      url: '/builds/regkey/new',
      method: 'GET',
      success: function(data) {
         /* Clean up the existing dialog, if one exists */
         if (self.newRegValueDialog) {
            self.newRegValueDialog.destroy();
         }

         /* Got the HTML; now create a dialog */
         self.newRegValueDialog = self.CreateDialog(holderId, title, data, [ {
            text: "OK",
            handler: function() {
               var json = AfSerializeForm($('#regkey-edit-form'));
               if (okCallback(json)) {
                  self.newRegValueDialog.hide();
               }
            },
            isDefault: true
         }, {
            text: "Cancel",
            handler: function() {
               self.newRegValueDialog.hide(); }
         }]);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.ForceServerRescan
 *
 * Tell the server that build settings need refreshing: i.e. they may have
 * been edited by hand, and not via the usual APIs. This is used, for example,
 * if the user opens the CWS project directory directly, and makes manual
 * changes.
 *
 * @param buildId
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
ForceServerRescan = function _forceServerRescan(buildId, whenDone)
{
   var url = '/api/builds/' + buildId + '/settings/refresh';
   AfLog('Refreshing build settings: ' + url);

   AfAjax({
      url: url,
      method: 'PUT',

      beforeSend: function() {
         AfStartPleaseWaitT('M.BUILDS.BUILD_RESCAN_IN_PROGRESS');
      },

      success: function(data) {
         if (whenDone) {
            whenDone();
         }
      },

      error: function(jqXHR, textStatus, errorThrown) {
         AfError('Refresh Failed', 'The rescan failed.');
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.ResetAndReload
 *
 * Confirm the user really wants to do this, and if so, then reload the
 * specified settings data and invoke the 'whenDone' callback with the new
 * data.
 *
 * @param buildId
 * @param what What to load: "packageini", "directory", "registry"
 * @param whenDone Called with the new data once loaded.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
ResetAndReload = function _resetAndReload(buildId, what, whenDone)
{
   if (AfConfirmT('T.BUILDS.DISCARD_CHANGES', 'M.BUILDS.DISCARD_CHANGES')) {
      this.LoadBuildSettings(buildId, what, whenDone);
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.registryCreateCallback
 *
 * A callback invoked from registryEditor.ProcessChanges() whenever that
 * function finds a new key to be created. Returns a URL to the newly
 * created registry resource. Synchronous!
 *
 * @param buildId
 * @param parentRegistryId
 * @param registryData
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
registryCreateCallback = function(buildId, parentRegistryId, registryData)
{
   var newUrl = null;

   /* Create request requires two arguments: */
   var requestData = {
      parentId: parentRegistryId,
      key: registryData
   };

   AfAjax({
      url:         '/api/builds/' + buildId + '/registry/new',
      method:      'POST',
      contentType: 'application/json',
      async:       false,
      data:        JSON.stringify(requestData),
      success:     function(registryUrl) {
         newUrl = registryUrl;
      },
      error: errorHandlerForRebuildState
   });

   return newUrl;
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.registryEditCallback
 *
 * A callback invoked from registryEditor.ProcessChanges() whenever that
 * function finds a new key to be created. Synchronous!
 *
 * @param buildId
 * @param registryId
 * @param registryData
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
registryEditCallback = function(buildId, registryId, registryData)
{
   AfAjax({
      url:         '/api/builds/' + buildId + '/registry/' + registryId,
      method:      'PUT',
      contentType: 'application/json',
      async:       false,
      data:        JSON.stringify(registryData),
      success:     function() {
      },
      error: errorHandlerForRebuildState
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.registryDeleteCallback
 *
 * A callback invoked from registryEditor.ProcessChanges() whenever that
 * function finds a key to be deleted. Synchronous!
 *
 * @param buildId
 * @param registryId
 * @param registryData
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
registryDeleteCallback = function(buildId, registryId, registryData)
{
   var newUrl = null;
   var self = this;
   AfLog('Deleting registry key ' + registryData.path +
         ' for build #' + buildId);

   AfAjax({
      url:         '/api/builds/' + buildId + '/registry/' + registryId,
      method:      'DELETE',
      async:       false,
      success:     function(registryUrl) {
         AfLog('Deletion successful!');
      },
      error: errorHandlerForRebuildState
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.filesystemCreateCallback
 *
 * Invoked by the directory editor when there is a new directory to be created
 * for a build.
 *
 * @param buildId
 * @param parentDirId
 * @param directory
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
filesystemCreateCallback = function(buildId, parentDirId, dir)
{
   // XXX Full UI editing is disabled right now
   AfLog('not implemented!');
   return null;};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.filesystemEditCallback
 *
 * Invoked by the directory editor when a directory has been edited: either the
 * files have changed, or the attributes have changed. This is a synchronous
 * AJAX call.
 *
 * @param buildId
 * @param directoryId
 * @param directory
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
filesystemEditCallback = function(buildId, directoryId, directory)
{
   var data = JSON.stringify(directory);

   AfAjax({
      url: '/api/builds/' + buildId + '/directory/' + directoryId,
      method: 'PUT',
      contentType: 'application/json',
      async: false,
      data: data,
      success: function() { },
      error: errorHandlerForRebuildState
   });
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.filesystemDeleteCallback
 *
 * Invoked by the directory editor when a directory has been deleted from a
 * build.
 *
 * @param buildId
 * @param directoryId
 * @param directory
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
filesystemDeleteCallback = function(buildId, directoryId, directory)
{
   // XXX Full UI editing is disabled right now
   AfLog('not implemented!');
   return null;
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.makeAppBlock
 *
 * Private function to create an app block specific to the projects page.
 * This deviates from the AfCreateApplicationBlock and hence the separation.
 *
 * @param table HTML "table" element that displays all the builds.
 * @param jsonData JSON-formatted build summary data.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
makeAppBlock = function (app, imageSize, editLink) {
   var iconUrl = AfGetIconUrl(app.icons);
    return [
       '<a href="',
       window.VmTAF.contextPath,
       editLink,
       '">',
       '<div class="app-details"><span class="app-icon"><img width="',
       imageSize,
       '" height="',
       imageSize,
       '" src="',
       iconUrl,
       '" alt=""></span>&nbsp;<span class="app-name bold">',
       app.name,
       '</span>&nbsp;<span class="app-version">',
       app.version,
       '</span></div></a>'
    ].join('');
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.createBuildRows
 *
 * Public function to update the rows of the build table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the builds.
 * @param jsonData JSON-formatted build summary data.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
createBuildRows = function _createBuildRows(self, jsonData, options) {

   // Remove all rows but the first (the header).
   self.tableWrapper.ClearTable();

   // Add a row per build.
   for (var i = 0; i < jsonData.builds.length; i++) {
      var build = jsonData.builds[i];

      // Check-box to select this build.
      var cb = $('<input type="checkbox">');

      var buildElem = '';
      if (window.VmTAF.newUI) {
         // Add horizon icon indicating build has horizon setting added and TAF has horizon enabled.
         if (build.hzSupported && jsonData.hzEnabled) {
            buildElem = '<img width="16" height="16" src="' + VmTAF.imagePath
               + '/horizon.gif" title="Horizon settings included" /> ';
         }

         if (build.status == "REBUILDING") {
            buildElem += build.buildName;
         } else {
            buildElem = [
               buildElem,
               '<a class="edit-link" href="',
               window.VmTAF.contextPath,
               '/builds/edit/',
               build.id,
               '">',
               build.buildName,
               '</a>'
            ].join('');
         }

      } else {
         // Build id: You can only edit if it's not rebuilding
         buildElem = self.createRecordLink(build.buildName, build.id, null);

         // Wrap build ID with a click function, if it's not rebuilding
         if (build.status != "REBUILDING") {
            var script = "VmTAF.buildManager.LoadBuildEditorView(" + build.id + ");";
            var link = $('<button onclick="' + script + '">');
            link.addClass('edit-link');
            link.append(buildElem);
            buildElem = link;
         }
         buildElem = AfHtml(buildElem);
      }

      // The application that was converted.
      var appDiv = self.makeAppBlock(build, window.VmTAF.iconSizeSmall, '/builds/like/' + build.id);

      // Need a rebuild?
      var rebuildAndStatus = '';
      if (self.needsRebuild(build)) {
         rebuildAndStatus = $('<img width="16" height="16">')
            .attr('src', VmTAF.imagePath + '/check.png');

         rebuildAndStatus = $('<div></div>')
            .append(rebuildAndStatus)
            .append(AfTranslate('T.BUILDS.STATUS.' + build.status))
            .attr('title' , AfTranslate('T.BUILDS.NEEDS_REBUILD'))
            .addClass("needs-rebuild underline-dots help");
      }
      else {
         rebuildAndStatus = AfTranslate('T.BUILDS.STATUS.' + build.status);
      }

      // If rebuilding, change checkbox class for style
      if (build.status == 'REBUILDING') {
         cb.addClass('rebuilding').attr('disabled', true);
      }

      // Lookup datastore. If not found, display error accordingly.
      var dsName = '';
      var ds = jsonData.datastoresMap[build.datastoreId];
      if (!ds) {
         // Display DS N/A status, as the datastore does not exist.
         dsName = 'Datastore was removed';
         cb.addClass('ds-removed').attr('disabled', true);
      } else {
         dsName = ds.name;
         if (ds.status == "offline") {
            cb.addClass('ds-offline').attr('disabled', true);
            dsName += ' (offline)';
         }
      }

      // osType & variant (optional for import projects)
      var os = '';
      if (build.osType) {
         os = AfTranslate('M.OSTYPE.' + build.osType);
         if(build.osVariant && build.osVariant != '') {
            os += ' ' + AfTranslate('M.OSVARIANT.' + build.osType + '.' + build.osVariant);
         }
      }

      // Display a strike-through text of the old runtime, if a new one is selected and yet to be rebuilt.
      var runtime = build.runtime;
      if (self.needsRebuild(build) && build.newRuntime && build.newRuntime != '' && build.newRuntime != runtime) {
         runtime = ' <span class="underline-dots help" title="Rebuild to apply this new ThinApp Version">'
            + build.newRuntime + ' <span class="line-through">' + runtime + '<span></span>';
      }

      var row = new Array();

      row.push(AfHtml(cb));
      row.push(buildElem);
      row.push(AfHtml(appDiv));
      row.push(AfTimeago(build.built));
      row.push(build.thinapps.length);
      row.push(AfHtml(rebuildAndStatus));
      row.push(os);
      row.push(AfHtml(runtime));
      row.push(AfHtml(dsName));
      row.push(AfTranslate('T.BUILDS.SOURCE.' + build.source));

      // Add row to the table.
      self.tableWrapper.AddRow(row, build);
   }

   self.tableWrapper.DrawTable();

   /* Disable any rows that are rebuilding, ds-offine or ds-removed:
    * 1) Add the disabled CSS class to the table row
    * 2) Disable the anchor click.
    */
   $('.rebuilding, .ds-offline, .ds-removed').parents('tr')
      .addClass('disabled')
      .find('.edit-link').attr('href', 'javascript:$.noop();');

   self.updateBuildSidebar(jsonData);
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.needsRebuild
 *
 * Private function to compute if the build needs a rebuild.
 * Rebuild is needed when the setting were changed after a build or there are no
 * thinapp files (due to project import)
 *
 * @param build
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
needsRebuild = function needsRebuild(build) {
   return (build.settingsEdited > build.built || !build.thinapps.length);
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.updateBuildSidebar
 *
 * Private function to update the sidebar if the sidebar holder exists.
 * The corresponding info is partially computed in the createAppRows and the
 * rest are computed and painted here.
 *
 * @param jsonData JSON-formatted build summary data.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
updateBuildSidebar = function(jsonData) {
   if (!(VmTAF.newUI && $('#idStatus'))) {
      // Not new ui or does not have a placeholder.
      return;
   }

   this.replaceHistogramList({
      ulElementId: '#idStatus',
      tableId: '#builds-table',
      data: this.buildHistogram(jsonData.builds, "status", 'T.BUILDS.STATUS.')
   });

   this.replaceHistogramList({
      ulElementId: '#idSource',
      tableId: '#builds-table',
      data: this.buildHistogram(jsonData.builds, "source", 'T.BUILDS.SOURCE.')
   });

   this.replaceHistogramList({
      ulElementId: '#idOS',
      tableId: '#builds-table',
      data: this.buildHistogram(jsonData.builds, "osType", 'M.OSTYPE.')
   });

   this.replaceHistogramList({
      ulElementId: '#idRuntime',
      tableId: '#builds-table',
      data: this.buildHistogram(jsonData.builds, "runtime")
   });

   // Create a histogram using ids, and then replace with names and sort the result.
   var dsArray = this.buildHistogram(jsonData.builds, "datastoreId");
   if (jsonData.datastoresMap) {
      for (var i=0; i< dsArray.length; i++) {
         eachElement = dsArray[i];
         ds = jsonData.datastoresMap[eachElement.name];
         if (ds) { // This is a valid datastore. Now use the name and status
            eachElement.name = ds.name;
            if (ds.status == "offline") {
               eachElement.name += ' (offline)';
            }
         }
         else {
            eachElement.name = 'Other-' + eachElement.name;
         }
      }
      dsArray.sort(function(a,b) {
         var result = b.count - a.count;   // decreasing by count
         if (result == 0) {
            result = a.name - b.name;  // increasing by name
         }
         return result;
      });
   }

   this.replaceHistogramList({
      ulElementId: '#idDatastore',
      tableId: '#builds-table',
      data: dsArray
   });
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.updateBuildSidebar
 *
 * Private function to update the sidebar if the sidebar holder exists.
 * The corresponding info is partially computed in the createAppRows and the
 * rest are computed and painted here.
 *
 * @param jsonData JSON-formatted build summary data.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
updateBuildByAppsSidebar = function _updateBuildByAppsSidebar(jsonData)
{
   if (!(VmTAF.newUI && $('#vendorList'))) {
      // Not new ui or does not have placeholders.
      return;
   }

   this.replaceHistogramList({
      ulElementId: '#vendorList',
      tableId: '#apps-table',
      data: this.buildHistogram(jsonData.buildGroups, "baseApp.vendor")
   });
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.createAppRows
 *
 * Public function to update the rows of the table that shows builds organized
 * by application (i.e. one row per app, no matter how many builds it has).
 *
 * @param table HTML "table" element that displays all the data.
 * @param jsonData JSON-formatted build summary data.
 * @param options
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
createAppRows = function _createAppRows(self, jsonData, options)
{
   var buildGroupByApps = jsonData.buildGroups || [];

   // To avoid memory leaks, unbind prior event handlers before clearing table
   $('.app-button').unbind('click');

   /*
    * The data is grouped by application. Makes it easy to create one table
    * row per unique application.
    */

   // Remove all data rows from the table
   self.tableWrapper.ClearTable();

   // Create Vendor
   for (var i = 0; i < buildGroupByApps.length; i++) {
      var buildGroup = buildGroupByApps[i];

         // Track total builds and published builds per application
      var builtCount = buildGroup.stateList.length;
      var publishedCount = 0;
      for (var j = 0; j < builtCount; j++) {
         if (buildGroup.stateList[j] == 'PUBLISHED') {
            publishedCount++;
         }
      }

      var imageSize = (window.VmTAF.newUI) ? window.VmTAF.iconSizeSmall : window.VmTAF.iconSizeLarge;

      var app;
      // Element to show the application
      if (window.VmTAF.newUI) {
         app = self.makeAppBlock(buildGroup.baseApp, imageSize, '/builds/like/' + buildGroup.recentBuiltBuildId);
      } else {
         app = self.wrapWithLoadLink(
            AfCreateApplicationBlock(buildGroup.baseApp),
            '/builds/like/' + buildGroup.recentBuiltBuildId);
      }

      // Wrap app with a link
      var appLink = $('<a>').append(app);

      var totals = [
         '<span title="',
         publishedCount,
         '">',
         publishedCount,
         ' / ',
         builtCount,
         '</span>'
         ].join('');

      // Create row
      var row = new Array();
      row.push(AfHtml(appLink));
      row.push(AfHtml(buildGroup.baseApp.vendor));
      row.push(AfTimeago(buildGroup.recentBuiltTimestamp));
      row.push(totals);

      // Add row to the table.
      self.tableWrapper.AddRow(row, buildGroup);
   }

   self.tableWrapper.DrawTable();

   // Update the sidebar with vendor info.
   self.updateBuildByAppsSidebar(jsonData);
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.LoadBuildEditorView
 *
 * Fetches the build settings for the given build, and if successful opens
 * the build editor view to display and edit them.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
LoadBuildEditorView = function(buildId)
{
   var self = this;

   /* Load build settings */
   self.LoadBuildSettings(buildId, null,
         function _success(data) {
            /* Success: open the build editor page */
            VmTAF.buildSettings = data;
            VmTAF.contentNavigator.LoadPage('/builds/edit/' + buildId);
         },
         function _error() {
            /* Fail: stay where we are */
            AfLog("Failed to load build settings; editor not opened");
         });
};

/**
 * -----------------------------------------------------------------------------
 * errorHandlerForRebuildState
 *
 * This handler takes care of the rebuild specific error handler for the
 * package ini / registry editor related CRUD operations.
 *
 * If the build is being rebuilt, an appropriate error message is displayed.
 *
 * This function is specifically outside of BuildManager scope, as its used in
 * BuildManager.js and RegistryEditor.js files with convoluted callbacks.
 *
 * @see afutils.AfAjaxDefaultErrorHandler()
 * @param jqXHR
 * @param textStatus
 * @param errorThrown
 * -----------------------------------------------------------------------------
 */
// BuildManager.prototype. XXX fix me! currently in quasi global state, try making it inline.
errorHandlerForRebuildState = function _errorHandlerForRebuildState(jqXHR, textStatus, errorThrown)
{
   if (jqXHR.status == 409 && 'NO_CRUD_DURING_REBUILD' == jqXHR.responseText) {
      AfErrorT(
         'T.BUILDS.NO_CRUD_DURING_REBUILD',
         'M.BUILDS.NO_CRUD_DURING_REBUILD');
   }
   else {
      // Use default handler in all other cases
      AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown);
   }
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.isSameApp
 *
 * Test to see if two (JSON defined) applications are the same.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
isSameApp = function(app1, app2)
{
   return app1.name == app2.name &&
          app1.version == app2.version &&
          app1.locale == app2.locale &&
          app1.installerRevision == app2.installerRevision;
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.createRecipePulldownGroups
 *
 * Create the value object that the stack editor needs to create a grouped
 * pulldown. We put all recipes of a given type into a group, and then put
 * common options, like "Use batch default", into another group.
 *
 * @param buildRequest
 * @param matchTypes All recipe match types ordered by increasing precision.
 * @param recipeMap Map of recipe IDs to display names.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
createRecipePulldownGroups = function(buildRequest, matchTypes, recipeMap)
{
   var self = this;
   var optionGroups = [];

   /* First, add recommended recipes, starting with the most precise. */
   for (var ti = matchTypes.length - 1; ti >= 0; ti--) {
      var type = matchTypes[ti];

      if (buildRequest.recipeMatches[type] && buildRequest.recipeMatches[type].length) {
         var recipeIds = buildRequest.recipeMatches[type];
         var recipeNames = [];

         for (var ri = 0; ri < recipeIds.length; ri++) {
            recipeNames.push(recipeMap[recipeIds[ri]]);
         }

         optionGroups.push({
            display: ("T.RECIPES.MATCH_TYPE." + type),
            translate : true,
            options: recipeNames
         });
      }
   }

   /* Other recipe choices */
   optionGroups.push({
      display: 'Other',
      options: [ {display: "Use batch default", key: "#BATCH"}, {display: "None", key: "#NONE"} ]
   });

   return optionGroups;
};


/**
 * -----------------------------------------------------------------------------
 * BuildManager.showRecipeVariables
 *
 * This is a callback function that is invoked whenever the user selects a
 * recipe from a pulldown list. It will update the UI to show input text fields
 * for each variable in the chosen recipe.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
showRecipeVariables = function _showRecipeVariables(newValue, changeData)
{
   var newRecipeId = newValue;
   var defs = changeData.defs;
   var request = changeData.request;
   var stackGroup = changeData.group;
   var numFixedFields = changeData.numFixedFields;

   /* Remove any old variable fields from a previous selection */
   while (stackGroup.NumRows() > numFixedFields) {
      stackGroup.DeleteRow(stackGroup.NumRows() - 1);
   }

   if (newRecipeId == "#NONE") {
      /* Use no recipe, so no variables to show */
      return;
   }

   if (newRecipeId == "#BATCH") {
      /* Use whatever is define for the batch, so don't show variables here */
      return;
   }

   /* Find the recipe that was selected */
   var recipe = null;
   for (var i = 0; i < defs.recipes.length; i++) {
      if (defs.recipes[i].id == newRecipeId) {
         recipe = defs.recipes[i];
         break;
      }
   }

   if (!recipe) {
      /* Internal error! */
      AfLog('Recipe "' + newRecipeId + "' not found in defaults list");
      return;
   }

   /* Add variables as stack group rows */
   for (var i = 0; i < recipe.variables.length; i++) {
      var v = recipe.variables[i];

      stackGroup.AddRow({
         name: '$' + v.name,
         label: v.name,
         required: v.required
      });
   }
};

/**
 * -----------------------------------------------------------------------------
 * BuildManager.Import
 *
 * Import ThinApp projects from a datastore.
 * -----------------------------------------------------------------------------
 */
BuildManager.prototype.
Import = function _import(form)
{
   var json = AfSerializeForm(form);

   /* Change 'serverShare' into 'server' and 'share' */
   AfSplitServerShare(json.datastore);
   AfLog(json);

   /* Update the addHorizonIntegration if compatibile with runtime. */
   json.addHorizonIntegration = (json.addHorizonIntegration && VmTAF.isHzCapable(json.runtimeId));

   AfAjax({
      method: 'POST',
      contentType: 'application/json',
      url: '/api/builds/import',
      data: json,

      beforeSend: function() {
         AfStartPleaseWaitT('M.STORAGE.SAVING');
      },

      success: function() {
         AfNotifyT('T.STORAGE.SAVED', 'M.STORAGE.SAVED');

         if (!VmTAF.dialogHolder) {
            VmTAF.contentNavigator.GoBack();
         } else {
            VmTAF.dialogHolder.dialog("close");
         }
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};
