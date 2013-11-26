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
 * function AppStoreManager
 *
 * Class for all methods related to the ThinApp Store page. Create one of these
 * for a store page.
 * -----------------------------------------------------------------------------
 */
function AppStoreManager(props)
{
   this.serverName = props.serverName;
   this.serverPort = props.serverPort;
   this.actionButton = props.actionButton;
   this.action = props.action;

   this.plugin = new ThinPlugin();

   this.actionIds = [];
   this.actionButtonVisible = false;
   this.tabView = props.tabView;

   /* There is a IE 8 limitation of a GET url being 2083 chars and a windows
    * limitation of over 2000 characters, hence stay under the limitation.
    */
   this.maxURLSize = 1990;

   AfLog('Created a new AppStoreManager for server ' + this.serverName);
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.InstallEventHandlers
 *
 * Install event handlers to each builds specified. When a build in this
 * list is clicked, it is added to the list of builds to act on, and the action
 * button is displayed as necessary.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
InstallEventHandlers = function _installEventHandlers(builds)
{
   var self = this;

   // Install click handler for the entire element.
   builds.click(function() {
      var ck = $(this).find('input:checkbox');
      ck.attr('checked', !ck.attr('checked'));
      self.HandleBuildClickEvent($(this));
   });
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.HandleBuildClickEvent
 *
 * Handle the click on a build element.
 * If selected, add it to the action list, else remove it from the action
 * list. Then, show or hide the action button as needed.
 *
 * @param build
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
HandleBuildClickEvent = function _handleInstallCheck(build)
{
   var box = build.find('input:checkbox');
   var buildId = box.attr("value");

   if (box.attr("checked")) {

      // Make sure the url size is not reached.
      if ( this.actionButton.attr('href').length >= this.maxURLSize) {
         box.attr("checked", false);
         AfErrorT('T.STORE.MAX_URL_SIZE', 'M.STORE.MAX_URL_SIZE');
         return;
      }

      // Add build to action list
      if (jQuery.inArray(buildId, this.actionIds) == -1) {
         this.actionIds.push(buildId);
      }
   }
   else {

      // Remove build from install list
      if (jQuery.inArray(buildId, this.actionIds) > -1) {
         this.actionIds.splice(jQuery.inArray(buildId, this.actionIds), 1);
      }
   }
   build.toggleClass('checked');
   this.UpdateActionButton(this.actionIds, this.action);
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.UpdateActionButton
 *
 * Update the HREF that the action button points to. The link includes a GET
 * parameter for each build to be acted on. Toggle action button display.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
UpdateActionButton = function _updateActionButton(pkgList, action)
{
   var str = null;

   if ((this.plugin == null) || (!this.plugin.IsInstalled()) || (!this.plugin.IsLatestVersion())) {
      /* There is no plug-in installed, so link to AppFactory to download it */
      AfLog('No plugin: href is for ' + VmTAF.contextPath);
      str = VmTAF.contextPath + '/store/bootstrap?action=' + action;
      if ( pkgList.length > 0) {
         str += '&id=' + pkgList.join('&id=');
         if (str.length >= this.maxURLSize) {
            AfNotifyT('T.STORE.MAX_URL_SIZE', 'M.STORE.MAX_URL_SIZE', 'warn');
         }
      }
   }
   else {
      /* There is a plug-in, so create a link that it will recognize */
      AfLog('Plugin detected: href is plugin argument');
      str = 'thinapp://' + this.serverName + ':' + this.serverPort
         + VmTAF.contextPath + '/' + action;

      // Create a comma separated list of buildIds
      if (pkgList.length > 0) {
         str += '?capture=' + pkgList.join('.');
         if (str.length >= this.maxURLSize) {
            AfNotifyT('T.STORE.MAX_URL_SIZE', 'M.STORE.MAX_URL_SIZE', 'warn');
         }
      }
   }

   this.actionButton.attr('href', str);
   this.actionButton.find('.appCount').text(
         (pkgList.length==0)? ' ' : '(' + pkgList.length + ')' );

   // Toggle display of ActionButton if necessary.
   this.ToggleActionButton();
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.ToggleActionButton
 *
 * Hide or show the action button, as needed.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
ToggleActionButton = function _toggleInstallButton()
{
   // Show it if there are builds to be installed
   var show = (this.actionIds.length > 0);

   if (show && !this.actionButtonVisible) {
      this.actionButtonVisible = true;
      this.actionButton.animate( {
         bottom : "0"
      });
   }
   else if (!show && this.actionButtonVisible) {
      this.actionButtonVisible = false;
      this.actionButton.animate( {
         bottom : "-280px"
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.PopulatePublishedBuilds
 *
 * Fill the page with all published builds.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
PopulatePublishedBuilds = function _populatePublishedBuilds()
{
   var self = this;
   AfLog('Populating page with published builds');

   /* Get a list of all published builds */
   AfLog('Getting list of published builds');
   AfAjax({
      method: 'GET',
      url: '/api/builds?checkDatastore=true&status=PUBLISHED',
      async: false,
      success: function(json) {
         AfLog('Build details OK');
         if (!json || json.builds.length == 0) {
            $('#category-tabs').append($('<h1></h1>').append(
                  AfTranslate('M.STORE_NO_APPS_AVAILABLE')));
         }
         else {
            /* set withTabs=true for tabs */
            var builds = json.builds;
            self.PopulateBuilds(builds, self.tabView);
         }
      },
      failure: function() {
         AfLog('FAILED to get list of published builds!');
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.PopulateInstalledBuilds
 *
 * Fill the page with all installed builds.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
PopulateInstalledBuilds = function _populateInstalledBuilds()
{
   var self = this;
   var builds = new Array();
   AfLog('Populating page with installed builds');

   /* Get the installed build list */
   AfLog('Getting installed ids from plugin');
   try {
      var installedList = self.plugin.GetInstalledApps();
      if (installedList == null) {
         return;
      }
   }
   catch(error) {
      AfError(
         'Error',
         'Error with ' + VmTAF.productName + ' browser plugin, or plugin not installed.'
         + '<p><b>Note:</b><br/>When installing a ThinApp package, the plugin will be installed if you dont have the plugin.</p>');
      return;
   }

   /* Convert each ID into the build */
   for (var i = 0; i < installedList.length; i++) {
      /* This is what the plug-in thinks it stores */
      var appId = installedList[i][0];
      var captureId = installedList[i][1];

      /* The captureId is really a buildId */
      var buildId = captureId;

      AfLog('Getting installed build ' + buildId);
      AfAjax({
         method: 'GET',
         url: '/api/builds/' + buildId,
         async: false,
         success: function _populateInstalledBuildsSuccess(json) {
            builds.push(json);
            AfLog('Build details OK');
         },
         error: function() {
            AfLog('FAILED to get build!');
         }
      });
   }

   if (builds.length > 0) {
      this.PopulateBuilds(builds, this.tabView);
   }
   else {
      $('#category-tabs').append($('<h1>').append(
            AfTranslate('M.STORE.NO_APPS_INSTALLED')));
   }

   return;
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.PopulateBuilds
 *
 * Fill the current page with the specified list of builds.
 * The display does not use tabs, but leaving it here just in case. If we dont
 * want to use tabs, then we need to cleanup this function and store.css
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
PopulateBuilds = function _populateBuilds(builds, withTabs)
{
   var self = this;
   var installedBuilds = null;

   AfLog("Populating builds: available builds: " + builds.length);

   /* Create a map, grouping together all builds by category */
   var buildMap = new Object();
   var buildCats = new Array();
   for (var i = 0; i < builds.length; i++) {
      var build = builds[i];

      var cat = 'Other';
      if (build.categories && build.categories.length > 0) {
         cat = build.categories[0]; // XXX ignoring all but the first
      }

      if (!buildMap.hasOwnProperty(cat)) {
         buildMap[cat] = new Array();
         buildCats.push(cat);
      }
      buildMap[cat].push(build);
      buildMap[cat].sort();
   }

   buildCats.sort();

   if (withTabs) {
      /* Create UL list of each category */
      var tabList = $('#category-tabs').find('ul');
      for (var i = 0; i < buildCats.length; i++) {
         var cat = buildCats[i];
         var anchor = cat.replace(' ', '_');
         var builds = buildMap[cat];
         AfLog('Category: ' + cat + ', numBuilds: ' + builds.length);

         /* Add tab for this category */
         var tabItem = $('<li>').append('<a href="#' + anchor + '">' + cat + '</a>');
         tabList.append(tabItem);
      }
   }
   else if (buildCats.length > 1) {
      /* If more than 1, display dropdown of categories. */
      this.createCategoryDropdown($('#category-tabs'), buildCats);
   }

   /* Create tab content for each category */
   for (var i = 0; i < buildCats.length; i++) {
      var cat = buildCats[i];
      var anchor = cat.replace(' ', '_');
      var builds = buildMap[cat];

      /* Create a div for this tab's content */
      var tabDiv = $('<div id="' + anchor + '">').addClass('category');
      $('#category-tabs').append(tabDiv);

      /* Create inline category headers when tabs are not used */
      if (!withTabs && buildCats.length > 1) {
         var icon = $('<div></div>').addClass('icon');
         var catHeader = $('<div></div>').addClass('catHeader');
         catHeader.append(icon).append(cat);
         tabDiv.append(catHeader);

         catHeader.bind('click', function() {
            $(this).find('.icon').toggleClass('collapsed');
            $(this).next().slideToggle();
         });
      }

      /* Content needs a container child */
      var containerDiv = $('<div></div>').addClass('category-container');
      tabDiv.append(containerDiv);

      /* Container contains a list (of builds) */
      var appList = $('<ul id="application-list" class="application-list">');
      containerDiv.append(appList);

      /* Each build, as a list item */
      for (var b = 0; b < builds.length; b++) {
         var build = builds[b];

         var li = $('<li>').addClass('clearfix thinapp');
         li.attr('id', build.name + ' ' + build.version);

         var check = $('<div></div>').addClass('app-checkbox');
         check.append($('<input type="checkbox" name="app" value="' + build.id + '">'));
         li.append(check);

         /* Create the ApplicationBlock listing with imgSize: 32px */
         var app = AfCreateApplicationInline(build, true, window.VmTAF.iconSizeLarge);
         li.append(app);

         appList.append(li);
      }

      /* Resize parent so all children are covered */
      var numRows = Math.ceil(builds.length / 3);
      var parentHeight = 80 * numRows; // height of li.application + margins
      containerDiv.css('height', parentHeight);
   }

   /* Display jquery-ui tabs if enabled. */
   if (withTabs) {
      $( "#category-tabs" ).tabs({
         cache: true,
         selected: 0,
         ajaxOptions: { async: false },
         show: function() { $(this).show(); }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.createCategoryDropdown
 *
 * Helper function to create a dropdown with the list of build category.
 * This would be loaded as the first child of parentDiv.
 * @param parentDiv - prepend select to this.
 * @param buildCats - List of build categories
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
createCategoryDropdown = function _createCategoryDropdown(parentDiv, buildCats)
{
   /* Default ALL selected drop-down */
   var all = $('<option>').val('ALL').text(AfTranslate('T.STORE.SELECT_ALL'));
   var select = $('<select>').append(all);
   select.val('ALL');

   /* Create an option value for each category */
   for (var i = 0; i < buildCats.length; i++) {
      var cat = buildCats[i];
      var opt = $('<option>').val(cat.replace(' ', '_')).text(cat);
      select.append(opt);
   }

   /* When the selection changes, update the registry data */
   select.change(function() {
      var catSelected = $(this).val();
      var catAll = $(this).parent().nextAll('.category');
      if ('ALL' == catSelected) {
         $('#page').focus();
         // Expand all category.
         for (var i = 0; i < catAll.length; i++) {
            var collapsedCat = $(catAll[i]).find('.catHeader > .collapsed');
            if (collapsedCat) {
               collapsedCat.click();
            }
         }
      }
      else {
         $('#' + catSelected).focus();
         // Collapse all category but 'catSelected'
         for (var i = 0; i < catAll.length; i++) {
            var cat = $(catAll[i]).find('.catHeader > .icon');
            if (catSelected == $(catAll[i]).attr('id')) {
               if (cat.hasClass('collapsed')) {
                  cat.click();
               }
            }
            else if (!cat.hasClass('collapsed')) {
               cat.click();
            }
         }
      }
   });

   var selectDiv = $('<div></div>').addClass('cat-filter');
   selectDiv.append(AfTranslate('T.STORE.CATEGORY_DROPDOWN_LABEL'));
   selectDiv.append(select);
   parentDiv.prepend(selectDiv);
};


/**
 * -----------------------------------------------------------------------------
 * AppStoreManager.CreateViewLink
 *
 * Helper function to create a link inside the passed element to switch between
 * different views on this page.
 *
 * @param linkHolder - append link here.
 * -----------------------------------------------------------------------------
 */
AppStoreManager.prototype.
CreateViewLink = function _createViewLink(linkHolder)
{
   var self = this;

   // Set title based on current view selection.
   var link = $('<a>').text(AfTranslate(
         (self.tabView)? 'T.STORE.PAGE_VIEW' : 'T.STORE.TAB_VIEW'));

   // Click handler to perform view transition.
   link.click(function() {
      self.tabView = !self.tabView;
      $(this).text(AfTranslate(
            (self.tabView)? 'T.STORE.PAGE_VIEW' : 'T.STORE.TAB_VIEW'));
      // XXX perform view transition & keep selected intact.
   });

   linkHolder.append (link);
};