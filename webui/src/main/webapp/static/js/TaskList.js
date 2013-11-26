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

function TaskList() {
   // todo: take proper arguments
   "use strict";
   this.init();
}

(function() {
   "use strict";

   TaskList.prototype.enableDisableButtons = function(firstLoad) {
      //li:not(.no-drag)
      if (!firstLoad) {
         $("#tasksLoading").hide();
      }
      $('#sortable li.taskItem:not(.no-drag) .taskMoveToTop').button("option", "disabled", false);
      $('#sortable li.taskItem:not(.no-drag) .taskMoveToBottom').button("option", "disabled", false);
      $('#sortable li.taskItem:not(.no-drag):first .taskMoveToTop').button("option", "disabled", true);
      $('#sortable li.taskItem:not(.no-drag):last .taskMoveToBottom').button("option", "disabled", true);

      if (0 == $('#sortable').children().length) {
         if (!firstLoad) {
            // don't hide the "no tasks" display until
            // we have received a message from the server
            $('#noTasks').show();
         }
         $("#idCleanupAll").button("option","disabled",true);
         $("#idCancelAll").button("option","disabled",true);
      } else {
         $('#noTasks').hide();
         if (!$("#idCleanupAll").inProgress) {
            $("#idCleanupAll").button("option","disabled",false);
         }
         if (!$("#idCancelAll").inProgress) {
            $("#idCancelAll").button("option","disabled",false);
         }
      }
   };
   TaskList.prototype.appendNewTaskRow = function(args) {
      var id = args.source.id;

      // Compute the icon URL for this task
      args.iconSize = window.VmTAF.iconSizeLarge;
      // If we are a capture task, use the CaptureRequest for
      // the icon.  If not, let AfGetIconUrl return a default
      // icon instead.
      args.iconUrl = AfGetIconUrl(
         args.source.captureRequest ? args.source.captureRequest.icons : null,
         window.VmTAF.iconSizeLarge);
      $.views.allowCode = true;
      $('#sortable').append($('#taskTemplate').render(args));
      $('#taskProgressBar' + id).progressbar();
      $('#idCancel' + id).button({icons:{primary: 'ui-icon-circle-close'},label:'Cancel'}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/'+ this.id + '/abort',
            beforeSend: function() {
               $("#idCancel"+id).button("option","disabled",true);
            },
            complete: function() {
               $("#idCancel"+id).button("option","disabled",false);
            }
         });
      }, args.source));
      $('#idClean' + id).button({icons:{primary: 'ui-icon-trash'},label:'Cleanup'}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/'+ this.id + '/cleanup',
            beforeSend: function() {
               $("#idClean"+id).button("option","disabled",true);
            },
            complete: function() {
               $("#idClean"+id).button("option","disabled",false);
            }
         });
      }, args.source));
      $('#idManualModeStart' + id).button({icons:{primary: 'ui-icon-person'},label:'Begin Manual Capture'});
      $('#idUnstall' + id).button({icons:{primary: 'ui-icon-arrowrefresh-1-s'},label:'Unstall'}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/'+ this.id + '/unstall',
            beforeSend: function() {
               $("#idUnstall" + id).button("option", "disabled", true);
            },
            error: function() {
               $("#idUnstall" + id).button("option", "disabled", false);
            }
         });
      }, args.source));
      $('#idCompleteButtons' + id).hide();
      $('#idEdit' + id).button({icons:{primary: 'ui-icon-pencil'},label:'Edit Package'});
      $('#idDownload' + id).button({icons:{primary: 'ui-icon-circle-arrow-s'},label:'Download'});
      $('#idRetry' + id).button({icons:{primary: 'ui-icon-refresh'},label:'Retry'}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         if (this.captureRequest) {
            // an automatic or manual capture
            var url = window.VmTAF.contextPath + '/apps/detail/' + this.captureRequest.applicationId;
            if (this.recipeId) {
               url += "?recipeId=" + this.recipeId;
            }
            document.location.href = url;
         } else if (this.type == "IMPORT_PROJECTS") {
            // no-op
         } else {
            // a rebuild task
            $("#idRetry"+id).button("option","disabled",true);
            AfAjaxPostJson(
               // post to this url to start a rebuild
               window.VmTAF.contextPath + "/api/builds/" + this.recordId + "/rebuild",
               null,
               function() { // success
                  $("#idRetry"+id).button("option","disabled",false);
               },
               function() { // failure
                  $("#idRetry"+id).button("option","disabled",false);
               }
            );
         }
      }, args.source));
      // remove the "retry" button from the import projects task
      if (args.source.type == "IMPORT_PROJECTS") {
         $("#idRetry"+id).remove();
      }
      $('#taskId' + id).click(jQuery.proxy(function() {
         $('#idTaskContent' + this.id).toggleClass('ui-corner-bottom');
         $('#idTaskDetails' + this.id).toggle();
      }, args.source));
      $('#idMoveToTop' + id).button({icons:{primary: 'ui-icon-arrowstop-1-n'}, text:false}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/'+ this.id + '/moveToTop',
            beforeSend: function() {
               $("#idMoveToTop"+id).button("option","disabled",true);
               $("#idMoveToTop"+id).removeClass("ui-state-hover");
            },
            error: function() {
               // don't re-enable on success,
               // as enableDisableButtons() will take care of it after the move
               $("#idMoveToTop"+id).button("option","disabled",true);
            }
         });
      }, args.source));
      $('#idMoveToBottom' + id).button({icons:{primary: 'ui-icon-arrowstop-1-s'}, text:false}).click(jQuery.proxy(function(event) {
         event.stopPropagation();
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/'+ this.id + '/moveToBottom',
            beforeSend: function() {
               $("#moveToBottom"+id).button("option","disabled",true);
               $("#moveToBottom"+id).removeClass("ui-state-hover");
            },
            error: function() {
               // don't re-enable on success,
               // as enableDisableButtons() will take care of it after the move
               $("#moveToBottom"+id).button("option","disabled",false);
            }
         });
      }, args.source));
   };
   TaskList.prototype.updateProgress = function(id, value) {
      var disabled = false;
      if (value < 0) {
         disabled = true;
         value = 0;
      }
      $('#taskProgressBar' + id).progressbar("option", "value", value);
      $('#taskProgressBar' + id).progressbar("option", "disabled", disabled);
      if (disabled) {
         $('#taskProgressPct' + id).hide();
      } else {
         $('#taskProgressPct' + id).text(['(',value,'%)'].join(''));
         $('#taskProgressPct' + id).show();
      }
   };
   TaskList.prototype.updateStarted = function(id, started) {
      $('#started'+id).empty().append(
         $('#startedTemplate').render({source:{started:started}})
      );
   };
   TaskList.prototype.updateStatusLink = function(id, statusLink) {
      if (!statusLink) {
         $('#idManualModeStart'+id).hide();
         return;
      }
      $('#idManualModeStart'+id).show();
      $('#idManualModeStart'+id).click(
         jQuery.proxy(function(event) {
            event.stopPropagation();
            AfOpenManualCaptureWindow(statusLink);
         }, {id:id, statusLink:statusLink})
      );
   };
   TaskList.prototype.updateQueueStatus = function(id, queueStatus) {
      switch (queueStatus) {
         case "WAITING":
            this.updateProgress(-1);
            // allow drag (on by default)
            break;
         case "RUNNING":
            // disable drag, hide drag handle.  This will also cancel
            // any sort in progress thanks to our "sort" handler on #sortable
            $('#taskId' + id).addClass('no-drag');
            $('#taskDragHandle' + id).hide();
            $('#idMoveButtons' + id).hide();
            break;
         case "FINISHED":
            // disable drag, hide drag handle
            $('#taskId' + id).addClass('no-drag');
            $('#taskDragHandle' + id).hide();
            $('#idMoveButtons' + id).hide();
            $('#idTaskProgressAndDescription' + id).hide();
            $('#idCancel' + id).hide();
            $('#idRunningButtons'+id).hide();
            //noinspection MagicNumberJS
            this.updateProgress(100);
            $('#idCompleteButtons' + id).show();
            $('#taskCompletionMsg' + id).show();
            break;
         default:
            window.AfLog("unknown queue status for id=" + id + ", " + queueStatus, 'error');
      }
      window.AfTranslate('T.TASKS.METASTATUS.' + queueStatus);
      this.enableDisableButtons();
   };
   TaskList.prototype.updateProjectId = function(id, projectId) {
      if (!projectId) {
         return;
      }
      // set project log link
      $('#downloadLogs' + id).empty().append(
         $('#logLinkTemplate').render({projectId:projectId})
      );
   };
   TaskList.prototype.updateBuildId = function(id, buildId) {
      if (!buildId) {
         return;
      }
      // set action for download button
      $('#idDownload' + id).click(
         jQuery.proxy(function(event) {
            event.stopPropagation();
            window.AfAjax({
               method:'GET',
               url:'/store/bootstrap?id=' + this + '&action=install'
            });
         }, buildId)
      );
      $('#idEdit' + id).click(
         jQuery.proxy(function(event) {
            event.stopPropagation();
            document.location.href = window.VmTAF.contextPath + '/builds/edit/' + this;
         }, buildId)
      );
   };
   TaskList.prototype.updateTaskCompletionMsg = function(id, msg) {
      if (!msg) {
         return;
      }
      $('#taskCompletionMsg' + id).text(msg);
   };
   TaskList.prototype.updateLastRunningState = function(id, val) {
      if (!val) {
         return;
      }
      this.updateTaskCompletionMsg(id, window.AfTranslate('T.TASKS.STATUS.FAILED.' + val));
   };
   TaskList.prototype.updateTaskLastCommand = function(id, msg) {
      if (!msg) {
         return;
      }
      $('#taskLastCommandMsg' + id).text(msg.command);
      $('#taskLastCommand' + id).show();
   };
   TaskList.prototype.updateTaskLastError = function(id, msg) {
      if (!msg) {
         return;
      }
      $('#taskLastErrorMsg' + id).text(msg);
      $('#taskLastError' + id).show();
   };
   TaskList.prototype.updateIsStalled = function(id, isStalled) {
      if (isStalled) {
         // Show the Stalled: label
         $('#taskIsStalled' + id).show();

         // Hide the progress bar
         $('#taskProgressBar' + id).hide();

         // Show and enable the Unstall button
         $('#idUnstall' + id).show();
         $("#idUnstall" + id).button("option", "disabled", false);
      } else {
         // Hide the Stalled: label
         $('#taskIsStalled' + id).hide();

         // Show the progress bar
         $('#taskProgressBar' + id).show();

         // Hide the Unstall button
         $('#idUnstall' + id).hide();
      }
   };
   TaskList.prototype.updateAborted = function(id, msg) {
      if (!msg) {
         return;
      }
      this.updateTaskCompletionMsg(id,window.AfTranslate("T.TASKS.STATUS.CANCELLED"));
      $("#idCancel"+id).button("option","disabled",true);
   };
   TaskList.prototype.updateStatus = function(id, status) {
      $('#taskProgressDescription' + id).text(window.AfTranslate("T.TASKS.STATUS." + status));
      switch (status.toLowerCase()) {
         case "newtask":
            break;
         case "complete":
            $('#taskId' + id + ' .ui-widget-content').addClass("ui-state-highlight");
            $('#idSuccessfulCaptureButtons' + id).show();
            this.updateTaskCompletionMsg(id, window.AfTranslate("T.TASKS.METASTATUS.FINISHED"));
            break;
         case "cancelled":
            this.updateTaskCompletionMsg(id, window.AfTranslate("T.TASKS.STATUS.CANCELLED"));
            break;
         case "failed":
            $('#taskHolder' + id).removeClass("ui-state-default");
            $('#taskHolder' + id).addClass("ui-state-error");
            $('#taskErrorIcon' + id).show();
            $('#idFailedCaptureButtons' + id).show();
            break;
         default:
            // nothing special going on
            break;
      }
   };
   TaskList.prototype.updateTaskRowState = function(args) {
      var id = args.source,
         i;
      for (i = 0; i < args.builderChanges.length; ++i) {
         var prop = args.builderChanges[i].property,
             val = args.builderChanges[i].value;

         switch (prop) {
            case "queueStatus":
               this.updateQueueStatus(id, val);
               break;
            case "progress":
               this.updateProgress(id, val);
               break;
            case "status":
               this.updateStatus(id, val);
               break;
            case "lastRunningState":
               this.updateLastRunningState(id, val);
               break;
            case "lastCommand":
               this.updateTaskLastCommand(id,val);
               break;
            case "lastError":
               this.updateTaskLastError(id,val);
               break;
            case "isStalled":
               this.updateIsStalled(id,val);
               break;
            case "queued":
            case "finished":
               // ignore, handled in queueStatus change
               break;
            case "started":
               this.updateStarted(id, val);
               break;
            case "converterId":
               // todo: something?
               break;
            case "projectId":
               this.updateProjectId(id, val);
               break;
            case "buildId":
               this.updateBuildId(id, val);
               break;
            case "aborted":
               this.updateAborted(id,val);
               break;
            case "statusLink":
               this.updateStatusLink(id, val);
               break;
            case "numFound": // project import status
               this.updateNumFound(id, val);
               break;
            case "numImported": // project import status
               this.updateNumImported(id, val);
               break;
            case "errors": // project import status
               this.updateErrors(id, val);
               break;
            default:
               window.AfLog("unknown property for id=" + id + ", " + prop, 'error');
               break;
         }
      }
   };
   TaskList.prototype.updateNumFound = function(id, found) {
      if (found < 1) {
         return;
      }
      $('#numFound'+id).empty().append(found + ' projects');
   };
   TaskList.prototype.updateNumImported = function(id, imported) {
      if (imported < 1) {
         return;
      }
      $('#numImported'+id).empty().append(imported + ' projects');
   };
   TaskList.prototype.updateErrors = function(id, errors) {
      if (!errors || errors.length < 1) {
         return;
      }
      var html = $('#errorsTemplate').render({source:{'errors':errors}});
      $('#errors'+id).empty().append(html);
   };
   TaskList.prototype.addTask = function(input) {
      var args = jQuery.extend({}, input, {
            VmTAF:window.VmTAF,
            // todo: in the future, we should do something more robust
            makesSingleProject: ('IMPORT_PROJECTS' != input.source.type)
         }),
         id = args.source.id;

      // if we reconnect, we might get an entry for a task which
      // already exists.  Do this so that we don't add it twice.
      if ($('#taskId' + id).length == 0) {
         this.appendNewTaskRow(args);
      } else {
         AfLog("Ignoring duplicate task for ID " + id, 'error');
      }

      // in either event, update the row to the current state
      try {
         this.updateProgress(id, args.source.progress);
         this.updateStatus(id, args.source.status);
         this.updateQueueStatus(id, args.source.queueStatus);
         this.updateProjectId(id, args.source.projectId);
         this.updateBuildId(id, args.source.buildId);
         this.updateStarted(id, args.source.started);
         this.updateStatusLink(id, args.source.statusLink);
         this.updateAborted(id, args.source.aborted);
         this.updateLastRunningState(id, args.source.lastRunningState);
         this.updateTaskLastCommand(id, args.source.lastCommand);
         this.updateTaskLastError(id, args.source.lastError);
         this.updateIsStalled(id, args.source.isStalled);
         this.updateNumFound(id, args.source.numFound);
         this.updateNumImported(id, args.source.numImported);
         this.updateErrors(id, args.source.errors);
         if ("REBUILD" == args.source.type) {
            // the rebuild task keeps its buildID in the recordId field
            this.updateBuildId(id, args.source.recordId);
         }
      } catch (err) {
         window.AfLog("error adding task " + err, 'error');
      }
   };
   TaskList.prototype.removeTask = function(line) {
      var id = line.source;
      $('#taskId' + id).remove();
      $("#sortable").sortable("refreshPositions");
   };
   TaskList.prototype.moveToTop = function(line) {
      var id = line.source;
      // if there are exiting tasks in the queue, move it after them.
      $('#taskId' + id).insertAfter('#sortable li.taskItem.no-drag:last');
      $("#sortable").sortable("refreshPositions");
   };
   TaskList.prototype.moveToBottom = function(line) {
      var id = line.source;
      $('#taskId' + id).insertAfter('#sortable li.taskItem:not(.no-drag):last');
      $("#sortable").sortable("refreshPositions");
   };
   TaskList.prototype.moveAfter = function(line) {
      var id = line.source;
      $('#taskId' + id).insertAfter('#taskId' + line.followingTaskId);
      $("#sortable").sortable("refreshPositions");
   };
   TaskList.prototype.callback = function(response) {
      // Websocket events.
      AfLog(response, 'debug');
      if (response.transport != 'polling' && response.state != 'connected' && response.state != 'closed') {
         //noinspection MagicNumberJS
         if (response.status == 200) {
            // it's possible that some web servers will not send messages back in a single
            // packet, but rather spread a single message across several distinct packets.
            // Atmosphere.js fires this callback immediately as each packet arrives, rather than
            // waiting for a single logical message.  This can lead to problems.
            //
            // To deal with this, we need to remember any content from the previous
            // invocation of this method which was not valid JSON, as it might have been
            // just a fragment of a legitimate message.  We will then prepend it to the message
            // we just got, to see if it made a whole one.
            //
            // It's also possible that we have a message that just simply isn't valid for
            // some other reason, unrelated to this bug.  So if we see our message separator
            // token (two blank lines), then discard the fragment because we no longer
            // have any hope of making it whole.
            var data = (this.fragmentFromLastCallback || '') + response.responseBody,
               dataLines = data.split(/\r\n\r\n|\r\r|\n\n/),
               i;
            this.fragmentFromLastCallback = '';
            for (i = 0; i< dataLines.length; ++i) {
               var dataLine = dataLines[i],
                  json = null;
               var ATMOSPHERE_HEADER = "-- Welcome to the Atmosphere Framework.";
               try {
                  json = jQuery.parseJSON(dataLine);
               } catch(err) {
                  if (i > 0) {
                     // fix bug 827586 (again)
                     // if this not the first line in our message, add back the
                     // line-separator that split() removed!
                     // otherwise we'll continue to merge everything into a single
                     // fragmentFromLastCallback which will never succeed
                     this.fragmentFromLastCallback += "\n\n";
                  }
                  this.fragmentFromLastCallback += dataLine;
                  if (-1 == this.fragmentFromLastCallback.indexOf(ATMOSPHERE_HEADER)) {
                     window.AfLog("*** failed to parse JSON, storing fragment: " + err + "\n" + dataLine, 'info');
                  }
               }
               try {
                  if (null != json) {
                     // we saw a valid message, so clear any existing fragment since it's
                     // part of an old message now.
                     //
                     // This will happen with some regularity because we can get a trailing
                     // fragment of the "filler" header sent at the top of each atmosphere
                     // response.  Atmosphere.js sends us this because it suffers from the
                     // same incorrect assumption that we once did; that all the filler content
                     // will be sent in a single packet.  If it's instead sent in two or more
                     // packets, it will fire the callback instead of discarding it.
                     if (this.fragmentFromLastCallback && "" != this.fragmentFromLastCallback) {
                        if (-1 != dataLine.indexOf(ATMOSPHERE_HEADER)) {
                           AfLog("Discarding unparseable message fragment:" + this.fragmentFromLastCallback, 'error');
                        }
                     }
                     this.fragmentFromLastCallback = '';

                     this.handleJson(json);
                  }
               } catch(err) {
                  window.AfLog("*** failed to handle message: " + err + "\n" + json, 'error');
               }
            }
         }
      }

      if (!window.VmTAF.unloading
         && (response.state == "completed" || response.state == "error")) {
         // bug 857379
         // The connection has dropped for one reason or another
         // (IE9 will send "completed", Chrome will send "error").
         // In either case, atmosphere's reconnection code does not
         // work.  Rather than monkey-patch to work around that,
         // just reload the whole page to reconnect.
         AfLog("session closed - reloading page");
         window.location.reload();
      }
   };
   TaskList.prototype.handleJson = function(line) {
      switch (line.type) {
         case "added":
            // a new task was added.  Put it at the bottom of the list.
            this.addTask(line);
            this.enableDisableButtons();
            break;
         case "updated":
         case "metastatus_change":
            // a running task has new state.  re-draw it, or add it
            // to the bottom of the running list on the off chance
            // we don't have it already
            this.updateTaskRowState(line);
            break;
         case "moved_to_top":
            // a pending task was moved to the top of the pending queue
            this.moveToTop(line);
            this.enableDisableButtons();
            break;
         case "moved_to_bottom":
            // a pending task was moved to the bottom of the pending queue
            this.moveToBottom(line);
            this.enableDisableButtons();
            break;
         case "reordered":
            // a pending task was reordered within the pending queue
            this.moveAfter(line);
            this.enableDisableButtons();
            break;
         case "removed":
            // a task was removed
            this.removeTask(line);
            this.enableDisableButtons();
            break;
         case "NO-TASKS":
            $("#tasksLoading").hide();
            $("#noTasks").show();
            break;
      }
   };
   TaskList.prototype.subscribe = function() {
      var location = window.VmTAF.contextPath + "/comet/api/conversion-tasks-comet";
      jQuery.atmosphere.subscribe(
         location,
         jQuery.proxy(this.callback,this),
         jQuery.atmosphere.request = { transport:"streaming" }
      );
      //var connectedEndpoint = jQuery.atmosphere.response;
   };
   TaskList.prototype.init = function() {
      var self = this;

      // register helper for jsrender
      jQuery.views.registerHelpers({
         timeago: function(object,property) {
            return AfTimeago(object[property]);
         },
         translate: function(prefix,object,property) {
            var val = object,
                subprops = property.split('.'),
                i;
            for (i = 0; i < subprops.length; ++i) {
               val = val[subprops[i]];
            }
            return window.AfTranslate(prefix + val);
         },
         translateOsType: function(object, property) {
            var osType = object,
            subprops = property.split('.'),
            result;
            for (var i = 0; i < subprops.length; ++i) {
               osType = osType[subprops[i]];
            }
            result = window.AfTranslate('M.OSTYPE.' + osType['osTypeName']);
            // If no valid variant property, then return empty string. (WinXP case)
            if (osType['osVariantName'] == '') {
               return result;
            }
            return result + ' ' + window.AfTranslate(
                  'M.OSVARIANT.' + osType['osTypeName'] + '.' + osType['osVariantName']);
         }
      });

      $("#idCleanupAll").button({label:"Cleanup All",icons:{primary:'ui-icon-trash'}}).click(function() {
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/cleanup',
            beforeSend: function() {
               $("#idCleanupAll").inProgress = true;
               $("#idCleanupAll").button("option","disabled",true);
            },
            complete: function() {
               $("#idCleanupAll").inProgress = false;
               $("#idCleanupAll").button("option","disabled",false);
            }
         });
      });
      $("#idCancelAll").button({label:"Cancel All",icons:{primary:'ui-icon-circle-close'}}).click(function() {
         window.AfAjax({
            method: 'POST',
            url: '/api/tasks/abortAll',
            beforeSend: function() {
               $("#idCancelAll").inProgress = true;
               $("#idCancelAll").button("option","disabled",true);
            },
            complete: function() {
               $("#idCancelAll").inProgress = false;
               $("#idCancelAll").button("option","disabled",false);
            }
         });
      });
      //noinspection MagicNumberJS
      $("#sortable").sortable({
         placeholder:"ui-state-highlight",
         opacity:0.8,
         handle:"span",
         containment: '#main',
         forcePlaceholderSize: true,
         axis:'y',
         items: "li:not(.no-drag)",
         cancel: ".no-drag",
         beforeStop: function(event, ui) {
            if (ui && ui.item && (ui.item.length > 0)) {
               if ($(ui.item[0]).hasClass("no-drag")) {
                  $('#sortable').sortable('cancel');
               }
            }
         },
         update: function(event,ui) {
            if (ui && ui.item && (ui.item.length > 0)) {
               var idDragged = ui.item[0].id.substr("taskId".length),
                   idBefore = null;
               if (ui.item[0].previousElementSibling) {
                  if (!$(ui.item[0].previousElementSibling).hasClass('no-drag')) {
                     idBefore = ui.item[0].previousElementSibling.id;
                     idBefore = idBefore.substr("taskId".length);
                  }
               }
               if (null == idBefore) {
                  window.AfAjax({
                     method: 'POST',
                     url: '/api/tasks/'+ idDragged + '/moveToTop'
                  });
               } else {
                  window.AfAjax({
                     method: 'POST',
                     url: '/api/tasks/'+ idDragged + '/moveBefore?taskIdBefore=' + idBefore
                  }); // TODO: if move fails, move back to old position!
               }
            }
            self.enableDisableButtons();
         },
         cursor:"move"
      });
      $("#sortable").disableSelection();

      this.subscribe();
      this.enableDisableButtons(true);
   };
}()); // end "use strict"