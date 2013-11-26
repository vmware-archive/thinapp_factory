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

//######################### Global Init START ############################
jQuery.validator.setDefaults(
{
   errorElement : "span",
   ignoreTitle: true, // ignore title attribute for error messages.
   errorPlacement : function(error, element) {
      var errText = error.text();
      /**
       * Set error message to 'title' attribute of the input element for a tooltip text.
       * By default, tooltop plugin loads text from title attribute.
       */
      element.attr('title', errText);
      error.html(''); // Wipe out error message from error span so that only error icon will be displayed.
      error.attr('title', errText); // But still set errText to 'title' for the error icon's tooltip text.
      error.addClass('validation-error-icon');
      error.insertAfter(element);
   },

   /**
    * This showErrors will trigger a call to 'errorPlacement' for each error.
    * Every validation error should call errorPlacement to update error message in the tooltip text.
    */
   showErrors: function (errorMap, errorList) {
      for (var i = 0; errorList[i]; i++) {
          var element = this.errorList[i].element;
          this.errorsFor(element).remove();
      }
      this.defaultShowErrors();
   },
   // This one updates from error icon to success icon.
   success: function(label) {
      label.addClass('validation-success-icon');
      // Remove tooltip text from the validation icon.
      label.removeAttr('title');
   },
   // This unhighlight callback hides the tooltip text.
   unhighlight: function(element, errorClass) {
      $(element).removeAttr('title');
   }
});

//######### User-defined validation methods ##########
/**
 * Check for a space in the input string.
 */
jQuery.validator.addMethod(
   "noSpace",
   function(value) {
      return value.indexOf(' ') == -1;
   },
   AfTranslate('M.VALIDATION.NO_SPACES')
);

/**
 * Check for alpha numeric with spaces and dashes.
 */
jQuery.validator.addMethod(
   "dsName",
   function(value) {
      var fsRegex = /^[a-zA-Z0-9\-]+$/;
      return fsRegex.test(value);
   },
   AfTranslate('M.STORAGE.INVALID_NAME')
);

/**
 * Check for a valid variable name. Uses the same
 * rules that Javascript uses.
 */
jQuery.validator.addMethod(
   "variableName",
   function(value) {
      var regex = new RegExp(VmTAF.variableNameRegex);
      return regex.test(value);
   },
   AfTranslate('M.VALIDATION.INVALID_VARIABLE_NAME')
);

/**
 * Check file share location using regex - ^(\\{0,2}(\w|[.-])+\\.+)$|^(\/{0,2}(\w|[.-])+\/.+)$
 * Valid File Shares:
 *  1. hostname.com/path OR hostname.com\path
 *  2. 127.0.0.1/path OR 127.0.0.1\path
 *  3. //hostname.com/path OR \\hostname.com\path
 *  4. //host-name.com/path-2 OR \\host-name.com\path-2
 *  5. hostname.com/path/to/share/... OR hostname.com\path\to\share\...
 *  6. //hostname.com/path/to/share... OR \\hostname.com\path\to\share...
 *  Invalid File Shares:
 *  1. host  name.com/path OR 127.1  2.0.1
 *  2. /////hostname.com/path OR \\\\hostname.com\path
 */
jQuery.validator.addMethod(
   "fileshare",
   function(value) {
      var fsRegex = /^(\\{0,2}(\w|[.-])+\\.+)$|^(\/{0,2}(\w|[.-])+\/.+)$/;
      return fsRegex.test(value);
   },
   AfTranslate('M.FILE_SHARES.INVALID_SHARELOC')
);

/**
 * Check whether the installer name ends with EXE or MSI (case-insensitive).
 */
jQuery.validator.addMethod(
   "endsWithExeOrMsi",
   function(value) {
      var installerNameRegex = /.*.(EXE|MSI)$/i;
      return installerNameRegex.test(value);
   },
   AfTranslate('M.APPS.INVALID_INSTALLER_NAME')
);

/**
 * Kind of like URL, but accepts any scheme (so we can check for datastore
 * URIs).
 */
jQuery.validator.addMethod(
   "uri",
   function(value) {
      // FIXME: not perfect: more like a URL regex with any scheme
      var regexp = /(\w+):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/i;
      return regexp.test(value);
   },
   AfTranslate('M.VALIDATION.INVALID_URI')
);

/**
 * Validates if its a valid URL, and at the same time escapes white spaces that can be escaped.
 */
