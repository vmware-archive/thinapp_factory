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
 * RecipeManager
 *
 * Manager for dealing with all aspects of conversion "recipes".
 * -----------------------------------------------------------------------------
 */
function RecipeManager(
      appId, lastRecipeId, selectionData,
      basicTab, basicChangeMarker,
      variablesTab, variablesChangeMarker,
      filesTab, filesChangeMarker,
      stepsTab, stepsChangeMarker
)
{
   if ('' == appId) {
      appId = null;
   }
   if ('' == lastRecipeId) {
      lastRecipeId = null;
   }
   if ('' == selectionData) {
      selectionData = null;
   }

   this.appId = appId;
   this.lastRecipeId = lastRecipeId;
   this.selectionData = selectionData;

   this.refreshUrl = '/api/recipes?sort=true';

   if (basicTab) {
      this.basicEditor = new RecipeBasicEditor(basicTab, basicChangeMarker);
   }
   if (variablesTab) {
      this.varEditor = new RecipeVarEditor(variablesTab, variablesChangeMarker);
   }
   if (filesTab) {
      this.filesEditor = new RecipeFileEditor(filesTab, filesChangeMarker);
   }
   if (stepsTab) {
      this.stepsEditor = new RecipeStepEditor(stepsTab, stepsChangeMarker);
   }
}

RecipeManager.prototype = new AbstractManager('Recipes');


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.HasChanges
 *
 * A convenience function to see if any section of the editor has
 * unsaved changes.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
