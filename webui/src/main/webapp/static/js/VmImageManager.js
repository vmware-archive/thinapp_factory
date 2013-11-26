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
 * VmImageManager
 *
 * A "class" that encapsulates all the methods for dealing with image images.
 * -----------------------------------------------------------------------------
 */
function VmImageManager(cloneSupported, table, status)
{
   this.refreshUrl = '/api/workpools/image';

   if (table) {
      this.CreateTableWrapper(table);
   }
}

VmImageManager.prototype = new AbstractManager('Workpool Image');


/**
 * -----------------------------------------------------------------------------
 * VmImageManager.UpdateImage
 *
 * Public function to load data from the server into the vm image table.
 *
 * @param table HTML "table" element that displays all the image.
 * @param refreshInterval If greater that zero, schedules automatic
 * updates every 'refreshInterval' milliseconds.
 * -----------------------------------------------------------------------------
 */
VmImageManager.prototype.
UpdateImage = function _updateImage(refreshInterval)
{
   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.WORKPOOL.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * -----------------------------------------------------------------------------
 * VmImageManager.InitDeleteSelectedImage
 *
 * Public function invoked when the delete button is clicked. This performs the
 * initial validation and gets further input from the user.
 * Upon successful validation and gaining info, proceed to delete images.
 *
 * @param popupHolder
 * -----------------------------------------------------------------------------
 */
VmImageManager.prototype.
InitDeleteSelectedImage = function _initDeleteSelectedImage(popupHolder)
{
   var images = this.tableWrapper.GetSelectedRowData();

   if (images.length == 0) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.WORKPOOL.IMAGE.NO_SELECTION');
      return;
   }

   // Create the delData with 2 columns.
   var delData = [];
   for (var i = 0; i < images.length; i++) {
      var cb = $('<input type="checkbox">').attr('id', 'img_' + images[i].id);
      delData.push([AfHtml(images[i].name), AfHtml(cb)]);
   }

   // Create the desc, and confirmation checkboxes on the dialog.
   var descDiv = $('<div></div>').addClass('desc').text(
         AfTranslate('M.WORKPOOL.IMAGE.VM.DELETE.DESC'));
   var confDiv = $('<label>').addClass('desc').attr('for','confirm').text(
         AfTranslate('M.WORKPOOL.IMAGE.VM.DELETE.CONFIRM'));
   var confirmCb = $('<input type="checkbox">').attr('id', 'confirm');
   confDiv.prepend(confirmCb);

   popupHolder.html('<table cellpadding="0" cellspacing="0" border="0" '
         + 'class="data" id="img-table"></table>');
   popupHolder.append(confDiv);
   popupHolder.prepend(descDiv);

   var dataTableHolder = $('#img-table');
   dataTableHolder.dataTable({
      'bPaginate': false,
      'bFilter': false,
      'bSort': false,
      'sDom': '<"top"f>rt<"bottom"ilp><"clear">',
      'aoColumns': [{ 'sTitle': 'Image Name' },
                    { 'sTitle': AfTranslate('T.WORKPOOL.IMAGE.VM.DELETE'),
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
      title: 'Delete Image & VM',
      buttons: { 'Delete': function() {
                    self.deleteSelectedImage($(this), dataTableHolder);
                 },
                 'Cancel': function() {
                    self.endDeleteImagePopup($(this), dataTableHolder);
                 }
              }
   });
};


/**
 * -----------------------------------------------------------------------------
 * VmImageManager.endDeleteImagePopup
 *
 * Public function to cleanup the ui elements if the user selects cancel or
 * delete processing is complete.
 *
 * @param popupHolder
 * @param dataTableHolder
 * -----------------------------------------------------------------------------
 */
VmImageManager.prototype.
endDeleteImagePopup = function _endDeleteImagePopup(
      popupHolder,
      dataTableHolder)
{
   dataTableHolder.dataTable().fnDestroy();
   popupHolder.dialog('destroy');
   popupHolder.empty();
};


/**
 * -----------------------------------------------------------------------------
 * VmImageManager.deleteSelectedImage
 *
 * Private function to delete all images selected in the given image table and
 * the vms if the checkboxes are checked.
 *
 * @param dialogDiv
 * @param dataTableDiv
 * -----------------------------------------------------------------------------
 */
VmImageManager.prototype.
deleteSelectedImage = function _deleteSelectedImage(dialogDiv, dataTableDiv)
{
   var self = this;
   var images = this.tableWrapper.GetSelectedRowData();

   // Check if the user has checked the confirm screen, if not send them back.
   if(!dialogDiv.find('#confirm').is(':checked')) {
      // The confirm checkbox is not checked, alert and do nothing.
      AfErrorT('T.WORKPOOL.IMAGE.DELETE.NO.CONFIRM',
               'M.WORKPOOL.IMAGE.DELETE.NO.CONFIRM');
      return;
   }
   // Hide the dialog from display until the data is used and then destroy it.
   dialogDiv.dialog('close');

   /* Delete each image and set the deleteMethod based on user choice */
   for (var i = 0, counter = 0; i < images.length; i++) {
      var deleteMethod = dialogDiv.find('#img_' + images[i].id).is(':checked')?
            'deleteFromDisk' : 'removeFromInventory';

      AfAjax({
         method: 'DELETE',
         url: '/api/workpools/image/' + images[i].id
            + '/method/' + deleteMethod,

         success: function() {
            AfNotifyT('T.COMMON.SUCCESS', 'M.WORKPOOL.IMAGE.DELETED');
         },

         complete: function() {
            counter++;
            if (counter == images.length) {
               /* Remove the popup dialog and remove the datatable. */
               self.endDeleteImagePopup(dialogDiv, dataTableDiv);

               /* Now the deletes are done, force a refresh the table. */
               self.refresh();
            }
         }
      });
   }
};

VmImageManager.prototype.
refresh = function() {
   this.PopulateDataTableNow({
      url: this.refreshUrl,
      table: this.tableWrapper.dataTable,
      dataHandler: this.createRows
   });
};


/**
 * -----------------------------------------------------------------------------
 * VmImageManager.createRows
 *
 * Function to update the rows of the image table using
 * the specified JSON data.
 *
 * @param table HTML "table" element that displays all the images.
 * @param jsonData JSON-formatted image summary data.
 * -----------------------------------------------------------------------------
 */
VmImageManager.prototype.
createRows = function _createRows(self, jsonData, options)
{
   // Remove all data rows from the table
   self.tableWrapper.ClearTable();

   var images = jsonData.images;

   // Map of imageId - workpoolCount
   var imageWpCount = jsonData.imageWpCount;

   // Add a row per image.
   for (var i = 0; i < images.length; i++) {
      var image = images[i];

      // Check-box to select this image: when clicked
      var cb = $('<input type="checkbox">');

      // Link to the image editor (modifiable only)
      var name = image.name;
      var osType = AfTranslate('M.OSTYPE.'
            + image.vmSource.osType['@class']);
      var osVariant = image.vmSource.osType.variant?
            AfTranslate('M.OSVARIANT.' + image.vmSource.osType['@class']
         + '.' + image.vmSource.osType.variant) : '';
      // Status: in a <span> so we can pick them out
      var state = $('<span>').text(AfTranslate(
            'M.WORKPOOL.IMAGE.STATE.' + image.state));

      // Create a progressbar for the state.
      var args = {
            widthPercent: image.percent,
            styleClass: image.failState? 'red' : 'green',
            stage : { widthArray: [15, 70, 90, 100] }
      };
      state.append(AfProgressBar(args));

      // Set the number of workpools this image is associated to.
      var wpCount = imageWpCount[image.id];
      if (!wpCount) {
         wpCount = 0;
      }
      else {
         // Disable row checkbox if associated to 1 or more workpools.
         cb.attr('disabled', true);
         cb.attr('title', AfTranslate('M.WORKPOOL.IMAGE.DISABLE.CHECK.TOOLTIP'));
      }

      // Do not allow image deletes if states do not allow deletion.
      if (!image.deletable) {
         cb.attr('disabled', true);
         cb.attr('title', AfTranslate(
               'M.WORKPOOL.NO.DELETE.TOOLTIP',
               AfTranslate('M.WORKPOOL.IMAGE.STATE.' + image.state)));
      }

      // Create a row
      var row = new Array();
      row.push(AfHtml(cb));
      row.push(AfHtml(name));
      row.push(AfHtml(wpCount));
      row.push(AfHtml(state));
      row.push(AfHtml(osType));
      row.push(AfHtml(osVariant));

      // Add row to the table.
      self.tableWrapper.AddRow(row, image);
   }

   /* Redraw the table with all its rows */
   self.tableWrapper.DrawTable();
};


