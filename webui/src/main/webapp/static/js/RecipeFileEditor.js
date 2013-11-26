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
 * RecipeFileEditor
 *
 * Editor for recipe files.
 *
 * TODO: Similar to RecipeVarEditor; share code one day
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype = new AbstractSettingsEditor();
RecipeFileEditor.prototype.constructor = RecipeFileEditor;
function RecipeFileEditor(tab, changeMarker)
{
   this.changeMarker = changeMarker;
   this.nextUploadFormId = 1000;

   /* Stack editor for the sections (left) */
   this.stackEditor = new StackEditor(
         tab.find("#recipe-files-stack"),
         "Files");
}


/**
 * -----------------------------------------------------------------------------
 * RecipeFileEditor.Serialize
 *
 * Serialize the editor into JSON data suitable for sending to the server
 * for creating or editing a recipe.
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype.
Serialize = function _serialize()
{
   var json = this.stackEditor.Serialize();
   return json;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeFileEditor.PopulateFiles
 *
 * Fill the stack editor with groups, where each group corresponds to a "file"
 * from the recipe. Note that a recipe "file" can either be an actual file
 * that is included with the recipe, or a reference to an application that the
 * user needs to provide.
 *
 * @param recipe The recipe to display.
 * @param readOnly
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype.
PopulateFiles = function _populateFiles(recipe, readOnly)
{
   var self = this;

   /* Remove whatever we had before */
   self.stackEditor.Clear();
   self.readOnly = readOnly;

   if (recipe) {
      for (var fi = 0; fi < recipe.files.length; fi++) {
         var file = recipe.files[fi];
         self.AddFileGroup(file);
      }
   }

   if (!self.readOnly) {
      self.stackEditor.AddFooter([{
         label: "Add Remote File (URL)",
         validate: false,
         readOnly: readOnly,
         clickFunc: function() {
            self.AddFileGroup({
               isNew: true
            });
            self.MarkAsChanged();
         }
      /* Disable until after M9!
       * Also see XXX comments in RecipeManager.startFileUploads
      },{
         label: "Add Local File (Upload)",
         validate: false,
         readOnly: readOnly,
         clickFunc: function() {
            self.AddUploadFileGroup({});
            self.MarkAsChanged();
         }
      */
      }]);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeFileEditor.AddFileGroup
 *
 * Add a stack editor group for a file. If 'file' is specified, values default
 * to that file, else values default to empty.
 *
 * @param file
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype.
AddFileGroup = function _addFileGroup(file)
{
   var self = this;

   var group = self.stackEditor.AddGroup({
      title: "File",
      collapsible: false,
      name: 'files',
      serializeAsArray: true,
      deleteFunc: self.readOnly ? null : function() {
         var delOk = self.ConfirmDelete();
         if (delOk) {
            self.MarkAsChanged();
         }
         return delOk;
      }
   });

   group.AddRow({
      name: 'name',
      label: 'Name',
      readOnly: self.readOnly,
      value: file ? file.name : undefined,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   group.AddRow({
      type: StackEditor.TEXT_LONG,
      name: 'description',
      label: 'Description',
      readOnly: self.readOnly,
      value: file ? file.description : undefined,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   if (file.path) {
      /*
       * This file was originally relative to the recipe location, so
       * let's show that. We can use a tooltip to show where the file
       * is actually stored.
       */
      group.AddRow({
         type: StackEditor.TEXT_LONG,
         name: 'path',
         label: 'Path',
         readOnly: !file.isNew, // only new files can be edited
         value: file.path,
         tooltip: file.uri
      });
   }

   group.AddRow({
      type: StackEditor.TEXT_LONG,
      label: 'URI',
      readOnly: self.readOnly,
      validation: {
         required : true,
         uri : true
      },
      name: 'uri',
      value: file.uri
   });

   group.AddRow({
      type: StackEditor.HIDDEN,
      name: 'isNew',
      value: file.isNew
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeFileEditor.AddUploadFileGroup
 *
 * XXX
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype.
AddUploadFileGroup = function _addUploadFileGroup(file)
{
   var self = this;

   var group = self.stackEditor.AddGroup({
      title: "File",
      collapsible: false,
      name: 'files',
      serializeAsArray: true,
      deleteFunc: self.readOnly ? null : function() {
         var delOk = self.ConfirmDelete();
         if (delOk) {
            self.MarkAsChanged();
         }
         return delOk;
      }
   });

   group.AddRow({
      name: 'name',
      label: 'Name',
      readOnly: self.readOnly,
      value: file ? file.name : undefined,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   group.AddRow({
      type: StackEditor.TEXT_LONG,
      name: 'description',
      label: 'Description',
      readOnly: self.readOnly,
      value: file ? file.description : undefined,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   var uploadFormId = self.nextUploadFormId++;

   group.AddRow({
      id: "file-chooser-" + uploadFormId,
      type: StackEditor.FILE_CHOOSER,
      name: 'recipeFile',
      label: 'File',
      // jQuery validator plug-in doesn't work well with file input fields
      // required: true,
      formId: uploadFormId
   });

   group.AddRow({
      type: StackEditor.HIDDEN,
      name: 'isNew',
      value: true
   });

   group.AddRow({
      type: StackEditor.HIDDEN,
      name: 'uploadFormId',
      value: uploadFormId
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeFileEditor.ConfirmDelete
 *
 * Confirm deletion of a file.
 * -----------------------------------------------------------------------------
 */
RecipeFileEditor.prototype.
ConfirmDelete = function _confirmDelete()
{
   return AfConfirm(
         "Confirm Delete",
         "Are you sure you want to delete this file?");
};