HasChanges = function _hasChanges()
{
   var changes = false;

   if (this.basicEditor) {
      changes = changes || this.basicEditor.HasChanges();
   }
   if (this.varEditor) {
      changes = changes || this.varEditor.HasChanges();
   }
   if (this.filesEditor) {
      changes = changes || this.filesEditor.HasChanges();
   }
   if (this.stepsEditor) {
      changes = changes || this.stepsEditor.HasChanges();
   }

   return changes;
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.MarkAsUnchanged
 *
 * A convenience function to mark each editor tab as unchanged.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
MarkAsUnchanged = function _markAsUnchanged()
{
   if (this.basicEditor) {
      this.basicEditor.MarkAsUnchanged();
   }
   if (this.varEditor) {
      this.varEditor.MarkAsUnchanged();
   }
   if (this.filesEditor) {
      this.filesEditor.MarkAsUnchanged();
   }
   if (this.stepsEditor) {
      this.stepsEditor.MarkAsUnchanged();
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.UpdateRecipes
 *
 * Start an automatic update of all displayed recipes, repeating at the
 * specified interval.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
UpdateRecipes = function _updateRecipes(table, refreshInterval)
{
   this.CreateTableWrapper(table);

   this.UpdateDataTable({
      table: this.tableWrapper.dataTable,
      url: this.refreshUrl,
      dataHandler: this.createTableRows,
      repeatMs: refreshInterval,
      firstTimeWaitMessage: AfTranslate('M.COMMON.FETCHING_DATA'),
      showRefreshFlag: true
   });
};


/**
 * Render the application's icon.
 */
RecipeManager.prototype.
RenderIcon = function (icons)
{
   var args = {};
   args.iconSize = window.VmTAF.iconSizeLarge;
   args.iconUrl = AfGetIconUrl(icons, window.VmTAF.iconSizeLarge);
   $('#appInfoIcon').append($('#appInfoIconTemplate').render(args));
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.DeleteSelected
 *
 * Delete the selected recipes from the table.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
DeleteSelected = function _deleteSelected()
{
   var recipes = this.tableWrapper.GetSelectedRowData();

   // Make sure at least one application was selected.
   if (recipes.length == 0) {
      AfError(
            'No Selection',
            'No recipes selected. Please select the recipes'
            + ' you want to delete, and try again.');
      return;
   }

   // Confirm delete with user.
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to delete ' +
         AfThisOrThese(recipes, 'recipe') + '?')) {
      return;
   }

   var self = this;
   var numOk = 0;
   var numErr = 0;

   for (var i = 0; i < recipes.length; i++) {
      var recipeId = recipes[i].id;
      AfLog('deleting recipe ' + recipeId);

      AfAjax({
         method: 'DELETE',
         contentType: 'application/json',
         url: '/api/recipes/' + recipeId,

         beforeSend: function() {
            AfStartPleaseWaitT("M.RECIPES.DELETING");
         },
         success: function() {
            numOk++;
         },
         error: function() {
            numErr++;
         },
         complete: function() {
            if (AfEndPleaseWait()) {
               if (numErr > 0) {
                  AfErrorT(
                     'T.COMMON.ERROR',
                     'M.RECIPES.SOME_DELETES_FAILED');
               }
               else if (numOk > 0) {
                  AfNotifyT(
                     'T.COMMON.SUCCESS',
                     'M.RECIPES.DELETED');
                  self.PopulateDataTableNow({
                     url: self.refreshUrl,
                     table: self.tableWrapper.dataTable,
                     dataHandler: self.createTableRows
                  });
               }
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.SelectRecipeForApp
 *
 * Select a single recipe for the app previously viewed.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
SelectRecipeForApp = function()
{
   var recipes = this.tableWrapper.GetSelectedRowData();

   // Make sure at least one application was selected.
   if (recipes.length == 0) {
      AfError(
            'No Selection',
            'Please select a recipe that you want to apply to the application, and try again.');
      return;
   }

   if (recipes.length > 1) {
      AfError(
            'Select One',
            'Please select a single recipe that you want to apply to the application, and try again.');
      return;
   }

   VmTAF.contentNavigator.LoadPage('/apps/detail/' + this.appId + '?recipeId=' + recipes[0].id
         + '&selectionData=' + this.selectionData);
};

/* link to new recipe page, preserving appId and selectionData */
RecipeManager.prototype.
newRecipe = function()
{
   VmTAF.contentNavigator.LoadPage('/recipes/create/'
      + '?appId=' + this.appId
      + '&selectionData=' + this.selectionData
   );
};

/**
 * -----------------------------------------------------------------------------
 * RecipeManager.CloneSelected
 *
 * Clone the selected recipes. This asks the server to make copies of each
 * selected recipe. The function returns once all the copies are made. Note
 * that the copies are named automatically.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
CloneSelected = function _cloneSelected()
{
   var recipes = this.tableWrapper.GetSelectedRowData();

   // Make sure at least one application was selected.
   if (recipes.length == 0) {
      AfError(
            'No Selection',
            'No recipes selected. Please select the recipes'
            + ' you want to clone, and try again.');
      return;
   }

   // Confirm clone with user.
   if (!AfConfirm(
         'Confirm Delete',
         'Are you sure you want to clone ' +
         AfThisOrThese(recipes, 'recipe') + '?')) {
      return;
   }

   var self = this;
   var numOk = 0;
   var numErr = 0;

   for (var i = 0, counter = 0; i < recipes.length; i++) {
      var recipeId = recipes[i].id;
      AfLog('cloning recipe ' + recipeId);

      AfAjax({
         method: 'POST',
         url: '/api/recipes/' + recipeId + '/clone',

         success: function() {
            numOk++;
         },
         error: function() {
            numErr++;
         },
         complete: function() {
            counter++;
            if (counter == recipes.length) {
               if (numErr > 0) {
                  AfErrorT(
                     'T.COMMON.ERROR',
                     'M.RECIPES.SOME_CLONES_FAILED');
               }
               else if (numOk > 0) {
                  AfNotifyT(
                     'T.COMMON.SUCCESS',
                     'M.RECIPES.CLONED');
                  self.PopulateDataTableNow({
                     url: self.refreshUrl,
                     table: self.tableWrapper.dataTable,
                     dataHandler: self.createTableRows
                  });
               }
            }
         }
      });
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.ExportSelected
 *
 * Export the selected recipes into a local ZIP file.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
ExportSelected = function _exportSelected()
{
   var recipes = this.tableWrapper.GetSelectedRowData();

   /* Make sure one recipe was selected */
   if (recipes.length == 0) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.RECIPES.NO_SELECTION');
      return;
   }
   if (recipes.length > 1) {
      AfErrorT(
         'T.COMMON.NO_SELECTION',
         'M.RECIPES.TOO_MANY_SELECTIONS');
      return;
   }

   var recipe = recipes[0];
   var recipeId = recipe.id;
   AfLog('exporting recipe "' + recipe.name + '" [' + recipeId + ']');

   /*
    * Since we can't write the response from an AJAX call into a local file,
    * or force the browser to treat it as a downloaded attachment, we can't
    * use AJAX to get the recipe ZIP file. Also, we can't just force a browser
    * location change with "window.location=", since we will then lose any
    * server errors that might be generated during the export.
    *
    * Therefore, we need to export a recipe in two phases: First, POST a request
    * to export the recipe to a server file. On success, we get a file ID: we
    * can then safely change the browser window to GET that file.
    */
   AfAjax({
      url: "/api/recipes/" + recipeId + "/export",
      method: "POST",
      beforeSend: function() {
         AfLog("Requesting export of recipe " + recipeId);
      },
      success: function(data, textStatus, jqXHR) {
         var exportId = jqXHR.responseText;
         AfLog("Export of recipe " + recipeId + " successful: exportId = " + jqXHR.responseText);
         /* Redirect to the export URL. This will cause a download */
         window.location = VmTAF.contextPath + "/api/recipes/" + recipeId + "/export/" + exportId;
      },
      error: function(jqXHR, textStatus, errorThrown) {
         AfLog("Export of recipe " + recipeId + " failed: " + jqXHR.responseText);
         AfError(
            'Export Failed',
            'Recipe export failed: ' + jqXHR.responseText);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.GetRecipeAndPopulateEditor
 *
 * Fetch the specified recipe from the server, and display it in the recipe
 * edit form.
 *
 * @param recipeId ID of recipe to be edited.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
GetRecipeAndPopulateEditor = function _getRecipeAndPopulateEditor(recipeId)
{
   var self = this;

   if (!recipeId) {
      /* No recipeId means we are creating a new recipe */
      self.PopulateEditor(null);
      return;
   }


   AfAjax({
      method: 'GET',
      url: '/api/recipes/' + recipeId,
      success: function(recipe) {
         self.PopulateEditor(recipe);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.PopulateEditor
 *
 * Populate all tabs of the recipe editor.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
PopulateEditor = function _populateEditor(recipe)
{
   var readOnly = recipe && recipe.readOnly;

   /* Populate all the tabs */
   if (this.basicEditor) {
      this.basicEditor.PopulateRecipe(recipe, readOnly);
   }
   if (this.varEditor) {
      this.varEditor.PopulateVariables(recipe, readOnly);
   }
   if (this.filesEditor) {
      this.filesEditor.PopulateFiles(recipe, readOnly);
   }
   if (this.stepsEditor) {
      this.stepsEditor.PopulateRecipe(recipe, readOnly);
   }

   /* Change the main panel title */
   $("#main-panel .panel-title").text(
         recipe == null ? "Add New Recipe" :
         recipe.readOnly ? "View Recipe \"" + recipe.name + "\"" :
         "Edit Recipe \"" + recipe.name + "\"");

   if (readOnly) {
      /*
       * Make sure a read-only recipe cannot be edited.
       * We have to disable the save button, and make all the input
       * fields read-only.
       */
      var title = "This recipe is from a " + recipe.dataSourceType + " and cannot be edited.";
      var saveTxt = AfTranslate("T.RECIPES.SAVE_RECIPE");
      var resetTxt = AfTranslate("T.COMMON.RESET");

      /* Grey out the save/reset buttons */
      $("button:contains('"+saveTxt+"')").enable(false).attr('title', title);
      $("button:contains('"+resetTxt+"')").enable(false).attr('title', title);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.SaveRecipe
 *
 * Save the current recipe.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
SaveRecipe = function _saveRecipe()
{
   var self = this;

   /* Basic form validation first */
   if (!self.basicEditor.stackEditor.Validate() ||
       !self.varEditor.stackEditor.Validate() ||
       !self.filesEditor.stackEditor.Validate() ||
       !self.stepsEditor.stackEditor.Validate()) {
      return;
   }

   /* Collect serialized version of each editor */
   var jsonParts = [
      self.basicEditor.Serialize(),
      self.varEditor.Serialize(),
      self.filesEditor.Serialize(),
      self.stepsEditor.Serialize()];

   /* Assemble all the pieces into an entire recipe */
   var recipe = {};
   for (var ji = 0; ji < jsonParts.length; ji++) {
      for (var prop in jsonParts[ji]) {
         recipe[prop] = jsonParts[ji][prop];
      }
   }

   /* Make sure the form is complete. */
   try {
      self.validate(recipe);
   }
   catch(error) {
      AfError(
         "Invalid Recipe",
         "Unable to save recipe: " + error);
      return;
   }

   if (self.startFileUploads(recipe, true, this.appId) > 0) {
      /*
       * There was at least one new file that needed to be uploaded.
       * Stop the recipe save right now ... once the last upload is complete,
       * the recipe save will continue.
       */
      AfLog("File uploads started: recipe save postponed");
   }
   else {
      /* There were no files to upload, so continue with saving as normal. */
      self.saveRecipeImpl(recipe, false, true, this.appId);
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.startFileUploads
 *
 * Look at every file in the given recipe, and look for files that are both
 * new and have an uploadFormId associated. For each one found, initiate a
 * file upload request.
 *
 * When the final upload request has completed, this function calls
 * saveRecipeImpl to continue saving the rest of the recipe.
 *
 * @param recipe
 * @param goBack
 * @param appId
 * @return The number of file upload requests that were made.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
startFileUploads = function _startFileUploads(recipe, goBack, appId)
{
   var self = this;

   if (!recipe.files || !recipe.files.length) {
      return 0;
   }

   var uploadsStarted = 0;
   for (var i = 0; i < recipe.files.length; i++) {
      var file = recipe.files[i];

      if (!file.isNew || !file.uploadFormId) {
         continue;
      }

      AfLog("Starting a recipe file upload:");
      AfLog("  path = " + file.path);
      AfLog("  uri = " + file.uri);
      AfLog("  recipeFile = " + file.recipeFile);
      AfLog("  uploadFormId = " + file.uploadFormId);

      // Get the file chooser and its parent in the DOM
      var fileChooser = $("#file-chooser-" + file.uploadFormId);
      var fileChooserParent = fileChooser.parents().first();

      // Define the HTML form element to submit the file
      var form = $("<form>")
         .attr({
            id: file.uploadFormId,
            enctype: "multipart/form-data"
         });

         // Add the file chooser input field
         // NOTE: Removes it from its original location in the DOM!
         form.append(fileChooser).

         // Convert to an AJAX form
         ajaxForm({
            url: VmTAF.contextPath + "/api/recipes/uploadfile",
            type: "POST",
            dataType: "json",
            fileIndex: i,

            beforeSubmit: function() {
               AfStartPleaseWait("Uploading recipe files");
            },

            success: function(response, statusText, xhr, form) {
               var f = recipe.files[$(this)[0].fileIndex];
               f.uri = response.uri;
               AfLog("Upload " + f.uploadFormId + " success, URI = " + f.uri);
            },

            error: function() {
               // XXX Not handled yet! Fix this when the feature is added.
            },

            complete: function() {
               // XXX There might have been errors
               if (AfEndPleaseWait()) {
                  AfLog("All uploads complete, saving recipe");
                  self.saveRecipeImpl(recipe, true, goBack, appId);
               }
            }
         });

      AfLog('  submitting file upload form');
      form.submit();

      /*
       * The file chooser was removed from the main form so replace it
       * with some text, else it looks like an empty field.
       */
      fileChooserParent.append($("<label>").text("Upload in progress"));

      uploadsStarted++;
   }

   AfLog('Started ' + uploadsStarted + ' file uploads');
   return uploadsStarted;
};

RecipeManager.prototype.
cancel = function() {
   // clear any modification markers so that the user
   // is not prompted
   // todo: is this desirable behavior?
   this.MarkAsUnchanged();

   if (this.lastRecipeId && this.appId) {
      VmTAF.contentNavigator.LoadPage('/apps/detail/' + this.appId
         + '?recipeId=' + this.lastRecipeId
         + '&selectionData=' + this.selectionData);
   } else if (this.appId) {
      VmTAF.contentNavigator.LoadPage('/apps/detail/' + this.appId
         + '?selectionData=' + this.selectionData);
   } else {
      VmTAF.contentNavigator.GoBack();
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.saveRecipeImpl
 *
 * Save the current recipe.
 * @param recipe
 * @param validate
 * @param goBack
 * @param appId
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
saveRecipeImpl = function _saveRecipeImpl(recipe, validate, goBack, appId)
{
   var self = this;

   /* Remove client-only properties so the server doesn't complain */
   if (recipe.files && recipe.files.length) {
      for (var i = 0; i < recipe.files.length; i++) {
         var file = recipe.files[i];
         delete file.isNew;
         delete file.uploadFormId;
         delete file.recipeFile;
      }
   }

   /* Make sure the recipe is complete. */
   if (validate) {
      try {
         self.validate(recipe);
      }
      catch(error) {
         AfError(
            "Invalid Recipe",
            "Unable to save recipe: " + error);
         return;
      }
   }

   /* Default to creating a new recipe */
   var method = "POST";
   var url = "/api/recipes";

   /* New recipes need a name */
   if (!recipe.id && !recipe.name) {
      AfError(
         'Invalid Recipe',
         'Invalid recipe. No recipe name entered.');
      return;
   }

   /* REST method and location */
   var method = (recipe.id ? "PUT" : "POST");
   var url = (recipe.id ? "/api/recipes/" + recipe.id : "/api/recipes");

   // Save the recipe id if exists, else assign the new id to this var.
   var recipeId = recipe.id;

   /* Remove request fields we don't need */
   delete recipe.id;
   delete recipe.dataSourceType;
   delete recipe.dataSourceName;

   AfAjax({
      method: method,
      url: url,
      contentType: 'application/json',
      data: recipe,

      success: function(result) {
         AfNotify(
            AfTranslate("T.COMMON.SUCCESS"),
            recipe.renameIfNeeded ?
               "Recipe saved as \"" + result.name + "\"" :
               AfTranslate("M.RECIPES.SAVED"));
         if (result) {
            recipeId = result.id;
         }
         self.MarkAsUnchanged();
         if (goBack) {
            // TODO Needed on new ui, as the request originates from app detail
            // page on new UI. Picks up new recipeId on creation.

            // If request is from app page, go back to the app page with the recipeId.
            if (appId) {
               VmTAF.contentNavigator.LoadPage(
                     "/apps/detail/" + appId
                     + "?recipeId=" + recipeId
                     + '&selectionData=' + self.selectionData);
            }
            else {
               VmTAF.contentNavigator.GoBack();
            }
         }
      },

      error: function(jqXHR, status, error) {
         AfError(
            'Recipe Save Failed',
            'Error: recipe was not saved: ' + jqXHR.responseText);
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.validate
 *
 * Validate the given recipe, making sure all required fields are present etc.,
 * before sending it to the server for create or update.
 *
 * TODO: Check for a unique recipe name on server.
 * TODO: Validate variables in recipe commands.
 * TODO: Recipe should not be read-only.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
validate = function(recipe)
{
   /* Recipe name required */
   if (!recipe.name) {
      throw "Recipe name is required.";
   }

   /* All variables need unique names */
   if (recipe.variables) {
      for (var i = 0; i < recipe.variables.length; i++) {
         var vari = recipe.variables[i];

         if (!vari.name) {
            throw "All variables need a name.";
         }

         for (var look = i + 1; look < recipe.variables.length; look++) {
            var var2 = recipe.variables[look];
            if (var2.name == vari.name) {
               throw "All variables need unique names.";
            }
         }
      }
   }

   /* All files need some sort of location */
   if (recipe.files) {
      for (var i = 0; i < recipe.files.length; i++) {
         var file = recipe.files[i];

         if (file.isNew) {
            if (file.uploadFormId) {
               // Upload files need a file to be chosen
               if (!file.recipeFile) {
                  throw "No local file chosen for file " + (file.name || "");
               }
            }
            else {
               // Other files need a URI
               if (!file.uri) {
                  throw "File " + (file.name || "") + " is missing a URI";
               }
            }
         }
      }
   }

   /* All commands must be non-blank */
   for (var stepName in recipe.steps) {
      var step = recipe.steps[stepName];
      for (var i = 0; i < step.commands.length; i++) {
         var cmd = step.commands[i];

         if (!cmd.command) {
            throw "Step commands cannot be blank.";
         }
      }
   }
};


/**
 * -----------------------------------------------------------------------------
 * RecipeManager.createTableRows
 *
 * Create all rows in the recipe table from the given JSON data.
 * -----------------------------------------------------------------------------
 */
RecipeManager.prototype.
createTableRows = function(self, jsonData, options)
{
   var recipes = jsonData;

   /* Remove all rows from the table. */
   self.tableWrapper.ClearTable();

   /* Add a row per recipe. */
   for (var bi = 0; bi < recipes.length; bi++) {
      var recipe = recipes[bi];
      var row = [];

      /* Check-box to select this recipe */
      var cb = $('<input>');
      cb.attr('type', 'checkbox');
      row.push(AfHtml(cb));

      /* Name of recipe; link to editor */
      var id = self.createRecordLink(
            recipe.name,
            recipe.id,
            '/recipes/edit/' + recipe.id + '?appId=' + self.appId + '&selectionData=' + self.selectionData);
      row.push(AfHtml(id));

      /* Recipe description */
      // TODO: Assumes text/plain only here
      row.push(recipe.description.content);

      /* Number of variables in recipe */
      row.push(recipe.variables.length);

      /* Number of files in recipe */
      row.push(recipe.files.length);

      /* Number of commands in recipe */
      var numCommands = 0;
      for (var pi = 0; pi < VmTAF.recipePhases.length; pi++) {
         var phase = VmTAF.recipePhases[pi];
         if (recipe.steps[phase]) {
            numCommands += recipe.steps[phase].commands.length;
         }
      }
      row.push(numCommands);

      /* Source */
      var src = recipe.dataSourceType ?
            AfCreateDataSourceSpan(recipe.dataSourceType, recipe.dataSourceName) :
            "User";
      row.push(AfHtml(src));

      self.tableWrapper.AddRow(row, recipe);
   }

   self.tableWrapper.DrawTable();
};
