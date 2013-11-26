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
 * WorkpoolManager
 *
 * A "class" that encapsulates all the methods for dealing with workpools.
 * -----------------------------------------------------------------------------
 */
function WorkpoolManager(cloneSupported, isWS, table, status)
{
   this.refreshUrl = '/api/workpools';
   if (status) {
      this.refreshUrl += '?status=' + status;
   }

   if (table) {
      this.CreateTableWrapper(table);
   }
   this.cloneSupported = cloneSupported;
   this.isWS = isWS;
}

WorkpoolManager.prototype = new AbstractManager('Workpool');


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.UpdateWorkpool
 *
 * Public function to load data from the server into the workpool table.
 *
 * @param table HTML "table" element that displays all the workpools.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
UpdateWorkpool = function _updateWorkpool(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.COMMON.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.GetWorkpoolAndPopulateEditForm
 *
 * Fetch the named workpool from the server, and if successful load that
 * into the workpool edit form.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
GetWorkpoolAndPopulateEditForm = function _PopulateWorkpoolEditForm(workpoolId)
{
   var self = this;

   AfAjax({
      method: 'GET',
      url: '/api/workpools/' + workpoolId,
      success: function(data) {
         self.PopulateEditWorkpoolForm(data);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.PopulateEditWorkpoolForm
 *
 * Public function to fill out the workpool edit view with the given
 * workpool (JSON format).
 *
 * @param workpool object
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
PopulateEditWorkpoolForm = function _PopulateEditWorkpoolForm(workpool)
{
   // Set the workpool slider value and then the display value.
   var sliderDiv = $('#wp-slider');
   if (sliderDiv) {
      sliderDiv.slider( "value" , workpool.maximum);
   }
   $('[name="maximum"]').val(workpool.maximum);
   $('[name="name"]').val(workpool.name);

   // Get the osType and decipher the type of workpool
   var wpAndOsType = this.decipherWorkpoolAndOsType(workpool, true);
   $('#wp-type').empty().append(wpAndOsType.wpTypeDesc);
   $('#wp-osType').empty().append(wpAndOsType.osTypeDesc);


   // Display the variant if this osType has 1.
   if(wpAndOsType.osVariant) {
      $('#wp-osVariant')
         .empty()
         .append(wpAndOsType.osVariantDesc)
         .closest('.stack-row').show();
   }

   $('#wp-state').empty().append(AfTranslate(
         'M.WORKPOOL.STATE.' + workpool.state));

   // If its a linked workpool, display the image name & state.
   if(wpAndOsType.wpType == 'LINKED') {
      var imgLink = $('<a>').addClass('button-link')
         .text(workpool.vmImage.name)
         .click(function(e) {
            // Clicking this link takes the user to the image list page.
            e.preventDefault();
            VmTAF.contentNavigator.LoadPage('/workpools/image');
         });
      var imgName = $('#image-name').empty().append(imgLink);
      var imgState = $('#image-state').empty().append(AfTranslate(
            'M.WORKPOOL.IMAGE.STATE.' + workpool.vmImage.state));

      // Toggle the display for the stack-row holding these fields
      imgName.closest('.stack-row').show();
      imgState.closest('.stack-row').show();
   }
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.InitDeleteSelectedWorkpool
 *
 * Public function invoked when the delete button is clicked. This performs the
 * initial validation and gets further input from the user.
 * Upon successful validation and gaining info, proceed to delete workpools.
 *
 * @param popupHolder
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
InitDeleteSelectedWorkpool = function _initDeleteSelectedWorkpool(popupHolder)
{
   var self = this;
   var workpools = this.tableWrapper.GetSelectedRowData();

   if (workpools.length == 0) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.WORKPOOL.NO_SELECTION');
      return;
   }

   // Create the delData with 2 columns.
   var delData = [];
   for (var i = 0; i < workpools.length; i++) {
      var cb = $('<input type="checkbox">').attr('id', 'wp_' + workpools[i].id);
      delData.push([AfHtml(workpools[i].name), AfHtml(cb)]);
   }

   // Create the desc, and confirmation checkboxes on the dialog.
   var descDiv = $('<div></div>').addClass('desc').text(
         AfTranslate('M.WORKPOOL.VM.DELETE.DESC'));
   var label = $('<label>').attr('for','confirm').text(
         AfTranslate('M.WORKPOOL.VM.DELETE.CONFIRM'));
   var confDiv = $('<div></div>').addClass('desc').prepend(label);
   var confirmCb = $('<input type="checkbox">').attr('id', 'confirm');
   confDiv.prepend(confirmCb);

   popupHolder.html('<table cellpadding="0" cellspacing="0" border="0" '
         + 'class="data ui-widget" id="wp-table"></table>');
   popupHolder.append(confDiv);
   popupHolder.prepend(descDiv);

   var dataTableHolder = $('#wp-table');
   dataTableHolder.dataTable({
      'bPaginate': false,
      'bFilter': false,
      'bSort': false,
      'sDom': '<"top"f>rt<"bottom"ilp><"clear">',
      'aoColumns': [{ 'sTitle': 'Workpool Name' },
                    { 'sTitle': AfTranslate('T.WORKPOOL.VM.DELETE'),
                      'sClass': 'center' }],
      'aaData': delData
   });

   // Add datatable css for table inside the dialog.
   dataTableHolder.find('thead th').addClass('ui-state-default');
   dataTableHolder.find('tbody > tr').addClass('ui-widget-content');

   var self = this;
   // Pop up a modal asking user to mark checkboxes to indicate vm removal.
   popupHolder.dialog({
      minHeight: 250,
      height: 350,
      minWidth: 400,
      width: 600,
      zIndex: 500,
      modal: true,
      title: 'Delete Workpool & VM',
      buttons: { 'Delete': function() {
                    self.deleteSelectedWorkpool($(this), dataTableHolder);
                 },
                 'Cancel': function() {
                    self.endDeleteWorkpoolPopup($(this), dataTableHolder);
                 }
              }
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.endDeleteWorkpoolPopup
 *
 * Public function to cleanup the ui elements if the user selects cancel or
 * delete processing is complete.
 *
 * @param popupHolder
 * @param dataTableHolder
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
endDeleteWorkpoolPopup = function _endDeleteWorkpoolPopup(
      popupHolder,
      dataTableHolder)
{
   dataTableHolder.dataTable().fnDestroy();
   popupHolder.dialog('destroy');
   popupHolder.empty();
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.deleteSelectedWorkpool
 *
 * Private function to delete selected workpools from the displayed workpool
 * table and the selected vm flags indicating vm deletion from vi.
 *
 * @param dialogDiv
 * @param dataTableDiv
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
deleteSelectedWorkpool = function _deleteSelectedWorkpool(dialogDiv, dataTableDiv)
{
   var self = this;
   var workpools = this.tableWrapper.GetSelectedRowData();

   // Check if the user has checked the confirm screen, if not send them back.
   if(!dialogDiv.find('#confirm').is(':checked')) {
      // The confirm checkbox is not checked, alert and do nothing.
      AfErrorT('T.WORKPOOL.DELETE.NO.CONFIRM',
               'M.WORKPOOL.DELETE.NO.CONFIRM');
      return;
   }
   // Hide the dialog from display until the data is used and then destroy it.
   dialogDiv.dialog('close');

   /* Delete each workpool and set the deleteMethod based on user choice */
   for (var i = 0, counter = 0; i < workpools.length; i++) {
      var deleteMethod = dialogDiv.find('#wp_' + workpools[i].id).is(':checked')?
            'deleteFromDisk' : 'removeFromInventory';
      AfAjax({
         method: 'DELETE',
         url: '/api/workpools/' + workpools[i].id
            + '/method/' + deleteMethod,

         success: function() {
            AfNotifyT('T.COMMON.SUCCESS', 'M.WORKPOOL.DELETED');
         },

         complete: function() {
            counter++;
            if (counter == workpools.length) {
               /* Remove the popup dialog and remove the datatable. */
               self.endDeleteWorkpoolPopup(dialogDiv, dataTableDiv);

               /* Now the deletes are done, force a refresh the table. */
               self.refresh();
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.workpoolImageDeleteEligible
 *
 * Private function to check if the workpool image is not associated with other
 * workpools and if so, check with user if the image needs to be deleted.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
workpoolImageDeleteEligible = function _workpoolImageDeleteEligible(
      workpoolId,
      workpoolName)
{
   var imageDeleteStatus = 'false';
   AfAjax({
      method: 'GET',
      url: '/api/workpools/imageLinkCount/' + workpoolId,
      async: false,

      success: function(data) {
         // Check if this workpool is the only one that has the image.
         if (data == 1) {
            var deleteImage = AfConfirm(
               AfTranslate('T.WORKPOOL.IMAGE.DELETE'),
               AfTranslate('M.WORKPOOL.IMAGE.DELETE') + ' "' + workpoolName + '"');
            imageDeleteStatus = (deleteImage)? 'true' : imageDeleteStatus;
         }
      },

      error: function() {
         // Indicate if there were any errors.
         imageDeleteStatus = 'error';
      }
   });
   return imageDeleteStatus;
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.SubmitEditWorkpoolForm
 *
 * Submit the workpool edit form for updating an existing workpool.
 * @param form
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
SubmitEditWorkpoolForm = function _SubmitEditWorkpoolForm(form)
{
   var json = AfSerializeForm(form);
   var self = this;
   var workpoolId = json['workpoolId'];
   delete json['workpoolId'];

   AfLog('submitting edit form for workpool ' + workpoolId);
   AfLog(json);

   AfAjax({
      method: 'PUT',
      contentType: 'application/json',
      url: '/api/workpools/' + workpoolId,
      data: json,

      success: function() {
         AfNotifyT('T.COMMON.SUCCESS', 'M.WORKPOOL.SAVED');
         if (VmTAF.dialogHolder) {
            VmTAF.dialogHolder.dialog("close");
         } else {
            VmTAF.contentNavigator.GoBack();
         }
      },

      error: self.handleWorkpoolError
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.submitNewWorkpoolForm
 *
 * Invoked when the add new Workpool is saved.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
SubmitNewWorkpoolForm = function _submitNewWorkpoolForm(form)
{
   var self = this;
   AfLog('submitting new workpool');

   // Convert form data into JSON
   var json = AfSerializeForm(form);
   AfLog(json);

   // Send create request to AppFactory
   AfAjax({
      method: 'POST',
      url: '/api/workpools',
      data: json,
      contentType: 'application/json',

      success: function() {
         AfNotifyT('T.COMMON.SUCCESS', 'M.WORKPOOL.SAVED');
         if (VmTAF.dialogHolder) {
            VmTAF.dialogHolder.dialog("close");
         } else {
            VmTAF.contentNavigator.GoBack();
         }
      },

      error: self.handleWorkpoolError
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.handleWorkpoolError
 *
 * Function to handle the workpool error case. If there is a duplicate name
 * conflict with create / update workpool calls, handles those cases here.
 *
 * @param jqXHR
 * @param textStatus
 * @param errorThrown
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
handleWorkpoolError = function _handleWorkpoolError(jqXHR, textStatus, errorThrown)
{
   if (jqXHR.status == 409 && jqXHR.responseText.indexOf('CONFLICT_NAME') > -1) {
      AfErrorT('T.WORKPOOL.NAME.CONFLICT', 'M.WORKPOOL.NAME.CONFLICT');
   }
   else {
      // Use default handler in all other cases
      AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown);
   }
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.createRows
 *
 * Function to update the rows of the workpool table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the workpools.
 * @param jsonData JSON-formatted workpool summary data.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
createRows = function _createRows(self, jsonData, options)
{
   var workpools = jsonData.workpools;
   var wpDefaultId = jsonData['default'];
   var popupData = {};

   // To avoid memory leaks, unbind prior event handlers before clearing table
   $('.wp-default-button').unbind('click');
   $('.popup-link').unbind('click');

   // Remove all data rows from the table
   self.tableWrapper.ClearTable();

   // Add a row per workpool.
   for (var i = 0; i < workpools.length; i++) {
      var workpool = workpools[i];

      // Check-box to select this workpool: when clicked
      var cb = $('<input type="checkbox">');

      // Allow deletion of workpools that have deletable state
      if (!workpool.deletable) {
         cb.attr('disabled', true);
         cb.attr('title', AfTranslate(
               'M.WORKPOOL.NO.DELETE.TOOLTIP',
               AfTranslate('M.WORKPOOL.STATE.' + workpool.state)));
      }

      // Link to the workpool editor (modifiable only)
      var link = self.createRecordLink(
            workpool.name,
            workpool.id,
            '/workpools/edit/' + workpool.id);

      // Decipher the osType and wpType based on workpool itself.
      var wpAndOsType = self.decipherWorkpoolAndOsType(workpool, true);

      // Status: in a <span> so we can pick them out
      var stateLit = AfTranslate('M.WORKPOOL.STATE.' + workpool.state);
      var state = $('<span>').text(stateLit);
      if (workpool.state != 'available') {
         state.addClass('inactive');
      }

      var maxInstances = $('<span>').text(workpool.maximum);

      // Radio button for the 'default' workpool
      var rb = '';
      if (workpool.state != 'deleting' && workpool.state != 'deleted') {
         var rbStr = '<input type="radio" name="wpDefault" value="'
                   + workpool.id + '"';

         if (workpool.id == wpDefaultId) {
            rbStr += ' checked';
         }
         rbStr += '>';
         rb = $(rbStr);
         rb.addClass("wp-default-button");
      }

      // Create a row
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(link));
      row.push(AfHtml(wpAndOsType.wpTypeDesc));
      row.push(AfHtml(wpAndOsType.osTypeDesc));
      row.push(AfHtml(wpAndOsType.osVariantDesc));
      row.push(AfHtml(maxInstances));

      if (workpool.lastError) {
         var message =
            '<p><b>Workpool</b>: ' + workpool.name +
            '</p><p><b>Status</b>  : ' + stateLit +
            '</p><p><b>Message</b> : ' + workpool.lastError + '</p>';

         popupData[workpool.id] = {
               title: 'Alert',
               message: message
         };

         var image = $('<img>')
                    .attr('src', VmTAF.imagePath + '/alert-warning-icon.png')
                    .attr('width', '12')
                    .attr('height', '12');

         var link = $('<a>')
                    .append(AfHtml(image))
                    .attr('popupId', workpool.id)
                    .addClass('popup-link');
         row.push(AfHtml(state) + ' ' + AfHtml(link));
      } else {
         row.push(AfHtml(state));
      }

      // Add a new column for image status only if cloneSupported is set.
      if (self.cloneSupported) {
         // If workpool is of type LINKED, only then get the vmImage state.
         var imageName = $('<span>').text(wpAndOsType.wpType == 'LINKED'?
               workpool.vmImage.name : '');
         var imageState = $('<span>').text(wpAndOsType.wpType == 'LINKED'?
               AfTranslate('M.WORKPOOL.IMAGE.STATE.' + workpool.vmImage.state) : '');
         // Create a progressbar for the state.
         var args = {
               widthPercent: workpool.vmImage.percent,
               styleClass: workpool.vmImage.failState? 'red' : 'green',
               stage : { widthArray: [15, 70, 90, 100] }
         };
         imageState.append(AfProgressBar(args));

         row.push(AfHtml(imageName));
         row.push(AfHtml(imageState));
      }
      row.push(AfHtml(rb));

      // Add row to the table.
      self.tableWrapper.AddRow(row, workpool);
   }

   /* Redraw the table with all its rows */
   self.tableWrapper.DrawTable();

   /* Add click event listeners to default links */
   // Note: Must be done AFTER the table has been drawn
   $('.wp-default-button').click(function() {
      var wpId = $(this).attr('value');
      self.setDefaultWorkpool(wpId, true);
   });

   /* Add listeners to all workpools that need a status popup to be shown */
   $('.popup-link').click(function() {
      var popupInfo = popupData[$(this).attr('popupId')];
      var dialogArgs = {
            title: popupInfo.title,
            message: popupInfo.message,
            buttons: popupInfo.labels
      };

      if (popupInfo.links) {
         dialogArgs.callback = function(pos) {
            var link = popupInfo.links[pos];
            if (link) {
               AfAjax({
                  url: popupInfo.links[pos],
                  method: 'PUT',

                  complete: function() {
                     self.refresh();
                  }
               });
            }
            return true;
         };
      }
      AfShowMessageDialog(dialogArgs);
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.decipherWorkpoolAndOsType
 *
 * This method is a utility method to figure out the osType and the Workpool
 * type based on the workpool data. If translate flag is set, the workpool type
 * and os type's translated text is also returned.
 *
 * @return json object with syntax: { "osType" : "...", "wpType" : "..." }
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
decipherWorkpoolAndOsType = function _decipherWorkpoolAndOsType(workpool, translate)
{
   var cipherData = new Object();

   // Compute the workpool and guest os types based on the workpool object
   if(workpool['@class'] == 'LinkedWorkpool' || workpool.vmImage) {
      cipherData.wpType = 'LINKED';
      cipherData.osType = workpool.vmImage.vmSource.osType['@class'];
      cipherData.osVariant = workpool.vmImage.vmSource.osType.variant;
   }
   else if (workpool['@class'] == 'FullWorkpool' || workpool.vmPattern) {
      cipherData.wpType = 'FULL';
      cipherData.osType = workpool.vmPattern.osType['@class'];
      cipherData.osVariant = workpool.vmPattern.osType.variant;
   }
   else {
      cipherData.wpType = 'CUSTOM';
      cipherData.osType = workpool.osType['@class'];
      cipherData.osVariant = workpool.osType.variant;
   }

   if(translate) {
      cipherData.wpTypeDesc = AfTranslate('M.WORKPOOL.TYPE.' + cipherData.wpType);
      cipherData.osTypeDesc = AfTranslate('M.OSTYPE.' + cipherData.osType);
      // Not all osTypes have variants, so translate only if variant is set.
      if (cipherData.osVariant) {
         cipherData.osVariantDesc = AfTranslate('M.OSVARIANT.' + cipherData.osType
               + '.' + cipherData.osVariant);
      }
   }
   return cipherData;
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.setDefaultWorkpool
 *
 * Send an Ajax request to the server to change the default Workpool to the
 * one specified.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
setDefaultWorkpool= function(workpoolId, refresh)
{
   var self = this;
   AfLog('Setting default workpool to ' + workpoolId);

   AfAjax({
      method: 'PUT',
      url: '/api/workpools/' + workpoolId + '/default',

      complete: function() {
         if (refresh) {
            self.refresh();
         }
      }
   });
};

WorkpoolManager.prototype.
refresh = function() {
   this.PopulateDataTableNow({
      url: this.refreshUrl,
      table: this.tableWrapper.dataTable,
      dataHandler: this.createRows
   });
};

/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.populateNetworkDropdown
 *
 * Private function to load network name list from server and create a dropdown.
 *
 * @param dropdownHolder HTML "div" element that will contain the dropdown.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
populateNetworkDropdown = function _populateNetworkDropdown(dropdownHolder)
{
   var self = this;
   var networkDiv = $('#'+ dropdownHolder);

   // If there is no dropdownHolder, do nothing.
   if (networkDiv == undefined) {
      // It looks like this is a WS case, defaults to 'NAT'
      return;
   }

   AfAjax({
      url: '/api/vi/inventory/networkList',
      method: 'GET',
      beforeSend: function() {
         networkDiv.empty().append('<img src="' + VmTAF.waitIcon + '"/>');
      },

      success: function _populateVINetSuccess(jsonData) {
         // Create network dropdown.
         if (jsonData) {
            var options = [];
            for(var key in jsonData) {
               options.push( { display: jsonData[key] } );
            }
            var input = {
                  name     : "networkName",
                  options  : options,
                  validate : "{required:false}"
            };
            var select = AfCreatePulldown(input);
            networkDiv.empty().append(select);
         }
         else {
            networkDiv.empty().append(AfTranslate('M.VI.LOAD.ERROR'));
         }
      },

      error: function _populateVINetError(jqXHR, textStatus, errorThrown) {
         var errSpan = $('<span>').text(AfTranslate('M.VI.LOAD.ERROR'));
         networkDiv.empty().append(errSpan);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.PopulateInventoryTree
 *
 * Public function to load inventory data from server into the inventory div.
 *
 * @param treeDiv HTML "div" element that will display the inventory tree.
 * @param viNodeType The nodeType defines the kind of data loaded into treeDiv
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
populateInventoryTree = function _populateInventoryTree(treeDiv, viNodeType)
{
   var self = this;

   AfAjax({
      url: '/api/vi/inventory/' + viNodeType,
      method: 'GET',
      beforeSend: function() {
         $('#'+ treeDiv).empty().append('<img src="' + VmTAF.waitIcon + '"/>');
      },

      success: function _populateVIInvSuccess(jsonData) {
         // Create inventory tree...
         self.createTree(treeDiv, jsonData, viNodeType);
      },

      error: function _populateVIInvError(jqXHR, textStatus, errorThrown) {
         // Failed to connect, Notify user about this.
         var errSpan = $('<span>').text(AfTranslate('M.VI.LOAD.ERROR'));
         $('#'+ treeDiv).empty().append(errSpan);
         AfErrorT('T.VI.LOAD.ERROR', 'M.VI.LOAD.ERROR');
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.createTree
 *
 * Create the inventory tree from JSON data, and insert it into the
 * specified DIV element.
 *
 * @param treeDiv
 * @param jsonData
 * @param nodeType
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
createTree = function(treeDiv, jsonData, nodeType)
{
   // Remove loading image.
   $(treeDiv).empty();

   // Make the tree from the new div.
   var viRoot = jsonData;
   var tree = new YAHOO.widget.TreeView(treeDiv);
   var node = this.addTreeNode(tree.getRoot(), viRoot);
   if(nodeType == 'Datastore') {
      tree.setDynamicLoad(this.browseVIDatastoreForISO);
   }

   // Listen for clicks on the labels.
   // trees with TextNodes will fire an event when the label is clicked
   tree.subscribe("labelClick", function(node) {
      if (nodeType == 'VirtualMachine') {
         if(node.data.nodeType == 'VirtualMachine') {
            $("input[name=selectedVMName]").val(node.label);
            $("input[name=moid]").val(node.data.morValue);
            // Close the parent - a jquery ui dialog
            $('#vi-browser').dialog('close');
         }
      }
      else if (nodeType == 'Datastore') {
         if(node.data.nodeType == 'iso') {
            $("input[name=sourceIso]").val(node.data.path);
            // Close the parent - a jquery ui dialog
            $('#vi-browser').dialog('close');
         }
      }
      return false;
   });
   tree.render();
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.browseVIDatastoreForISO
 *
 * Used to load the datastore subtree and displaying the folders and iso files
 * from JSON data.
 *
 * @param node
 * @param onCompleteCallback
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
browseVIDatastoreForISO = function _browseVIDatastoreForISO(node, onCompleteCallback)
{
   var self = node.data.self;
   var tree = node.tree;

   AfLog('Dynamic data load for ' + node.label + ' from ' + node.data.path);

   if (node.data.nodeType == 'ComputeResource') {
      /* Don't load anything for the nodes that are loaded */
      AfLog('Loading skipped for root - compute resource');
   }
   else {
      var dsNode = node;
      // dsNode is atleast a child or a Datastore node.
      while(dsNode.data.nodeType != 'Datastore') {
         dsNode = dsNode.parent;
      }
      var newNode = {
         path     : node.data.path,
         morValue : dsNode.data.morValue,
         nodeType : dsNode.data.nodeType,
         name     : dsNode.label
      };

      AfAjax({
         method: 'POST',
         url: '/api/vi/inventory/datastore/browseIso',
         contentType: 'application/json',
         data: JSON.stringify(newNode),
         async: false,

         success: function(nodeArray) {
            AfLog('Datastore file browser data load success!');
            if (!nodeArray || nodeArray.length == 0) {
               node.isLeaf = true;
            }
            else {
               for (var i = 0; i < nodeArray.length; i++) {
                  self.addTreeNode(node, nodeArray[i]);
               }
            }
         },

         error: function(data) {
            alert(data);
         }
      });
   }

   // Invoke the treeview callback.
   if (onCompleteCallback) {
      onCompleteCallback();
   }
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.addTreeNode
 *
 * Add a node to a tree. This is a recursive function, adding children
 * as needed.
 *
 * @param parentNode
 * @param invItem
 * @returns the newly created node
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
addTreeNode = function _addTreeNode(parentNode, invItem)
{
   var nType = "";
   if (invItem.nodeType) {
      nType = invItem.nodeType;
   }
   else if(invItem['@class'] == 'FolderNode'){
      nType = 'Folder';
   }
   else if (invItem['@class'] == 'FileNode') {
      nType = 'iso';
   }

   // Create this node with only root expanded.
   var newNode = new YAHOO.widget.TextNode( {
         label: invItem.name,
         contentStyle: nType,
         isLeaf: !invItem.hasChild,
         expanded: invItem.root
      },
      parentNode);

   newNode.data.self = this;
   newNode.data.morValue = invItem.morValue;
   newNode.data.nodeType = nType;
   newNode.data.path = invItem.path;
   // @TODO filter by vm guest OS, vmware tools isntalled.

   // Create sub-tree.
   if (invItem.children) {
      for (var i = 0; i < invItem.children.length; i++) {
         this.addTreeNode(newNode, invItem.children[i]);
      }
   }
   return newNode;
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.ToggleImageForm
 *
 * Display the necessary form fields for the selected workpool creation types.
 * In case of selectVM, load the vm tree onto the jquery ui dialog.
 * In case of selectISO, load the datastore tree onto the jquery ui dialog.
 *
 * @param radioObject
 * @param viBrowser
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
ToggleImageForm = function _toggleImageForm(radioObject, viBrowser)
{
   var vmImageClass = radioObject.value;
   // Clear previous contents for the tree data to be populated
   viBrowser.find('#inv-tree').empty();

   // Load the appropriate data for the selected vm type.
   if (vmImageClass == 'selectVM') {
      // Load the vm tree thats used to display the ws/vc inventory tree
      this.populateInventoryTree("inv-tree", 'VirtualMachine');
      viBrowser.dialog('option', 'title', AfTranslate('T.VI.BROWSE.VM.DESC'));
      viBrowser.find('.desc').empty().append(AfTranslate(this.isWS?
               'M.VI.WS.BROWSE.VM.DESC_HTML' :
               'M.VI.VC.BROWSE.VM.DESC_HTML'));
   }
   else if (vmImageClass == 'selectISO') {
      // Populate the network name dropdown.
      this.populateNetworkDropdown("net-dropdown");

      // Load the datastores so ISO could be picked.
      this.populateInventoryTree("inv-tree", 'Datastore');
      viBrowser.dialog('option', 'title', AfTranslate('T.VI.BROWSE.ISO.DESC'));
      viBrowser.find('.desc').empty().append(AfTranslate(this.isWS?
            'M.VI.WS.BROWSE.ISO.DESC_HTML' :
            'M.VI.VC.BROWSE.ISO.DESC_HTML'));
   }

   // Hide all sections and show only the selected section.
   $('.imgCreate').hide();
   var b = $.browser;
   $('.' + vmImageClass).each(function() {
      if (b.mozilla && b.version.slice(0,3) == '1.9') {
         // Firefix 3.x specific css bug (#853378) slideDown() doesnt quite work, hence show().
         $(this).show();
      } else {
         $(this).slideDown();
      }
   });

   // Handle license key / kms Server disabling parts.
   this.disableKeyKmsServerInput(vmImageClass);
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.PopulateOsTypeDropdown
 *
 * Public function to load osType from server and create a dropdown.
 *
 * @param osTypeDiv jQuery "div" element that will contain the dropdown.
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
PopulateOsTypeDropdown = function _populateOsTypeDropdown(osTypeDiv)
{
   var self = this;

   AfAjax({
      url: '/api/workpools/osTypes',
      method: 'GET',

      success: function _getOsTypeSuccess(jsonData) {
         // Create osType dropdown.
         if (jsonData) {

            // Find the option which was selected to get to the variant list.
            var options = [];

            // Transform variant key and display text into 2 arrays.
            for(var key in jsonData) {
               options.push({
                  key      : key,
                  display  : ('M.OSTYPE.' + key),
                  translate: true
               });
            }

            // Create the pulldown with WinXPProOsType as the default.
            var input = {
                  name        : "@class",
                  value       : 'WinXPProOsType',
                  options     : options,
                  validate    : "{required:true}",
                  subproperty : "osType"
            };
            var select = AfCreatePulldown(input);
            self.allOsTypes = jsonData;
            osTypeDiv.empty().append(select);
            // Set the onchange handler to enable variant & kms server inputs.
            select.change(function() {
               self.ToggleOsTypeField(this);
            });
         }
         else {
            var errSpan = $('<span>').text(AfTranslate('M.OSTYPE.LOAD.ERROR'));
            osTypeDiv.empty().append(errSpan);
         }
      },

      error: function _getOsInfoError(jqXHR, textStatus, errorThrown) {
         var errSpan = $('<span>').text(AfTranslate('M.OSTYPE.LOAD.ERROR'));
         osTypeDiv.empty().append(errSpan);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.ToggleOsTypeField
 *
 * Public function to display the kms server & variant fields when osType is
 * not WinXPProOsType, else this fields are hidden.
 *
 * @param selectObject
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
ToggleOsTypeField = function _toggleOsTypeField(selectObject)
{
   var variantDiv = $('.variant');
   if (selectObject.value == 'WinXPProOsType') {
      variantDiv.find('.select').empty();
      variantDiv.slideUp();
   }
   else {
      var displayOptions = [];
      // Find the option which was selected to get to the variant list.
      var values = this.allOsTypes[selectObject.value];

      // Translate the variant info and create a pulldown.
      for(var j = 0; values && j < values.length; j++) {
         displayOptions.push({
            key      : values[j],
            display  : ('M.OSVARIANT.' + selectObject.value + '.' + values[j]),
            translate: true
         });
      }
      var input = {
            name        : "variant",
            options     : displayOptions,
            validate    : "{required:true}",
            subproperty : "osType"
      };
      var select = AfCreatePulldown(input);
      variantDiv.find('.select').empty().append(select);
      variantDiv.show();
   }

   // Display licType fields based on osType & licKey selection.
   this.ToggleLicenseForm($('[name="licType"]:checked').val());
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.ToggleLicenseForm
 *
 * Toggles display and disable input fields b/w license key / kms server for non
 * WinXPProOsType osTypes.
 *
 * Toggle disabled is used to disable client side validation on fields that dont
 * matter when not displayed.
 *
 * In case of KmsServer, the activation key is pickedup automatically.
 *
 * @param radioValue
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
ToggleLicenseForm = function _toggleLicenseForm(radioValue)
{
   var osType = $('[name="@class"]').val();
   if(osType == 'WinXPProOsType') {
      // Show license key only option.
      $('.licType').slideUp();
      $('.licenseKey').show();
      $('.kmsServer').slideUp();

      // Disable the field thats not being displayed to disable validation.
      $('[name="kmsServer"]').attr('disabled', true);
      $('[name="licenseKey"]').attr('disabled', false).focus();
   }
   else {
      // Give both license key / kmsServer options based on radio selection
      $('.licType').show();
      if(radioValue == 'licenseKey') {
         $('.licenseKey').show().fadeTo(200, 0.5).fadeTo(500, 1.0);
         $('.kmsServer').hide();

         // Disable the field thats not being displayed to disable validation.
         $('[name="kmsServer"]').attr('disabled', true);
         $('[name="licenseKey"]').attr('disabled', false).focus();
      }
      else {
         $('.kmsServer').show().fadeTo(300, 0.3).fadeTo(300, 1.0);
         $('.licenseKey').hide();

         // Disable the field thats not being displayed to disable validation.
         $('[name="licenseKey"]').attr('disabled', true);
         $('[name="kmsServer"]').attr('disabled', false).focus();
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * WorkpoolManager.DisableKeyKmsServerInput
 *
 * Disable the license key / kms server fields to disable their validations
 * if the workpool creation involves only selecting an existing Image.
 *
 * @param vmImageClass
 * -----------------------------------------------------------------------------
 */
WorkpoolManager.prototype.
disableKeyKmsServerInput = function _disableKeyKmsServerInput(vmImageClass)
{
   // These types need one of license key / kms Server
   if (vmImageClass == 'selectVM' || vmImageClass == 'selectISO') {
      // Display licType fields based on osType & licType selection.
      this.ToggleLicenseForm($('[name="licType"]:checked').val());
   }
   else { // vmImageClass = existingImage
      // Disable both licenseKey and kmsServer inputs to disable their validations.
      $('[name="licenseKey"]').attr('disabled', true);
      $('[name="kmsServer"]').attr('disabled', true);
   }
};
