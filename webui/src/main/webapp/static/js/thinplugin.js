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


function ThinPlugin()
{
   /* These methods work for both */
   this.IsInstalled = function() {
      try {
         return this.obj.IsInstalled();
      } catch (err) {
         return false;
      }
   };

   this.IsLatestVersion = function() {
      var latest = new Array(1, 0, 0, 0);

      var cur = this.GetVersion();
      if (cur == null || cur.length != 4) {
         return false;
      }

      for (var i = 0; i < 4; i++) {
         if (cur[i] < latest[i]) {
            return false;
         }
      }

      return true;
   };

   if (navigator.appName == 'Microsoft Internet Explorer') {
      this.GetInstalledApps = function() {
         try {
            this.obj = new ActiveXObject('VMware.ThinPlugin.1');

            /* Must convert this flat array to an array of arrays. */
            var flat = new VBArray(this.obj.GetInstalledApps()).toArray();
            var ret = [];
            for (var i = 0; i < flat.length / 2; i++) {
               ret[i] = [flat[i], flat[i + flat.length / 2]];
            }
            return ret;
         }
         catch(err) {
            if (err.number == -2146827859) {
               // Note: that's 0x800A01AD (but you can't compare using hex)
               // which means "can't create object", i.e. the plugin is almost
               // certainly not installed.
               return [];
            }
            throw err;
         }
      };

      this.GetVersion = function() {
         try {
            return new VBArray(this.obj.GetVersion()).toArray();
         }
         catch (err) {
            if (err.number == -2146827859) {
               // Note: that's 0x800A01AD (but you can't compare using hex)
               // which means "can't create object", i.e. the plugin is almost
               // certainly not installed.
               return [];
            }
            throw err;
         }
      };
   }
   else if (navigator.appVersion.indexOf('Win') != -1) {
      /* Other Windows based browsers - we expect NPAPI compatible */
      navigator.plugins.refresh(false);

      /* The including page must have a <div id="thinplugin" />. */
      var div = document.getElementById('thinplugin');

      if (div == null) {
         /* Need the div... */
         alert("There is no 'thinplugin' div on this page");
         return null;
      } else {
         var found = false;
         for (var i = 0; i < navigator.plugins.length; i++) {
            if (navigator.plugins[i].filename == 'npthinstore.dll') {
               found = true;
               break;
            }
         }

         if (!found) {
            /* Plugin not installed */
            return null;
         }
      }

      div.innerHTML =
         '<object width="1" height="1" id="thinplugin-np" ' +
         'type="application/x-vmware-thinapp-store"></object>';

      this.obj = document.getElementById('thinplugin-np');
      this.GetInstalledApps = function() {
         try {
            return this.obj.GetInstalledApps();
         }
         catch (err) {
            return [];
         }
      };

      this.GetVersion = function() {
         try {
            return this.obj.GetVersion();
         }
         catch (err) {
            return [];
         }
      };
   } else {
      /* OS not supported */
      return null;
   }

   return this;
}
