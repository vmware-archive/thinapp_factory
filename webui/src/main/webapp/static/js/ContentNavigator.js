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
 * function ContentNavigator
 *
 * A ContentNavigator maintains a history of views that are visited by keeping
 * them in a stack. This is designed to work the same way as a browser's
 * 'Back' and 'Forward' buttons.
 * -----------------------------------------------------------------------------
 */
function ContentNavigator()
{
   this.readyFunctions = [];
   this.cleanupFunctions = [];
   this.verifyFunctions = [];

   if (!VmTAF.newUI && window.dhtmlHistory) {
        /* Initialization for RSH */
       window.dhtmlHistory.create({
         toJSON: function(o) {
            return JSON.stringify(o);
         },
         fromJSON: function(s) {
            return JSON.parse(s);
         }
       });
   }
}


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.InitializeOnLoad()
 *
 * Perform any ContentNavigator initialization that must be done AFTER the
 * DOM has been loaded.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
InitializeOnLoad = function _initializeOnLoad()
{
    if (!VmTAF.newUI) {
      window.dhtmlHistory.initialize();
      window.dhtmlHistory.addListener(this.historyChangeListener);
   } else {
        // this is the missing piece triggered by historyChangeListener
      this.InstallDefaultEventHandlers();
    }
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.SetLoadFunction()
 *
 * Replace the function that loads a new page with a function that you provide.
 * This is useful if you need specific control over whether or not the load
 * should happen, or if you need to modify the page load request in some way.
 *
 * The function you supply must have the signature:
 *   function func(contentNav, origLoadFunc, pageUrl, waitMessage)
 *
 * If your function decides the page load should execute normally, you must
 * invoke the original load function. Once you do this, your custom load
 * function is removed:
 *   origLoadFunc(contentNav, pageUrl, waitMessage).
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
SetLoadFunction = function _setLoadFunction(func)
{
   this.customLoadFunc = func;
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.LoadPage
 *
 * Load new content into the main content panel. This will set the
 * content for the main panel (#main-panel), which is the most common way of
 * displaying data in the AppFactory web UI. This will also remember the
 * page being navigated to, so that it can be recalled later from the Back
 * button.
 *
 * @param contentUrl
 * @param pleaseWaitMessage (optional)
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
LoadPage = function _loadPage(contentUrl, pleaseWaitMessage)
{
   var self = this;

    if (VmTAF.newUI) {
        if (0 !== contentUrl.indexOf(window.VmTAF.contextPath)) {
            contentUrl = window.VmTAF.contextPath + contentUrl;
        }
        document.location.href = contentUrl;
    } else {
        if (self.customLoadFunc) {
           /* Don't load content yet: invoke custom function instead */
           self.customLoadFunc(self, self.loadPageImpl, contentUrl, pleaseWaitMessage);
        }
        else {
           /* Go ahead and load it */
           self.loadPageImpl(self, contentUrl, pleaseWaitMessage);
        }
    }
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.GoBack
 *
 * Go back to the previous location and load that content.
 * If no calls to "LoadPage()" have been made, this function does nothing.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
GoBack = function _goBack()
{
   history.go(-1);
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.AddReadyFunction
 *
 * Add a function that will be called when the next page load is successful.
 * Pages typically use this to create and initialize subclasses of
 * AbstractManager when a view has been loaded.
 *
 * After the page has loaded, all ready functions are removed so they will not
 * be called again for other page loads.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
AddReadyFunction = function _addReadyFunction(func)
{
   if (VmTAF.newUI) {
      // just call the function immediately
      func.call(window);
   } else {
      this.readyFunctions.push(func);
   }
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.AddVerifyFunction
 *
 * Add a function that will be called just BEFORE the next page is loaded.
 * This function can be used to abort the page load (by returning false).
 * For example, if a page has unsaved edits, it would return false.
 *
 * After the verify functions are called, they are removed so they will not
 * be called again for other page loads.
 * @returns the index of the function that just added.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
AddVerifyFunction = function _addVerifyFunction(func)
{
   this.verifyFunctions.push(func);
   return this.verifyFunctions.length - 1;
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.RemoveVerifyFunction
 *
 * Remove a function from the verifyFunctions array.
 * @param index -  an index of the function to be removed.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
RemoveVerifyFunction = function _removeVerifyFunction(index)
{
   if (index > -1 && index < this.verifyFunctions.length) {
      this.verifyFunctions[index] = null;
   }
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.AddCleanupFunction
 *
 * Add a function that will be called just BEFORE the next page is loaded.
 * Pages typically use this to cleanup anything they created in a matching call
 * to a 'ready' function.
 *
 * After the cleanup functions are called, they are removed so they will not
 * be called again for other page loads.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
AddCleanupFunction = function _addCleanupFunction(func)
{
   this.cleanupFunctions.push(func);
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.InitializeOnLoad()
 *
 * Perform any ContentNavigator initialization that must be done AFTER the
 * DOM has been loaded.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
historyChangeListener = function _historyChangeListener(
      newLocation,
      historyData)
{
   /* If no data, we must be going to the page before TAF */
   if (historyData == null) {
      history.go(-1);
      return;
   }

   /*
    * (Re)load the data from the given history step.
    * Note this function is not necessarily called from our object,
    * so we must use the 'navigator' reference from the historyData.
    * */
   var navigator = historyData.navigator;
   if (navigator instanceof ContentNavigator) {
      navigator.loadContent(historyData.contentUrl);
   }
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.loadContent
 *
 * Load new content into the main content panel. This loads the fragment from
 * the given URL.
 *
 * @param contentUrl
 * @param pleaseWaitMessage (optional)
 *
 * @return True if the request to load was *issued* successfully, false
 *         otherwise.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
loadContent = function _loadContent(contentUrl, pleaseWaitMessage)
{
   var self = this;
   var mainPanel = $('#main-panel');

   /* First verify that a page transition is OK */
   for (var fi = 0; fi < self.verifyFunctions.length; fi++) {
      if (!self.verifyFunctions[fi]) {
         continue;
      }
      try {
         if (!self.verifyFunctions[fi]()) {
            AfLog('Current page vetoed a page change');
            return false;
         }
      }
      catch(error) {
         /* Verification error same as verification failed */
         AfLog('Page verification failed: ' + error);
         return false;
      }
   }

   /* Now call the cleanups */
   for (var fi = 0; fi < self.cleanupFunctions.length; fi++) {
      try {
         self.cleanupFunctions[fi]();
      }
      catch(error) {
         /* Log the error, but carry on */
         AfLog('Page cleanup error: ' + error);
      }
   }

   self.verifyFunctions = [];
   self.cleanupFunctions = [];

   /* Load the content */
   AfLog('loading content: ' + contentUrl);

   if (!pleaseWaitMessage) {
      pleaseWaitMessage = 'Loading - please wait';
   }

   /* If there is new content, load it */
   if (contentUrl) {
      /*
       * Get the HTML and insert into the DOM.
       * Note: jQuery provides the load(url) method for elements, but it seems
       * to fail more often than just fetching the HTML and updating the
       * DOM in two separate steps.
       */
      AfAjax({
         method: 'GET',
         url: contentUrl,
         dataType: 'html',

         beforeSend: function() {
            if (pleaseWaitMessage) {
               AfStartPleaseWait(pleaseWaitMessage);
            }
         },

         success: function _mainFragmentOk(html) {
            /* Update main panel with this HTML */
            mainPanel
               .empty()
               .append(html);

            /* Perform any other common DOM post-processing here */
            self.InstallDefaultEventHandlers();

            /* Now tell the content it is good to start working */
            for (var fi = 0; fi < self.readyFunctions.length; fi++) {
               try {
                  self.readyFunctions[fi]();
               }
               catch(error) {
                  /* Log error but then carry on as normal */
                  AfLog('Page ready function failed: ' + error);
               }
            }
            self.readyFunctions = [];
         },

         error: function _mainFragmentError() {
            AfLog('fragment load failed!');
            AfError(
                  'Page Load Failed',
                  'Page load failed: please try again.');
         },

         complete: function() {
            if (pleaseWaitMessage) {
               AfEndPleaseWait();
            }
         }
      });
   }

   return true;
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.InstallDefaultEventHandlers
 *
 * TODO Move into TableWrapper so its more tightly coupled and uses
 * tableWrapper.MarkAllRowCheckboxes()
 *
 * Install event handlers on common UI elements. This includes handling the
 * table header check boxes, and other stuff. This function is called whenever
 * new content is loaded.
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
InstallDefaultEventHandlers = function _installDefaultEventHandlers()
{
   /*
    * Reset (hide) all refresh indicators
    */
   AfRefreshFlagSuccess();
};


/**
 * -----------------------------------------------------------------------------
 * ContentNavigator.loadPageImpl
 *
 * Default function for loading content. We have this broken out from
 * LoadContent in case a custom load function is installed: that custom function
 * will be instructed to call loadPageImpl() if the content should be loaded
 * normally.
 *
 * @param self The ContentNavigator instance
 * @param contentUrl
 * @param pleaseWaitMessage (optional)
 * -----------------------------------------------------------------------------
 */
ContentNavigator.prototype.
loadPageImpl = function _loadPageImpl(self, contentUrl, pleaseWaitMessage)
{
   /* We don't need to custom load function any more */
   self.customLoadFunc = null;

   /* Load the content */
   var loaded = self.loadContent(contentUrl, pleaseWaitMessage);

   if (loaded && !VmTAF.newUI) {
      /* Remember where we are */
      var historyData = {
         navigator : self,
         contentUrl: contentUrl
      };
      window.dhtmlHistory.add(escape(contentUrl), historyData);
   }
};

