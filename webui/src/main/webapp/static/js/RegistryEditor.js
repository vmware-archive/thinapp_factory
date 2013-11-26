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
 * function RegistryEditor
 *
 * A "class" that performs all the tasks for editing registry data.
 *
 * This class uses data as described by the CWS API for editing registry
 * settings. This data format describes nodes at a very shallow level: any
 * node contains names of its immediate children: further data is loaded
 * as those children are expanded in the editor.
 *
 * All edits are tracked by the editor and kept locally. The ProcessChanges()
 * method will look for edits, and send changed data to the back-end, as
 * a series of adds, deletes, and changes.
 * -----------------------------------------------------------------------------
 */
function RegistryEditor(buildId, registryRootData, editorDiv, changeMarker)
{
   this.buildId = buildId;
   this.registryRootData = registryRootData;

   this.treeHolder = editorDiv.find('#registry-tree-holder');
   this.valuesDiv = editorDiv.find('.registry-values');
   this.rootNode = null;
   this.deletionQueue = new Array();
   this.changeMarker = changeMarker;
}

RegistryEditor.prototype = new AbstractSettingsEditor();


/**
 * These are markers we attach to registry nodes to track edits that
 * need to performed when writing data back to the server
 */
RegistryEditor.prototype.EDIT_CHANGE = "CHANGE";
RegistryEditor.prototype.EDIT_CREATE = "CREATE";
RegistryEditor.prototype.EDIT_DELETE = "DELETE";

/**
 * This is the path separator for the URL.
 */
RegistryEditor.prototype.REG_PATH_SEPARATOR = '/';

// TODO: These should come from the server
// Meanwhile, make sure they match CwsSettingsRegKey.IsolationMode
RegistryEditor.prototype.ISOLATION_MODES = {
   /* Value    : Display Name */
   "full"      : "Full",
   "merged"    : "Merged",
   "writecopy" : "WriteCopy",
   "sb_only"   : "Sandbox Only"
};

RegistryEditor.prototype.DEFAULT_ISOLATION_MODE = "full";



/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.Populate()
 *
 * Fills the registry editor with the given registry root data. This creates
 * trees for each child of the registry root (i.e. the registry hives).
 * Further descendants are not loaded until needed, i.e. when the nodes
 * are expanded.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
