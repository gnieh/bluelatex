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
/**
* The main controller of the website, this controller handle window resize event
* and retransmit events launched by the menu
*/
angular.module('bluelatex.Shared.Controllers.Main', [
  'bluelatex.User.Services.User',
  'bluelatex.User.Services.Session',
  'bluelatex.Paper.Services.Ace',
  'bluelatex.Shared.Services.WindowActive'])
  .controller('MainController', 
    ['$rootScope', 
     '$scope',
     '$window',
     'UserService',
     'SessionService',
     '$route',
     '$location',
     '$routeParams',
     'AceService',
     'WindowActiveService',
     'config',
    function ($rootScope,
              $scope,
              $window,
              UserService,
              SessionService,
              $route,
              $location,
              $routeParams,
              AceService,
              WindowActiveService,
              config) {

      $scope.config = config;

      // give access to current route
      $scope.$route = $route;
      // give access to current url
      $scope.$location = $location;
      // give access to current path parameters
      $scope.$routeParams = $routeParams;
      // give access to aceService
      $scope.ace = AceService;

      // retransmits window resize event
      $window.onresize = function (event) {
        $rootScope.$broadcast('windowResize', event);
      };

      //
      $scope.$on("$routeChangeSuccess", function (event, route) {
        $scope.currentRoute = route;
      });

      // login the user with data present in sessionStorage
      var getUserSession = function () {
        if ($rootScope.loggedUser == null || $rootScope.loggedUser.name ) {
          SessionService.getSession().then(function (data) {
            UserService.getInfo({
              name: data.name
            }).then(function (data) {
              $rootScope.loggedUser = data;
              $rootScope.$$phase || $rootScope.$apply();
            }, function (error) {
              $rootScope.loggedUser = {};
            });
          }, function (error) {
            $rootScope.loggedUser = {};
            $rootScope.$$phase || $rootScope.$apply();
          });
        }
      };

      //check user session every  minute
      var interval = 1 /* min */ * 60 /* sec */ * 1000 /* nano-sec*/;
      var intervalId = setInterval(getUserSession, interval);
      getUserSession();

      // don't check session when the page is not active
      WindowActiveService.registerObserverCallback(function () {
        var data = WindowActiveService.isActiveWindow();
        if (data == false) {
          clearInterval(intervalId);
          intervalId = null;
        } else if (intervalId == null) {
          intervalId = setInterval(getUserSession, interval);
          getUserSession();
        }
      });
    }
  ]);