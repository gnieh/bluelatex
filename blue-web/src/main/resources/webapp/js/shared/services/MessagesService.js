'use strict';
angular.module('bluelatex.Shared.Services.Messages', [])
  .factory("MessagesService", ['localize','$sce',
    function (localize,$sce) {
      var errors = [];
      var messages = [];
      var warnings = [];

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
      /**
      * Remove all messages
      */
      function clean () {
        errors.splice(0,errors.length);
        messages.splice(0,messages.length);
        warnings.splice(0,warnings.length);
      }

      return {
        error: error,
        message: message,
        warning: warning,
        clean: clean,
        clear: clean,
        errors: errors,
        messages: messages,
        warnings: warnings,
        close: closeMessage
      };
    }
  ]);