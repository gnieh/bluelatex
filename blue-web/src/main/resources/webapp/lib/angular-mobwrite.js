/**
 * MobWrite - Real-time Synchronization and Collaboration Service
 *
 * Copyright 2006 Google Inc.
 * http://code.google.com/p/google-mobwrite/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
  
var angularMobwrite = angular.module('MobWrite', []);

angularMobwrite
  .value('MobWriteConfig',{
    syncGateway: '/scripts/q.py',
    get_maxchars: 1000,
    debug: false,
    timeoutInterval: 30000,
    minSyncInterval: 1000,
    maxSyncInterval: 4000,
    syncInterval: 2000,
    idPrefix: '',
    nullifyAll: false,
    communicationType: 'ajax',
    messageType: 'json'
  });

angularMobwrite.factory("MobWriteService", ['$http', '$log', '$q','MobWriteConfig',
  function ($http, $log, $q,MobWriteConfig) {

    /**
     * PID of task which will trigger next Ajax request.
     * @type {number?}
     * @private
     */
    var syncRunPid_ = null;


    /**
     * PID of task which will kill stalled Ajax request.
     * @type {number?}
     * @private
     */
    var syncKillPid_ = null;

    /**
     * Track whether something changed client-side in each sync.
     * @type {boolean}
     * @private
     */
    var clientChange_ = false;


    /**
     * Track whether something changed server-side in each sync.
     * @type {boolean}
     * @private
     */
    var serverChange_ = false;


    /**
     * Temporary object used while each sync is airborne.
     * @type {Object?}
     * @private
     */
    var syncAjaxObj_ = null;
    /**
    * Return a random id that's 8 letters long.
    * 26*(26+10+4)^7 = 4,259,840,000,000
    * @return {string} Random id.
    */
    var uniqueId = function() {
      // First character must be a letter.
      // IE is case insensitive (in violation of the W3 spec).
      var soup = 'abcdefghijklmnopqrstuvwxyz';
      var id = soup.charAt(Math.random() * soup.length);
      // Subsequent characters may include these.
      soup += '0123456789-_:.';
      for (var x = 1; x < 8; x++) {
       id += soup.charAt(Math.random() * soup.length);
      }
      // Don't allow IDs with '--' in them since it might close a comment.
      if (id.indexOf('--') != -1) {
       id = uniqueId();
      }
      return id;
      // Getting the maximum possible density in the ID is worth the extra code,
      // since the ID is transmitted to the server a lot.
    };

    /**
    * Unique ID for this session.
    * @type {string}
    */
    var syncUsername = uniqueId();
    /**
    * Hash of all shared objects.
    * @type {Object}
    */
    var shareHandlers = [];

    /**
    * Array of registered handlers for sharing types.
    * Modules add their share functions to this list.
    * @type {Array.<Function>}
    */
    var shared = {};

    /**
     * Prototype of shared object.
     * @param {string} id Unique file ID.
     * @constructor
     */
    var shareObj = function(id) {
      if (id) {
        this.file = id;
        this.dmp = new diff_match_patch();
        this.dmp.Diff_Timeout = 0.5;
        // List of unacknowledged edits sent to the server.
        this.editStack = [];
        if (MobWriteConfig.debug) {
          $log.info('Creating shareObj: "' + id + '"');
        }
      }
    };


    /**
     * Client's understanding of what the server's text looks like.
     * @type {string}
     */
    shareObj.prototype.shadowText = '';


    /**
     * The client's version for the shadow (n).
     * @type {number}
     */
    shareObj.prototype.clientVersion = 0;


    /**
     * The server's version for the shadow (m).
     * @type {number}
     */
    shareObj.prototype.serverVersion = 0;


    /**
     * Did the client understand the server's delta in the previous heartbeat?
     * Initialize false because the server and client are out of sync initially.
     * @type {boolean}
     */
    shareObj.prototype.deltaOk = false;


    /**
     * Synchronization mode.
     * True: Used for text, attempts to gently merge differences together.
     * False: Used for numbers, overwrites conflicts, last save wins.
     * @type {boolean}
     */
    shareObj.prototype.mergeChanges = true;


    /**
     * Fetch or compute a plaintext representation of the user's text.
     * @return {string} Plaintext content.
     */
    shareObj.prototype.getClientText = function() {
      alert('Defined by subclass');
      return '';
    };


    /**
     * Set the user's text based on the provided plaintext.
     * @param {string} text New text.
     */
    shareObj.prototype.setClientText = function(text) {
      alert('Defined by subclass');
    };


    /**
     * Modify the user's plaintext by applying a series of patches against it.
     * @param {Array.<patch_obj>} patches Array of Patch objects.
     */
    shareObj.prototype.patchClientText = function(patches) {
      var oldClientText = this.getClientText();
      var result = this.dmp.patch_apply(patches, oldClientText);
      // Set the new text only if there is a change to be made.
      if (oldClientText != result[0]) {
        // The following will probably destroy any cursor or selection.
        // Widgets with cursors should override and patch more delicately.
        this.setClientText(result[0]);
      }
    };


    /**
     * Notification of when a diff was sent to the server.
     * @param {Array.<Array.<*>>} diffs Array of diff tuples.
     */
    shareObj.prototype.onSentDiff = function(diffs) {
      // Potential hook for subclass.
    };


    /**
     * Fire a synthetic 'change' event to a target element.
     * Notifies an element that its contents have been changed.
     * @param {Object} target Element to notify.
     */
    shareObj.prototype.fireChange = function(target) {
      if ('createEvent' in document) {  // W3
        var e = document.createEvent('HTMLEvents');
        e.initEvent('change', false, false);
        target.dispatchEvent(e);
      } else if ('fireEvent' in target) { // IE
        target.fireEvent('onchange');
      }
    };


    /**
     * Return the command to nullify this field.  Also unshares this field.
     * @return {string} Command to be sent to the server.
     */
    shareObj.prototype.nullify = function() {
      mobwrite.unshare(this.file);
      return {
        "name": "nullify"
      }
    };


    /**
     * Asks the shareObj to synchronize.  Computes client-made changes since
     * previous postback.  Return '' to skip this synchronization.
     * @return {string} Commands to be sent to the server.
     */
    shareObj.prototype.syncText = function() {
      var sync = {
        "filename": MobWriteConfig.idPrefix + this.file,
        "revision": this.serverVersion,
        "action": null
      };
      var clientText = this.getClientText();
      if (this.deltaOk) {
        // The last delta postback from the server to this shareObj was successful.
        // Send a compressed delta.
        var diffs = this.dmp.diff_main(this.shadowText, clientText, true);
        if (diffs.length > 2) {
          this.dmp.diff_cleanupSemantic(diffs);
          this.dmp.diff_cleanupEfficiency(diffs);
        }
        var changed = diffs.length != 1 || diffs[0][0] != DIFF_EQUAL;
        if (changed) {
          clientChange_ = true;
          this.shadowText = clientText;
        }
        // Don't bother appending a no-change diff onto the stack if the stack
        // already contains something.
        if (changed || !this.editStack.length) {
          var action = {
            "name": "delta",
            "revision": this.clientVersion,
            "data": this.dmp.diff_toDelta_notEncoded(diffs),
            "overwrite": !this.mergeChanges
          };
          this.editStack.push(sync);
          sync.action=(action);
          this.clientVersion++;
          this.onSentDiff(diffs);
        }
      } else {
        // The last delta postback from the server to this shareObj didn't match.
        // Send a full text dump to get back in sync. This will result in any
        // changes since the last postback being wiped out. :(
        if (this.shadowText != clientText) {
          this.shadowText = clientText;
        }
        this.clientVersion++;
        sync.action = {
          "name": "raw",
          "revision": this.clientVersion,
          "data": clientText,
          "overwrite": false
        };
        this.editStack.push(sync);
        // Sending a raw dump will put us back in sync.
        // Set deltaOk to true in case this sync fails to connect, in which case
        // the following sync(s) should be a delta, not more raw dumps.
        this.deltaOk = true;
      }
      // Opera doesn't know how to encode char 0. (fixed in Opera 9.63)
      return this.editStack;
    };


    /**
     * Collect all client-side changes and send them to the server.
     * @private
     */
    var synchronize = function() {
      // Initialize clientChange_, to be checked at the end of analyzeResponse.
      clientChange_ = false;
      var data = {};
      data.commands = [];
      data.peerId =  syncUsername;

      var empty = true;
      // Ask every shared object for their deltas.
      for (var x in shared) {
        if (shared.hasOwnProperty(x)) {
          data.paperId = shared[x].paperId;
          if (MobWriteConfig.nullifyAll) {
            data.commands.push(shared[x].nullify());
          } else {
            data.commands = data.commands.concat(shared[x].syncText());
            if(shared[x].messages) {
              var message = shared[x].messages();
              if(message!=null){
                data.commands.push(message);
              }
            }
          }
          empty = false;
        }
      }

      if (empty) {
        // No sync objects.
        if (MobWriteConfig.debug) {
          $log.info('MobWrite task stopped.');
        }
        return;
      }
      if (data.length == 1) {
        // No sync data.
        if (MobWriteConfig.debug) {
          $log.info('All objects silent; null sync.');
        }
        if(MobWriteConfig.messageType != 'json') {
          analyzeResponse('\n\n');
        } else {
          analyzeResponse({
            commands:[]
          });
        }
        return;
      }

      if (MobWriteConfig.debug) {
        $log.info('TO server:\n' + JSON.stringify(data));
      }

      // Schedule a watchdog task to catch us if something horrible happens.
      syncKillPid_ = setTimeout(syncKill_, MobWriteConfig.timeoutInterval);

      if(MobWriteConfig.messageType != 'json') {
        data = convertMobwriteJsonToText(data);
      }

      // Issue Ajax post of client-side changes and request server-side changes.
      var defer = $q.defer();

      if(MobWriteConfig.communicationType == 'ajax') {
        syncAjaxObj_ = $http({
            url: MobWriteConfig.syncGateway,
            method: "POST",
            data: data,
            timeout: defer.promise
        }).success(function (value, status){
          syncCheckAjax_(value, status);
          defer.resolve(value);
        });
        syncAjaxObj_.resolve = defer.resolve;
      } else if(MobWriteConfig.communicationType == 'webSocket') {

      }
      return defer.promise;
    };

    /**
     * Parse all server-side changes and distribute them to the shared objects.
     * @param {string} text Raw content from server.
     * @private
     */
    analyzeResponse = function(sync) {
      if(MobWriteConfig.messageType != 'json') {
        sync = convertMobWriteTextToJson(sync);
      }
      // Initialize serverChange_, to be checked at the end of analyzeResponse.
      serverChange_ = false;
      if (MobWriteConfig.debug) {
        $log.info('FROM server:\n' + JSON.stringify(sync));
      }
      var file = null;
      var clientVersion = null;
      for (var i = 0; i < sync.commands.length; i++) {
        var command = sync.commands[i];

        // message
        if(command.action == null && command.json) {
          if(file.onMessage) {
            // send message
            file.onMessage(command);
          }
          continue;
        }
        
        if (shared.hasOwnProperty(command.filename)) {
          file = shared[command.filename];
          file.deltaOk = true;
          clientVersion = command.revision;
          // Remove any elements from the edit stack with low version numbers
          // which have been acked by the server.
          for (var x = 0; x < file.editStack.length; x++) {
            if (file.editStack[x].action.revision <= clientVersion) {
              file.editStack.splice(x, 1);
              x--;
            }
          }

        } else {
          // This file does not map to a currently shared object.
          file = null;
          if (MobWriteConfig.debug) {
            $log.error('Unknown file: ', command);
          }
          continue;
        }
        // Parse out a version number for file, delta or raw.
        if (command.action != null && (command.action.name == 'delta' || command.action.name =="raw") && isNaN(command.revision)) {
            $log.error('NaN version number: ',command);
            continue;
        }
        // File indicates which shared object following delta/raw applies to.
        if (command.filename.substring(0, MobWriteConfig.idPrefix.length) == MobWriteConfig.idPrefix) {
          // Trim off the ID prefix.
          command.filename = command.filename.substring(MobWriteConfig.idPrefix.length);
        } else {
          // This file does not have our ID prefix.
          file = null;
          $log.error('File does not have "' + MobWriteConfig.idPrefix + '" prefix: ' + command.filename);
          continue;
        }
        if (command.action.name == 'raw') {
          // The server reports it was unable to integrate the previous delta.
          if (file) {
            file.shadowText = command.action.data;
            file.clientVersion = clientVersion;
            file.serverVersion = command.action.revision;
            file.editStack = [];
            if (command.action.overwrite) {
              // Accept the server's raw text dump and wipe out any user's changes.
              file.setClientText(file.shadowText);
            }
            // Server-side activity.
            serverChange_ = true;
          }
        } else if (command.action.name == 'delta') {
          // The server offers a compressed delta of changes to be applied.
          if (file) {
            if (clientVersion != file.clientVersion) {
              // Can't apply a delta on a mismatched shadow version.
              file.deltaOk = false;
              $log.error('Client version number mismatch.\n' +
                    'Expected: ' + file.clientVersion + ' Got: ' + clientVersion);
            } else if (command.action.revision > file.serverVersion) {
              // Server has a version in the future?
              file.deltaOk = false;
              $log.error('Server version in future.\n' +
                    'Expected: ' + file.serverVersion + ' Got: ' + command.action.revision);
            } else if (command.action.revision < file.serverVersion) {
              // We've already seen this diff.
              if (MobWriteConfig.debug) {
                $log.warn('Server version in past.\n' +
                    'Expected: ' + file.serverVersion + ' Got: ' + command.action.revision);
              }
            } else {
              // Expand the delta into a diff using the client shadow.
              var diffs;
              try {
                diffs = file.dmp.diff_fromDelta_notEncoded(file.shadowText, command.action.data);
                file.serverVersion++;
              } catch (ex) {
                // The delta the server supplied does not fit on our copy of
                // shadowText.
                diffs = null;
                // Set deltaOk to false so that on the next sync we send
                // a complete dump to get back in sync.
                file.deltaOk = false;
                // Do the next sync soon because the user will lose any changes.
                MobWriteConfig.syncInterval = 0;
                $log.error('Delta mismatch.\n' + encodeURI(file.shadowText));
              }
              if (diffs && (diffs.length != 1 || diffs[0][0] != DIFF_EQUAL)) {
                // Compute and apply the patches.
                if (command.action.overwrite) {
                  // Overwrite text.
                  file.shadowText = file.dmp.diff_text2(diffs);
                  file.setClientText(file.shadowText);
                } else {
                  // Merge text.
                  var patches = file.dmp.patch_make(file.shadowText, diffs);
                  // First shadowText.  Should be guaranteed to work.
                  var serverResult = file.dmp.patch_apply(patches, file.shadowText);
                  file.shadowText = serverResult[0];
                  // Second the user's text.
                  file.patchClientText(patches);
                }
                // Server-side activity.
                serverChange_ = true;
              }
            }
          }
        }
      }

      computeSyncInterval_();

      // Ensure that there is only one sync task.
      clearTimeout(syncRunPid_);
      // Schedule the next sync.
      syncRunPid_ = setTimeout(synchronize, MobWriteConfig.syncInterval);
      // Terminate the watchdog task, everything's ok.
      clearTimeout(syncKillPid_);
      syncKillPid_ = null;
    };


    /**
     * Compute how long to wait until next synchronization.
     * @private
     */
    var computeSyncInterval_ = function() {
      var range = MobWriteConfig.maxSyncInterval - MobWriteConfig.minSyncInterval;
      if (clientChange_) {
        // Client-side activity.
        // Cut the sync interval by 40% of the min-max range.
        MobWriteConfig.syncInterval -= range * 0.4;
      }
      if (serverChange_) {
        // Server-side activity.
        // Cut the sync interval by 20% of the min-max range.
        MobWriteConfig.syncInterval -= range * 0.2;
      }
      if (!clientChange_ && !serverChange_) {
        // No activity.
        // Let the sync interval creep up by 10% of the min-max range.
        MobWriteConfig.syncInterval += range * 0.1;
      }
      // Keep the sync interval constrained between min and max.
      MobWriteConfig.syncInterval = Math.max(MobWriteConfig.minSyncInterval, MobWriteConfig.syncInterval);
      MobWriteConfig.syncInterval = Math.min(MobWriteConfig.maxSyncInterval, MobWriteConfig.syncInterval);
    };


    /**
     * If the Ajax call doesn't complete after a timeout period, start over.
     * @private
     */
    var syncKill_ = function() {
      syncKillPid_ = null;
      if (syncAjaxObj_) {
        // Cleanup old Ajax connection.
        syncAjaxObj_.resolve();
        syncAjaxObj_ = null;
      }
      if (MobWriteConfig.debug) {
        $log.warn('Connection timeout.');
      }
      clearTimeout(syncRunPid_);
      // Initiate a new sync right now.
      syncRunPid_ = setTimeout(synchronize, 1);
    };

    /**
     * Callback function for Ajax request.  Checks network response was ok,
     * then calls analyzeResponse.
     * @private
     */
    syncCheckAjax_ = function(value, status) {
      if (!analyzeResponse) {
        // This might be a callback after the page has unloaded,
        // or this might be a callback which we deemed to have timed out.
        return;
      }
      // Only if "OK"
      if (status == 200) {
        syncAjaxObj_ = null;
        analyzeResponse(value);
      } else {
        if (MobWriteConfig.debug) {
          $log.warn('Connection error code: ' + status);
        }
        syncAjaxObj_ = null;
      }
    };


    /**
     * When unloading, run a sync one last time.
     * @private
     */
    var unload_ = function() {
      if (!syncKillPid_) {
        // Turn off debug mode since the console disappears on page unload before
        // this code does.
        // MobWriteConfig.debug = false;
        synchronize();
      }
      // By the time the callback runs analyzeResponse, this page will probably
      // be gone.  But that's ok, we are just sending our last changes out, we
      // don't care what the server says.
    };


    // Attach unload event to window.
    if (window.addEventListener) {  // W3
      window.addEventListener('unload', unload_, false);
    } else if (window.attachEvent) {  // IE
      window.attachEvent('onunload', unload_);
    }


    /**
     * Start sharing the specified object(s).
     * @param {*} var_args Object(s) or ID(s) of object(s) to share.
     */
    var share = function(var_args) {
      var promises = [];
      for (var i = 0; i < arguments.length; i++) {
        var el = arguments[i];
        var result = null;
        // Ask every registered handler if it knows what to do with this object.
        for (var x = 0; x < shareHandlers.length && !result; x++) {
          result = shareHandlers[x](el);
        }
        if (result && result.file) {
          if (!result.file.match(/^[A-Za-z][-.:\w]*$/)) {
            if (MobWriteConfig.debug) {
              $log.error('Illegal id "' + result.file + '".');
            }
            continue;
          }
          if (result.file in shared) {
            // Already exists.
            // Don't replace, since we don't want to lose state.
            if (MobWriteConfig.debug) {
              $log.warn('Ignoring duplicate share on "' + el + '".');
            }
            continue;
          }
          shared[result.file] = result;

          if (syncRunPid_ === null) {
            // Startup the main task if it doesn't already exist.
            if (MobWriteConfig.debug) {
              $log.info('MobWrite task started.');
            }
          } else {
            // Bring sync forward in time.
            clearTimeout(syncRunPid_);
          }
          var deferredTimeout = $q.defer();
          promises.push(deferredTimeout.promise);
          syncRunPid_ = setTimeout(function () {
            synchronize().then(function (data) {
              deferredTimeout.resolve(data);
            }, function(error) {
              deferredTimeout.reject(error);
            });
          }, 10);
        } else {
          if (MobWriteConfig.debug) {
            $log.warn('Share: Unknown widget type: ' + el + '.');
          }
        }
      }
      return $q.all(promises);
    };


    /**
     * Stop sharing the specified object(s).
     * Does not handle forms recursively.
     * @param {*} var_args Object(s) or ID(s) of object(s) to unshare.
     */
    var unshare = function(var_args) {
      var promises = [];
      for (var i = 0; i < arguments.length; i++) {
        var el = arguments[i];
        if (typeof el == 'string' && shared.hasOwnProperty(el)) {
          var promise = synchronize();
          promises.push(promise);
          delete shared[el];
          if (MobWriteConfig.debug) {
            $log.info('Unshared: ' + el);
          }
        } else {
          // Pretend to want to share this object, acquire a new shareObj, then use
          // its ID to locate and kill the existing shareObj that's already shared.
          var result = null;
          // Ask every registered handler if it knows what to do with this object.
          for (var x = 0; x < shareHandlers.length && !result; x++) {
            result = shareHandlers[x](el);
          }
          if (result && result.file) {
            if (shared.hasOwnProperty(result.file)) {
              var promise = synchronize();
              promises.push(promise);
              delete shared[result.file];
              if (MobWriteConfig.debug) {
                $log.info('Unshared: ' + el);
              }
            } else {
              if (MobWriteConfig.debug) {
                $log.warn('Ignoring ' + el + '. Not currently shared.');
              }
            }
          } else {
            if (MobWriteConfig.debug) {
              $log.warn('Unshare: Unknown widget type: ' + el + '.');
            }
          }
        }
      }
      return $q.all(promises)
    };

    var convertMobwriteJsonToText = function (json) {
      var text = '';

      text += 'U:'+json.peerId + '\n';
      var messages = {};
      for (var i = 0; i < json.commands.length; i++) {
        var command = json.commands[i];
        // message
        if(command.json) {
          messages[command.from] = command.json;
        } else {
          text += 'F:'+command.revision+':'+command.filename + '\n';
          if(command.action.name == 'raw') {
            text += (command.action.overwrite?'R':'r')+':';
          } else if(command.action.name == 'delta') {
            text += (command.action.overwrite?'D':'d')+':';
          }
          if(command.action.revision) {
            text += command.action.revision + ':';
          }
          if(command.action.name == 'raw') {
            text += encodeURI(command.action.data);
          } else if(command.action.name == 'delta') {
            var delta = '';
            for (var j = 0; j < command.action.data.length; j++) {
              var d = command.action.data[j];
              var opt = d.charAt(0);
              d = d.substring(1)
              delta += opt + encodeURI(d).replace(/\x00/g, '%00').replace(/%20/g, ' ') + (j<command.action.data.length-1?'\t':'');
            }
            text += delta;
          }
          text += '\n';
        }
      };
      if(messages[syncUsername]) {
        text += 'm:'+JSON.stringify(messages);
      }
      return text+"\n";
    };

    var convertMobWriteTextToJson = function (text) {
      var jsonMobwrite = {
        commands: []
      };
      var lines = text.split('\n');
      var currentCommand = null;
      for (var i = 0; i < lines.length; i++) {
        var line = lines[i];
        var name = line.charAt(0);
        var value = line.substring(2);
        switch (name) {
          case 'U':
          case 'u':
            jsonMobwrite.peerId = value;
            break;
          case 'F':
          case 'f':
            currentCommand = {};
            var div = value.indexOf(':');
            var revision = parseInt(value.substring(0, div), 10);
            if (!isNaN(revision)) {
              currentCommand.revision = revision;
            }
            // remove revision
            value = value.substring(div + 1);
            div = value.indexOf('/');
            if(div > -1) {
              var paperId = value.substring(0, div);
              jsonMobwrite.paperId = paperId;

              // remove paperID
              value = value.substring(div + 1);
            }

            currentCommand.filename = value;
            jsonMobwrite.commands.push(currentCommand);
            break;
          case 'D':
          case 'd':
          case 'R':
          case 'r':
            var action = {
              name: (name == 'd' || name == 'D')?'delta':'raw'
            };
            var div = value.indexOf(':');
            var revision = parseInt(value.substring(0, div), 10);
            if (!isNaN(revision)) {
              action.revision = revision;
            }
            // remove revision
            value = value.substring(div + 1);

            if(name == 'd' || name == 'D') {
              action.data = value.replace(/%00/g,'\x00').replace(/ /g,'%20').split('\t'); 
              for (var j = 0; j < action.data.length; j++) {
                var d = action.data[j];
                var opt = d.charAt(0);
                action.data[j] = opt + decodeURI(d.substring(1));
              }
            } else {
              action.data = decodeURI(value);
            }

            action.overwrite = name == 'R' || name == 'D';
            if(currentCommand) {
              currentCommand.action = action;
            }
            break;
          case 'm':
          case 'M':
            var json = JSON.parse(value);
            for(var peerId in json) {
              var message = {
                from: peerId,
                filename: currentCommand.filename,
                json: json[peerId] 
              };
              jsonMobwrite.commands[jsonMobwrite.commands.length-1] =  message;
            }
            break;
        }
      }
      return jsonMobwrite;
    };

    // Copyright 2010 Google Inc. All Rights Reserved.
    /**
     * @fileoverview Converts a series of inserts/deletes against a base string
     *     (some of which may be overlapping) into a simplified diff (in which
     *     operations never overlap and are applied from end to beginning).
     *     This does not take the base string itself as an parameter, so its
     *     performance will not be directly affected by the size of the input.
     *
     * @author bloom@google.com (David Bloom)
     */

    /**
     * Creates a new simple differ.
     * @constructor
     */
    var SimpleDiffer = function() {
      this.chunks_ = [
        {
          start: 0,
          length: Infinity,
          type: 'copy'
        }
      ];
    };


    /**
     * An internal representation of how to construct the new text from an empty
     * document. This is made up of two types of chunks: "copy", which copies
     * text from the original document, and "insert", which inserts a string. We can
     * later figure out deletions from the original document by measuring the gap
     * between "copy" chunks, thus producing the simple diff.
     * @type {Array.<Object>} The chunks of the mutated document.
     * @private
     */
    SimpleDiffer.prototype.chunks_;


    /**
     * Pushes an insertion onto the sequence of mutations.
     * @param {number} index The character index to insert at.
     * @param {string} text The inserted text.
     */
    SimpleDiffer.prototype.applyInsert = function(index, text) {
      var x = this.findChunk_(index);
      var chunkIndex = x[0], splitOffset = x[1];
      this.splitChunk_(chunkIndex, splitOffset);
      var insertAtIndex = chunkIndex + 1;
      var newChunk = {
        type: 'insert',
        text: text,
        length: text.length
      };
      this.chunks_.splice(insertAtIndex, 0, newChunk);
    };


    /**
     * Pushes a deletion onto the sequence of mutations.
     * @param startIndex
     * @param endIndex
     */
    SimpleDiffer.prototype.applyDelete = function(startIndex, endIndex) {
      if (startIndex == endIndex) {
        return;
      }
      var start = this.findChunk_(startIndex);
      var startChunkIndex = start[0],
          startOffset = start[1];
      this.splitChunk_(startChunkIndex, startOffset);
      var deleteStartIndex = startChunkIndex + 1;
      var end = this.findChunk_(endIndex);
      var endChunkIndex = end[0],
          endOffset = end[1];
      this.splitChunk_(endChunkIndex, endOffset);
      var deleteEndIndex = endChunkIndex + 1;
      this.chunks_.splice(deleteStartIndex, deleteEndIndex - deleteStartIndex);
    };


    /**
     * Splits a chunk into to adjacent chunks, at a point within the chunk's text.
     * @param {number} chunkIndex The index of the chunk to split in this.chunks_.
     * @param {number} splitAt The text offset to split the chunk at.
     * @private
     */
    SimpleDiffer.prototype.splitChunk_ = function(chunkIndex, splitAt) {
      var chunk = this.chunks_[chunkIndex];
      var newChunk = {
        length: chunk.length - splitAt,
        type: chunk.type
      };
      chunk.length = splitAt;
      if (chunk.type == 'insert') {
        newChunk.text = chunk.text.substr(splitAt);
        chunk.text = chunk.text.substr(0, splitAt);
      } else if (chunk.type == 'copy') {
        newChunk.start = chunk.start + splitAt;
      }
      this.chunks_.splice(chunkIndex + 1, 0, newChunk);
    };


    /**
     * Finds the chunk at a character index in the text with all mutations so far
     * applied.
     * @param {number} targetCharIndex The character index to find the chunk for.
     * @return {Array} A tuple of the index of the chunk in chunks_, and the
     *     remainder character index within that chunk.
     * @private
     */
    SimpleDiffer.prototype.findChunk_ = function(targetCharIndex) {
      var currentCharIndex = 0;
      for (var i = 0, chunk; chunk = this.chunks_[i]; i++) {
        if (currentCharIndex + chunk.length > targetCharIndex) {
          return [ i, targetCharIndex - currentCharIndex ];
        }
        currentCharIndex += chunk.length;
      }
    };


    /**
     * Merges adjacent insert chunks.
     * @private
     */
    SimpleDiffer.prototype.mergeInserts_ = function() {
      var prevInsert = null;
      for (var i = 0; i < this.chunks_.length; i++) {
        var chunk = this.chunks_[i];
        if (chunk.type == 'insert') {
          if (chunk.text == '') {
            this.chunks_.splice(i, 1);
          } else if (prevInsert) {
            prevInsert.text += chunk.text;
            prevInsert.length += chunk.length;
            this.chunks_.splice(i, 1);
            i--;
          } else {
            prevInsert = chunk;
          }
        } else {
          prevInsert = null;
        }
      }
    };


    /**
     * Create a simple diff representing the mutations performed.
     * @return {Array.<Object>} The mutations from the simple diff, sorted from
     *     the end of the source to the beginning.
     */
    SimpleDiffer.prototype.getSimpleDiff = function() {
      this.mergeInserts_();
      var simpleDiff = [];
      var lastCopyCharIndex = Infinity;
      for (var i = this.chunks_.length - 1; i >= 0; i--) {
        var chunk = this.chunks_[i];
        if (chunk.type == 'copy') {
          var deletedChars = lastCopyCharIndex - (chunk.start + chunk.length);
          if (deletedChars > 0) {
            simpleDiff.push({
              type: 'delete',
              start: chunk.start + chunk.length,
              end: chunk.start +  chunk.length + deletedChars
            });
          }
          lastCopyCharIndex = chunk.start;
        } else if (chunk.type == 'insert') {
          simpleDiff.push({
            type: 'insert',
            start: lastCopyCharIndex,
            text: chunk.text
          })
        }
      }
      // Merge adjacent deletes in the simple diff. This is a very important
      // optimization -- without it, thousands of adjacent delete operations
      // generated by diff_match_patch.
      var prevDelete = null;
      for (var i = simpleDiff.length - 1; i >= 0; i--) {
        var action = simpleDiff[i];
        if (action.type == 'delete') {
          if (prevDelete) {
            if (prevDelete.end == action.start) {
              // Expand the previous delete mutation to include the contents of
              // the current one. Then, remove the current one from the simple diff.
              prevDelete.end = action.end;
              simpleDiff.splice(i, 1);
              continue;
            }
          }
          prevDelete = action;
        } else {
          prevDelete = null;
        }
      }
      return simpleDiff;
    };


    return {
      syncUsername: syncUsername,
      shareObj: shareObj,
      shared: shared,
      synchronize: synchronize,
      share: share,
      unshare: unshare,
      unload_: unload_,
      shareHandlers: shareHandlers,
      SimpleDiffer: SimpleDiffer,
      convertMobwriteJsonToText: convertMobwriteJsonToText,
      convertMobWriteTextToJson: convertMobWriteTextToJson
    }
  }]);
})();