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


// TODO: Easier way to add a standard (Save,Cancel) footer
// TODO: Option for editable/not editable
// TODO: Add a row without a group.

/* When adding rows, use one of these field types */
StackEditor.TEXT_SHORT = "text-short";
StackEditor.TEXT_LONG = "text-long";
StackEditor.PWD_SHORT = "pwd-short";
StackEditor.NUMBER = "number";
StackEditor.BOOLEAN = "boolean";
StackEditor.PULLDOWN = "pulldown";
StackEditor.GROUPED_PULLDOWN = "grouped-pulldown";
StackEditor.BUTTONS = "buttons";
StackEditor.LIST = "list";
StackEditor.LABEL = "label";
StackEditor.HIDDEN = "hidden";
StackEditor.FILE_CHOOSER = "file-chooser";


/**
 * -----------------------------------------------------------------------------
 * StackEditor
 *
 * Use an instance of StackEditor to create stack editor forms on the fly
 * or modify an existing stackEditor.
 * There is a lot of HTML and CSS classes to get right in order to create
 * a stack editor, so it's much easier to use this code instead.
 *
 * @param insertPoint - Where the stack editor should either be added to the DOM
 *                      or where the stackeditor exists already.
 *                      If stackEditor exists, do not create form/validatior.
 * @param title       - Optional title bar for the editor.
 * -----------------------------------------------------------------------------
 */
function StackEditor(insertPoint, title)
{
   /* Stack editor exists, so associate editorElement, formElement */
   if (insertPoint.hasClass('stack-editor')) {
      this.editorElement = insertPoint;
      this.formElement = insertPoint.closest('form');
      return;
   }

   this.editorElement = $('<div></div>');
   this.editorElement.addClass('stack-editor editable');

   if (title) {
      var span = $('<span>').text(title);
      var div = $('<div></div>');
      div.addClass('stack-header').append(span);
      this.editorElement.append(div);
   }

   /* Create a form around the stack editor */
   this.formElement = $('<form>')
      .submit(function() { return false; })
      .keypress(function(event) { return (event.keyCode != 13); });
   this.formElement.attr('id', insertPoint.attr('id') + '-form');
   this.formElement.append(this.editorElement);

   this.validator = new Validator(this.formElement);

   insertPoint.append(this.formElement);
}


/**
 * -----------------------------------------------------------------------------
 * StackEditor.Destruct
 *
 * Destroy this instance of StackEditor.
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
Destruct = function()
{
   this.validator = this.validator.Destruct();
   this.formElement = null;
   this.editorElement = null;
   return null;
};


/**
 * -----------------------------------------------------------------------------
 * StackEditor.AddGroup
 *
 * Add a new group to the stack editor. The group is appended to the end of
 * the form (unless there is a footer; the footer is always at the very bottom
 * no matter what order you add things).
 *
 * @param label       - Text label
 * @param collapsible - If true, the group can be expanded & collapsed
 * @param deleteFunc  - If present, the group can be deleted; this function is
 *                      called first.
 * @param readOnly - If true, all rows will be read only.
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
AddGroup = function(args)
{
   /* Translate the title? */
   if (args.titleT) {
      args.title = AfTranslate(args.titleT);
   }

   var group = new StackEditorGroup(args);
   this.editorElement.append(group.groupElement);

   /* Keep the footer at the bottom */
   var footer = this.editorElement.find('.stack-footer');
   this.editorElement.append(footer);

   return group;
};


/**
 * -----------------------------------------------------------------------------
 * StackEditor.AddFooter
 *
 * Add a footer to the stack editor that contains one or more buttons. This
 * is typically used for the 'Save' and 'Cancel' buttons.
 *
 * @param buttons - Array of buttons to add. Each button should have the
 *                  following properties:
 *                  .label - Button label
 *                  .validate - Validate the form when clicked
 *                  .clickFunc - Callback for 'click' event
 *                  .clickData - Passed to clickFunc
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
AddFooter = function(buttons)
{
   var self = this;

   /* Create the footer */
   var footer = $('<div></div>').addClass('stack-footer');
   this.editorElement.append(footer);

   /* Add each button */
   for (var bi = 0; bi < buttons.length; bi++) {
      var btnArgs = buttons[bi];

      var button = $('<button>').addClass('button');
      button.text(btnArgs.label);

      if (btnArgs.clickFunc) {
         var script =
         button.data('btnArgs', btnArgs);
         button.click(function() {
            var args = $(this).data('btnArgs');
            if (args.validate) {
               self.validator.OnSubmit(args.clickFunc, args.clickData);
            }
            else {
               args.clickFunc(args.clickData);
            }
         });
      }

      footer.append(button);
   }

   AfInitStackEditor(this.editorElement);
};


