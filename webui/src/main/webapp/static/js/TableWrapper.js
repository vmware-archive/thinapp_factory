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


TableWrapper.ROW_CHECKBOX_CHANGE = "checkbox_change_taf";
/**
 * -----------------------------------------------------------------------------
 * function TableWrapper
 *
 * This class wraps around a data table object in order to provide some features
 * that are otherwise hard to deal with. These are mainly related to the
 * inability to attach jQuery data or event handlers to HTML elements that you
 * wish to add to the table (since dataTable.fnDrawTable() will strip them
 * out).
 *
 * To attach data objects to rows, we maintain a mapping from row key to object.
 * The user can change the displayed row order (sorting) without disrupting this
 * mapping. To do this, we add a secret column to every table so we can store
 * each row's key in it (there is no other way to attach data to rows using
 * dataTable). To hide this secret column, we use CSS styling, since the API
 * that dataTable provides to hide columns is crap (it breaks table resizing).
 *
 * @param table Standard HTML table element
 * @param options Initialization options for the data table.
 * -----------------------------------------------------------------------------
 */
function TableWrapper(table, options) {
   "use strict";

   var allOptions, opt;

   // this tells allows Datatables to sort specially-crafted
   // column values in a special way.
   //
   // The special value is made by taking an arbitrary string,
   // enclosing it in a <span>, and giving that span a title
   // attribute with a numeric value.
   //
   // For example:
   //   e.g. <span title="12">12 days of christmas</span>
   //   e.g. <span title="87">four score and seven years ago</span>
   //
   // Columns so represented can then be easily sorted by their
   // original numeric value, affording flexibility about the
   // strings inside.
   //
   jQuery.fn.dataTableExt.oSort['title-numeric-desc']  = function(a,b) {
      var x = a.match(/title="(-?[0-9\.]+)/)[1];
      var y = b.match(/title="(-?[0-9\.]+)/)[1];
      x = parseFloat( x );
      y = parseFloat( y );
      return ((x < y) ? -1 : ((x > y) ?  1 : 0));
   };

   jQuery.fn.dataTableExt.oSort['title-numeric-asc'] = function(a,b) {
      var x = a.match(/title="(-?[0-9\.]+)/)[1];
      var y = b.match(/title="(-?[0-9\.]+)/)[1];
      x = parseFloat( x );
      y = parseFloat( y );
      return ((x < y) ?  1 : ((x > y) ? -1 : 0));
   };

   // The following 2 functions are used for version sorting on TAF.
   // It splits the input strings into multiple chunks of digits and non-digits
   // and then compare these chunk to decide which value precedes the other.
   jQuery.fn.dataTableExt.oSort['taf-version-asc'] = function(a,b) {
      return CompareAlnumVersionString(a,b);
   };

   jQuery.fn.dataTableExt.oSort['taf-version-desc'] = function(a,b) {
      var result = CompareAlnumVersionString(a,b);
      return (result * -1);
   };

    if (window.VmTAF.newUI) {
       allOptions = {
          bDeferRender:true,

          /*
           * Variable: sDom
           * Purpose:  Dictate the positioning that the created elements will take
           * Scope:    jQuery.dataTable.classSettings
           * Notes:
           *   The following options are allowed:
           *     'l' - Length changing
           *     'f' - Filtering input
           *     't' - The table!
           *     'i' - Information
           *     'p' - Pagination
           *     'r' - pRocessing
           *   The following constants are allowed:
           *     'H' - jQueryUI theme "header" classes
           *     'F' - jQueryUI theme "footer" classes
           *   The following syntax is expected:
           *     '<' and '>' - div elements
           *     '<"class" and '>' - div with a class
           *   Examples:
           *     '<"wrapper"flipt>', '<lf<t>ip>'
           *//*
           * Without this, tables look a mess in Firefox.
           * See http://datatables.net/forums/comments.php?DiscussionID=1131
           * and http://www.datatables.net/usage/options
           */
          sDom: '<"H"Tfr>tS<"F"<"refresh-indicator">i><"clear">',
          iDisplayLength: -1,
          bLengthChange: false,
          asStripeClasses: ["ui-widget-content"],
          bJQueryUI: true
       };
    } else {
       // use old settings for old UI
       allOptions = {
          bDeferRender:true,
          sDom: '<"top"f>rt<"bottom"ilp><"clear">',
          bDestroy:$.browser.msie
       };
    }

    if (!options || !options.firstColumnSortable) {
        /* Disable sorting on column 0 (usually a checkbox) */
        allOptions.aoColumnDefs = [
            { "bSortable": false, "aTargets": [0] }
        ];
    }

   /* Add/change custom options */
   for (opt in options) {
      if (options.hasOwnProperty(opt)) {
         allOptions[opt] = options[opt];
      }
   }

   /*
    * A checkbox in a table header is used to select/unselect
    * all the rows in that table.
    */
   var self=this;
   table.find('.table-row-selector').each(function() {
      $(this).click(function() {
         var check = $(this).is(':checked');
         self.MarkAllRowCheckboxes(check);
      });
   });

   this.dataTable = table.dataTable(allOptions);
   this.masterCheckbox = this.dataTable.find('th input:checkbox');

   if (window.VmTAF.newUI) {
      // move the table header buttons inside the table
      var wrapper = table.parent().children('.fg-toolbar:first');
      if (wrapper.length) {
         var buttons = table.closest('form').children('.button-row');
         buttons.appendTo(wrapper);

         buttons.removeClass('button-row');
         buttons.children('button').button();
      }
   }

   table.disableSelection();
   table.show();
   this.tableRowData = [];
}

(function() {
   "use strict";

   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.Destruct
    *
    * Cleans up anything this TableWrapper might have instantiated.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      Destruct = function() {
      /* If there is a data table, free it  */
      if (this.dataTable) {
         /*
          * On IE8 (maybe IE9?) calling dataTable.fnDestroy() will cause a
          * Javascript error if the table is not visible. For now, work
          * around this until it is fixed in dataTable.
          */
         if (!$.browser.msie || this.dataTable.is(":visible")) {
            this.dataTable.fnDestroy();
         }
         this.dataTable = null;
      }

      return null;
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.ClearTable
    *
    * Removes all data from this table.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      ClearTable = function() {
      /* Remember what page we're on, since a redraw will reset it */
      this.currentPage = this.GetCurrentPage();

      this.dataTable.fnClearTable();
      this.tableRowData = [];
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.DrawTable
    *
    * Redraws this table.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      DrawTable = function(resetView) {
      var keepSortAndFilter = !resetView;
      this.dataTable.fnDraw(keepSortAndFilter);

        if (keepSortAndFilter) {
            // Keeping sort & filter resets to page 1, so put back the previous page
            while (this.currentPage) {
                this.dataTable.fnPageChange('next');
                this.currentPage--;
            }
        }
        if (window.VmTAF.newUI) {
           var self = this;
            var fnHandler = function(e) {
               if ("TD" != e.target.tagName && "LABEL" != e.target.tagName
                   && "checkbox" != e.target.type) {
                  $(e.target).focus();
                  return;
               }
               var checkboxes = $(this).find('input:checkbox').not(":disabled");
               if (checkboxes.length > 0) {
                  if ( $(this).hasClass('ui-state-highlight') ) {
                     $(this).removeClass('ui-state-highlight');
                     checkboxes.prop('checked', false).trigger(TableWrapper.ROW_CHECKBOX_CHANGE);
                     self.UpdateHeaderCheckbox();
                  } else {
                     $(this).addClass('ui-state-highlight');
                     checkboxes.prop('checked', true).trigger(TableWrapper.ROW_CHECKBOX_CHANGE);
                     self.UpdateHeaderCheckbox();
                  }
               }
            },
                rows = this.dataTable.fnGetNodes(),
                i;
            for (i = 0; i < rows.length; ++i) {
                $(rows[i]).click(fnHandler);
            }
            //  Apply the row selection highlighting after table draw.
            this.dataTable.find('tr > td input:checkbox:checked').not(':disabled')
            .closest('tr').addClass('ui-state-highlight');
        }
        this.UpdateHeaderCheckbox();
    };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.GetSelectedRowData
    *
    * Get an array containing user-attached data for each table row that contains
    * a checked checkbox.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      GetSelectedRowData = function() {
      /* Some jQuery art to get keys of selected rows */
      return this.getRowData('td input[type=checkbox]:checked');
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.GetUnSelectedRowData
    *
    * Get an array containing user-attached data for each table row that contains
    * an unchecked checkbox.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      GetUnSelectedRowData = function(withDataElement) {
      /* Some jQuery art to get keys of selected rows */
      return this.getRowData('td input[type=checkbox]:unchecked', withDataElement);
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.getRowData
    *
    * Returns a list of the the tableRowData elements whose rows match the
    * given jQuery selector expression.
    *
    * @param queryString - a jQuery format query string to find rows in a table.
    * @param withDataElement - an optional boolean flag to include the
    * element itself in the result in addition to the row data.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      getRowData = function(queryString, withDataElement) {

      var /* store a handle to the TableWrapper object, since its context
       will be lost in the handler function.  See
       http://www.alistapart.com/articles/getoutbindingsituations
       */
         self = this,

         /* get the data from the dataTable API, because this will
          * contain the DT_RowId property
          */
         matchingRows = self.dataTable.find(queryString).closest('tr');

      return $.map(matchingRows, function(tr) {
         // the ID of the matching TR is the index into our
         // tableRowData array
         //
         var data = self.tableRowData[tr.id];
         return withDataElement ? {app:data, element:tr} : data;
      });
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.MarkAllRowCheckboxes
    *
    * @param applyCheck - If false resets all checkboxes, if true, mark all checked
    *
    * When implemented, invoke that function here. Or replace the select all
    * checkbox with a link, so its readable as well.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      MarkAllRowCheckboxes = function(applyCheck) {
      // Get the checkboxes that needs attr 'checked' flipped.
      if (applyCheck) {
         this.dataTable.find('tr > td input:checkbox').not(':checked').not(':disabled')
            .closest('tr').addClass('ui-state-highlight');
         this.dataTable.find('tr > td input:checkbox').not(':checked').not(':disabled')
            .attr('checked', applyCheck);
      }
      else {
         // Get matching checkboxes with that of strCheckbox and set the checked value.
         this.dataTable.find('tr > td input:checkbox:checked').not(':disabled')
            .closest('tr').removeClass('ui-state-highlight');
         this.dataTable.find('tr > td input:checkbox:checked').not(':disabled')
            .removeAttr('checked');
      }
      // Trigger the row checkbox change & update header checkbox with the checkbox value passed.
      this.dataTable.find('tr > td input:checkbox').not(':disabled').trigger(TableWrapper.ROW_CHECKBOX_CHANGE);
      this.UpdateHeaderCheckbox();
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.UpdateHeaderCheckbox
    *
    * For the given page, check if all the rows are checked, if so, set the
    *  header row to checked else set unchecked.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      UpdateHeaderCheckbox = function() {
      var checkedInput = this.dataTable.find('tr > td input:checkbox:checked')
         .first().length,
         notCheckedInput = this.dataTable.find('tr > td input:checkbox')
            .not(':checked').not(':disabled').first().length,
         applyCheck = false,
         indeterminate = (checkedInput > 0) && (notCheckedInput > 0);
      // This validates to false when there are no rows either.
      if (checkedInput !== 0 && notCheckedInput === 0) {
         // There are only checked rows, hence check the header row.
         applyCheck = true;
      }

      this.masterCheckbox.prop({
         indeterminate: indeterminate,
         checked: applyCheck
      });

      // also, enable or disable the buttons in the toolbar if
      // there is at least one item checked
      this.dataTable.closest('.dataTables_wrapper').find('.fg-toolbar button:not(.single-button)')
         .button("option","disabled",(0 == checkedInput));
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.AddRow
    *
    * Add a new row to the table. The table is not redrawn so you must also call
    * DrawTable() once all rows have been added. If specified, 'rowData' is
    * mapped to the newly added row.
    *
    * @param row Array of cells defining the new row.
    * @param rowData Optional object to associate with the new row.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      AddRow = function(row, rowData) {
      // if we have an array, convert it to an Object so that
      // we can put the DT_RowId field on it
      //

      var idx = this.tableRowData.length,
         rowHash = $.extend({DT_RowId:idx}, row);

      this.tableRowData.push(rowData);
      this.dataTable.fnAddData(rowHash, false);
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.ReplaceData
    *
    * Replace all data in the table with the supplied list of rows.
    *
    * @param rowArray      2-dimensional array of cells defining the new rows.
    * @param rowDataArray  Parallel list of objects to associate with each new row.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      ReplaceData = function(rowArray, rowDataArray) {
      var i;

      /* Remember what page we're on, since a redraw will reset it */
      this.currentPage = this.GetCurrentPage();

      /* clear the table, but don't redraw it */
      this.dataTable.fnClearTable(false);
      this.tableRowData = rowDataArray;

      for (i = 0; i < rowArray.length; ++i) {
         /* Each row gets a unique key */
         $.extend(rowArray[i], {DT_RowId:i});
      }

      /* add all rows in bulk to the table but still don't redraw it */
      this.dataTable.fnAddData(rowArray, false);

      /* finally, draw the table without resetting the view */
      this.DrawTable(false);
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.GetCurrentPage
    *
    * Get the index of the currently displayed page (starts at 0).
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      GetCurrentPage = function() {
      var settings = this.dataTable.fnSettings();
      if (!settings) {
         return 0;
      }
      return Math.ceil(settings._iDisplayStart / settings._iDisplayLength);
   };


   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.DeleteRow
    *
    * Delete a given row from the table if the given row number is in valid range.
    * It also set null to the tableRowData object from this class,
    * but the tableNextRowKey will not change.
    * @param rowNum - a valid row number.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      DeleteRow = function(rowNum) {
      if (rowNum >= 0 && rowNum < this.GetNumRows()) {
         delete this.tableRowData[rowNum];
         this.dataTable.fnDeleteRow(rowNum);
      }
   };

   /**
    * -----------------------------------------------------------------------------
    * TableWrapper.GetNumRows
    *
    * Get the number of rows from the table.
    * -----------------------------------------------------------------------------
    */
   TableWrapper.prototype.
      GetNumRows = function() {
      var dataArray = this.dataTable.fnGetData();

      return (dataArray) ? dataArray.length : 0;
   };
}()); // end "use strict"
