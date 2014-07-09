/*
 * This file is part of the \BlueLaTeX project.
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
 
'use strict';
angular.module('bluelatex.Shared.Services.Messages', [])
  .factory("MessagesService", ['$rootScope','localize','$sce',
    function ($rootScope,localize,$sce) {
      var errorsSession = [],
          messagesSession = [],
          warningsSession = [];
      var errors = [],
          messages = [],
          warnings = [];

      $rootScope.$on("$routeChangeSuccess", function (event, route) {
        clearNotSession();
      });

      /**
      * Close a message
      */
      var closeMessage = function (message) {
        // if the message is an error
        if(errors.indexOf(message)>=0) {
          errors.splice(errors.indexOf(message), 1);
        // if the message is a message
        } else if(messages.indexOf(message)>=0) {
          messages.splice(messages.indexOf(message), 1);
        // if the message is a warning
        } else if(warnings.indexOf(message)>=0) {
          warnings.splice(warnings.indexOf(message), 1);
        }else if(errorsSession.indexOf(message)>=0) {
          errorsSession.splice(errorsSession.indexOf(message), 1);
        }else if(messagesSession.indexOf(message)>=0) {
          messagesSession.splice(messagesSession.indexOf(message), 1);
        }else if(warningsSession.indexOf(message)>=0) {
          warningsSession.splice(warningsSession.indexOf(message), 1);
        }
      };
      /**
      * Translate the message
      */
      var getMessageLocalized = function (m) {
        var tempMessage = localize.getLocalizedString(m);
        if(tempMessage == '' || tempMessage == null) {
          tempMessage = m.replace(/_/g,' ').trim();
        }
        return tempMessage;
      };
      /**
      * Translate and add the message in the array
      */
      function pushMessage(array, m) {
        array.push($sce.trustAsHtml(getMessageLocalized(m)));
      }
      /**
      * Add an error message
      */
      function error (m, err) {
        pushMessage(errors,m);
      }
      /**
      * Add a message
      */
      function message (m, mess) {
        pushMessage(messages,m);
      }
      /**
      * Add a warning
      */
      function warning (m, wra) {
        pushMessage(warnings,m);
      }
      /*
      * clear messages
      */
      function clearNotSession () {
        errors.splice(0,errors.length);
        messages.splice(0,messages.length);
        warnings.splice(0,warnings.length);
      }
      /*
      * clear session messages
      */
      function clearSession () {
        errorsSession.splice(0,errors.length);
        messagesSession.splice(0,errors.length);
        warningsSession.splice(0,errors.length);
      }
      /**
      * Remove all messages
      */
      function clean () {
        clearNotSession();
        clearSession();
      }

      return {
        error: error,
        message: message,
        warning: warning,
        errorSession: function(m) {
          pushMessage(errorsSession,m);
        },
        messageSession: function(m) {
          pushMessage(messagesSession,m);
        },
        warningSession: function(m) {
          pushMessage(warningsSession,m);
        },
        clean: clean,
        clear: clean,
        errors: errors,
        errorsSession: errorsSession,
        messages: messages,
        messagesSession: messagesSession,
        warnings: warnings,
        warningsSession: warningsSession,
        close: closeMessage
      };
    }
  ]);