/**
 * -----------------------------------------------------------------------------
 * StackEditor.Validate
 *
 * Validate the entire stack editor. This is a custom function because it is
 * common to have multiple input elements with the same name, so the validation
 * plug-in fails (it only checks the first one). Therefore we have to validate
 * each field individually.
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
Validate = function _validate()
{
   var numErrors = 0;

   /* Must call form.validate() before input.valid() */
   this.formElement.validate();

   /* Check each input, count number of failures */
   this.formElement.find("input").each(function() {
      if (!$(this).valid()) {
         numErrors++;
      }
   });

   /* If there were errors, show a message box */
   if (numErrors) {
      var errMsg = 'You have ' + numErrors + AfPluralOf(' error',numErrors) + ' in the form.';
      AfError('Error', errMsg);
   }

   /* Return true if all OK, false otherwise */
   return (numErrors == 0);
};


/**
 * -----------------------------------------------------------------------------
 * StackEditor.Serialize
 *
 * Convert the stack editor into JSON data. Basically, each group creates an
 * object, and each input in that group creates a name/value property for that
 * object.
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
Serialize = function _serialize()
{
   var json = {};

   this.editorElement.find('.stack-group').each(function() {
      var groupElement = $(this);
      var group = groupElement.data('group');

      var currObj = json;

      if (group.name) {
         if (!currObj[group.name]) {
            /* Create new object or array */
            currObj[group.name] = group.serializeAsArray ? [] : {};
         }

         if (group.serializeAsArray) {
            /* For arrays, object is a new array element */
            var obj = {};
            currObj[group.name].push(obj);
            currObj = obj;
         }
         else {
            currObj = currObj[group.name];
         }
      }

      /* Add all inputs as name/value pairs */
      group.groupElement.find("input,select").each(function() {
         var input = $(this);
         if (input.attr('name')) {
            var value = (input.is(':checkbox')) ? this.checked : input.val();
            currObj[input.attr('name')] = value;
         }
      });
   });

   return json;
};


/**
 * -----------------------------------------------------------------------------
 * StackEditor.Clear
 *
 * Remove all rows from this stack editor, resetting it to an empty state.
 * -----------------------------------------------------------------------------
 */
StackEditor.prototype.
Clear = function _clear()
{
   this.editorElement.find('.stack-group').remove();
   this.editorElement.find('.stack-row').remove();
   this.editorElement.find('.stack-footer').remove();
};


/**
 * -----------------------------------------------------------------------------
 * StackEditorGroup
 *
 * Do not create one of these directly! Use StackEditor.AddGroup instead,
 * which will return an instance of this class.
 *
 * @param args.title - Title for this group.
 * @param args.collapsible - If true, group can be collapsed.
 * @param args.collapsed - If collapsible and true, group is initially collapsed.
 * @param args.icon - Shown left of the title at 16x16
 * @param args.value - Shown as text to the  right of the title
 * -----------------------------------------------------------------------------
 */
