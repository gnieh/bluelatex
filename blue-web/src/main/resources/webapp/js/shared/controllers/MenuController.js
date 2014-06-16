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
 
angular.module('bluelatex.Shared.Controllers.Menu', ['bluelatex.Gravatar'])
  .controller('MenuController', ['$rootScope', '$scope','localize',
    function ($rootScope, $scope,localize) {
      $scope.openUserMenu = false;
      $scope.menus = [];
      $scope.options = {};
      $scope.pageName = null;

      // close the user menu when  the user is not connected
      $rootScope.$watch('loggedUser', function (value) {
        if (value == null || value.name == null) {
          $scope.openUserMenu = false;
        }
        $scope.user = value;
      });

      // change the menu when the page changes
      $scope.$on("$routeChangeSuccess", function (event, route) {
        if (route.$$route == null || route.$$route.options == null) return;
        for (var i = 0; i < $scope.menus.length; i++) {
          $scope.menus[i].active = route.$$route.options.name == $scope.menus[i].name;
          $scope.pageName = route.$$route.options.name;
        }
      });
      $scope.menus = [{
        label: '_New_paper_',
        link: '#/paper/new',
        name: 'new_paper',
        connected: true,
        unconnected: false,
        active: false
      }, {
        label: '_Papers_',
        link: '#/papers',
        name: 'papers',
        connected: true,
        unconnected: false,
        active: false
      }, {
        label: '_LoginTitle_',
        link: '#/login',
        name: 'login',
        connected: false,
        unconnected: true,
        active: false
      }, {
        label: '_Register_',
        link: '#/register',
        name: 'register',
        connected: false,
        unconnected: true,
        active: false
      }];

      $scope.options = {
        'papers': [
        ],
        'paper': [
          {
            label: '_Share_',
            type: 'menu',
            class: '',
            icon: null,
            action: 'partials/paper_share_menu.html'
          }, {
            label: '_Send_to_Arxiv_',
            type: 'action',
            class: '',
            icon: null,
            action: 'arxiv'
          }, {
            label: '_Settings_',
            type: 'menu',
            class: '',
            icon: null,
            action: 'partials/paper_settings.html'
          }
        ]
      };
      $rootScope.$on('localizeResourcesUpdated', function () {
        for (var i = $scope.menus.length - 1; i >= 0; i--) {
          $scope.menus[i].label = localize.getLocalizedString($scope.menus[i].label);
        };
        for(var i in $scope.options) {
          for (var j = $scope.options[i].length - 1; j >= 0; j--) {
            $scope.options[i][j].label = localize.getLocalizedString($scope.options[i][j].label);
          };
        }
      });

      $scope.action = function (option) {
        if (option.type == 'action') {
          $rootScope.$emit('handleMenuAction', option.action);
        } else if (option.type == 'menu') {
          option.display = !option.display;
        }
        return false;
      };
    }
  ]);
