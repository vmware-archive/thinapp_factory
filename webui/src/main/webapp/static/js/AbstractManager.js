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
 * function AbstractManager
 *
 * Superclass for all the "Manager" classes used by AppFactory.
 * -----------------------------------------------------------------------------
 */
function AbstractManager(label) {
    "use strict";

    this.pendingAjax = [];
    this.timeoutId = null;
    this.label = label;
}

(function() {
    "use strict";

    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.Destruct
     *
     * Destructor simulator.
     * Cleans up anything this object might have instantiated.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        Destruct = function () {
        /* If there is a pending timeout, kill it */
        if (this.timeoutId) {
            window.clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }

        /* If there are pending AJAX calls, abort them */
        while (this.pendingAjax.length > 0) {
            var jqXHR = this.pendingAjax.pop();
            jqXHR.abort();
        }

        /* If there is a tableWrapper, free it  */
        if (this.tableWrapper) {
            this.tableWrapper = this.tableWrapper.Destruct();
        }

        return null;
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.CreateTableWrapper
     *
     * Create a 'TableWrapper' from the given table element. This table can be
     * accessed via the 'tableWrapper' field. It will be freed automatically when
     * the Destruct() method is called.
     *
     * @param table HTML table to convert into a datatable
     * @param options Optional table options as defined at
     *                http://www.datatables.net/usage/options
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        CreateTableWrapper = function(table, options) {
        this.tableWrapper = new TableWrapper(table, options);
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.Assert
     *
     * Check if a condition is true. If not, show the error message.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        Assert = function (cond, msg) {
        if (!cond) {
            window.AfLog('Assert failed! ' + msg);
            window.AfError('Assert Failed', msg);
        }
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.UpdateDataTable
     *
     * If there are no rows selected in the given table, it is updated using an
     * Ajax GET call to the specified URL, passing the data back to the given
     * callback. Another update is then scheduled after the given delay.
     *
     * @param options Arguments, as follows:
     *
     * table - The datatable (actually, any UI component) that you want to
     *         update. Will be passed back to the dataHandler.
     *
     * dataHandler - Function that is invoked when the data has been fetched.
     *
     * errorHandler - Function that is invoked when an error occurs trying
     *                to fetch the data.
     *
     * repeatMs - If greater than zero, updates will be scheduled to repeat
     *            at that interval. Else, the update is done just once.
     *
     * repeatOnError - If repeating, updates will stop if an error is
     *                 encountered unless this is set to true.
     *
     * firstTimeWaitMessage - If defined, a wait blocker is displayed (on the
     *                        first update only).
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        UpdateDataTable = function (options) {
        var self = this;

        /*
         * If there is a table and it has rows selected, skip this update but
         * schedule a later one.
         */
        if (!this.OkToUpdate(options.table)) {
            AfLog('Updates paused');
            self.ScheduleNextUpdate(options);
            return;
        }

        if (!options.addedInterceptors) {
            /*
             * Create an intercept to the dataHandler so that we can schedule the
             * next update after this one succeeds.
             */
            options.oldDataHandler = options.dataHandler;
            options.dataHandler = function(manager, data, options) {
                /* Call the original function */
                if (options.oldDataHandler) {
                    options.oldDataHandler(manager, data, options);
                }
                /* Schedule next update */
                self.ScheduleNextUpdate(options);
            };

            /*
             * Create an intercept to the errorHandler so that we can schedule the
             * next update after this one fails.
             */
            options.oldErrorHandler = options.errorHandler;
            options.errorHandler = function(manager, options) {
                /* Call the original function */
                if (options.oldErrorHandler) {
                    options.oldErrorHandler(manager, options);
                }
                if (options.repeatOnError) {
                    /* Schedule next update */
                    self.ScheduleNextUpdate(options);
                }
            };

            /* Since this function calls itself, remember that we did this already */
            options.addedInterceptors = true;
        }

        /* Kick off an update */
        self.PopulateDataTableNow(options);
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.PopulateDataTableNow
     *
     * Invokes an Ajax call to the specified URL to fetch JSON data, and passes
     * that back, along with the table, to the specified callback function.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        PopulateDataTableNow = function (options) {
        var self = this;

        if (window.VmTAF.newUI) {
           if (undefined == options.ifModified) {
             options.ifModified = true;
           }

           // See bug 833750
           // Most browsers, when given a cached XHR, will follow the guidance
           // of the w3 spec at http://www.w3.org/TR/XMLHttpRequest/:
           //  "For 304 Not Modified responses that are a result of a user
           //   agent generated conditional request the user agent must act
           //   as if the server gave a 200 OK response with the appropriate content."
           //
           // That is: if the XHR content was previously cached, and the request
           // generated a 304 response from the server, the browser will pretend
           // that a 200 response was returned and hand the page the cached content.
           //
           // It will only do this for the first request on each page.
           //
           // However, it appears that firefox 8-10 does not do this.  Instead, they
           // always return a 304 when the server does.  This means that while we
           // know the data hasn't changed on the server, we have no way of knowing
           // WHAT the cached data is!  It's in the browser cache, but the browser
           // never sends it to us.
           //
           // To work around this, we disable caching on firefox.  This means that
           // the first AJAX request will IGNORE the browser's cache, even if it
           // contained the current content.  All subsequent calls, however, will
           // still use the cache.  So it still costs us time on the initial page load,
           // but doesn't cost us while the page is open.
           //
           if (undefined == options.cache) {
             options.cache = !$.browser.mozilla;
           }
        }

        /* Send the AJAX call to get the data */
        var jqXHR = AfAjax({
                               method: 'GET',
                               url: options.url,
                               /* Without this 'ifModified' parameter, jQuery always returns 200
                                * regardless of what the actual Http response code is.
                                */
                               ifModified: options.ifModified,
                               /*
                                * if the cache is true (default), the first 304 response
                                * will have the same content from browser cache, which is
                                * the same response from previous 200 response.
                                */
                               cache: options.cache,
                               /* Before send, show the refresh flag and possibly a wait blocker */
                               beforeSend: function() {
                                   if (options.showRefreshFlag) {
                                       AfRefreshFlagStart();
                                   }
                                   if (options.firstTimeWaitMessage) {
                                       AfStartPleaseWait(options.firstTimeWaitMessage);
                                   }
                               },

                               /* On success, call the client back, and hide the refresh flag */
                               success: function(data, textStatus, jqXHR) {

                                   // ignore updates if no change has occurred,
                                   // but only in the new UI
                                   //
                                   if (jqXHR.status == 304 && window.VmTAF.newUI) {
                                       if (options.showRefreshFlag) {
                                           AfRefreshFlagSuccess();
                                       }
                                       self.ScheduleNextUpdate(options);
                                       return;
                                   }
                                   if (options.dataHandler) {
                                       options.dataHandler(self, data, options);
                                   }
                                   if (options.showRefreshFlag) {
                                       AfRefreshFlagSuccess();
                                   }
                               },

                               /* On error, call the client back, and mark the refresh flag */
                               error: function() {
                                   if (options.errorHandler) {
                                       options.errorHandler(self, options);
                                   }
                                   else {
                                       AfLog('Data table update for ' + options.url + ' failed!');
                                   }
                                   if (options.showRefreshFlag) {
                                       AfRefreshFlagError();
                                   }
                               },

                               complete: function(jqXHR /*, textStatus*/) {
                                   /* End the optional wait blocker */
                                   if (options.firstTimeWaitMessage) {
                                       AfEndPleaseWait();
                                       delete options.firstTimeWaitMessage;
                                   }

                                   /* Find this request in the 'pending' list... */
                                   var requestIndex = -1;
                                   for (var i = 0; i < self.pendingAjax.length; i++) {
                                       if (self.pendingAjax[i] == jqXHR) {
                                           requestIndex = i;
                                           break;
                                       }
                                   }

                                   /* ...and forget about it */
                                   if (requestIndex >= 0) {
                                       self.pendingAjax.splice(requestIndex, 1);
                                   }
                               }
                           });

        this.pendingAjax.push(jqXHR);
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.ScheduleNextUpdate
     *
     * Set up a timer which will refresh the specified data table after the given
     * delay.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        ScheduleNextUpdate = function (options) {
        var self = this;

        /* Do nothing if there is no repeat time defined */
        if (!options.repeatMs || options.repeatMs <= 0) {
            return;
        }

        /* Create a timeout, and remember it. */
        self.timeoutId = window.setTimeout(
            function() {
                self.UpdateDataTable(options);
            },
            options.repeatMs,
            self);
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.OkToUpdate
     *
     * Return true if it's OK to update the table. This is true if there are no
     * rows selected. Add other conditions as needed.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        OkToUpdate = function (table) {
        if (!table) {
            return true;
        }

        /* Must be no checked rows */
        var checkedRows = table.find('tr:gt(0) input[type=checkbox]:checked');
        return checkedRows.length <= 0;
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.RowsAreChecked
     *
     * Return true if the specified table contains any rows that contain a
     * checkbox which is checked, except the first row which is assumed to be a
     * header.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        RowsAreChecked = function (table) {
        var checkedRows = table.find('tr:gt(0) input[type=checkbox]:checked');
        return (checkedRows.length > 0);
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.createRecordLink
     *
     * Create a link to a typical record (name and ID). If 'url' is specified,
     * then the returned element is a link which will open that page fragment when
     * clicked. Otherwise, the returned element is just a plain text span.
     *
     * @param name Record name.
     * @param id Record ID (optional).
     * @param url URL to open when clicked (optional).
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        createRecordLink = function(name, id, url) {
        // Create the text span
        var elem = $('<span>');
        elem.text(name);

        // If there is a URL, convert into a link
        if (url) {
            elem = this.wrapWithLoadLink(elem, url);
        }

        return elem;
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.wrapWithLoadLink
     *
     * Take an element and convert it into a link that, when clicked, will load
     * the specified page fragment. NOTE: Since 'onClick' is only supported for
     * input elements, we create a button and style it to look like a link.
     *
     * @param element Original element.
     * @param url URL to open when clicked.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        wrapWithLoadLink = function(element, url) {
        var script = "VmTAF.contentNavigator.LoadPage('" + url + "');";
        var link = $('<button onclick="' + script + '">');
        link.addClass('button-link');
        link.append(element);
        return link;
    };


    /**
     * -----------------------------------------------------------------------------
     * AbstractManager.createAlertLink
     *
     * Create a link that when clicked will display an alert dialog with the
     * specified title and message. NOTE: Since 'onClick' is only supported for
     * input elements, we create a button and style it to look like a link.
     * -----------------------------------------------------------------------------
     */
    AbstractManager.prototype.
        createAlertLink = function(text, alertTitle, alertMessage) {
        /* Escape single quotes and encode double quotes. */
        var safeMessage = alertMessage.replace(/'|"|\n|\r/gi, "\\'");

        /* Script to execute when clicked */
        var script = "AfShowAlert('" + alertTitle + "','" + safeMessage + "');";

        var link = $('<button onclick="' + script + '">');
        link.addClass('button-link');
        link.attr('title', alertMessage);
        link.append($('<span>').text(text));
        return link;
    };

   /**
    * Takes an array of objects, and the name of a property
    * on each of those objects.  Returns a sorted array
    * of the tuples  {name: xx, count: nn}
    * where "name" is a distinct value of the given property,
    * and "count" is the number of objects who had that
    * value.
    *
    * @param arrayHash        an array of objects whose
    * properties to use as the base of the histogram.
    *
    * @param subProperties    a string of the form "foo"
    * or "foo.bar" or "foo.bar.baz", denoting the first,
    * second or third-level property to take from each
    * element of arrayHash.
    *
    * @param translatePrefix  o a string thats prefixed with the value
    * and a AfTranslate() lookup is performed on the resulting string.
    * NOTE: Pass empty string to indicate no prefix based translate.
    *
    * example:
    *  arrayHash:
    *    [
    *       {foo: {bar: {baz: 'robbery'}}, name: 'Alice',   guilty: true  },
    *       {foo: {bar: {baz: 'arson'}},   name: 'Bob',     guilty: false },
    *       {foo: {bar: {baz: 'arson'}},   name: 'Charlie', guilty: true  },
    *    ]
    *
    * buildHistogram(arrayHash, "guilty") =>
    *    [ true: 2, false: 1 ]
    *
    * buildHistogram(arrayHash, "foo.bar.baz") =>
    *    [ arson: 2, robbery: 1 ]
    */
   AbstractManager.prototype.
      buildHistogram = function (arrayHash, subProperties, translatePrefix) {

      var histogram = new Array();
      var propArray = subProperties.split('.');

      if (arrayHash) {
         for (var i = 0; i < arrayHash.length; ++i) {
            var hash = arrayHash[i];
            if (hash) {
               var val = hash;
               for (var j = 0; j < propArray.length; ++j) {
                  val = val[propArray[j]];
               }
               if (val) {
                  // Store against the translated value if translatePrefix is set.
                  if (translatePrefix) {
                     val = AfTranslate(translatePrefix + val);
                  }
                  if (!histogram[val]) {
                     histogram[val] = 0;
                  }
                  histogram[val] += 1;
               }
            }
         }
      }

      var histArray = new Array();
      for (var name in histogram) {
         if (histogram.hasOwnProperty(name)) {
            histArray.push({ name: name, count: histogram[name]});
         }
      }

      histArray.sort(function(a,b) {
         var result = b.count - a.count;   // decreasing by count
         if (result == 0) {
            result = a.name - b.name;  // increasing by name
         }
         return result;
      });

      return histArray;
   };

   AbstractManager.prototype.
      replaceHistogramList = function (args) {

      var holder = $(args.ulElementId),
          data = args.data,
          table = args.tableId;

      holder.children().remove();
      $.each(data, function(idx, item) {
         var clkValue = '',
            displayName = item.name;
         if (!displayName || 0 == displayName.length) {
            displayName = "unknown";
         }
         if (table) {
            clkValue = "var t = $('" + table + "'), v = '" + item.name + "'; if($(this).hasClass('filter-selected'))"
            + " { if(t.parent().find('.dataTables_filter input').val() == v) { v=' '; $(this).removeClass('filter-selected'); }"
            + " } else { $(this).parents('.sidebar').find('li.filter-selected').removeClass('filter-selected');"
            + " $(this).addClass('filter-selected'); } t.dataTable().fnFilter(v);";
         }
         holder.append($('<li class="button-link" onclick="' + clkValue + '">')
            .text(displayName + ' (' + item.count + ')'));
      });
      if (!data || 0 == data.length) {
         holder.append($('<li>none</li>'));
      }

      var expandArgs = $.extend({}, {
         visibleRowCount: 3,
         showHiddenCount : true
      }, args);

      hideNthRowAddLinkToExpandCollapse(expandArgs);
   };
}()); // end "use strict"