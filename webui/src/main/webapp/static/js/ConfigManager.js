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
 * function ConfigManager
 *
 * A "class" that encapsulates all the methods for dealing
 * with configuration.
 * -----------------------------------------------------------------------------
 */
function ConfigManager()
{
   this.superDestruct = AbstractManager.prototype.Destruct;
}

ConfigManager.prototype = new AbstractManager('Config');


/**
 * -----------------------------------------------------------------------------
 * ConfigManager.Destruct
 *
 * Override the destructor in AbstractManager so we can clean up our own stuff.
 * Note that AbstractManager.Destruct is still called anyway.
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
Destruct = function()
{
   this.superDestruct();
   if (this.stackEditor) {
      this.stackEditor = this.stackEditor.Destruct();
   }
};


/**
 * -----------------------------------------------------------------------------
 * ConfigManager.CreateEditor
 *
 * Fetch all the configuration data from the server, and once fetched create
 * a stack editor to display it.
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
CreateEditor = function _createEditor(insertPoint)
{
   var self = this;

   AfAjax({
      method: 'GET',
      url: '/api/config',
      beforeSend: function() {
         AfStartPleaseWaitT('M.COMMON.FETCHING_DATA');
      },
      success: function(data) {
         try {
            self.CreateEditorFromJson(data, insertPoint);
         }
         catch(error) {
            AfError('Internal Error!', 'CreateEditorFromJson failed: ' + error);
            AfLog('CreateEditorFromJson failed: ' + error);
         }
      },
      complete: function() {
         AfEndPleaseWait();
      }
   });
};

/**
 * -----------------------------------------------------------------------------
 * ConfigManager.getChangeFunc
 *
 * Get a callback function for the given Config Param key.
 * @return
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
getChangeFunc = function _getChangeFunc(key) {
   switch(key) {
      case 'horizon.enabled':
         return function _horizonEnabledSet(val, changeData) {
            if (val) {
               // Trigger a click on the horizon configuration link to launch the dialog.
               $('#idHorizonConfigLink').trigger('click');
            }
         };
      default: return null;
   }
};

/**
 * -----------------------------------------------------------------------------
 * ConfigManager.Validate
 *
 * Get a validation rule for the given Config Param key (user editable keys only).
 * @return a jQuery validation rule Json or null if the key is not found.
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
Validate = function _validate(key) {
   switch(key) {
      case 'cws.stall_timeout': return { integer:true, range: [1, 86400] }; // from 1 sec to 24 hrs.
      case 'cws.stall_cpu':
      case 'cws.stall_net':
      case 'cws.stall_disk': return { range: [1, 100] }; // 1-100%
      case 'feeds.rescan_period_mins': return { integer:true, range: [1, 525949] }; // 1 year = 525,948.766 minutes
      case 'feeds.max_convert_attempts': return { integer:true, range: [1, 100] };
      case 'fileshares.max_dir_depth_scan': return { integer:true, range: [1, 100] };
      case 'horizon.url': return { url:true };
      case 'taskq.max_projects_per_batch': return { integer:true, range: [1, 500] };
      case 'taskq.max_concurrent': return { integer:true, range: [-1, 100] };
      case 'taskq.max_concurrent_scans': return { integer:true, range: [-1, 100] };
      case 'taskq.max_finished_count':
      case 'taskq.max_finished_scans': return { integer:true, range: [-1, 1000] };
      default : AfLog('No jQuery validation rule defined for the config param key -> ' + key);
   }
   return null;
};

/**
 * -----------------------------------------------------------------------------
 * ConfigManager.CreateEditorFromJson
 *
 * Use the specified JSON data to create a new stack editor that lets the user
 * view and edit configuration parameters.
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
CreateEditorFromJson = function _createEditorFromJson(json, insertPoint)
{
   var self = this;

   if (self.stackEditor) {
      self.stackEditor = self.stackEditor.Destruct();
   }

   self.stackEditor = new StackEditor(
      insertPoint,
      AfTranslate('T.CONFIG'));

   /* Sort all the groups */
   var groupNames = [];
   for (var groupName in json) {
      groupNames.push(groupName);
   }
   groupNames.sort();

   /* Create a section for each group */
   for (var gi = 0; gi < groupNames.length; gi++) {
      var groupName = groupNames[gi];
      var stackGroup = self.stackEditor.AddGroup({
         title: AfTranslate('T.CONFIG.GROUP.' + groupName),
         collapsible: true
      });

      /* Add all parameters in the group (note: already sorted) */
      var params = json[groupName];
      for (var pi = 0; pi < params.length; pi++) {
         var param = params[pi];
         var rowType = null;
         var options = null;

         switch(param.type) {
         case "INTEGER":
         case "LONG":
            rowType = StackEditor.NUMBER;
            break;

         case "BOOLEAN":
            rowType = StackEditor.BOOLEAN;
            break;

         case "SINGLE_SELECT":
            rowType = StackEditor.PULLDOWN;
            break;

         case "STRING":
            rowType = StackEditor.TEXT_SHORT;
            break;
         }

         stackGroup.AddRow({
            type: rowType,
            id: param.key,
            name: param.key,
            label: AfTranslate(param.name),
            value: param.value,
            options: param.options,
            units: param.units,
            validation : self.Validate(param.key),
            changeFunc : self.getChangeFunc(param.key)
         });
      }
   }

   /* Footer buttons */
   var footerButtons = [{
      label: AfTranslate('T.COMMON.SAVE'),
      validate: true,
      clickFunc: function() { self.SubmitForm(self.stackEditor.formElement); }
   }];

   if (VmTAF.devMode) {
      footerButtons.push({
         label: "Use Simulator",
         validate: true,
         clickFunc: function() {
            $('input[name="cws.service_url"]').val("http://localhost:8080/webui/cws");
            $('input[name="cws.conversions_service_url"]').val("http://localhost:8080/webui/cws");
            $('input[name="datastore.service_url"]').val("http://localhost:8080/webui/ds");
            $('input[name="workpool.service_url"]').val("http://localhost:8080/webui/wp");
            $('input[name="taskq.max_concurrent"]').val("4");
         }
      });
      footerButtons.push({
         label: "Use Appliance",
         validate: true,
         clickFunc: function() {
            var ip = prompt('Enter appliance IP address:');
            if (ip) {
               $('input[name="cws.service_url"]').val("http://" + ip + ":5000");
               $('input[name="cws.conversions_service_url"]').val("http://" + ip + ":8080/mm");
               $('input[name="datastore.service_url"]').val("http://" + ip + ":5000");
               $('input[name="workpool.service_url"]').val("http://" + ip + ":8080/mm/workpool");
            }
         }
      });
   }

   /* Add footer with a 'Save' button */
   self.stackEditor.AddFooter(footerButtons);
};