jQuery.validator.addMethod(
      "urlSpaceEscaped",
      function(value, element) {
         var newValue = jQuery.trim(value).replace(/ /g, '%20');
         // Copied from jquery.validate.js, and modified to handle trim and escape whitespace in url.
         var isNewValid = /^(https?|ftp):\/\/(((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:)*@)?(((\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d\d|2[0-4]\d|25[0-5]))|((([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|\d|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.)+(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])*([a-z]|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])))\.?)(:\d*)?)(\/((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)+(\/(([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)*)*)?)?(\?((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|[\uE000-\uF8FF]|\/|\?)*)?(\#((([a-z]|\d|-|\.|_|~|[\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])|(%[\da-f]{2})|[!\$&'\(\)\*\+,;=]|:|@)|\/|\?)*)?$/i.test(newValue);
         if (isNewValid && newValue != jQuery.trim(value)) {
            element.value = newValue;
         }
         return this.optional(element) || isNewValid;
      },
      jQuery.validator.messages.url
);

/**
 * Checks whether the value is made up of alphanumeric characters or '-'.
 *
 * NOTE: This is more of a generic validation, as the length validation is done on a different one.
 */
jQuery.validator.addMethod(
   "licenseKey",
   function(value) {
      var regexp = /^[a-zA-Z0-9\-]+$/;
      return regexp.test(value);
   },
   AfTranslate('M.VALIDATION.INVALID_LICENSE')
);

/**
 * Check whether the input value is an integer or not.
 * E.g: 0012 => TRUE
 *      1.52 => FALSE
 *      101  => TRUE
 *      10.0 => TRUE
 */
jQuery.validator.addMethod(
   "integer",
   function(value) {
      // Check modulus
      return (value % 1 == 0);
   },
   AfTranslate('M.VALIDATION.INTEGER')
);

// This setting enables tagging validation rules/messages via "validate" attribute in the element.
// E.g. <input name="name" type="text" validate="{required:true, noSpace:true}"></input>
$.metadata.setType("attr", "validate");
//######################### Global Init END ############################

/**
 * -----------------------------------------------------------------------------
 * function Validator
 *
 * This class binds a new instance of validator to the given form so that both
 * field-level and form (onSubmit) validation can be used.
 *
 * If onSuccessCallback is supplied, two things happen:
 *  1. we register Validator.OnSubmit to be called when the form is submitted.
 *     This will trigger validation whenever the form is submitted.
 *  2. in Validator.OnSubmit, if the validation passes, the onSuccessCallback
 *     is invoked.
 *
 *  Finally, in all cases, we initialize the stack editor for the form.
 * -----------------------------------------------------------------------------
 */
function Validator(formSelector, onSuccessCallback) {
   if (!formSelector) {
      this._jqueryValidator = null;
      AfLog("Unexpected: the given form is null!");
      return;
   }

   var self=this;
   this._jqueryValidator = $(formSelector).validate();
   if (onSuccessCallback) {
      $(formSelector).submit(function(event) {
         self.OnSubmit(onSuccessCallback,event.currentTarget);
      });
   }

   $(formSelector).submit(function(event) {
      // now that we hijacked the form submission, we want to
      // prevent the browser from doing anything further with it.

      // this next statement cancels default handling in IE8
      event.preventDefault();

      // for other browsers, this is sufficient.
      return false;
   });

   $(formSelector).keypress(function(e) {
      if (e.keyCode == 13) {
         $(formSelector).submit();
      }
   });

   /**
    * When using a stack editor, it needs to be initialized
    *
    * Anything marked as collapsible first needs to be updated, and
    * an event handler is then added to toggle its state on mouse click.
    **/
   $(formSelector).find('.collapsible').each(function () {
      // If collapsed, toggle it
      if ($(this).hasClass('collapsed')) {
         $(this).children().not('legend').not('.stack-row-head').toggle();
      }
      // Add the click handler
      $(this).find('legend').click($(this), function (event) {
         var section = event.data;
         section.children().not('legend').toggle('fast');
      });
      // Add expand/collapse click handler for stack-group's stack-row-head.
      $(this).find('.stack-row-head > .label').click($(this), function (event) {
         var stackDiv = event.data;
         stackDiv.toggleClass('collapsed');
         stackDiv.children().not('.stack-row-head').toggle();
      });
   });

   AfInitStackEditor($(formSelector));
}

/**
 * This onSubmit form validation triggers validation on each fields which
 * are defined in the rules set. If all the validation passed, then it
 * will invoke a given function.
 * @param fun - a function to be called after validation passed.
 * @param params - an optional parameter for to be passed in the function.
 */
Validator.prototype.OnSubmit = function _OnSubmit(fun, params) {
   if (this._jqueryValidator) {
      if (this._jqueryValidator.form()) {
         if (fun) {
            if (params) {
               fun(params);
            } else {
               fun();
            }
         }
         return true;
      } else {
         var errCount = this._jqueryValidator.numberOfInvalids();
         var errMsg = 'You have ' + errCount + ((errCount > 1) ? ' errors' : ' error') + ' in the form!';
         AfError(errMsg);
         this._jqueryValidator.focusInvalid();
         return false;
      }
   } else  {
      AfLog("Unexpected: Validator hasn't initialized yet!");
   }
};

/**
 * Validator.Destruct
 * Cleans up anything this object might have instantiated.
 * @returns null
 */
Validator.prototype.Destruct = function _destruct()
{
   if (this._jqueryValidator) {
      this._jqueryValidator = null;
   }
   return null;
};
