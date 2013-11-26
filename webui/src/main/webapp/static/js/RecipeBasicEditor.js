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
 * RecipeBasicEditor
 *
 * Editor for recipe files.
 * -----------------------------------------------------------------------------
 */
RecipeBasicEditor.prototype = new AbstractSettingsEditor();
RecipeBasicEditor.prototype.constructor = RecipeBasicEditor;
function RecipeBasicEditor(tab, changeMarker)
{
   this.changeMarker = changeMarker;

   this.stackEditor = new StackEditor(
      tab.find("#recipe-basic-stack"),
      "General");
}


/**
 * -----------------------------------------------------------------------------
 * RecipeBasicEditor.Serialize
 *
 * Serialize the editor into JSON data suitable for sending to the server
 * for creating or editing a recipe.
 * -----------------------------------------------------------------------------
 */
RecipeBasicEditor.prototype.
Serialize = function _serialize()
{
   var json = this.stackEditor.Serialize();

   /* Description needs to be like AfText */
   json.description = {
      contentType: 'text/plain',
      content: json.description
   };

   return json;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeBasicEditor.PopulateRecipe
 *
 * Fill the stack editor with the basic recipe data: name, description, etc,
 * plus the list of applications that it applies to.
 *
 * @param recipe The recipe to display.
 * -----------------------------------------------------------------------------
 */
RecipeBasicEditor.prototype.
PopulateRecipe = function _populateRecipe(recipe, readOnly)
{
   var self = this;

   /* Remove whatever we had before */
   self.stackEditor.Clear();
   self.readOnly = readOnly;

   var group = self.stackEditor.AddGroup({
      title: "Basic",
      readOnly : self.readOnly,
      collapsible: false
   });

   /* Recipe ID */
   group.AddRow({
      type: StackEditor.HIDDEN,
      label: 'ID',
      name: 'id',
      value: recipe ? recipe.id : null
   });

   /* Recipe name */
   group.AddRow({
      label: 'Name',
      name: 'name',
      required: true,
      value: recipe ? recipe.name : null,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Recipe description */
   group.AddRow({
      type: StackEditor.TEXT_LONG,
      label: 'Description',
      name: 'description',
      value: recipe ? recipe.description.content : null,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Recipe data source name */
   group.AddRow({
      type: StackEditor.HIDDEN,
      name: 'dataSourceName',
      value: recipe ? recipe.dataSourceName : null
   });

   /* Recipe data source type */
   group.AddRow({
      type: StackEditor.HIDDEN,
      name: 'dataSourceType',
      value: recipe ? recipe.dataSourceType : null
   });

   /* Add a group for each application key */
   if (recipe) {
      for (var ai = 0; ai < recipe.appKeys.length; ai++) {
         var app = recipe.appKeys[ai];
         self.AddAppliesToGroup(app);
      }
   }

   if (!self.readOnly) {
      self.stackEditor.AddFooter([{
         label : "Add Applies To",
         validate: false,
         clickFunc : function() {
            self.AddAppliesToGroup({});
            self.MarkAsChanged();
         },
         clickData : null
      }]);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeBasicEditor.AddAppliesToGroup
 *
 * Add a stack editor group for an "appliesTo" application key. If 'app' is
 * specified, values default to that app, else they are left blank.
 *
 * @param app
 * -----------------------------------------------------------------------------
 */
RecipeBasicEditor.prototype.
AddAppliesToGroup = function(app)
{
   var self = this;

   var group = self.stackEditor.AddGroup({
      title: "Applies To",
      collapsible: false,
      name: 'appKeys',
      serializeAsArray: true,
      readOnly : self.readOnly,
      deleteFunc: self.readOnly ? null : function() {
         var delOk = self.ConfirmDeleteAppliesTo();
         if (delOk) {
            self.MarkAsChanged();
         }
         return delOk;
      }
   });

   /* Application name */
   group.AddRow({
      label: 'Application',
      name: 'name',
      value: app.name,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Application version */
   group.AddRow({
      label: 'Version',
      name: 'version',
      value: app.version,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Application locale */
   group.AddRow({
      label: 'Locale',
      name: 'locale',
      value: app.locale,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });

   /* Application installer revision */
   group.AddRow({
      label: 'Installer Rev',
      name: 'installerRevision',
      value: app.installerRevision,
      changeFunc: function() {
         self.MarkAsChanged();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeBasicEditor.ConfirmDeleteAppliesTo
 *
 * Confirm deletion of an 'appliesTo' key from the recipe. This is triggered
 * from the stack editor trash can icon.
 * -----------------------------------------------------------------------------
 */
RecipeBasicEditor.prototype.
ConfirmDeleteAppliesTo = function _confirmDelete()
{
   /* Confirm delete */
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete this application key?')) {
      return false;
   }

   /* Return true to have the editor row deleted automatically */
   return true;
};