/**
 * -----------------------------------------------------------------------------
 * ConfigManager.SubmitForm
 *
 * Gather all the configuration form inputs into the JSON format that the
 * AppFactory API expects, then submit them via an Ajax post.
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
SubmitForm = function(theForm)
{
   /* Convert form to 'name:value' JSON data */
   var json = AfSerializeForm(theForm);

   /* Post it */
   AfAjax({
      method: 'POST',
      url: '/api/config',
      contentType: 'application/json',
      data: json,

      beforeSend: function() {
         AfStartPleaseWaitT('M.CONFIG.SAVING');
      },

      success: function() {
         AfNotifyT('T.CONFIG.SAVED', 'M.CONFIG.SAVED');
      },

      complete: function() {
         AfEndPleaseWait();
      }
   });
};


/**
 * -----------------------------------------------------------------------------
 * ConfigManager.SubmitHorizonConfigForm
 *
 * Get the horizon url and activation token in JSON format that the
 * AppFactory API expects, then submit them via an Ajax post.
 * @param formId
 * -----------------------------------------------------------------------------
 */
ConfigManager.prototype.
SubmitHorizonConfigForm = function _submitHorizonConfigForm(formId)
{
   /* Convert form to 'name:value' JSON data */
   var form = $(formId);
   var horizonConfig = AfSerializeForm(form);

   /* Post it */
   AfAjax({
      method: 'POST',
      url: '/api/config/horizon',
      contentType: 'application/json',
      data: horizonConfig,

      beforeSend: function() {
         $('.inline-error').empty();
      },

      success: function(response) {
         // Returns success only if data changed.
         if (response) {
            AfNotifyT('T.COMMON.SUCCESS', 'M.CONFIG.HORIZON.SAVED');
         } else {
            AfNotify('No Change', 'Horizon configuration did not change.', 'warn');
         }
         // Horizon config is valid and saved.
         VmTAF.dialogHolder.dialog("close");
      },

      error: function(jqXHR, textStatus, errorThrown) {
         // Handle the case of untrusted SSL certificate (horizon API is over https).
         if (jqXHR.responseText === 'ERROR_UNTRUSTED_SSL_CERTIFICATE') {
            AfNotify('ERROR: SSL certificate error');
            // Show security warning, and allow them to re submit by trusting the cert.
            AfError(AfTranslate('M.HORIZON.SECURITY_WARN'));
            // Now show the ssl cert checkbox.
            form.find('.trust-ssl-cert').show();
         } else {
            // Allow the default error handler to kick in.
            AfError(jqXHR.responseText);
         }
      }
   });
};
