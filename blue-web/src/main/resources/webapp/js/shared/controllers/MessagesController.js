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

angular.module('bluelatex.Shared.Controllers.Messages', ['bluelatex.Shared.Services.Messages'])
  .controller('MessagesController', ['$rootScope', '$scope', 'MessagesService','$log',
    function ($rootScope, $scope, MessagesService, $log) {
      // give access to messages, warnings and errors
      $scope.messages = MessagesService.messages;
      $scope.warnings = MessagesService.warnings;
      $scope.errors = MessagesService.errors;

      $scope.messagesSession = MessagesService.messagesSession;
      $scope.warningsSession = MessagesService.warningsSession;
      $scope.errorsSession = MessagesService.errorsSession;

      $scope.close = MessagesService.close;
    }
  ]);