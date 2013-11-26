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
 * function FileSystemEditor
 *
 * A "class" that performs all the tasks for editing directory data.
 *
 * This class uses data as described by the CWS API for editing file system
 * settings. This data format describes nodes at a very shallow level: any
 * node contains names of its immediate children: further data is loaded
 * as those children are expanded in the editor.
 *
 * All edits are tracked by the editor and kept locally. The ProcessChanges()
 * method will look for edits, and send changed data to the back-end, as
 * a series of adds, deletes, and changes.
 * -----------------------------------------------------------------------------
 */
function FileSystemEditor(
         buildId,
         directoryRootData,
         projectPath,
         editorDiv,
         directLinkDiv,
         refreshButton,
         extendedMode,
         changeMarker)
{
   this.buildId = buildId;
   this.directoryRootData = directoryRootData;
   this.projectPath = projectPath;

   this.treeHolder = editorDiv.find('#filesystem-tree-holder');
   this.filesDiv = editorDiv.find('.filesystem-files');
   this.rootNode = null;
   this.deletionQueue = new Array();
   this.directLinkDiv = directLinkDiv;
   this.refreshButton = refreshButton;
   this.extendedMode = extendedMode;
   this.changeMarker = changeMarker;

   /* Only extended mode lets us do all sorts of editing through the UI */
   if (!extendedMode) {
      $('.extended-edit').remove();
   }
}

FileSystemEditor.prototype = new AbstractSettingsEditor();


/**
 * These are markers we attach to directories/files to track edits that
 * need to performed when writing data back to the server
 */
FileSystemEditor.prototype.EDIT_CHANGE = "CHANGE";
FileSystemEditor.prototype.EDIT_CREATE = "CREATE";
FileSystemEditor.prototype.EDIT_DELETE = "DELETE";

// TODO: These should come from the server
FileSystemEditor.prototype.ISOLATION_MODES = {
   /* Value    : Display Name */
   "full"      : "Full",
   "merged"    : "Merged",
   "writecopy" : "WriteCopy",
   "sb_only"   : "Sandbox Only"
};

FileSystemEditor.prototype.DEFAULT_ISOLATION_MODE = "full";



