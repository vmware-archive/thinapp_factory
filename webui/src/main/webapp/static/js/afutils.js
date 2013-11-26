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
 * AfLog
 *
 * Print a log message to the JS pop-up console.
 * If the given message is either string or number type, the caller's name is
 * prepended. Otherwise, it will try to print message in JSON string.
 * NOTE: Global log4javascript logger (LOG) is defined in head.vm template.
 *
 * @param message - a message to be logged.
 * @param logLevel - a log level {info, error, or debug (default)}
 * -----------------------------------------------------------------------------
 */
function AfLog(message, logLevel)
{
   if (!LOGGING_ENABLED) {
      return;
   }
   var msg = null;
   if (typeof message === 'string' || typeof message === 'number') {
      msg = 'TAF: ' + message;
   } else {
      // NOTE - JSON.stringify() may not work in some old browsers.
      msg = JSON.stringify(message, replacer);
   }
   if (logLevel === 'info') {
      LOG.info(msg);
   } else if (logLevel === 'error') {
      LOG.error(msg);
   } else {
      LOG.debug(msg);
   }
}


/**
 * -----------------------------------------------------------------------------
 * AfTranslate
 *
 * Utility function to look up a localized string for a given key.
 * @param key the key to lookup
 * @param args Used to replace placeholders: {0}, {1} in the translated text.
 * -----------------------------------------------------------------------------
 */
function AfTranslate(key, args)
{
   var txt = '';

   while (true) {
      txt = VmTAF.translationTable[key.toUpperCase()];

      /* If the translated text is another key, translate again */
      if (txt && txt.match(/[MT]\./)) {
         key = txt;
      }
      else {
         break;
      }
   }

   if (txt) {
      // If args, replace placeholders with the args values passed.
      if (args) {
         if ($.isArray(args)) {
            // array - iterate and replace all place holders
            for (var i = 0; i < args.length; i++) {
               txt = txt.replace("{" + i + "}", args[i]);
            }
        }
        else {
            // string - replace first place holder
           txt = txt.replace("{0}", args);
        }
      }
   }
   else {
      txt = 'NO TRANSLATION: ' + key;
   }

   return txt;
}


/**
 * -----------------------------------------------------------------------------
 * Utility function to be used with JSON.stringify function.
 *
 * @param key - an object key
 * @param value - an object value
 * @returns string format value.
 * -----------------------------------------------------------------------------
 */
function replacer(key, value) {
   if (typeof value === 'number' && !isFinite(value)) {
      return String(value);
   }
   return value;
}


/**
 * -----------------------------------------------------------------------------
 * AfAssert
 *
 * Assert that a condition is true, else report the given message.
 * -----------------------------------------------------------------------------
 */
function AfAssert(condition, message)
{
   if (!condition) alert('ASSERT FAIL: ' + message);
}


/**
 * -----------------------------------------------------------------------------
 * AfAjaxPostJson
 *
 * Wrapper function to POST JSON data using Ajax.
 * -----------------------------------------------------------------------------
 */
