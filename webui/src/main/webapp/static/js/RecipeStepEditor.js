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
 * RecipeStepEditor
 *
 * Editor for a recipe recipe.
 *
 * TODO: Reorder commands within a phase
 * TODO: Edit the label for a command
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype = new AbstractSettingsEditor();
RecipeStepEditor.prototype.constructor = RecipeStepEditor;

function RecipeStepEditor(tab, changeMarker)
{
   this.changeMarker = changeMarker;

   /* Stack editor for the sections (left) */
   this.stackEditor = new StackEditor(
      tab.find("#recipe-steps-stack"),
      "Steps");

   this.phaseGroups = {};
}


/**
 * -----------------------------------------------------------------------------
 * RecipeStepEditor.Serialize
 *
 * Serialize editor contents into JSON data. Since the steps editor is more than
 * just a vanilla stack editor, we have to customize the way the form is
 * converted into JSON.
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype.
Serialize = function _serialize()
{
   var json = {};

   /*
    * Since commands can have the same label, we have to serialize this
    * stack editor in a non-standard way.
    */
   this.stackEditor.editorElement.find('.stack-group').each(function() {
      var groupElement = $(this);
      var group = groupElement.data('group');

      json[group.name] = {
            commands: []
      };

      group.groupElement.find("input").each(function() {
         json[group.name].commands.push({
            label: $(this).attr('name'),
            command: $(this).val()
         });
      });
   });

   return { steps: json };
};


/**
 * -----------------------------------------------------------------------------
 * RecipeStepEditor.PopulateRecipe
 *
 * Populate the recipe editor with the recipe from the given recipe.
 *
 * @param recipe
 * @param readOnly
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype.
PopulateRecipe = function _populateRecipe(recipe, readOnly)
{
   var self = this;

   /* Remove whatever we had before */
   self.stackEditor.Clear();
   self.readOnly = readOnly;

   /* Add a group for every known phase */
   for (var pi = 0; pi < VmTAF.recipePhases.length; pi++) {
      var phase = VmTAF.recipePhases[pi];

      /* Create stack editor group */
      var groupLabel = AfTranslate("T.RECIPES.PHASE." + phase);
      self.phaseGroups[phase] = self.stackEditor.AddGroup({
         title: groupLabel,
         collapsible: true,
         readOnly: self.readOnly,
         name: phase
      });

      /* Add any commands we might have for this phase */
      if (recipe) {
         var step = recipe.steps[phase];
         if (step) {
            for (var ci = 0; ci < step.commands.length; ci++) {
               var cmd = step.commands[ci];
               self.AddRowForCommand(self.phaseGroups[phase], cmd);
            }
         }
      }

      /* Add buttons to this phase */
      if (!self.readOnly) {
         self.phaseGroups[phase].AddRow({
            label: " ",
            type: StackEditor.BUTTONS,
            buttons: [{
               label: 'Add',
               clickData: self.phaseGroups[phase],
               clickFunc: function(clickData) {
                  var group = clickData;
                  if (self.AddNewCommand(group)) {
                     self.MarkAsChanged();
                  }
               }
            }]
         });
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeStepEditor.ConfirmDeleteCommand
 *
 * Confirm deletion of a command from the recipe. This is triggered from the
 * stack editor trash can icon.
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype.
ConfirmDeleteCommand = function _confirmDeleteCommand()
{
   /* Confirm delete */
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete this command?')) {
      return false;
   }

   /* Return true to have the editor row deleted automatically */
   return true;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeStepEditor.AddNewCommand
 *
 * Ask the user for the name of a new command, and add it to the recipe.
 * If a change was made, return true, else return false.
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype.
AddNewCommand = function _addNewCommand(group)
{
   var self = this;

   var newName = AfPrompt(
      'Enter Label',
      'Enter a label for the command');

   newName = jQuery.trim(newName);
   if (!newName) {
      return false;
   }

   /* Add command to the stack editor */
   var cmd = {
      label: newName,
      command: ''
   };
   var where = group.NumRows() - 1;
   self.AddRowForCommand(group, cmd, where);

   return true;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeStepEditor.AddRowForCommand
 *
 * Add a row to the stack editor that will correspond to a command in the
 * given recipe.
 *
 * @param command
 * -----------------------------------------------------------------------------
 */
RecipeStepEditor.prototype.
AddRowForCommand = function _addRowForCommand(group, command, where)
{
   var self = this;

   group.AddRow({
      type: StackEditor.TEXT_LONG,
      name: command.label,
      label: command.label,
      value: command.command,
      index: where,

      /* When the command line changes: */
      changeFunc: function(newValue) {
         self.MarkAsChanged();
      },

      /* When the command line is moved: */
      moveFunc: self.readOnly ? null : function() {
         self.MarkAsChanged();
         return true;
      },

      /* When the command line is deleted: */
      deleteFunc: self.readOnly ? null : function() {
         if (self.ConfirmDeleteCommand()) {
            self.MarkAsChanged();
            return true;
         }
         return false;
      }
   });
};
