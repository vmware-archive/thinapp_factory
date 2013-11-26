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
 * RecipeVarEditor
 *
 * Editor for recipe variables.
 * TODO: Add new variable
 * TODO: Delete variable
 * TODO: Rename group when var name edited
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype = new AbstractSettingsEditor();
RecipeVarEditor.prototype.constructor = RecipeVarEditor;
function RecipeVarEditor(tab, changeMarker)
{
   this.changeMarker = changeMarker;

   this.stackEditor = new StackEditor(
         tab.find("#recipe-vars-stack"),
         "User Variables");
}


/**
 * -----------------------------------------------------------------------------
 * RecipeVarEditor.Serialize
 *
 * Serialize the editor into JSON data suitable for sending to the server
 * for creating or editing a recipe.
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype.
Serialize = function _serialize()
{
   var json = this.stackEditor.Serialize();
   return json;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeVarEditor.PopulateVariables
 *
 * Fill the list of variable names with the names of all variables from the
 * given recipe. By default, the first one is selected and it's details are
 * show, unless 'defaultSelection' is specified instead.
 *
 * @param recipe The recipe to display.
 * @param readOnly
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype.
PopulateVariables = function _populateVariables(recipe, readOnly)
{
   var self = this;

   /* Remove whatever we had before */
   self.stackEditor.Clear();
   self.readOnly = readOnly;

   if (recipe) {
      for (var vi = 0; vi < recipe.variables.length; vi++) {
         var variable = recipe.variables[vi];
         self.AddVariableGroup(variable);
      }
   }

   if (!self.readOnly) {
      self.stackEditor.AddFooter([{
         label: "Add Variable",
         validate: false,
         clickFunc: function() {
            var name = self.generateNewName();
            self.AddVariableGroup({ name: name });
            self.MarkAsChanged();
         }
      }]);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeVarEditor.AddVariableGroup
 *
 * Add a stack editor group for a variable. If 'variable' is specified,
 * values default to that variable, else values default to empty.
 *
 * @param variable
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype.
AddVariableGroup = function _addVariableGroup(variable)
{
   var self = this;

   // TODO: rename this when the user edits the name
   var group = self.stackEditor.AddGroup({
      title: variable.name,
      collapsible: false,
      name: 'variables',
      serializeAsArray: true,
      readOnly: self.readOnly,
      deleteFunc: self.readOnly ? null : function() {
         var delOk = self.ConfirmDelete();
         if (delOk) {
            self.MarkAsChanged();
         }
         return delOk;
      }
   });

   /* Variable name */
   group.AddRow({
      name: 'name',
      label: 'Name',
      value: variable ? variable.name : undefined,
      type: StackEditor.TEXT_SHORT,
      validation: {
         variableName: true
      },
      required: true,
      changeData: group,
      changeFunc: function(newValue, group) {
         self.MarkAsChanged();
         group.SetTitle(newValue);
      }
   });

   /* Variable regex pattern */
   group.AddRow({
      name: 'pattern',
      label: 'Pattern',
      value: variable ? variable.pattern : undefined,
      type: StackEditor.TEXT_LONG,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Variable is required? */
   group.AddRow({
      name: 'required',
      label: 'Required',
      value: variable ? variable.required : undefined,
      type: StackEditor.BOOLEAN,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeVarEditor.ConfirmDelete
 *
 * Confirm deletion of a variable.
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype.
ConfirmDelete = function _confirmDelete()
{
   return AfConfirm(
         "Confirm Delete",
         "Are you sure you want to delete this variable?");
};


/**
 * -----------------------------------------------------------------------------
 * RecipeVarEditor.generateNewName
 *
 * Generate a default name for a new variable.
 * Existing names are searched, and a new variable name based on the pattern
 * "NewVariable #1" is generated.
 * -----------------------------------------------------------------------------
 */
RecipeVarEditor.prototype.
generateNewName = function()
{
   var baseName = "NewVariable";
   var count = 1;
   var newName = baseName;
   var isUnique = true;

   do {
      isUnique = true;
      this.stackEditor.editorElement.find("input[name=name]").each(function() {
         if ($(this).val() == newName) {
            newName = baseName + (count++);
            isUnique = false;
         }
      });
   } while (!isUnique);

   return newName;
};
