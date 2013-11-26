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
 * function InventoryManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with the inventory.
 * -----------------------------------------------------------------------------
 */
function InventoryManager()
{
   trees = new Array();
}

InventoryManager.prototype = new AbstractManager('Inventory');


/**
 * -----------------------------------------------------------------------------
 * InventoryManager.PopulateInventoryTree
 *
 * Public function to load data from the server into the inventory div.
 *
 * @param treeDiv HTML "div" element that will display the inventory.
 * -----------------------------------------------------------------------------
 */
InventoryManager.prototype.
PopulateInventoryTree = function _populateInventoryTree(
      treeDiv,
      refreshInterval)
{
   var self = this;

   AfAjax({
      url: '/api/inventory',
      method: 'GET',
      success: function _populateInvTreeSuccess(data) {
         // Create inventory tree...
         self.createTree(treeDiv, data);

         // ...then schedule the next update.
         if (refreshInterval > 0) {
            window.setTimeout(
                  function(invMgr) {
                     self.PopulateInventoryTree(treeDiv, refreshInterval);
                  },
                  refreshInterval,
                  self);
         }
      },
      error: function _populateInvTreeError(jqXHR, textStatus, errorThrown) {
         // Failed to connect: try again in 5 seconds
         AfLog("Inventory update failed! Retrying in 5 seconds");
         window.setTimeout(
               function(invMgr) {
                  self.PopulateInventoryTree(treeDiv, refreshInterval);
               },
               5000,
               self);
         }
   });
};


/**
 * -----------------------------------------------------------------------------
 * InventoryManager.createTree
 *
 * Create the inventory tree from JSON data, and insert it into the
 * specified DIV element.
 *
 * @param treeDiv
 * @param jsonData
 * @returns
 * -----------------------------------------------------------------------------
 */
InventoryManager.prototype.
createTree = function(treeDiv, jsonData)
{
   var inventory = jsonData;
   var numTrees = inventory.children.length;
   var newTrees = new Array();

   // Remove the loading image (and any previous trees).
   treeDiv.empty();

   // Each top-level child creates a tree.
   for (var i = 0; i < numTrees; i++) {
      var invItem = inventory.children[i];

      // Make a new DIV for the tree.
      treeDiv.append('<div id="' + invItem.name + '"></div>');

      // Make the tree from the new div.
      var tree = new YAHOO.widget.TreeView(invItem.name);
      var node = this.addTreeNode(tree.getRoot(), invItem);

      // Listen for clicks on the labels.
      tree.subscribe("clickEvent", this.navigateTo);
      tree.render();

      // New trees are put into a new array so we can still search
      // for old nodes while creating the new ones
      newTrees.push(tree);
   }

   // Delete all existing trees, and set to new array
   if (this.trees) {
      for (var i = 0; i < this.trees.length; i++) {
         this.trees[i].destroy();
         this.trees[i] = null;
      }
      this.trees = null;
   }
   this.trees = newTrees;
};


/**
 * -----------------------------------------------------------------------------
 * InventoryManager.addTreeNode
 *
 * Add a node to a tree. This is a recursive function, adding children
 * as needed.
 *
 * @param parentNode
 * @param invItem
 * @param oldTree
 *
 * @returns
 * -----------------------------------------------------------------------------
 */
InventoryManager.prototype.
addTreeNode = function(parentNode, invItem, oldTree)
{
   var expanded = false;
   var highlight = 0;

   // Create node label.
   var label = invItem.name;
   if (invItem.count && invItem.count > 0) {
      label += '<span class="inventory-count">' + invItem.count + '</span>';
   }

   // Create the unique path ("Sources->Feeds->Ninite", for example).
   var pathLabel = label;
   if (parentNode.data.pathLabel) {
      pathLabel = parentNode.data.pathLabel + '->' + pathLabel;
   }

   // Look for existing node with the same path.
   var oldNode = null;
   if (this.trees) {
      for (var t = 0; t < this.trees.length && oldNode == null; t++) {
         oldNode = this.trees[t].getNodeByProperty('pathLabel', pathLabel);
      }
   }

   // If there was an old node like this one, copy attributes from it.
   if (oldNode) {
      expanded = oldNode.expanded;
      highlight = oldNode.highlightState;
   }

   // Create this node.
   var newNode = new YAHOO.widget.TextNode( {
         label: label,
         view: invItem.view,
         title: invItem.name,
         highlightState: highlight,
         expanded: expanded,
         pathLabel: pathLabel
      },
      parentNode);

   // And all its ancestors.
   for (var i = 0; i < invItem.children.length; i++) {
      this.addTreeNode(newNode, invItem.children[i], oldTree);
   }

   return newNode;
};


/**
 * -----------------------------------------------------------------------------
 * InventoryManager.navigateTo
 *
 * When the user clicks an inventory tree node, this callback is invoked
 * with the node that was selected. We set the main panel title and
 * content to match the view associated with the node.
 *
 * @param where
 * @returns true, to continue with default edit handling (collapse/expand).
 * -----------------------------------------------------------------------------
 */
InventoryManager.prototype.
navigateTo = function(where)
{
   var node = where.node;
   var view = node.data.view;

   if (view) {
      VmTAF.contentNavigator.LoadPage(view);
   }

   return true;
};