Populate = function _populate()
{
   AfLog('Populating registry editor');

   /* Clear away old stuff */
   this.treeHolder.empty();
   this.deletionQueue.length = 0;

   /*
    * The registry root corresponds to "My Computer" when using the Windows
    * registry editor. This root node contains all the hives, but has no
    * data of its own.
    */
   this.treeHolder.append('<div id="registry-holder"></div>');
   this.treeHolder.addClass('ygtv-highlight');

   this.tree = new YAHOO.widget.TreeView('registry-holder');
   this.tree.registryEditor = this;
   this.tree.setDynamicLoad(this.loadKey);
   this.tree.singleNodeHighlight = true;

   /* Tree root represents 'My Computer' */
   this.rootNode = this.createNodeForKey(
         'My Computer',
         this.tree.getRoot(),
         null,
         true);
   this.rootNode.expanded = true;
   this.rootNode.data.registryData = null;

   /* Create each hive as child node. */
   for (var hiveName in this.registryRootData.subkeys) {
      var regKeyData = this.registryRootData.subkeys[hiveName];

      this.createNodeForKey(
            hiveName,
            this.rootNode,
            regKeyData.url,
            regKeyData.hasChildren);
   }

   /* Listen for clicks so we can show registry values */
   this.tree.subscribe("clickEvent", function(where) {
      var node = where.node;

      /*
       * Hack away for YUI. when isLeaf is set, the dynamic loader is
       * removed, there by allowing the ahref='#' to kick in. This is
       * not desired, and hence we avoid this when its a leaf node.
       */
      if(node.isLeaf && where.event) {
         where.event.preventDefault();
      }
      node.data.self.PopulateValues(node);
      node.data.self.tree.onEventToggleHighlight(where);
      return false; // stop the expand/collapse event handler
   });

   this.tree.render();
   AfLog('Registry editor populated');
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.CreateKey()
 *
 * Asks the user for the name of a registry key, and adds it as a new child
 * to the currently selected key.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
CreateKey = function _createKey()
{
   AfLog('creating new registry key');

   /* Get the selected key to be the parent */
   var parentNode = this.tree.getHighlightedNode();

   if (!parentNode) {
      AfError(
         'No Selection',
         'To add a new key, first select a parent key.');
      return;
   }
   else if (parentNode === this.rootNode) {
      AfError(
         'Invalid Selection',
         'New keys cannot be added to "' + parentNode.label + '"');
      return;
   }

   AfLog('adding key to ' + parentNode.label);

   // Get a unique name thats not an existing child node.
   var name = AfPromptForUniqueName(
            this.ValidateUniqueRegNameCallback,
            parentNode);

   if (!name || name == '') {
      return;
   }

   /*
    * Create the new node.
    * Note that we use a marker URL to indicate it's a node we created,
    * and not an existing node from the server, set this node with no children
    */
   var node = this.createNodeForKey(
         name,
         parentNode,
         null,
         false);

   /* The server won't have data for it, so fake some. */
   node.data.registryData = {
      path : parentNode.data.registryData.path + '\\' + name,
      isolation : this.DEFAULT_ISOLATION_MODE,
      subkeys : new Object(),
      values : new Object(),
      editAction: this.EDIT_CREATE
   };

   /* Add into the parent */
   parentNode.data.registryData.subkeys[name] = null;
   /* Explicitly mark the parent as not a leaf node and display expanded. */
   parentNode.isLeaf = false;
   parentNode.expanded = true;

   this.MarkAsChanged();

   /* Reflect this in the tree */
   this.tree.render();
   AfLog('registry key created');
};

/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.RenameKey()
 *
 * Rename the currently selected registry key (and by inference, all its
 * children path will be affected internally).
 *
 * @param otherNode - if this is valid, rename this node, else selected node.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
RenameKey = function _renameKey(otherNode)
{
   /* Pick the otherNode, if invalid get the selected key to rename */
   var node = otherNode || this.tree.getHighlightedNode();

   if (!node) {
      AfError('No Selection', 'No key selected.');
      return;
   }
   else if (node === this.rootNode) {
      AfError('Invalid Selection', 'You can\'t rename this key!');
      return;
   }
   else if (node.data.registryUrl) {
      /*
       * Could be a new key that we have created, but not yet saved, skip it.
       * XXX Enable rename feature in backend, and remove this get rid of this.
       */
      AfError('Not Supported', 'At the moment we only support renaming of a newly added key.');
      return;
   }

   // Get a unique name thats not an existing child node.
   var name = AfPromptForUniqueName(
            this.ValidateUniqueRegNameCallback,
            node.parent);

   // User gave up, no renaming anymore.
   if (!name || name == '') {
      return;
   }

   /* Update the path recursively for this node and its children. */
   node.label = name;
   this.updatePathForSubTree(node);
   AfLog('Rename reg key: ' + node.label + ' to: ' + name);
   this.markKeyAsEdited(node.data.registryData);
   this.MarkAsChanged();

   /* Reflect this in the tree */
   this.tree.render();
   return;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.DuplicateKey()
 *
 * Duplicates the currently selected registry key.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
DuplicateKey = function _duplicateKey()
{
   /* Get the selected key to delete */
   var node = this.tree.getHighlightedNode();

   if (!node) {
      AfError('No Selection', 'No key selected.');
      return;
   }
   else if (node === this.rootNode) {
      AfError('Invalid Selection', 'You can\'t duplicate this key!');
      return;
   }
   else if (node.parent === this.rootNode) {
      AfError(
         'Invalid Selection',
         'Keys cannot be added to "' + node.parent.label + '"');
      return;
   }

   // Load the subtree if not already loaded.
   this.loadSubTree(node);

   // Ensure the selected keyName + '-copy-' + counter does not exist.
   var newName = node.label + '-COPY-';
   var counter = 0;

   do {
      counter += 1;
   } while (!this.ValidateUniqueRegNameCallback(
         newName + counter,
         node.parent));

   // Set the new name using counter as: keyName + '-copy-' + counter
   newName += counter;

   var newNode = this.createNodeForKey(newName, node.parent, null, false);

   this.duplicateSubTree(node, newNode);

   /* Add this into the parent */
   if (node.parent != this.rootNode) {
      node.parent.data.registryData.subkeys[newName] = null;
   }
   this.MarkAsChanged();
   /* Reflect this in the tree */
   this.tree.render();

   // Check if user wants to rename the newly created folder
   this.RenameKey(newNode);
   return;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.duplicateSubTree()
 *
 * Copy all the data from source to dest node and repeat this for all the
 * source children.
 *
 * @param source - source node to copy from
 * @param dest - destination node to copy into.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
duplicateSubTree = function _duplicateSubTree(source, dest)
{
   /* Path does not exist for root node. */
   var path = (dest.parent == this.rootNode)?
         "" : dest.parent.data.registryData.path + this.REG_PATH_SEPARATOR;
   path += dest.label;

   /* Deep copy the registry data for the new node. */
   dest.data.registryData = {
      path : path,
      isolation : source.data.registryData.isolation,
      subkeys : jQuery.extend(true, {}, source.data.registryData.subKeys),
      values : jQuery.extend(true, {}, source.data.registryData.values),
      editAction: this.EDIT_CREATE
   };

   /* Set all the subkeys[key] = null as they are new nodes added. */
   for (var k = 0; k < dest.data.registryData.subkeys.length; k++) {
      dest.data.registryData.subkeys[k] == null;
   }

   /* Recursively copy the nodes from source to dest. */
   var srcChildren = source.children;
   if (srcChildren && srcChildren.length > 0) {
      // The destination is not a leaf node, and will start expanded.
      dest.isLeaf = false;
      dest.expanded = true;

      for (var k = 0; k < srcChildren.length; k++) {
         // Create a new child. and feed info into it.
         var newChild = this.createNodeForKey(
               srcChildren[k].label,
               dest,
               null,
               false);

         this.duplicateSubTree(srcChildren[k], newChild);
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.updatePathForSubTree()
 *
 * Recursively updates the path of the registry node. The path set for the node
 * is the parent path + node label.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
updatePathForSubTree = function _updatePathForSubTree(node)
{
   /* Path does not exist for root node. */
   var path = (node.parent == this.rootNode)?
         "" : node.parent.data.registryData.path + this.REG_PATH_SEPARATOR;
   path += node.label;

   node.data.registryData.path = path;

   /* Recursively update the node's path after a rename. */
   if ( node.children && node.children.length > 0 ) {
      for (var k = 0; k < node.children.length; k++) {
         this.updatePathForSubTree(node.children[k]);
      }
   }

};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.loadSubTree()
 *
 * Recursively updates the path of the registry node. The path set for the node
 * is the parent path + node label.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
loadSubTree = function _loadSubTree(node)
{
   // When not a root node and data doesnt exist, Load data for this node.
   if(node != this.rootNode && node.data.registryData == null) {
      this.loadKey(node);
      node.expanded = true;
   }

   // Recursively load the subtree nodes' data if not already loaded.
   for (var k = 0; k < node.children.length; k++) {
      this.loadSubTree(node.children[k]);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.DeleteKey()
 *
 * Deletes the currently selected registry key (and by inference, all its
 * children too).
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
DeleteKey = function _deleteKey()
{
   /* Get the selected key to delete */
   var node = this.tree.getHighlightedNode();

   if (node == null) {
      AfError('No Selection', 'No key selected.');
      return;
   }
   else if (node === this.rootNode) {
      AfError('Invalid Selection', 'You can\'t delete this key!');
      return;
   }
   else if (node.parent === this.rootNode) {
      AfError(
         'Invalid Selection',
         'Top-level registry keys cannot be deleted.');
      return;
   }
   else {
      if (!AfConfirm(
            'Confirm Delete',
            'Are you sure you want to delete the selected key?')) {
         return;
      }
   }

   /*
    * We need to remove the node from the tree. If this is not a newly created node
    * keep a hold of it, so we can delete it from the server later on.
    */
   node.tree.removeNode(node);
   if (node.data.registryUrl) {
      // This is an existing node, hence mark as changed.
      this.deletionQueue.push(node);
      // Replace with DELETE.
      node.data.registryData.editAction = this.EDIT_DELETE;
      this.MarkAsChanged();
   }

   /* Reflect this in the tree */
   this.tree.render();
   return;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.CreateValue()
 *
 * Creates a new value, and adds it to the currently selected registry key.
 * This requires showing a dialog, which 'buildManager' can do quite nicely,
 * so we need one of those. Plus, we need a DIV for creating dialogs.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
CreateValue = function _createValue(dialogHolderId, buildManager)
{
   /* Get the selected key to be the parent */
   var self = this;
   var node = self.tree.getHighlightedNode();

   /* Check the node selection */
   if (node == null) {
      AfError(
         'No Selection',
         'To add a new value, first select a key.');
      return;
   }
   else if (node === self.rootNode) {
      AfError(
         'Invalid Selection',
         'Values cannot be added to "' + node.label + '"');
      return;
   }

   /*
    * Ask buildManager to show the dialog for us, and call us back (with an
    * inline function) when the user presses OK.
    */
   buildManager.ShowNewRegistryValueDialog(
         dialogHolderId,
         function(formData) {
            if (!formData.name) {
               AfError('Invalid Name', 'A value name is required.');
               return false;
            }
            else if (node.data.registryData.values[formData.name]) {
               AfError('Invalid Name', 'A value named "' + formData.name + '"already exists.');
               return false;
            }
            /* Update the registry key, at last */
            self.markKeyAsEdited(node.data.registryData);
            node.data.registryData.values[formData.name] = {
                  type: formData.type,
                  data: '',
                  nameExpand: false,
                  dataExpand: false
            };
            self.MarkAsChanged();
            self.PopulateValues(node);
            return true;
         });
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.DeleteValue
 *
 * Delete the named value from the specified registry key.
 * This will then cause the editor display to update by redrawing all the
 * values for the currently selected node.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
DeleteValue = function _deleteValue(valueName)
{
   /* Get the selected key (whose value is to be deleted) */
   var node = this.tree.getHighlightedNode();
   if (node == null) {
      AfError(
         'No Selection',
         'To delete a value, first select a key.');
      return;
   }

   /* Remove the value, mark the key as being edited */
   var regData = node.data.registryData;
   delete regData.values[valueName];
   this.markKeyAsEdited(regData);
   this.MarkAsChanged();

   /* Refresh the display */
   this.PopulateValues(node);
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.ProcessChanges
 *
 * Walk this registry editor and look for edits that have been made since this
 * function was last called. If new keys are found, the 'createFunction' is
 * called. If edited keys are found, the 'editFunction' is called.
 *
 * @param createFunction Callback invoked for new keys.
 * @param editFunction Callback invoked for edited keys.
 * @param deleteFunction Callback invoked for deleted keys.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
ProcessChanges = function _processChanges(
      createFunction,
      editFunction,
      deleteFunction)
{
   AfLog('---- REGISTRY TREE EDITS: -----');
   this.processNode(createFunction, editFunction, this.rootNode, 0);

   AfLog('---- DELETION QUEUE: -----');
   for (var i = 0; i < this.deletionQueue.length; i++) {
      var node = this.deletionQueue[i];
      var regData = node.data.registryData;

      AfLog('Deleting key ' + node.label);
      deleteFunction(
            this.buildId,
            regData.id,
            regData);
   }
   AfLog('-------------------------------');

   // XXX: return success (true or false)
   return true;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.processNode()
 *
 * Process the given node, find out what needs to be done, and do it.
 * This includes creating keys, editing values, etc. This is a recursive
 * function: it will process this node first, and then all its children.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
processNode = function _processNode(
      createFunction,
      editFunction,
      node,
      depth)
{
   var regData = node.data.registryData;
   var debugPreamble = (regData ? '.../' + regData.path : node.label);

   for (var i = 0; i < depth; i++) {
      debugPreamble = '  ' + debugPreamble;
   }

   if (!regData) {
      /*
       * No data for this node. This means nothing was ever loaded for it,
       * so therefore nothing can have been changed. Skip it.
       */
      AfLog(debugPreamble + ' -> no data');
   }
   else if (regData.editAction) {
      /* Remember the action, and delete it (else it messes up the JSON) */
      var action = regData.editAction;
      delete regData.editAction;
      AfLog(debugPreamble + ' -> ' + action);

      if (action === this.EDIT_CREATE) {
         /*
          * This registry node was added interactively by us.
          * Therefore, create a new registry key on back-end.
          */
         var parentNode = node.parent;
         var newUrl = createFunction(
               this.buildId,
               parentNode.data.registryData.id,
               regData);
         node.data.registryUrl = newUrl;
         // This is needed, in case the user selects this node for delete again.
         node.data.registryData.id = this.parseRegistryIdFromUrl(newUrl);
      }
      else if (action === this.EDIT_CHANGE) {
         /*
          * One or more values attached to this subkey node have been edited
          * (values changed), so we must update the registry key on the back-end.
          */
         editFunction(
               this.buildId,
               regData.id,
               regData);
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
    * This node is complete. Now process each child node, which
    * correspond to sub-keys of this key.
    */
   for (var i = 0; i < node.children.length; i++) {
      this.processNode(createFunction, editFunction, node.children[i], depth + 1);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.loadKey
 *
 * Loads all the child registry nodes for a given registry node. This allows
 * the registry key tree to be loaded on demand, as the user expands nodes.
 *
 * NOTE: This function prototype is defined by the YUI API.
 *
 * @param node The node to be expanded
 * @param loadCompleteCallback YUI callback we must invoke to tell the tree
 *                             to redraw
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
loadKey = function _loadKey(node, loadCompleteCallback)
{
   // Get the RegistryEditor instance
   var self = node.data.self;
   var tree = node.tree;

   AfLog('Dynamic data load for ' + node.label + ' from ' + node.data.registryUrl);

   if (node === self.rootNode) {
      /* Don't load anything for the root node */
      AfLog('root node --- skipped');
   }
   else if (!node.data.registryUrl) {
      /*
       * Could be a new key that we have created, but not yet saved.
       * In this case, there is no resource URL for it. So skip it.
       */
      AfLog('no registryUrl (new node?) --- skipped');
   }
   else if (!node.data.registryData) {
      /*
       * Synchronously fetch the registry data for this node.
       * NOTE: the registryUrl value is a CWS resource URL, so we can't
       * load it directly (cross-site scripting), so we pass it to the
       * webui, which will act as a proxy for us.
       */

      /* Get the registry ID from its CWS resource */
      var regId = self.parseRegistryIdFromUrl(node.data.registryUrl);

      AfAjax({
         method: 'GET',
         url: '/api/builds/' + self.buildId + '/registry/' + regId,
         async: false,

         success: function(registryData) {
            AfLog('Dynamic data load success!');
            node.data.registryData = registryData;

            /* Create child nodes */
            for (var label in registryData.subkeys) {
               var regKeyData = registryData.subkeys[label];
               self.createNodeForKey(
                     label,
                     node,
                     regKeyData.url,
                     regKeyData.hasChildren);
            }

            /* If selected, populate the values too */
            if (node === node.tree.currentFocus) {
               AfLog('this is the current node; setting values');
               self.PopulateValues(node);
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
 * RegistryEditor.createNodeForKey()
 *
 * Create a new tree node for a registry key. This utility function ensures
 * that all nodes are created consistently.
 *
 * @param label Label for node (key name)
 * @param parentNode
 * @param url The CWS resource URL (Note: might be on a foreign server)
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
createNodeForKey = function _createNodeForKey(
      label,
      parentNode,
      url,
      hasChildren)
{
   /* YUI isLeaf is buggy. YUI removes dynamic load, which gives way to
    * the anchor's href="#", causing the build page refresh in our case.
    * Hence we explicitly remove the default event from happening.
    */
   var node = new YAHOO.widget.TextNode(
         {
            label: label,
            isLeaf: !hasChildren
         },
         parentNode);
   node.data.self = this;
   node.data.registryUrl = url;
   node.data.registryData = undefined; // i.e. not yet loaded

   return node;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.PopulateValues
 *
 * Fill in the registry value editor form by creating input fields for each
 * registry value in the given node.
 *
 * @param node
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
PopulateValues = function _PopulateValues(node)
{
   var self = this;
   var legend = this.valuesDiv.find('legend');
   var insertPoint = $('#registry-data-holder');

   /* Delete existing values */
   insertPoint.empty();
   this.valuesDiv.find('#registry-values-header').text('');

   if (!node) {
      AfLog('Removed all registry values');
      return;
   }

   var regData = node.data.registryData;
   /*
    * If the data is not yet loaded, force it to be loaded now.
    * This happens if the user selects a node before expanding it.
    */
   if (regData === undefined) {
      this.loadKey(node, function() {
         self.PopulateValues(node);
      });
      return;
   }
   else {
      AfLog('Populating registry values for ' + node.label);
      this.valuesDiv.find('#registry-values-header').text(regData.path);
   }


   /*
    * If there are no values, there is nothing else to do with this node.
    */
   if (regData.values === null) {
      /* There are no values for this registry key. */
      return;
   }

   /*
    * Create an input for choosing the isolation mode for this
    * registry key.
    */
   var groupDiv = self.createStackEditorGroup(node.label);
   var isoModeField = this.createIsolationModeField(regData);
   groupDiv.append(isoModeField);
   insertPoint.append(groupDiv);

   /*
    * There are value for this node.
    * Create form fields for each one.
    */
   for (var valueName in regData.values) {
      var value = regData.values[valueName];
      var showExpandChecks = false;
      var groupDiv = self.createStackEditorGroup(valueName);
      var fieldClasses = 'text long';

      var input = $('<input>');
      input.attr('name', valueName);

      /* When input value changes, update the registry data */
      input.change(function() {
         var valueName = $(this).attr('name');
         var val = $(this).val();
         self.setValueData(regData.values[valueName], val);
         self.markKeyAsEdited(regData);
         self.MarkAsChanged();
      });

      input.attr('onblur', 'this.value=jQuery.trim(this.value)');

      /* Create input field according to registry data type */
      if (value.type == "REG_DWORD" ||
            value.type == "REG_QWORD" ||
            value.type == "REG_DWORD_LITTLE_ENDIAN") {
         input.attr('size', 10);
         input.attr('type', 'text');
         input.attr('value', value.data);
         fieldClasses = 'text short';
      }
      else if (value.type == "REG_SZ" ||
               value.type == "REG_EXPAND_SZ") {
         input.attr('size', 40);
         input.attr('type', 'text');
         input.attr('value', value.data);
         showExpandChecks = true;
      }
      else if (value.type == "REG_MULTI_SZ") {
         // FIXME: text area?
         input.attr('size', 40);
         input.attr('type', 'text');
         input.attr('value', value.data);
         showExpandChecks = true;
      }
      else if (value.type == "REG_BINARY") {
         input.attr('size', 40);
         input.attr('type', 'text');
         input.attr('value', value.data);
      }
      else {
         /* Unsupported type! */
         input = null;
         AfLog('Unknown registry data type ' + value.type + ': skipped!');
         continue;
      }

      /* A known type: create a field for it. */
      fieldDiv = self.createStackFieldDiv(input);
      fieldDiv.addClass(fieldClasses);

      /* Delete button: click this to delete this entry */
      var delIcon = $('<span class="delete-icon"></span>');
      delIcon.click(valueName, function(event) {
         self.DeleteValue(event.data);
      });
      fieldDiv.append(delIcon);

      /* Add the row for the Name and Input */
      var row = $('<div></div>').addClass('stack-row');
      row.append(self.createStackLabelDiv(valueName, valueName));
      row.append(fieldDiv);
      groupDiv.append(row);

      /* Add the row for the Data Type */
      var typeInput = $('<input type="text" readonly>');
      typeInput.val(value.type);

      var row = $('<div></div>').addClass('stack-row');
      row.append(self.createStackLabelDiv('Type'));
      row.append(self.createStackFieldDiv(typeInput));
      groupDiv.append(row);


      if (showExpandChecks) {
         var expandCheck;
         var row;

         /* Name requires expansion? */
         expandCheck = $('<input>');
         expandCheck.attr({
            name: valueName,
            type: 'checkbox',
            checked: regData.values[valueName].nameExpand,
            disabled: 'disabled'});

         expandCheck.change(function() {
            var name = $(this).attr('name');
            var checked = $(this).attr('checked');
            regData.values[name].nameExpand = checked;
            regData.editAction = self.EDIT_CHANGE;
            self.MarkAsChanged();
         });

         row = $('<div></div>').addClass('stack-row');
         row.append(self.createStackLabelDiv('Name requires expansion'));
         row.append(self.createStackFieldDiv(expandCheck));
         groupDiv.append(row);


         /* Data requires expansion? */
         expandCheck = $('<input>');
         expandCheck.attr({
            name: valueName,
            type: 'checkbox',
            checked: regData.values[valueName].dataExpand,
            disabled: 'disabled'});

         expandCheck.change(function() {
            var name = $(this).attr('name');
            var checked = $(this).attr('checked');
            regData.values[name].dataExpand = checked;
            regData.editAction = self.EDIT_CHANGE;
            self.MarkAsChanged();
         });

         row = $('<div></div>').addClass('stack-row');
         row.append(self.createStackLabelDiv('Data requires expansion'));
         row.append(self.createStackFieldDiv(expandCheck));
         groupDiv.append(row);
      }

      insertPoint.append(groupDiv);
   }
};


RegistryEditor.prototype.
setValueData = function _setValueData(registryValue, data)
{
   var jsType = typeof(data);

   switch (registryValue.type) {
      case "REG_DWORD":
      case "REG_QWORD":
      case "REG_DWORD_LITTLE_ENDIAN":
         /* Keep numbers as-is, but parse strings */
         if (jsType == "number") {
            registryValue.data = data;
         }
         else if (jsType == "string") {
            registryValue.data = parseInt(data);
         }
         else {
            // Don't know what to do!
            AfError('Invalid Data Type', 'Invalid data type ' + jsType);
            registryValue.data = null;
         }
         break;

      case "REG_SZ":
      case "REG_EXPAND_SZ":
         /* Forced type conversion to string */
         registryValue.data = '' + data;
         break;

      case "REG_MULTI_SZ":
         // TODO
         registryValue.data = '' + data;
         break;

      case "REG_BINARY":
         // TODO
         registryValue.data = '' + data;
         break;
   }
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.findNodeForData()
 *
 * XXX Make this as a common function across package ini, registry, filesystem.
 * Return the node in the tree which has the specified registry data
 * instance attached to it. This should never return null, or more than one
 * match.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
findNodeForData = function _findNodeForData(registryData)
{
   var match = null;

   this.tree.getNodesBy(function _test(node) {
      if (node.data.registryData === registryData) {
         match = node;
      }
   });
   return match;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.ValidateUniqueRegNameCallback
 *
 * This method validates if the passed value is already in use.
 *
 * @see afutils.AfPromptForUniqueName()
 * @param callbackParam - parent node, whose children are matched for
 *        uniqueness.
 * @return true - if name not used, false otherwise.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
ValidateUniqueRegNameCallback = function _validateUniqueRegNameCallback (
      newName,
      callbackParam)
{
   if(!callbackParam) {
      return false;
   }
   var childNodes = callbackParam.children;
   // Get the parentNode and validate if it has children.
   for (var i = 0; i < childNodes.length; i++) {
      if (childNodes[i].label == newName) {
         return false;
      }
   }
   return true;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.parseRegistryIdFromUrl()
 *
 * Extract a registry ID from a CWS registry resource URL.
 * Since we can't access CWS directly (cross-host scripting is bad), we need
 * to pull the ID from a CWS URL and use that to pass back to our own server.
 *
 * The assumption is that the CWS resource URL ends with "..../{id}"
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
parseRegistryIdFromUrl = function _parseRegistryIdFromUrl(url)
{
   return url.split(this.REG_PATH_SEPARATOR).pop();
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.createIsolationModeField()
 *
 * Creates a "field" class div for displaying a label and pulldown selector
 * for isolation mode values.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
createIsolationModeField = function(regData)
{
   var self = this;

   /* Make a pulldown selector */
   var select = $('<select>');
   select.data('regData', regData);

   /* Add each value option */
   for (var isoMode in this.ISOLATION_MODES) {
      var displayName = this.ISOLATION_MODES[isoMode];
      var opt = $('<option>').val(isoMode);
      opt.text(displayName);
      select.append(opt);
   }

   select.val(regData.isolation);

   /* When the selection changes, update the registry data */
   select.change(function() {
      var regData = $(this).data('regData');
      regData.isolation = $(this).val();
      regData.editAction = RegistryEditor.prototype.EDIT_CHANGE;
      self.MarkAsChanged();
   });

   /* Add label and select into a div.stack-row */
   var label = $('<label>').text("Isolation Mode");
   var labelDiv = $('<div></div>').addClass('label');
   labelDiv.append(label);

   var div = $('<div class="field select">');
   div.append(select);

   var row = $('<div></div>').addClass('stack-row');
   row.append(labelDiv).append(div);
   return row;
};


/**
 * -----------------------------------------------------------------------------
 * RegistryEditor.createStackEditorGroup
 *
 * Create the div used for a collapsible stack editor group.
 * -----------------------------------------------------------------------------
 */
RegistryEditor.prototype.
createStackEditorGroup = function(groupName)
{
   var iconDiv = $('<div></div>');
   iconDiv.addClass('icon');

   var label = $('<label>');
   label.text(groupName);

   var labelDiv = $('<div></div>');
   labelDiv.addClass('label');
   labelDiv.append(iconDiv);
   labelDiv.append(label);

   var groupHeadDiv = $('<div></div>');
   groupHeadDiv.addClass('stack-row-head');
   groupHeadDiv.append(labelDiv);

   var groupDiv = $('<div></div>');
   groupDiv.addClass('stack-group  ui-widget-content collapsible');
   groupDiv.append(groupHeadDiv);

   return groupDiv;
};


RegistryEditor.prototype.
createStackLabelDiv = function(text, forId)
{
   var label = $('<label>');
   label.text(text);

   if (forId) {
      label.attr('for', forId);
   }

   var labelDiv = $('<div></div>').addClass('label');
   labelDiv.append(label);
   return labelDiv;
};


RegistryEditor.prototype.
createStackFieldDiv = function(input)
{
   /* Field DIV */
   var fieldDiv = $('<div></div>');
   fieldDiv.addClass('field');
   fieldDiv.append(input);
   return fieldDiv;
};


RegistryEditor.prototype.
markKeyAsEdited = function(registryData)
{
   if (registryData.editAction === this.EDIT_CREATE) {
      AfLog("key " + registryData.path + " is CREATE, not set to EDIT");
   }
   else {
      AfLog("key " + registryData.path + " set to EDIT");
      registryData.editAction = this.EDIT_CHANGE;
   }
};