function StackEditorGroup(args)
{
   var self = this;

   /* Allow no arguments, but make life simple for ourselves */
   if (!args) {
      args = {};
   }

   self.title = args.title;
   self.name = args.name;
   self.readOnly = args.readOnly;
   self.serializeAsArray = args.serializeAsArray;
   self.groupElement = $('<div></div>');
   self.groupElement.addClass('stack-group');
   self.groupElement.data('group', self);

   if (args.title) {
      /* Create group label */
      var label = $('<label>').text(args.title);
      var icon = $('<div></div>').addClass('icon');
      var img = null;

      if (args.icon) {
         img = $("<img>").attr({
            width: 16,
            height: 16,
            src: args.icon});
      }

      if (args.collapsible) {
         self.groupElement.addClass('collapsible');

         /* Start collapsed? */
         if (args.collapsed) {
            self.groupElement.addClass('collapsed');
         }

         /* Expand/collapse on click */
         label.click(self.groupElement, function(event) {
            event.data.toggleClass('collapsed');
            event.data.children().not('.stack-row-head').toggle();
         });
      }

      /* Wrap label with divs, add to group */
      var innerDiv = $('<div></div>').addClass('label').append(icon).append(img).append(label);
      var outerDiv = $('<div></div>').addClass('stack-row-head').append(innerDiv);

      if (args.value) {
         var valueSpan = $('<span>').append(args.value);
         valueDiv = $('<div></div>').addClass('field').append(valueSpan);
         outerDiv.append(valueDiv);
      }

      if (args.deleteFunc) {
         /* Trash can calls a custom delete function */
         var delSpan = $('<span class="delete-icon" title="Delete"></span>');
         delSpan.click(args.deleteData, function(event) {
            if (args.deleteFunc(event.data)) {
               self.groupElement.remove();
            }
         });
         innerDiv.append(delSpan);
      }

      self.groupElement.append(outerDiv);
   }
}


/**
 * -----------------------------------------------------------------------------
 * StackEditorGroup.AddRow
 *
 * Adds a new row into the stack editor form.
 *
 * @param args - Various arguments, as follows:
 *
 * args.id   - ID for the input element
 *
 * args.name - Name for the input element
 *
 * args.label - Label displayed to the user. If there is no label, the field
 *              will span the entire width of the editor. If you want a normal
 *              size field with no label, the label should be " ".
 *
 * args.tooltip - Tool tip for the input field.
 *
 * args.type - One of TEXT_SHORT, TEXT_LONG, etc
 *
 * args.required - Value is required?
 * args.readOnly - Value is read only?
 *
 * args.deleteData
 * args.deleteFunc - Adds a trashcan icon next to the field, which calls this
 *                   function when pressed. If this function returns true, the
 *                   row is removed from the form automatically.
 *
 * args.moveData
 * args.moveFunc - If defined, adds UI widgets to each row that let the user
 *                 move them up and down to reorder them. The 'moveFunc'
 *                 function is invoked with the 'moveData' argument.
 *
 * args.changeData
 * args.changeFunc - Adds a change listener to the input field which is invoked
 *                   when the value changes. The changeFunc is passed the new
 *                   value and changeData as arguments.
 *
 * args.inputData - Added as jQuery 'data' to the input element.
 *
 * args.buttons - If type is BUTTONS. Each button should have a 'label'
 *                 property, and optionally 'clickFunc' and 'clickData'
 *                 properties.
 *
 * args.index - Where to insert this row, if not at the end.
 * -----------------------------------------------------------------------------
 */