/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.Populate()
 *
 * Fills the file system editor with the given root directory data. This creates
 * trees for each sub folder. Further descendants are not loaded until needed,
 * i.e. when the nodes are expanded.
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
Populate = function _populate(
      newDirectoryRootData,
      newProjectPath)
{
   AfLog('Populating file system editor');

   /* Possibly update data */
   if (newDirectoryRootData) {
      this.directoryRootData = newDirectoryRootData;
   }
   if (newProjectPath) {
      this.projectPath = newProjectPath;
   }

   this.directLinkDiv.empty();

   if (this.projectPath) {
      var p1 = $('<p>').text(AfTranslate(
         /* On IE, a link will open Explorer in the right way */
         $.browser.msie ? 'M.BUILDS.ACCESS_PROJECT_DIR_IE' :
         /* Other browsers need manual user intervention */
         'M.BUILDS.ACCESS_PROJECT_DIR_EXPLORER'));

      var link = $('<a>');
      link.attr('href', this.projectPath);
      link.text(this.projectPath);

      var p2 = $('<p>').text(
         'If you make any changes to files or directories using Windows ' +
         'Explorer, check the box below before pressing ' +
         '"' + AfTranslate('T.BUILDS.SAVE_FILE_SYSTEM') + '".');

      this.directLinkDiv.append(p1).append(link).append(p2);
      this.refreshButton.show();
   }
   else {
      /* No point refreshing if you can't access it */
      this.refreshButton.hide();
   }

   /* Clear away old stuff */
   this.treeHolder.empty();
   this.deletionQueue.length = 0;

   /*
    * The file system root corresponds to project's root output directory.
    */
   this.treeHolder.append('<div id="filesystem-holder"></div>');
   this.treeHolder.addClass('ygtv-highlight');

   this.tree = new YAHOO.widget.TreeView('filesystem-holder');
   this.tree.filesystemEditor = this;
   this.tree.setDynamicLoad(this.loadDirectory);
   this.tree.singleNodeHighlight = true;

   /* Tree root represents '/' */
   this.rootNode = this.createNodeForDir(
         'Project Root',
         this.tree.getRoot(),
         this.directoryRootData);
   this.rootNode.expanded = true;
   this.rootNode.data.directoryData = this.directoryRootData; // XXX
   this.populateData(this.rootNode);

   /* Create each sub-directory as a child node. */
   if (this.directoryRootData) {
      for (var dirName in this.directoryRootData.directories) {
         var dirUrl = this.directoryRootData.directories[dirName];

         this.createNodeForDir(
               dirName,
               this.rootNode,
               this.directoryRootData.directories[dirName]);
      }
   }

   /* Listen for clicks so we can show files values */
   this.tree.subscribe("clickEvent", function(where) {
      var node = where.node;
      node.data.self.populateData(node);
      node.data.self.tree.onEventToggleHighlight(where);
      return false; // stop the expand/collapse event handler
   });

   this.tree.render();
   AfLog('File system editor populated');
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.ProcessChanges
 *
 * Walk this directory editor and look for edits that have been made since this
 * function was last called. If new directories are found, the 'createFunction'
 * is called. If edited directories are found, the 'editFunction' is called. If
 * deleted directories are found, the 'deleteFunction' is called.
 *
 * @param createFunction Callback invoked for new directories.
 * @param editFunction Callback invoked for edited directories.
 * @param deleteFunction Callback invoked for deleted directories.
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
ProcessChanges = function _processChanges(
      createFunction,
      editFunction,
      deleteFunction)
{
   var success = true;

   AfLog("Processing directory changes");
   try {
      AfLog('---- DIRECTORY CREATES/EDITS: -----');
      this.processNode(createFunction, editFunction, this.rootNode, 0);

      AfLog('---- DELETION QUEUE: -----');
      for (var i = 0; i < this.deletionQueue.length; i++) {
         var node = this.deletionQueue[i];
         var dirData = node.data.directoryData;

         AfLog('Deleting directory ' + node.label);
         deleteFunction(
               this.buildId,
               dirData.id,
               dirData);
      }
      AfLog('-------------------------------');
   }
   catch(error) {
      AfLog("Error: " + error);
      success = false;
   }

   return success;
};


/**
 * -----------------------------------------------------------------------------
 * FileSytemEditor.processNode()
 *
 * Process the given node, find out what needs to be done, and do it.
 * This includes creating directories, editing directories, etc. This is a
 * recursive function: it will process this node first, and then all its
 * children.
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
processNode = function _processNode(
      createFunction,
      editFunction,
      node,
      depth)
{
   var dirData = node.data.directoryData;
   var debugPreamble = (dirData ? '.../' + dirData.path : node.label);

   for (var i = 0; i < depth; i++) {
      debugPreamble = '  ' + debugPreamble;
   }

   if (!dirData) {
      /*
       * No data for this node. This means nothing was ever loaded for it,
       * so therefore nothing can have been changed. Skip it.
       */
      AfLog(debugPreamble + ' -> no data');
   }
   else if (dirData.editAction) {
      /* Remember the action, and delete it (else it messes up the JSON) */
      var action = dirData.editAction;
      delete dirData.editAction;
      AfLog(debugPreamble + ' -> ' + action);

      if (action === this.EDIT_CREATE) {
         /*
          * This directory was added interactively by the user.
          * Therefore, create a new directory key on the back-end.
          */
         var parentNode = node.parent;
         var newUrl = createFunction(
               this.buildId,
               parentNode.data.directoryData.id,
               dirData);
         node.data.directoryUrl = newUrl;
      }
      else if (action === this.EDIT_CHANGE) {
         /*
          * One or more files/attributes attached to this directory node have
          * been edited (values changed), so we must update the directory'
          * on the back-end.
          */
         editFunction(
               this.buildId,
               dirData.id,
               dirData);
      }
      else if (action === this.EDIT_DELETE) {
         // TODO: deleted...
      }
      else if (action === this.EDIT_RENAME) {
         // TODO: renamed...
      }
      else {
         //AfLog(debugPreamble + ': unchanged');
      }
   }
   else {
      AfLog(debugPreamble + ' -> no edit action');
   }

   /*
    * This directory node is complete. Now process each child node, which
    * correspond to sub-directories of this directory.
    */
   for (var i = 0; i < node.children.length; i++) {
      this.processNode(createFunction, editFunction, node.children[i], depth + 1);
   }
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.loadDirectory
 *
 * Loads all the child directories for a given directory. This allows
 * the file system explorer to be loaded on demand, as the user expands nodes.
 *
 * NOTE: This function prototype is defined by the YUI API.
 *
 * @param node The node to be expanded
 * @param loadCompleteCallback YUI callback we must invoke to tell the tree
 *                             to redraw
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
loadDirectory = function _loadKey(node, loadCompleteCallback)
{
   // Get the FileSystemEditor instance
   var self = node.data.self;
   var tree = node.tree;

   AfLog('Dynamic data load for ' + node.label + ' resource=' + node.data.directoryUrl);

   if (node === self.rootNode) {
      /* Don't load anything for the root node */
      AfLog('root node --- skipped');
   }
   else if (!node.data.directoryUrl) {
      /*
       * Could be a new directory that we have created, but not yet saved.
       * In this case, there is no resource URL for it. So skip it.
       */
      AfLog('no directoryUrl (new directory?) --- skipped');
   }
   else if (node.data.directoryUrl) {
      /*
       * Synchronously fetch the directory data for this node.
       * NOTE: the directoryUrl value is a CWS resource URL, so we can't
       * load it directly (cross-site scripting), so we pass it to the
       * webui, which will act as a proxy for us.
       */

      /* Get the directory ID from its CWS resource */
      var dirId = self.parseDirectoryIdFromUrl(node.data.directoryUrl);
      var url = '/api/builds/' + self.buildId + '/directory/' + dirId;
      AfLog('Dynamic data load URL = ' + url);

      AfAjax({
         method: 'GET',
         url: url,
         async: false,

         success: function(directoryData) {
            AfLog('Dynamic data load success! data has ' + directoryData.directories + ' dirs');
            node.data.directoryData = directoryData;

            /* Create child nodes */
            for (var label in directoryData.directories) {
               var url = directoryData.directories[label];
               self.createNodeForDir(label, node, url);
            }

            /* If selected, populate the values too */
            if (node === node.tree.currentFocus) {
               AfLog('this is the current node; setting values');
               self.populateData(node);
            }
         },
         error: errorHandlerForRebuildState
      });
   }

   if (loadCompleteCallback) {
      loadCompleteCallback();
   }
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.createNodeForDir()
 *
 * Create a new tree node for a directory. This utility function ensures
 * that all nodes are created consistently.
 *
 * @param label Label for node (directory name)
 * @param parentNode
 * @param url The CWS resource URL (Note: might be on a foreign server)
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
createNodeForDir = function _createNodeForDir(
      label,
      parentNode,
      url)
{
   var node = new YAHOO.widget.TextNode(label, parentNode, false);
   node.data.self = this;
   node.data.directoryUrl = url;
   node.data.directoryData = undefined; // i.e. not yet loaded

   return node;
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.populateData
 *
 * Fill in the files and attributes for a selected directory node.
 *
 * @param node
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
populateData = function _populateData(node)
{
   this.populateFiles(node);
   this.populateAttrs(node);
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.populateFiles
 *
 * Fill in the file system editor form by listing all the files in the given
 * directory node.
 *
 * @param node
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
populateFiles = function _populateFiles(node)
{
   AfLog('Populating files for ' + node.label);

   var self = this;
   var insertPoint = $('#filesystem-files-holder');
   var directoryData = node.data.directoryData;

   /* Delete existing files */
   insertPoint.empty();

   /*
    * If there are no files, there is nothing else to do with this node.
    */
   if (!directoryData || !directoryData.files) {
      /* There are no files for this directory. */
      return;
   }

   /*
    * There are files for this directory node.
    * Create form fields for each one.
    */
   for (var fileName in directoryData.files) {
      var file = directoryData.files[fileName];

      /* Field DIV */
      var fieldDiv = $('<div></div>');
      fieldDiv.addClass('field');

      var fileSpan = $('<span>').text(fileName);
      fieldDiv.append(fileSpan);

      if (self.extendedMode) {
         /* Delete button: click this to delete this entry */
         var delIcon = $('<span class="delete-icon"></span>');
         delIcon.click(fileName, function(event) {
            self.DeleteFile(event.data);
         });
         fieldDiv.append(delIcon);
      }

      /* Create a label for this file, link it to the input field */
      var label = $('<label>');
      label.attr('for', fileName);
      label.text('File Name');

      var labelDiv = $('<div></div>').addClass('label');
      labelDiv.append(label);

      var row = $('<div></div>').addClass('stack-row');
      row.append(labelDiv).append(fieldDiv);
      insertPoint.append(row);
   }
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.populateAttrs
 *
 * Fill in the file system editor form by listing all the files in the given
 * directory node.
 *
 * @param node
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
populateAttrs = function _populateAttrs(node)
{
   AfLog('Populating attrs for ' + node.label);

   var self = this;
   var insertPoint = $('#filesystem-attrs-holder');
   var directoryData = node.data.directoryData;

   /* Delete existing 'stack-row' and set the title */
   insertPoint.empty();

   if (!directoryData.attributes) {
      /* There are no attributes for this directory. */
      return;
   }

   /*
    * There are attributes for this directory node.
    * Create form fields for each one.
    */
   for (var sectionName in directoryData.attributes) {
      var section = directoryData.attributes[sectionName];

      for (var attrName in section) {
         var attr = section[attrName];
         var canDelete = true;

         /* Field DIV */
         var fieldDiv = $('<div></div>');
         fieldDiv.addClass('field');

         var input = null;
         if (attrName == "DirectoryIsolationMode") {
            /* Create input field for isolation mode */
            input = self.createIsolationModeInput(directoryData);
            fieldDiv.addClass('select');
         }
         else {
            /* Create generic input field for attribute */
            input = $('<input>');
            input.attr('size', 40);
            input.attr('type', 'text');
            fieldDiv.addClass('text long');
         }


         if (input) {
            input.attr('name', attrName);
            input.data('sectionName', sectionName);
            fieldDiv.append(input);

            /* When input value changes, update the attribute */
            input.change(function() {
               var sectionName = $(this).data('sectionName');
               var attrName = $(this).attr('name');
               var val = $(this).val();
               node.data.directoryData.attributes[sectionName][attrName] = val;
               node.data.directoryData.editAction = self.EDIT_CHANGE;
               self.MarkAsChanged();
            });
         }
         else {
            var attrSpan = $('<span>').text(attr.type);
            fieldDiv.append(attrSpan);
         }

         if (self.extendedMode) {
            /* Delete button: click this to delete this entry */
            var delIcon = $('<span class="delete-icon"></span>');
            delIcon.addClass("extended-edit");
            delIcon.click(attrName, function(event) {
               self.DeleteAttr(event.data);
            });

            fieldDiv.append(delIcon);
         }

         /* Create a label for this attribute, link it to the input field */
         var label = $('<label>');
         label.attr('for', attrName);
         label.text(attrName);

         var labelDiv = $('<div></div>').addClass('label');
         labelDiv.append(label);

         var row = $('<div></div>').addClass('stack-row');
         row.append(labelDiv).append(fieldDiv);
         insertPoint.append(row);
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.findNodeForData()
 *
 * Return the node in the tree which has the specified directory data
 * instance attached to it. This should never return null, or more than one
 * match.
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
findNodeForData = function _findNodeForData(directoryData)
{
   var match = null;

   this.tree.getNodesBy(function _test(node) {
      if (node.data.directoryData === directoryData) {
         match = node;
      }
   });
   return match;
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.parseDirectoryIdFromUrl()
 *
 * Extract a directory ID from a CWS directory resource URL.
 * Since we can't access CWS directly (cross-host scripting is bad), we need
 * to pull the ID from a CWS URL and use that to pass back to our own server.
 *
 * The assumption is that the CWS resource URL ends with "..../{id}"
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
parseDirectoryIdFromUrl = function _parseDirectoryIdFromUrl(url)
{
   return url.split('/').pop();
};


/**
 * -----------------------------------------------------------------------------
 * FileSystemEditor.createIsolationModeInput()
 *
 * Creates an HTML input element for displaying isolation mode values.
 * -----------------------------------------------------------------------------
 */
FileSystemEditor.prototype.
createIsolationModeInput = function(directoryData)
{
   /* Make a pulldown selector */
   var select = $('<select>');
   select.data('directoryData', directoryData);

   /* Add each value option */
   for (var isoMode in this.ISOLATION_MODES) {
      var displayName = this.ISOLATION_MODES[isoMode];
      var opt = $('<option>').val(displayName);
      opt.text(displayName);
      select.append(opt);
   }

   /* Set to current value */
   select.val(directoryData.attributes.Isolation.DirectoryIsolationMode);

   return select;
};