function AfAjaxPostJson(url, data, successFunc, errorFunc)
{
   return AfAjax({
      method:        'POST',
      contentType:   'application/json',
      url:           url,
      data:          data,
      success:       successFunc,
      error:         errorFunc
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfAjaxGetJson
 *
 * Wrapper function to GET some JSON data using Ajax.
 * -----------------------------------------------------------------------------
 */
function AfAjaxGetJson(url, successFunc, errorFunc)
{
   return AfAjax({
      method:     'GET',
      url:        url,
      dataType:   'json',
      success:    successFunc,
      error:      errorFunc
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfAjax
 *
 * General purpose wrapper around Ajax.
 * Sorry, but the jQuery ajax function is still a bit verbose. This wrapper
 * cleans up some of the more common tasks.
 * -----------------------------------------------------------------------------
 */
function AfAjax(options)
{
   // Default arguments to the ajax call
   var args = { };

   // Add custom args
   for (optName in options) {
      args[optName] = options[optName];
   }

   // If sending JSON data, convert a map into a string
   if (args['data'] && args['contentType'] == 'application/json') {
      if (args['data'] instanceof Object) {
         args['data'] = JSON.stringify(args['data']);
      }
   }

   // Fix the URL?
   if (args['absUrl']) {
      args['url'] = args['absUrl'];
   }
   else if (!args['url']) {
      alert('no URL for ajax call!');
   }
   else {
      args['url'] = VmTAF.contextPath + args['url'];
   }

   args['type'] = options['method'];

   // Inject an error handler
   var errorCallback = args['error'],
       handleDisconnected = args['handleDisconnected'] || false;
   args['error'] = function _errorIntercept(jqXHR, textStatus, errorThrown) {
      var msg = 'AJAX FAILURE:' +
         ' status=' + jqXHR.status +
         ' statusText=' + textStatus +
         ' error=' + errorThrown +
         ' message=' + jqXHR.responseText;
      AfLog(msg);
      if (AfCheckLicense(jqXHR)) {
         var disconnected = (0 == jqXHR.status);

         if (errorCallback && (!disconnected || handleDisconnected)) {
            errorCallback(jqXHR, textStatus, errorThrown);
         } else if (!VmTAF.unloading) {
            AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown, this);
         }
      }
   };

   // Send create request to AppFactory
   return $.ajax(args);
}

/**
 * -----------------------------------------------------------------------------
 * Check license status
 *
 * @param jqXHR - jQuery Ajax Response
 * @returns true if the response status code (jqXHR.status) is not 420.
 *    Otherwise, it will show an AfError message and return false.
 * -----------------------------------------------------------------------------
 */
function AfCheckLicense(jqXHR)
{
   if (jqXHR && jqXHR.status == VmTAF.licenseExpiredErrorCode) {
      AfError(
         'Expired',
         'This trial version of ' + VmTAF.productName + ' has expired.');
      return false;
   }
   return true;
}

/**
 * -----------------------------------------------------------------------------
 * AfAjaxDefaultErrorHandler
 *
 * General purpose Error handler for ajax error scenarios.
 * This is carved out to be a function of its own, so that it can
 * be reused with custom error handling.
 *
 * @see afutils.AfAjax()
 * -----------------------------------------------------------------------------
 */
function AfAjaxDefaultErrorHandler(jqXHR, textStatus, errorThrown, settings)
{
   if (window.VmTAF.rebooting) {
      // do nothing, because the appliance is known to be temporarily unavailable
      return;
   }

   if (true === $(errorDialog).dialog("isOpen")) {
      return;
   }

   var title = 'AJAX Error',
       errorDialog = '#idAjaxError',
       disconnected = false;

   if (jqXHR) {
      disconnected = (0 == jqXHR.status);
      if (disconnected) {
         title = AfTranslate("T.COMMON.CONNECTION_TITLE");

      } else {
         switch (jqXHR.status) {
            case 400:
               AfError('Error: Invalid Request', jqXHR.responseText);
               return;
            case 403:
               title = 'Error: Forbidden Request';
               break;
            case 404:
               title = 'Error: Resource Not Found';
               break;
            case 500:
               title = 'Server Error';
               break;
            default:
               title = AfTranslate("T.COMMON.SERVER_ERROR");
         }
      }
   }

   $(errorDialog).empty().append(
      $('#ajaxErrorTemplate').render({
         jqxhr: jqXHR,
         settings: settings,
         disconnected: disconnected
      })
   );

   $("html").css({ overflow: 'hidden' });

   $('#idErrorDetailsExpand').button().click(function () {
      $('#idErrorDetailsExpand span.ui-icon').toggle();
      $('#idErrorDetails').toggle();
   });

   $(errorDialog).dialog({
         modal: true,
         title: title,
         width: 600,
         height: 390,
         position:'center',
         resizable: false,
         buttons: {
            Reload: function() {
               location.reload(true);
            }
         },
         beforeClose: function() {
            $("html").css({ overflow: 'auto' });
         }
      }
   );
}


/**
 * -----------------------------------------------------------------------------
 * AfAddOnLoadEvent
 *
 * Register a function to be invoked when the window load is complete.
 * This improves upon window.onload by allowing more than one function to
 * be registered: each function is chained to the previous ones when there
 * is more than one.
 *
 * @param func Function to be invoked on the window.onload event.
 * -----------------------------------------------------------------------------
 */
function AfAddOnLoadEvent(func)
{
   var oldFunc = window.onload;

   if (typeof window.onload != 'function') {
      /* We don't have an onload func yet, so set one */
      window.onload = func;
   }
   else {
      /* We already have an onload, so chain together */
      window.onload = function() {
         if (oldFunc) {
            oldFunc();
         }
         func();
      };
   }
}


/**
 * -----------------------------------------------------------------------------
 * AfBestAppIcon
 *
 * Search all the given icons, and find the one that is
 * closest to the given size.
 *
 * @param icons
 * @param imageSize
 * -----------------------------------------------------------------------------
 */
function AfBestAppIcon(icons, imageSize)
{
   var bestIcon = null;
   var bestDelta = 0;

   for (var i = 0; i < icons.length; i++) {
      var icon = icons[i];
      var delta = Math.abs(icon.size - imageSize);

      if (!bestIcon || delta < bestDelta) {
         bestIcon = icon;
         bestDelta = delta;
      }
   }

   if (!bestIcon) {
      bestIcon = VmTAF.defaultIcon;
   }

   return bestIcon;
}


/**
 * AfGetIconUrl
 *
 * Given a possibly undefined list of icon objects, return the url of an icon.
 * Determines the best icon for the given imageSize, if provided.  Returns the first icon
 * if imageSize is not provided.  if icons is undefined, returns the default icon url.
 */
function AfGetIconUrl(icons, imageSize)
{
   // Start off with the default icon
   var iconUrl = window.VmTAF.defaultIcon.url;

   // Make sure the app has at least one icon
   if (icons && icons.length > 0) {
      var icon = icons[0];

      // Calculate the best fit icon if necessary
      if (imageSize) {
         icon = AfBestAppIcon(icons, imageSize);
      }

      // Use the locally cached icon URL if available
      if (icon.localUrl) {
         iconUrl = window.VmTAF.contextPath + icon.localUrl;
      } else {
         iconUrl = icon.url;
      }
   }
   return iconUrl;
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateApplicationBlock
 *
 * Create a standard DIV which is used to represent an application from its
 * JSON data. This assumes the JSON data contains the usual 'name', 'version',
 * 'description' fields. The result is a block that should look something like:
 *
 * [ICON] *Name*
 * [ICON]  version
 *        /description/
 *
 * For a more compact view, use AfCreateApplicationInline().
 *
 * XXX Add locale and installer revision!
 * @param app
 *
 * desired html
 * -----------------------------------------------------------------------------
 *   <div class="application app-block">
 *       <div class="app-icon">
 *           <img width=${imageSize} height=${imageSize} src="{iconUrl}" alt="">
 *       </div>
 *       <div class="app-details">
 *           <div class="app-name">
 *               <button onclick="{editLink}" class="button-link">
 *                 <span>.NET</span>
 *               </button>
 *           </div>
 *           <div class="app-version">3.5 SP1</div>
 *           <div class="app-desc">Microsoft .NET 3.5 Service Pack 1</div>
 *       </div>
 *   </div>
 * -----------------------------------------------------------------------------
 */
function AfCreateApplicationBlock(app, imageSize, editLink)
{
   var iconUrl = AfGetIconUrl(app.icons);

    if (window.VmTAF.newUI) {
       if (editLink) {
          return [
             '<a href="',
             window.VmTAF.contextPath,
             editLink,
             '">',
             '<img width="',
             imageSize,
             '" height="',
             imageSize,
             '" src="',
             iconUrl,
             '" alt="">&nbsp;',
             app.name,
             '</a>'
          ].join('');
       }
       return [
          '<img width="',
          imageSize,
          '" height="',
          imageSize,
          '" src="',
          iconUrl,
          '" alt="">&nbsp;',
          app.name
       ].join('');
    } else {
       return [
          '<div class="application app-block"><div class="app-icon"><img width="',
          imageSize,
          '" height="',
          imageSize,
          '" src="',
          iconUrl,
          '" alt=""></div><div class="app-details"><div class="app-name"><button onclick="',
          editLink,
          '" class="button-link"><span>',
          app.name,
          '</span></button></div><div class="app-version">',
          app.version,
          '</div><div class="app-desc">',
          app.description.content,
          '</div></div></div>'
       ].join('');
    }
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateApplicationInline
 *
 * Create an element for showing an application "inline", i.e. all on a
 * single row. The result should look something like:
 *
 * [ICON] Name version (locale) [Rev R]
 *
 * For a more 'expanded' view, use AfCreateApplicationBlock().
 *
 * @param app Application to render
 * @param withExtra If true, include locale and installer revision
 * @param iconSize The size of icon to display. Default: VmTAF.iconSizeSmall
 * -----------------------------------------------------------------------------
 */
function AfCreateApplicationInline(app, withExtras, iconSize)
{
   var nameDiv = $('<div></div>').text(app.name);
   nameDiv.addClass('app-name');

   var verDiv = $('<div></div>').text(app.version);
   verDiv.addClass('app-version');

   var detailsDiv = $('<div></div>');
   detailsDiv.addClass('app-details');
   detailsDiv.append(nameDiv).append(verDiv);

   // Add language and revision?
   if (withExtras) {
      if (app.locale) {
         var locDiv = $('<div class="app-desc">').text('('+app.locale+')');
         detailsDiv.append(locDiv);
      }
      if (app.installerRevision) {
         var revDiv = $('<div class="app-desc">').text('['+app.installerRevision+']');
         detailsDiv.append(revDiv);
      }
   }

   // Use the icon size passed, else use the small icon.
   var imgSize = iconSize || window.VmTAF.iconSizeSmall;

   // Application IMG
   var img = $('<img src="' + AfGetIconUrl(app.icons, imgSize) + '">');
   img.width(imgSize).height(imgSize);

   var iconDiv = $('<div></div>').append(img);
   iconDiv.addClass('app-icon');

   // Create the top-level application DIV */
   var element = $('<div></div>');
   element.addClass('application app-inline');
   element.append(iconDiv).append(detailsDiv);

   return element;
}


/**
 * -----------------------------------------------------------------------------
 * AfSimplifyDiskSize
 *
 * Reduce a size in bytes to the same number but with larger units.
 * Keeps reducing until the value is less than 1024 or the units is Tb.
 * -----------------------------------------------------------------------------
 */
function AfSimplifyDiskSize(bytes)
{
   var value = bytes;
   var units = 'b';

   // Reduce scale of absolute free space.
   if (value > 1024) { value /= 1024; units = 'kb'; }
   if (value > 1024) { value /= 1024; units = 'Mb'; }
   if (value > 1024) { value /= 1024; units = 'Gb'; }
   if (value > 1024) { value /= 1024; units = 'Tb'; }
   value = Math.round(value);

   return value + units;
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateProgressBarDivColor
 *
 * Create a DIV element that looks like a progress bar.
 *
 * @see afutils.AfCreateOrUpdateProgressBar for param and return details
 * @depricated use AfCreateOrUpdateProgressBar
 * -----------------------------------------------------------------------------
 */
function AfCreateProgressBarDivColor(text, percent, color1, color1Stop, color2, color2Stop, color3)
{
   return AfCreateOrUpdateProgressBar(
         null, text, percent,
         color1, color1Stop,
         color2, color2Stop,
         color3);
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateProgressBarDiv
 *
 * Create a DIV element that looks like a progress bar.
 * @param text Text to show inside the progress bar.
 * @param percent Percent complete from 0 to 100.
 * @param color is a css class
 * @see AfCreateOrUpdateProgressBar
 * -----------------------------------------------------------------------------
 */
function AfCreateProgressBarDiv(text, percent, color)
{
   return AfProgressBar({
      label: text,
      widthPercent: percent,
      styleClass: color
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateOrUpdateProgressBar
 *
 * Create a DIV element that looks like a progress bar. There are no explicit
 *  error validations for params.
 *
 * @param holderDiv The progress-container if it already exists.
 * @param text Text to show inside the progress bar.
 * @param percent Percent complete from 0 to 100.
 * @param color1 is the color of progress bar (default green)
 * @param color2 is the color of progress bar
 * @param color3 is the color of progress bar
 *    Possible color values are: green, orange, red as defined in template.css
 *
 * @param color1Stop: Progress bar is color1 when 0 < percent <= color1Stop
 * @param color2Stop: Progress bar is color2 when color1Stop < percent <= color2Stop
 *                    Progress bar is color3 when color2Stop < percent <= 100
 * -----------------------------------------------------------------------------
 */
function AfCreateOrUpdateProgressBar(
      holderDiv, text, percent,
      color1, color1Stop,
      color2, color2Stop,
      color3)
{
   var colorClass;
   if (typeof color2Stop === 'number' &&  percent > color2Stop) {
      colorClass = color3;
   }
   else if (typeof color1Stop === 'number' && percent > color1Stop) {
      colorClass  = color2;
   }
   else {
      colorClass = color1;
   }
   colorClass = (typeof colorClass === 'string')? colorClass : 'green';

   var args = {
      holderDiv : holderDiv,
      label: text,
      widthPercent: percent,
      styleClass: colorClass
   };

   return AfProgressBar(args);
/*
   var div = holderDiv;
   var textSpan;
   var barSpan;

   // Create the necessary divs and spans.
   if (!(div && div.hasClass('progress-container'))) {
      div = $('<div></div>').addClass('progress-container');
      textSpan = $('<span>').addClass('progress-text');
      barSpan = $('<span>').addClass('progress-bar');
      div.append(textSpan);
      div.append(barSpan);
   }
   else {
      textSpan = div.find('.progress-text');
      barSpan =  div.find('.progress-bar');
   }
   // Add class at index 0, so it gets precedence for loading.
   if (!barSpan.hasClass(colorClass)){
      barSpan.addClass(colorClass);
   }
   barSpan.width(percent + '%');
   textSpan.text(text);
   return div;
*/
}

/**
 * -----------------------------------------------------------------------------
 * AfProgressBar
 *
 * A utility that generates a progress bar. There are 2 kinds of progress bar:
 * 1. Simple progress bar with text, progress percent and color
 * 2. Staged progress bar with text, color and many stages, with the current
 *    stage showing as an indeterminate progress bar, highlighting the stage
 *    currently in progress.
 *
 * @param args - Contains the following:
 *    args.holderDiv         - Div with class progress-container / new if empty
 *        .label             - Description displayed on progress bar
 *        .widthPercent      - Width of the progress bar in width (0 - 100).
 *        .styleClass        - style class for progress bar (green, orange, red)
 *        .stage.current     - Current stage# (index of widthArray)
 *                             If not set, its computed based on widthPercent.
 *              .widthArray  - Stage widths as array (use size if equal stages)
 *              .size        - Number of stages (equal width per stage)
 *                             (Only one of widthArray / size can be used.)
 * -----------------------------------------------------------------------------
 */
function AfProgressBar(args)
{
   if (!args) {
      return;
   }
   var div = args.holderDiv;
   var textSpan;
   var barSpan;
   var stageDiv;
   var styleClass;

   // Create the necessary divs and spans.
   if (!(div && div.hasClass('progress-container'))) {
      div = $('<div></div>').addClass('progress-container');
      textSpan = $('<span>').addClass('progress-text');
      barSpan = $('<span>').addClass('progress-bar');
      stageDiv = $('<span>').addClass('progress-step');
      div.append(textSpan).append(barSpan).append(stageDiv);
   }
   else {
      textSpan = div.find('.progress-text');
      barSpan =  div.find('.progress-bar');
      stageDiv = div.find('.progress-step');
   }

   // Set the text for the progressBar
   textSpan.text(args.label || '');

   // Set the default styleClass if not passed.
   styleClass = args.styleClass || 'green';

   // Add progress class at index 0, so it gets loading precedence.
   if(!barSpan.hasClass(styleClass)){
      barSpan.addClass(styleClass);
   }

   // Set the progress bar width.
   var widthPercent = (typeof args.widthPercent === 'number')?
         args.widthPercent : 0;
   barSpan.width(widthPercent + '%');

   // If the progress bar needs to display stages, this object is set.
   if(args.stage) {
      // Default to stage 0 (if not passed/cannot be computed).
      var curStage = 0;
      var widthArray = args.stage.widthArray;

      /*
       * If args.stage.size is set, then each stage is equidistant.
       * Hence split it up and calculate each step size and assign to
       * the widthArray.
       */
      if(args.stage.size && typeof args.stage.size === 'number') {
         widthArray = new Array();
         for (var i = 0; i < args.stage.size; i++) {
            widthArray[i] = Math.round((i + 1) / args.stage.size * 100);
         }
      }

      /*
       * At this point, local var widthArray should be set, if not the params
       * are invalid for stage progress bar, and we consider this is as a
       * retular progress bar.
       */
      if(widthArray && $.isArray(widthArray)) {
         // set the curStage if value is a number and is within range.
         if(typeof args.stage.current == 'number'
            && args.stage.current > 0
            && args.stage.current < widthArray.length) {
            curStage = args.stage.current;
         }
         else {
            // Compute the currentStage based on the input width percent.
            for (var i = 0; widthPercent && i < widthArray.length; i++) {
               // If widthPercent = widthArray[i] do not display stage pBar.
               if(widthArray[i] == widthPercent) {
                  break;
               }
               else if(widthArray[i] > widthPercent) {
                  // Set current stage only if the widthPercent is inb/w stages.
                  curStage = i+1;
                  break;
               }
            }
         }

         // Push 0 as the init stage before stage 1 and sort the data.
         widthArray.push(0);
         widthArray.sort(function sortNumberAsc(a, b) {
               return a - b;
            });

         // Create the stage spans to display the stage boundary.
         stageDiv.empty();
         for (var i = 1; i < widthArray.length; i++) {
            span = $('<span>').width((widthArray[i]-widthArray[i-1]) + '%');
            if (curStage == i) {
               span.addClass('step-loading');
               // Set the barSpan for any previously completed stages.
               barSpan.width(widthArray[i-1] + '%');
            }
            stageDiv.append(span);
         }
      }
   }
   // Return the constructed progress div.
   return div;
}


/**
 * -----------------------------------------------------------------------------
 * AfSerializeForm
 *
 * A much better function for serializing a form.
 * This function will convert a form into a Javascript object, which has
 * a property name for each field name. Differences over other functions
 * include:
 *
 * - Creates a direct name->value map, not an array of tuples with name
 *   and value properties.
 *
 * - Checkboxes have a value of "true" or "false" (instead of "on")
 *
 * - Unchecked checkboxes are included
 *
 * - Inputs with the data property 'subProperty' are added to a new
 *   sub-property with that name.
 *
 * - Inputs with no name, or an empty name, are skipped.
 * @param form - a html form to submit
 * @param regexToSkip - a regular expression to skip elements in the process.
 * -----------------------------------------------------------------------------
 */
function AfSerializeForm(form, regexToSkip)
{
   if (!form.find) {
      form = $(form);
   }
   var json = {};
   var regex = new RegExp(regexToSkip, 'i');
   form.find('input,select,textarea').each(
         function() {
            var parent = json;
            var sub = $(this).data('subProperty');
            // Get value of subProperty attribute for the html element.
            if(!sub) {
               sub = $(this).attr('subproperty');
            }
            var isArray = $(this).data('isArray');

            /* JSON property name is 'name' or 'id', whichever */
            var propName = $(this).attr('name') || $(this).attr('id');
            if (!propName) {
               return;
            }

            if (regexToSkip) {
               if (regex.test(this.name)) {
                  return;
               }
            }
            if (sub) {
               /* Property goes into a sub-property: create if needed */
               /* Allow subproperties like "group.subgroup" */
               var subProps = sub.split('.');
               for (var si = 0; si < subProps.length; si++) {
                  var subProp = subProps[si];

                  if (parent[subProp] === undefined) {
                     parent[subProp] = { };
                  }
                  parent = parent[subProp];
               }
            }

            /* Get the value */
            var value = null;
            if ($(this).is(':checkbox')) {
               value = this.checked;
            }
            else if ($(this).is(':radio') && !$(this).is(':checked')) {
               // Ignore radio buttons that are not checked.
               return;
            }
            else {
               // Including ':radio:checked'
               value = $(this).val();
            }

            if (isArray) {
               /* Propery is an array */
               if (!parent[propName]) {
                  parent[propName] = [];
               }
               parent[propName].push(value);
            }
            else {
               /* Property is just a property */
               parent[propName] = value;
            }
         });

   AfLog('Form serialization for ' + form.attr('id'));
   AfLog(json);

   return json;
}


/**
 * -----------------------------------------------------------------------------
 * AfAlertT()
 *
 * Block the entire UI for a message display and internationalizes the title,
 * message. If no message is passed, the title is used up as message.
 *
 * @param titleKey Title translation key.
 * @param messageKey Message translation key.
 * @param args Placeholder replacement value(s) for translation text
 * -----------------------------------------------------------------------------
 */
function AfAlertT(titleKey, messageKey, args)
{
   /* If there's only one argument, it was a message, not a title */
   if (!messageKey) {
      messageKey = titleKey;
      titleKey = null;
   }

   /* Translate and display */
   AfAlert(
      (titleKey ? AfTranslate(titleKey, args) : null),
      (messageKey ? AfTranslate(messageKey, args) : null));
}


/**
 * -----------------------------------------------------------------------------
 * AfAlert()
 *
 * Block the entire UI for a message display using jquery ui dialog as modal.
 * NOTE: Deprecated! Use AfShowAlert whenever possible.
 *
 * @param title to be displayed in the AfAlert dialog.
 * @param message to be displayed in the AfAlert dialog.
 * -----------------------------------------------------------------------------
 */
function AfAlert(title, message)
{
   alert(message);

   // TODO: Use AfShowMessageDialog(...), if we can figure out how to
   // make that function block.
}


/**
 * -----------------------------------------------------------------------------
 * AfShowAlert()
 *
 * Block the entire UI for a message display using jquery ui dialog as modal.
 *
 * @param title to be displayed in the AfError dialog.
 * @param message to be displayed in the AfError dialog.
 * -----------------------------------------------------------------------------
 */
function AfShowAlert(title, message)
{
   AfShowMessageDialog({
      title: title,
      message: message,
      buttons: [ "OK" ]});
}


/**
 * -----------------------------------------------------------------------------
 * AfShowMessageDialog()
 *
 * Show a simple message dialog, consisting of a title, a message, and
 * optionally some buttons.
 *
 * If buttons are not specified, a simple "OK" button is added by default, which
 * closes the dialog when pressed.
 *
 * If buttons are provided, the specified callback is invoked with the index of
 * the button that is pressed. If the callback returns 'true', the dialog will
 * close. If no callback is specified, the dialog will close no matter which
 * button is pressed.
 *
 * NOTE: This function returns immediately: it does not wait for user input.
 *
 * @param args Object containing the following arguments:
 *    title Dialog title
 *    message Dialog content.
 *    buttons Array of labels for buttons.
 *    callback Invoked when a button is pressed. Function is passed the
 *             index of the button that was pressed, and must return 'true'
 *             to close the dialog or 'false' to keep it open.
 * -----------------------------------------------------------------------------
 */
function AfShowMessageDialog(args)
{
   var buttons = [];

   if (!args.buttons) {
      /* Add a default "OK" button */
      buttons.push({
         text: "OK",
         click: function(event) {
            d.dialog("close");
         }
      });
   }
   else {
      /* Add a button for each specified label */
      for (var i = 0; i < args.buttons.length; i++) {
         buttons.push({
            text: args.buttons[i],
            click: function(event) {
               var text = event.target.textContent || event.target.innerText;
               var index = $.inArray(text, args.buttons);
               if (!args.callback || args.callback(index)) {
                  d.dialog("close");
               }
            }
         });
      }
   }

   /* Create the modal dialog */
   var d = $('<div class="modal-data-holder"></div>').html(args.message).dialog({
      title: args.title,
      modal: true,
      buttons: buttons
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfConfirmT()
 *
 * Block the entire UI for a confirmation using jquery ui dialog as modal.
 *
 * @param titleKey Translation key of title to be displayed in the dialog.
 * @param message Translation key of message to be displayed in the dialog.
 * @param args Placeholder replacement value(s) for translation text
 * -----------------------------------------------------------------------------
 */
function AfConfirmT(titleKey, messageKey, args)
{
   /* If there's only one argument, it was a message, not a title */
   if (!messageKey) {
      messageKey = titleKey;
      titleKey = null;
   }

   /* Translate and display */
   return AfConfirm(
      (titleKey ? AfTranslate(titleKey, args) : null),
      (messageKey ? AfTranslate(messageKey, args) : null));
}


/**
 * -----------------------------------------------------------------------------
 * AfConfirm()
 *
 * Block the entire UI for a confirmation using jquery ui dialog as modal.
 * TODO: Replace confirm() with something nicer. See AfShowMessageDialog.
 *
 * @param title to be displayed in the dialog.
 * @param message to be displayed in the dialog.
 * -----------------------------------------------------------------------------
 */
function AfConfirm(title, message)
{
   return confirm(message);
}


/**
 * -----------------------------------------------------------------------------
 * AfPrompt()
 *
 * Block the entire UI and prompt the user to enter some text.
 * TODO: Replace prompt() with something nicer. See AfShowMessageDialog.
 *
 * @param title to be displayed in the dialog.
 * @param message to be displayed in the dialog.
 * -----------------------------------------------------------------------------
 */
function AfPrompt(title, message, defaultText)
{
   if (!defaultText) {
      defaultText = "";
   }

   var ans = prompt(message, defaultText);
   return (ans ? ans : null);
}


/**
 * -----------------------------------------------------------------------------
 * AfRefreshFlagStart()
 * AfRefreshFlagSuccess()
 * AfRefreshFlagError()
 *
 * These three functions are used when dynamically updating data. Any refresh
 * indicator on the page is configured appropriately: shown when starting,
 * hidden when succeeded, and highlighted when failed.
 * -----------------------------------------------------------------------------
 */
function AfRefreshFlagStart()
{
   $('.refresh-indicator').each(function() {
      $(this).removeClass("error");
      $(this).slideUp(1).delay(1000).slideDown(1);
   });
}


function AfRefreshFlagSuccess()
{
   $('.refresh-indicator').each(function() {
      $(this).stop(true, true).hide();
   });
}


function AfRefreshFlagError()
{
   $('.refresh-indicator').each(function() {
      $(this).addClass("error");
      $(this).stop(true, true).show();
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfPluralOf()
 *
 * Return the plural version of a noun.
 * planet -> planets
 * mess -> messes
 * watch -> watches
 * bash -> bashes
 * TODO: cactus -> cacti
 * -----------------------------------------------------------------------------
 */
function AfPluralOf(noun, count)
{
   if (count && count == 1) {
      return noun;
   }

   if (noun.match('s$'))
      return noun + 'es';

   if (noun.match('ch$') || noun.match('sh$'))
      return noun + 'es';

   return noun + 's';
}


/**
 * -----------------------------------------------------------------------------
 * AfThisOrThese()
 *
 * Return either "no things", "this thing" or "these N things", depending on
 * the length of the 'items' array. The noun should be singular.
 * -----------------------------------------------------------------------------
 */
function AfThisOrThese(items, noun)
{
   if (items.length == 0) {
      return 'no ' + AfPluralOf(noun);
   }
   else if (items.length == 1) {
      return 'this ' + noun;
   }
   else {
      return 'these ' + items.length + ' ' + AfPluralOf(noun);
   }
}


/**
 * -----------------------------------------------------------------------------
 * AfHtml()
 *
 * Convert a jQuery object into HTML. Note that unlike the $.html() function,
 * this returns the element itself, not just its contents.
 * -----------------------------------------------------------------------------
 */
function AfHtml(element)
{
   return $('<div></div>').append(element).html();
}


/**
 * -----------------------------------------------------------------------------
 * AfNotifyT()
 *
 * A helper function to throw notification popups to the top right of the page.
 * This is an i18n wrapper around AfNotify().
 *
 * @args translation text's placeholder replacement value(s)
 * @see AfNotify for the remaining params.
 * -----------------------------------------------------------------------------
 */
function AfNotifyT(title, message, divClass, args)
{
   if (message) {
      message = AfTranslate(message, args);
   }
   if (title) {
      title = AfTranslate(title, args);
   }
   AfNotify(title, message, divClass);
}


/**
 * -----------------------------------------------------------------------------
 * AfNotify()
 *
 * A helper function to throw notification popups to the top right of the page.
 * These can work like a stack and allows multiple notifications.
 *
 * @param title (options) displays as line 1 of notify in bold.
 * @param message
 * @param divClass (optional values: warn, error)
 *        <default> - default is success mode (green text, check icon).
 *          info    - default is info mode (blue text, check icon).
 *          warn    - display in warning mode (orange text, alert icon).
 *          error   - display in error mode (red text, error icon).
 * -----------------------------------------------------------------------------
 */
function AfNotify(title, message, divClass)
{
   if (!message) {
      // nothing to notify, return.
      return;
   }
   var titleSpan = '';
   if (title) {
      titleSpan = $('<div></div>').addClass('title');
      titleSpan.append(title);
   }

   // Log this message.
   AfLog(titleSpan + message);

   // Create the close div, on click, hide notify div and then destroy it.
   var closeDiv = $('<div></div>').addClass('close');
   closeDiv.click(function() {
      $(this).parent().fadeTo(500, 0.3).slideUp(400).delay(1).queue(function() {
         $(this).remove();
      });
   });

   // Create the message div and populate msg and title.
   var msgDiv = $('<div></div>').addClass('msg');
   msgDiv.append(titleSpan).append(message);

   // Create the div containing the notify and append icon, msg, close divs
   var notify = $('<div></div>').addClass('notify');
   notify.append( $('<div></div>').addClass('icon') )
      .append(msgDiv)
      .append(closeDiv)
      .append( $('<div></div>').addClass('clearDiv') );
   if (divClass) {
      notify.addClass(divClass);
   }

   // Set a timeout of 5000ms to auto hide and destroy.
   setTimeout( function() {
      notify.fadeTo(500, 0.3).slideUp(800).delay(1)
         .queue(function() {
            $(this).remove();
         });
   }, 5000);

   // Show a slide-down effect once displayed on page.
   notify.hide();
   $('#notify-holder').prepend(notify);
   notify.slideDown(800);
}


/**
 * -----------------------------------------------------------------------------
 * AfCheckPopupBlockerAndOpenUrlInNewWindow()
 * A helper function to detect browser popup blocker. It will load the url in a
 * new window if there is no popup blocker. Otherwise, it will show an alert message
 * to disable the popup blocker.
 *
 * @param url - a URL to load in a new window.
 * @param name - a window name.
 * @param opt_param - an optional param for the window.open(...,...,opt_param)
 * -----------------------------------------------------------------------------
 */
function AfCheckPopupBlockerAndOpenUrlInNewWindow(url, name, opt_param)
{
   var popup = window.open(url, name, opt_param);

   // Detect popup blocker
   setTimeout(function() {
       if(!popup || popup.closed || parseInt(popup.innerWidth) == 0) {
           // Close the window that is hidden behind Chrome's popup blocker
           // This is useful because you can then reclaim the name of the window that is being hidden
           // Without this, window.open(..., name_of_popup, ...) will be blocked because it is currently hidden by Chrome
           popup && popup.close();
           AfErrorT('T.COMMON.POPUP_BLOCKER',
           'M.COMMON.POPUP_BLOCKER');
       } else {
          if(window.focus) {
             popup.focus();
          }
       }
   }, 500);
}


/**
 * -----------------------------------------------------------------------------
 * AfTimeSpan()
 *
 * Returns a formatted string that describes a length of time.
 *
 * @param seconds number of seconds of time span
 * @return a formatted string representing the length of time for the given number
 *         of seconds
 * -----------------------------------------------------------------------------
 */
function AfTimeSpan(seconds)
{
   var SECONDS_PER_MINUTE = 60;
   var SECONDS_PER_HOUR = 3600;
   var SECONDS_PER_DAY = 86400;

   var days = Math.floor(seconds / SECONDS_PER_DAY);
   seconds -= SECONDS_PER_DAY * days;
   var hours = Math.floor(seconds / SECONDS_PER_HOUR);
   seconds -= SECONDS_PER_HOUR * hours;
   var minutes = Math.floor(seconds / SECONDS_PER_MINUTE);
   seconds -= SECONDS_PER_MINUTE * minutes;

   // trivial, just adds 's'
   function pluralize(val, singlularString) {
      return (val === 1) ? [ val, ' ', singlularString].join('')
                         : [ val, ' ', singlularString, 's'].join('');
   }

   return [
      (days > 0) ? pluralize(days, 'day') : '',
      (hours > 0) ? pluralize(hours, 'hour') : '',
      pluralize(minutes, 'minute'),
      pluralize(seconds, 'second')
   ].join(' ');
}

/**
 * -----------------------------------------------------------------------------
 * AfOpenPopupWindow
 *
 * Open url in a pop-up window.
 *
 * @param url - an url.
 * @param name - a window name.
 * @returns {Boolean}
 * -----------------------------------------------------------------------------
 */
function AfOpenPopupWindow(url, name)
{
   // Extract ticketId from the url to uniquely launch VMRC client pop-up.
   // E.g. url => '/manualMode/index?appId=347&ticketId=34'
   var pos = url.lastIndexOf('=') + 1;
   var id = url.substring(pos);
   AfLog('Loading '+ url + ' with window name - ' + name + id);
   var w = 895;
   var h = 840;
   var left = (screen.width/2)-(w/2);
   var top = (screen.height/2)-(h/2);
   var opt_param = 'height='+h+',width='+w+',resizable=1,top='+top+',left='+left;

   AfCheckPopupBlockerAndOpenUrlInNewWindow(url, name + id, opt_param);
   return false;
}


/**
 * -----------------------------------------------------------------------------
 * AfPollProgressAndUpdate()
 *
 * Polls for upload status from: /api/upload/{uploadId}/progress
 *
 * @param progressDiv - Div that displays the progress.
 * @param uploadId - Uniquely identify the ProgressListener.
 * @param pollFrequency - Frequency of updating file upload status (Default .4s)
 * @param failRetryCount - Number of retry when no progress response (Default 80)
 *            (80 times) - This is high as the server may take time to initialize progressListener
 * -----------------------------------------------------------------------------
 */
function AfPollProgressAndUpdate(progressDiv, uploadId, pollFrequency, failRetryCount)
{
   // The timeouts and stopPollCounter should atleast addup to 2seconds for server to initialize.
   var pollTime = (typeof pollFrequency === 'number')? pollFrequency : 400;
   var counter = (typeof failRetryCount === 'number') ? failRetryCount : 80;

   // Update progress while progressDiv and uploadId exists.
   if (!progressDiv || !uploadId) {
      return;
   }

   // Send progress polling request if upload not complete.
   AfAjax({
      method: 'GET',
      url: '/api/upload/' + uploadId + '/progress',
      contentType: 'application/json',
      async: true,

      success: function(response) {
         // Check for valid response to exist.
         if (response && response.current) {
            var display = 'Upload: ' + response.percentDone + '% ('
                  + AfSimplifyDiskSize(response.current) + ' of '
                  + AfSimplifyDiskSize(response.total) + ' total)';

            // Check if upload complete, but server is busy storing the installer
            if (response.percentDone == 100 && !response.finished) {
               display += ' Saving file...';
               progressDiv.parent().siblings('#saveImgLoading').show();
               // Now only the file is getting saved, no need for further updates.
               counter = 0;
            }

            // Update the progress of the upload.
            if (progressDiv) {
               // Remove the initial static progress bar and replace with dynamic progress.
               progressDiv.find('.progress-step').remove();
               AfCreateOrUpdateProgressBar(progressDiv, display, response.percentDone);
            }
         }
         else {
            // No progress available.
            AfLog('Upload progress is empty');
            counter--;
         }
      },

      error: function(jqXHR, textStatus, errorThrown) {
         // Either something messed up or the upload is over
         counter = 0;
      },

      // Call the same method again with possibly updated counter value.
      complete: function() {
         if (counter > 0) {
            setTimeout(function() {
               AfPollProgressAndUpdate(progressDiv, uploadId, pollTime, counter);
            }, pollTime); // End setTimeout()
         }
      }
   });
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateInputText
 *
 * Create an input text element and add attributes.
 *
 * @param input - a name-value pair json.
 * @returns an input element.
 * -----------------------------------------------------------------------------
 */
function AfCreateInputText(input)
{
   var element = $('<input type="text" value="' + input.value + '" />');
   return AfAddAttributes(element, input);
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateInputCheckbox
 *
 * Create input checkbox element and add attributes.
 *
 * @param input - a name-value pair json.
 * @returns an input element.
 * -----------------------------------------------------------------------------
 */
function AfCreateInputCheckbox(input)
{
   return AfAddAttributes($('<input type="checkbox"/>'), input);
}


/**
 * -----------------------------------------------------------------------------
 * AfCreatePulldown
 *
 * Creates a pulldown HTML element that can contain option groups (a SELECT
 * that optionally contains OPTGROUPs which in turn contain OPTIONs).
 *
 * @param args     - a key-value pair json containing the following.
 *    id           - Id of the select element.
 *    name         - Name of select element.
 *    value        - Initially selected option.
 *    subproperty  - subproperty value to be associated to the pulldown.
 *    ...          - @see afutils.AfAddAttributes params that apply here.
 *    options      - List of EachOptions (SingleOption / GroupOption)
 *       disabled     - Disabled flag: true/false indicating if option is disabled.
 *       display      - Values for the options.
 *       translate    - Translate display value before display.
 *       key          - value to be used for option.
 *       '@class'     - differentiating GroupOption vs SingleOption.
 *       id           - id for the option/ optiongroup element.
 *      *options      - Nested option group as above.
 *
 * @see SelectOptions, EachOption, GroupOption, SingleOption classes
 * @returns the select element.
 * -----------------------------------------------------------------------------
 */
function AfCreatePulldown(args)
{
   var select = $('<select></select>');

   // Recursively add option / optgroup
   for (var gi = 0; gi < args.options.length; gi++) {
      var optElement = AfCreatePulldownOption(args.value, args.options[gi]);
      select.append(optElement);
   }
   return AfAddAttributes(select, args);
}


/**
 * -----------------------------------------------------------------------------
 * AfCreatePulldownOption
 *
 * Creates a recursive optionGroup/options for pulldowns. If input contains
 * another optionGroup, call this function recursively. Returns a html option
 * or optiongroup element.
 *
 * @param selectedValue the option that should be selected
 * @param option representing one of EachOption types (SingleOption/GroupOption)
 * @see AfCreatePulldown.options for further details.
 * -----------------------------------------------------------------------------
 */
function AfCreatePulldownOption(selectedValue, option)
{
   var optElement;
   var display = (option.translate)? AfTranslate(option.display) : option.display;
   // Check if the option is an optionGroup or an option.
   if(option['@class'] == 'GroupOption' || option.options) {
      optElement = $('<optgroup></optgroup>').attr('label', display);
      for (var oi = 0; option.options && oi < option.options.length; oi++) {
         var eachOption = AfCreatePulldownOption(selectedValue, option.options[oi]);
         optElement.append(eachOption);
      }
   }
   else {   // SingleOption, set values.
      var key = (option.key)? option.key : option.display;
      optElement = $('<option></option>').val(key).text(display);
      // Check if the disabled attribute is to be set, else check if selected should be set.
      if(option.disabled) {
         optElement.attr('disabled', true);
      }
      else if (key == selectedValue) {
         optElement.attr('selected', true);
      }
   }
   // Add anymore common attributes to options.
   return AfAddAttributes(optElement, option);
}


/**
 * -----------------------------------------------------------------------------
 * AfAddAttributes
 *
 * Add the following attributes to the given element.
 * - name, id, placeholder, validate (jQuery validation rule) and extra custom
 * attribute.
 * NOTE: Setting the 'value' attribute via element.attr('value', xx) or element.val(xx)
 *  does not work in FF4.
 * @param element - a DOM element.
 * @param input - a name-value pair json.
 * @returns the given element.
 * -----------------------------------------------------------------------------
 */
function AfAddAttributes(element, input)
{
   var attrs = ['name', 'id', 'placeholder', 'validate' , 'size', 'cssClass', 'isArray', 'subproperty'];
   $.each(attrs, function(index, name){
      var attr_val = input[name];
      if (attr_val) {
         if (name == 'cssClass') element.addClass(attr_val);
         else element.attr(name, attr_val);
      }
   });

   if (input.extra) {
      element.attr(input.extra.name, input.extra.value);
   }

   return element;
}


/**
 * -----------------------------------------------------------------------------
 * AfPromptForUniqueName
 *
 * Ask the user for a "name". If the validateUniqueCallback is passed, the
 * name is validated for uniquness, and if not unique, it asks for another.
 * Keeps doing this until the user gives up, or enters a unique "name".
 *
 * @param validateUniqueCallback - call back to validate uniqueness.
 * @param validateUniqueParam - params for validate callback.
 * -----------------------------------------------------------------------------
 */
function AfPromptForUniqueName(validateUniqueCallback, callbackParam)
{
   var newName = null;
   do {
      /* Ask for name */
      var newName = AfPrompt(
         'Enter Name',
         'Enter a unique Name');

      /* Did the user give up? */
      if (!newName) {
         return null;
      }
      /* Invoke callback to check uniqueness, If name in use, start over. */
      if ($.isFunction(validateUniqueCallback)
            && !validateUniqueCallback(newName, callbackParam)) {
         AfError(
            'Name In Use',
            'Name: "' + newName + '" exists already, enter another.');
         newName = null;
      }
   } while (!newName);

   return newName;
}


/**
 * -----------------------------------------------------------------------------
 * AfCreateDataSourceSpan
 *
 * Create a span that includes an icon corresponding to the datasource type
 * and a text label of 'name'.
 *
 * @param type - One of "feed" or "fileshare"
 * @param name - Optional name to add next to icon.
 * -----------------------------------------------------------------------------
 */
function AfCreateDataSourceSpan(type, name)
{
    var srcImgHtml = [
      '<span class="data-source-icon data-source-type-',
      type,
      '"></span>'
    ].join('');

    return [
      '<span>',
      srcImgHtml || '',
      name || '',
      '</span>'
    ].join('');
}

/**
 * -----------------------------------------------------------------------------
 * AfSplitServerShare(json)
 *
 * Convert a single 'serverShare' property in the given JSON object into two
 * properties: 'server' for the server name and 'share' for the path.
 * NOTE: 'share' will *not* contain a leading slash.
 * -----------------------------------------------------------------------------
 */
function AfSplitServerShare(json)
{
   if (!json.serverShare) {
      return;
   }
   // Remove leading '\\' from the server share.
   json.serverShare = json.serverShare.replace(/^\\\\/, "");
   // Replace all '/' with '\'.
   json.serverShare = json.serverShare.replace(/\//g, "\\");
   var pos = json.serverShare.indexOf('\\');
   switch (pos) {
      case -1:
         // "server";
         json.server = json.serverShare;
         json.share = "";
         break;
      case 0:
         // "path/sub"
         json.server = "";
         json.share = json.serverShare.substring(1);
         break;
      default:
         // "server/path"
         json.server = json.serverShare.substring(0, pos);
         json.share = json.serverShare.substring(pos+1);
         break;
   }
   delete json.serverShare;
};


/**
 * -----------------------------------------------------------------------------
 * CheckVmrcSupportedBrowser
 *
 * Check whether the browser type is supported by VMRC client.
 * VMRC only has plug-ins for FireFox.
 * @returns {Boolean}
 * -----------------------------------------------------------------------------
 */
function CheckVmrcSupportedBrowser()
{
   var browserType = $.browser;
   // TODO: find out supported versions for FF, and add the version check.
   return browserType.mozilla || browserType.msie;

}


/**
 * -----------------------------------------------------------------------------
 * hideNthRowAddLinkToExpandCollapse
 *
 * Given a ul containing a bunch of children li, if there are more than n li
 * elements, hide all elements after nth li, add a new li with a click handler
 * to show the rest. When the hidden rows are visible, a click will collapse
 * it back to its nth size.
 *
 * @param args.ulElementId      - #id for the ul element.
 *            .visibleRowCount  - Min number of rows to show when collapsed.
 *            .linkTextExpand   - Collapse link text (Default: "See More...")
 *            .linkTextCollapse - Expand link text (Default: "See Fewer...")
 *            .showHiddenCount  - (optional) display hidden row count as (x).
 * -----------------------------------------------------------------------------
 */
function hideNthRowAddLinkToExpandCollapse(args)
{
   if (!args || !args.ulElementId || !args.visibleRowCount) {
      AfLog('Missing key input data');
      return;
   }
   var ulElement = $('ul' + args.ulElementId);
   if (!ulElement) {
      AfLog('Invalid ul element id: ' + args.ulElementId);
      return;
   }
   var expandedText = args.linkTextExpand || AfTranslate('T.COMMON.VIEW_MORE');
   var collapseText = args.linkTextCollapse || AfTranslate('T.COMMON.VIEW_LESS');

   var liStr = 'li:gt(' + (args.visibleRowCount - 1) + ')';
   // Now find rows beyone visibleRowCount rows, hide them, and add click handler.
   var rows = ulElement.find(liStr);

   if (rows && rows.length > 0) {
      if (args.showHiddenCount) {
         expandedText += ' (' + rows.length + ')';
      }
      var liClickable = $('<li class="expander button-link">')
         .append('<div><span class="ui-icon ui-icon-triangle-1-e"></span>'  + expandedText + '</div>'
            + '<div style="display:none;"><span class="ui-icon ui-icon-triangle-1-s"></span>' + collapseText + '</div>')
         .click(function(event) {
            $(this).parent().find(liStr).not('.expander').slideToggle();
            $(this).find('div').toggle();
         });
      // Hide the rows > visibleRowCount and add the expand/collapse li.
      rows.hide();
      liClickable.insertAfter(ulElement.find('li:nth-child(' + args.visibleRowCount + ')'));
   }
}


/**
 * Initializes a jquery dialog which is loaded asynchronously on a link or button click.
 *
 * @param idDialog
 * HTML id of a <div> placeholder which will receive the contents of the dialog.
 *
 * @param idButton
 * Button which will, when clicked, open the modal dialog.
 *
 * @param url
 * URL to load the html contents of the dialog from.  This content will be loaded when
 * the dialog is opened.
 *
 * @param title
 * The title of the dialog.
 *
 * @param button - customization of buttons.
 *              .OK - Customizations for button OK
 *              .CANCEL - Customizations for button cancel.
 *              .***.text - Alternate button text
 *
 * TODO - 1. Implement a lot of flexible params that lack in this implementation.
 *        2. GET rid of global onTafDialogOk function.
 */
function AfMakeAsyncDialog(idDialog, idButton, url, title, button) {
   var dialogOpts = {
      title: title,
      modal: true,
      autoOpen: false,
      width:600,
      minHeight: 300,
      open: function() {

         window.VmTAF.dialogHolder = $(idDialog);

         $(idDialog).load(url, null, function() {
            // when loaded, focus on first input element
            $(idDialog + " :input:visible:enabled:first").focus();
         });
      },
      close: function() {
         window.VmTAF.dialogHolder = null;
      },
      buttons: [
         {
            text: (button && button.OK && button.OK.text)? button.OK.text : "Ok",
            id: "idDialogOk",
            click: function() {
               if ($(this).prop('disabled')) {
                  return; // If disabled, do nothing.
               }
               if (window.onTafDialogOk && typeof window.onTafDialogOk === "function") {
                  window.onTafDialogOk($(this));
               } else {
                  $(this).dialog("close");
               }
            }
         },
         {
            text: (button && button.CANCEL && button.CANCEL.text)? button.CANCEL.text :"Cancel",
            id: "idDialogCancel",
            click: function() {
               if (window.onTafDialogCancel && typeof window.onTafDialogCancel === "function") {
                  window.onTafDialogCancel($(this));
               } else {
                  $(this).dialog("close");
               }
            }
         }
      ]
   };
   $(idButton).click(function(event) {
      event.preventDefault();
      $(idDialog).dialog(dialogOpts);
      $(idDialog).dialog('open');
      return false;
   });
}
function AfRefreshTimestamps() {
   jQuery.each(this.find('.timeago'), function(idx,el) {
      if (el.title) {
         $(el).text(AfTimelabel(el.title));
      }
   });
}
function AfInitStackEditor(root) {
   if (!window.VmTAF.newUI) {
      return;
   }
   root.find('button').button();
   root.find('.stack-editor').addClass("ui-widget");
   root.find('.stack-header').addClass("ui-widget-header ui-corner-top");
   root.find('.stack-editor form').addClass("ui-widget-content");
   var firstTry = root.find('.stack-editor form').addClass("ui-widget-content");
   if (firstTry && firstTry.size() > 0) {
      firstTry.addClass("ui-widget-content");
   } else {
      root.find('.stack-group').addClass("ui-widget-content");
   }
   root.find('.stack-footer').addClass("ui-widget-header ui-corner-bottom");
   root.find('.stack-footer button:first').addClass("ui-priority-primary");
//   root.find('.stack-row-head').addClass("ui-state-highlight");

   // also, schedule a timer that runs each minute to update the timeago timestamps
   setInterval(jQuery.proxy(AfRefreshTimestamps,root), 60000);
}

function AfTimelabel(timestamp) {
   if (timestamp <= 0) {
      return "Never";
   } else {
      var date = new Date(); // note: the Date(number) constructor won't work if timestamp is a string
      date.setTime(timestamp);
      return jQuery.timeago(date);
   }
}
/**
 * Given a timestamp in UTC milliseconds since the epoch,
 * returns a localized "fuzzy timestamp", such as:
 *  "1 minute ago"
 *  "4 days ago"
 *
 * For more info, see http://timeago.yarp.com/
 *
 * This timestamp is live, and updates automatically. Use on paginated datatables causes inacturate display.
 *
 * NOTE: This does not work well when we use pagination and the AfTimeAgo() will only update the visible records.
 * After the update, if the user were to navigate to a different page, the time displayed will be incorrect. This
 * results in an unsorted timestamp value. For more details see TAF bug: #857164
 *
 * @param timestamp  milliseconds since 1970/01/01 UTC
 */
function AfTimeago(timestamp) {
   return [
      '<span class="timeago" title="',
      timestamp,
      '">',
      AfTimelabel(timestamp),
      '</span>'
      ].join('');
}


/**
 * A variable to lazily detect browser plugins.
 *
 * @see http://www.ifadey.com/2011/09/browser-plugin-detection/
 */
var Plugin = {
   /**
    * Format: { name: ActiveXObject's ProgID }
    * Use http://www.nirsoft.net/utils/axhelper.html to lookup ProgIDs of
    * the IE plugins.
    */
   activeXObjects : {
      flash: "ShockwaveFlash.ShockwaveFlash",
      silverlight: "AgControl.AgControl",
      quicktime: "QuickTime.QuickTime",
      // NOTE: Need to update this for any VMRC plugin major version upgrade!
      vmrc: "VMware.VMwareRemoteConsole.4.0"
   },

   checkIE: function() {
      var browserType = $.browser;
      if( browserType.msie ) {
         this.isIE = true;
      }
      else {
         this.isIE = false;
      }
   },

   detect: function( name ) {
      if( !this.isIE ) {
         this.detect = this.detectPlugin;
      }
      else {
         this.detect = this.detectActiveX;
      }

      return this.detect( name );
   },

   detectPlugin: function( name ) {
      // unsupported browser
      if ( !navigator.plugins ) {
         return false;
      }
      name = name.toLowerCase();

      for( var i = 0; navigator.plugins[ i ]; ++i ) {
         if( navigator.plugins[ i ].name.toLowerCase().indexOf( name ) > -1 )
            return true;
      }

      return false;
   },

   detectActiveX: function( name ) {
      try {
         new ActiveXObject( name );
         return true;
      } catch( e ) {
         return false;
      }
   }
};

/**
 * Check whether the browser has VMRC plugin or not.
 * @returns true if the plugin is installed; otherwise, return false.
 */
function HasVMRCPlugin() {
   Plugin.checkIE();

   if (Plugin.isIE) {
      return Plugin.detect(Plugin.activeXObjects.vmrc);
   } else {
      return Plugin.detect("VMware Remote Console");
   }
}

/**
 * -----------------------------------------------------------------------------
 * AfError
 *
 * Create an inline error message box if it's called from a dialog. Otheriwse,
 * it renders the error message using jQuery modal dialog.
 * NOTE: This error message box doesn't stack up if it's called multiple times.
 * Instead, the newer one will override.
 * If needed, changing errDiv.html(html) to errDiv.prepend(html) will enable
 * a stack-up.
 * @param title a message dialog title.
 * @param message an error message to be displayed.
 * -----------------------------------------------------------------------------
 */
function AfError(title, message) {
   if (!message) {
      message = title;
      title = 'Error';
   }
   var errDiv = $('.inline-error');
   // check whether the error-div exists
   if (errDiv.length) {
      // 'validationErrTemplate' is defined in error-dialog.vm
      var html = $('#validationErrTemplate').render({ 'Msg' : message });
      errDiv.html(html);
      errDiv.slideDown(800);
      var closeIcon = $('#close-icon');
      closeIcon.click(function() {
         $(this).parent().parent().fadeTo(500, 0.3).delay(1).queue(function() {
            $(this).remove();
         });
      });
   } else {
      AfShowAlert(title, message);
   }
}

/**
 * Translate error message and show it in jQuery modal dialog.
 *
 * @param titleKey a title key of resource bundle.
 * @param messageKey a message key of resource bundle.
 * @param args optional arguments.
 */
function AfErrorT(titleKey, messageKey, args) {
   /* If there's only one argument, it was a message, not a title */
   if (!messageKey) {
      messageKey = titleKey;
      titleKey = null;
   }

   /* Translate and display */
   AfError(
         (titleKey ? AfTranslate(titleKey, args) : null),
         (messageKey ? AfTranslate(messageKey, args) : null));
}

/**
 * Trim the given string and hide any overflow in a tooltip.
 *
 * @param str an input string
 * @param max a maximum length of the text to be displayed (optional)
 * @returns a label with str element if the length is within the limit.
 *    Otherwise, it returns a label (XXXX...) with a tooltip to see the rest of the text.
 */
function AfOverflowToTooltip(str, max) {
   if (!str || str.length < 1) {
      return null;
   }
   var limit = 45; // default limit
   if (max && max > 0) {
      limit = max;
   }

   if (str.length <= limit) {
      return $('<label>  ' + str + '</label>');
   } else {
      var displayTxt = str.substring(0, limit);
      var tooltipTxt = str.substring(limit);
      return $("<label>  " + displayTxt + "<a href='#' title='"+ tooltipTxt + "'>...</a></label>");
   }
}

/**
 * Check browser compatibility and VMRC plugin status. If both are passed, then
 * launch the url in a pop-up window. Otherwise, it will show the error messages.
 *
 * @param url a manual capture url
 */
function AfOpenManualCaptureWindow(url) {
   if (!CheckVmrcSupportedBrowser()) {
      AfErrorT('T.APPS.MM.BROWSER_NOT_SUP', 'M.APPS.MM.BROWSER_NOT_SUP');
   } else if (!HasVMRCPlugin()) {
      AfErrorT('T.APPS.MM.MISSING_PLUGIN', 'M.APPS.MM.MISSING_VMRC_PLUGIN');
   } else {
      AfOpenPopupWindow(url, 'Manual_Capture');
   }
}


/**
 * Given a URL, return the last non-empty component of the URI, after removing
 * any query strings or hash tags.
 *
 * examples:
 *    http://www.google.com/download.exe   => download.exe
 *    http://www.google.com/download/      => download
 *    http://www.google.com/download       => download
 *    http://www.google.com/               => www.google.com
 *    http://www.google.com                => www.google.com
 *    http://www.google.com/foo/bar/download.exe   => download.exe
 *    http://www.google.com/foo/bar/download.exe#32   => download.exe
 *    http://www.google.com/foo/bar/download.exe?foo=bar   => download.exe
 *    http://www.google.com/foo/bar/download.exe?foo=bar#32   => download.exe
 *    /////????### => /////????###
 *
 * @param uri  The URI to parse
 *
 * @return the last non-empty component, per the above rules.
 * If such a component does not exist, returns the input string.
 */
function AfShortNameFromUri(uri) {

   var ary = uri.split('/');

   ary = $.map(ary, function(result) {
      // filter out anything after ? or #
      var idx = result.search('[\\?#]');
      return (-1 == idx) ? result : result.substring(-1,idx);
   });

   // remove empty elements from ary
   ary = $.grep(ary,function(n){
      return(n);
   });

   // return the last element, or if the array is empty for some reason,
   // return the input
   return ary[ary.length - 1] || uri;
}

function AfShortUri(uri) {
   var name = AfShortNameFromUri(uri);
   // matches both http and https
   if (0 == uri.indexOf("http")) {
      // display as a link
      return [
         '<a href="',
         encodeURI(uri),
         '" target="_blank" title="',
         encodeURI(uri),
         '">',
         name,
         '</a>'
      ].join('');
   } else {
      // display as a link
      return [
         '<span class="underline-dots" title="',
         encodeURI(uri),
         '">',
         encodeURIComponent(name),
         '</span>'
      ].join('');
   }
}

/**
 * This function compares the 2 strings passed that contain digits and non-digits. The comparision
 * works by creating a sequense of digits or non-digits and compare these chunks to decide which one of
 * these strings preceedes the other.
 *
 * @param str1
 * @param str2
 * @returns
 */
function CompareAlnumVersionString(str1,str2) {
   var s1Index = 0, s2Index = 0, s1Length = str1.length, s2Length = str2.length;

   while (s1Index < s1Length && s2Index < s2Length) {
      var thisStr = GetDigitOrNonDigitChunk(str1, s1Index);
      s1Index += thisStr.length;

      var thatStr = GetDigitOrNonDigitChunk(str2, s2Index);
      s2Index += thatStr.length;

      // Compare these str chunks and ignore case.
      var result = thisStr.toLowerCase() - thatStr.toLowerCase();

      if (result != 0) {
         return result;
      }
   }
   return s1Length - s2Length;
}

/**
 * Helper method for version comparator. This method returns a subset of characters from the input that are
 * either digits or non-digits.
 *
 * @param s
 * @param index
 * @returns
 */
function GetDigitOrNonDigitChunk(s, index) {
   var c = s.charAt(index);
   var result = c;
   index++;
   var digitOrNot = $.isNumeric(c);
   while (index < s.length) {
      c = s.charAt(index);
      if (digitOrNot != $.isNumeric(c)) {
         break;
      }
      result += c;
      index++;
   }
   return result;
}