StackEditorGroup.prototype.
AddRow = function(args)
{
   var self = this;

   /* Translate the label? */
   if (args.labelT) {
      args.label = AfTranslate(args.labelT);
   }

   var labelDiv = null;
   if (args.label) {
      labelDiv = $('<div></div>');
      labelDiv.addClass('label');

      var label = $('<label>');
      label.attr('for', args.id);
      label.text(args.label);

      if (args.required) {
         label.addClass('stack-required');
      }

      labelDiv.append(label);
   }

   /*
    * We take 'validation' as an object, but we need to stringify it so we
    * can set it as a field attribute.
    */
   var validationObj = {};

   /* Add validation provided by caller */
   if (args.validation) {
      for (var key in args.validation) {
         validationObj[key] = args.validation[key];
      }
   }
   /* Add 'required' if provided by caller */
   if (args.required) {
      validationObj.required = true;
   }

   /* Convert to string */
   var validationString = JSON.stringify(validationObj);

   var fieldInput = null;
   var fieldClass = null;
   var fieldHidden = false;

   /* Create an input field. */
   switch (args.type) {
      case undefined:
      case StackEditor.TEXT_SHORT:
         /* Create a short text field. This is the default type. */
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "text",
            onblur: "this.value=jQuery.trim(this.value)"
         });
         fieldInput.val(args.value);
         fieldClass = 'text short';
         break;

      case StackEditor.TEXT_LONG:
         /* Create a long text field */
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "text",
            onblur: "this.value=jQuery.trim(this.value)"
         });
         fieldInput.val(args.value);
         fieldClass = 'text long';
         break;

      case StackEditor.PWD_SHORT:
         /* Create a short password field. */
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "password"
         });
         fieldInput.val(args.value);
         fieldClass = 'text short';
         break;

      case StackEditor.NUMBER:
         /* Create a long text field */
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "text",
            onblur: "this.value=jQuery.trim(this.value)"
         });
         fieldInput.val(args.value);
         fieldClass = 'text number';
         break;

      case StackEditor.BOOLEAN:
         /* Create a checkbox field */
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "checkbox",
            value: "true"
         });
         if (args.value && args.value != "false") {
            fieldInput.attr("checked", "checked");
         }
         fieldInput.val(args.value);
         fieldClass = "checkbox";
         break;

      case StackEditor.FILE_CHOOSER:
         fieldInput = $('<input>').attr({
            name: args.name,
            id: args.id,
            type: "file"
         });
         break;

      case StackEditor.LABEL:
         /* Not really a field, just text. */
         fieldInput = $('<span>').text(args.value);
         break;

      case StackEditor.HIDDEN:
         fieldHidden = true;
         fieldInput = $('<input>').attr({
            name : args.name,
            id: args.id,
            type: "hidden"});
         fieldInput.val(args.value);
         break;

      case StackEditor.PULLDOWN:
      case StackEditor.GROUPED_PULLDOWN:
         if (args.options) {
            fieldInput = AfCreatePulldown(args);
            fieldClass = "select";
         }
         break;

      case StackEditor.LIST:
         fieldInput = $('<select size="10">').attr({
            name : args.name,
            id: args.id
         });
         if (args.options) {
            for (var oi = 0; oi < args.options.length; oi++) {
               var val = args.options[oi];
               var opt = $('<option>');

               // Check if val is of EachOption type
               if (val.display) {
                  var displayText = val.translate?
                        AfTranslate(val.display) : val.display;
                  opt.val(val.key).text(displayText);
               }
               else { // val is simply a text and is used as both key & value
                  opt.val(val).text(val);
               }
               if (val == args.value) {
                  opt.attr('selected', 'selected');
               }
               fieldInput.append(opt);
            }
         }
         fieldClass = "list";
         break;

      case StackEditor.BUTTONS:
         /* A span to hold all the buttons */
         fieldInput = $('<span>');
         fieldInput.attr('style', 'padding:0;margin-bottom:-1px');

         for (var bi = 0; bi < args.buttons.length; bi++) {
            var btnArgs = args.buttons[bi];

            /* Create the button */
            var btn = null;
            if (btnArgs.image) {
               btn = $('<img>').attr({
                  name : btnArgs.name,
                  width: '16',
                  height: '16',
                  src : btnArgs.image,
                  id: btnArgs.id
               });
            }
            else {
               btn = $('<button>').attr({
                  name : btnArgs.name,
                  id: btnArgs.id
               });
               btn.text(btnArgs.label);
            }

            /* Add button click callback */
            if (btnArgs.clickFunc) {
               btn.data('btnArgs', btnArgs);
               btn.click(function() {
                  var args = $(this).data('btnArgs');
                  args.clickFunc(args.clickData);
               });
            }

            /* Add button to the button span */
            fieldInput.append(btn);
         }
         break;
   }

   /* Add input field ID */
   if (args.id && fieldInput) {
      fieldInput.attr('id', args.id);
   }

   /* Add change listener */
   if (args.changeFunc) {
      fieldInput.change(function() {
         var self = $(this);
         var value = (self.is(':checkbox') ? self.attr('checked') : self.val());
         args.changeFunc(value, args.changeData, self);
      });
   }

   /* Add validation string */
   if (validationString) {
      fieldInput.attr('validate', validationString);
   }

   /* Add other arbitrary 'data' that the caller passed in. */
   for (var dataKey in args.inputData) {
      fieldInput.data(dataKey, args.inputData[dataKey]);
   }

   /* Read only? */
   var readOnly = this.readOnly; // by default, same as group setting
   if (args.readOnly !== undefined) {
      readOnly = args.readOnly; // override from this row's args
   }
   if (readOnly) {
      fieldInput.attr("readonly", true);
      fieldInput.enable(false);
   }

   if (fieldHidden) {
      /* Just add the input, do nothing else */
      this.groupElement.append(fieldInput);
      return;
   }

   /* Wrap input field with a field <div> */
   var inputDiv = $('<div></div>');
   inputDiv.addClass('field').addClass(fieldClass);
   inputDiv.append(fieldInput);

   /* Add units */
   if (args.units) {
      inputDiv.append($('<span>').html(args.units));
   }

   /* Add tool tip */
   if (args.tooltip) {
      inputDiv.attr('title', args.tooltip);
   }

   /* Add a trash can */
   if (args && args.deleteFunc) {
      /* Trash can calls a custom delete function */
      var delSpan = $('<span class="delete-icon" title="Delete"></span>');
      delSpan.click(args.deleteData, function(event) {
         var removeRow = args.deleteFunc(event.data);
         if (removeRow) {
            $(this).parents('.stack-row').first().remove();
         }
      });
      inputDiv.append(delSpan);
   }

   /* If there was no label, make the input stretch across */
   if (!labelDiv) {
      inputDiv.addClass("full-width");
   }

   /* Assemble the row */
   var row = $('<div></div>');
   row.addClass('stack-row');
   row.append(labelDiv);
   row.append(inputDiv);

   /* ID/Class for the whole row? */
   if (args.rowId) {
      row.attr("id", args.rowId);
   }
   if (args.rowClass) {
      row.addClass(args.rowClass);
   }

   /*
    * Movable?
    * If the row is movable, add spans for "up" and "down" controls. When
    * clicked, these controls reposition the row within the group. NOTE: A
    * row can only be moved up/down if the row above/below it is movable too.
    */
   if (args && args.moveFunc) {
      row.addClass('movable');

      /* Move up */
      var upSpan = $('<span class="move-up-icon" title="Move up"></span>');
      upSpan.click(args.moveData, function(event) {
         var move = args.moveFunc(event.data);
         if (move) {
            var row = $(this).parents('.stack-row').first();
            row.prev('.stack-row.movable').before(row);
         }
      });
      inputDiv.append(upSpan);

      /* Move down */
      var downSpan = $('<span class="move-down-icon" title="Move down"></span>');
      downSpan.click(args.moveData, function(event) {
         var move = args.moveFunc(event.data);
         if (move) {
            var row = $(this).parents('.stack-row').first();
            row.next('.stack-row.movable').after(row);
         }
      });
      inputDiv.append(downSpan);

      /* If long text, make a little shorter */
      $(inputDiv, '.long').removeClass('long').addClass('almost-long');
   }

   /* Add row to the form... */
   if (args && args.index !== undefined) {
      /* ...at the given index */
      var ip = this.groupElement.children('.stack-row:eq(' + args.index + ')');
      ip.length ? row.insertBefore(ip) : this.groupElement.append(row);
   }
   else {
      /* ...at the end */
      this.groupElement.append(row);
   }
};


/**
 * -----------------------------------------------------------------------------
 * StackEditorGroup.DeleteRow
 *
 * Delete a row from this stack group.
 * -----------------------------------------------------------------------------
 */
StackEditorGroup.prototype.
DeleteRow = function _DeleteRow(index)
{
   /* Do nothing if index is invalid */
   if (index < 0 || index >= this.NumRows()) {
      return;
   }

   this.groupElement.find('.stack-row').eq(index).remove();
   return;
};


/**
 * -----------------------------------------------------------------------------
 * StackEditorGroup.NumRows
 *
 * Return the number of rows in this stack group.
 * -----------------------------------------------------------------------------
 */
StackEditorGroup.prototype.
NumRows = function()
{
   return this.groupElement.children('.stack-row').size();
};


/**
 * -----------------------------------------------------------------------------
 * StackEditorGroup.SetTitle
 *
 * Change the title of a stack editor group. This only changes the title
 * displayed on the page.
 * -----------------------------------------------------------------------------
 */
StackEditorGroup.prototype.
SetTitle = function _setTitle(newTitle)
{
   var self = this;

   self.title = newTitle;
   self.groupElement.find(".stack-row-head label").text(newTitle);
};
