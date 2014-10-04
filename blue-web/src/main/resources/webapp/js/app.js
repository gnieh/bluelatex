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

angular.module('bluelatex', [
  'localization',
  'ngRoute',
  'ngInputDate',
  'reCAPTCHA',
  'autoFillSync',
  'bluelatex.Paper.Controllers.InitPaper',
  'bluelatex.Paper.Controllers.LatexPaper',
  'bluelatex.Shared.Services.WindowActive',
  'bluelatex.Paper.Controllers.EditPaper',
  'bluelatex.Paper.Controllers.NewPaper',
  'bluelatex.Paper.Controllers.Papers',
  'bluelatex.Shared.Controllers.Main',
  'bluelatex.Shared.Controllers.Menu',
  'bluelatex.Shared.Directives.Menu',
  'bluelatex.Shared.Directives.Messages',
  'bluelatex.Shared.Controllers.Messages',
  'bluelatex.Shared.Directives.Autofocus',
  'bluelatex.Shared.Directives.tooltip',
  'bluelatex.User.Controllers.Login',
  'bluelatex.User.Controllers.Logout',
  'bluelatex.User.Controllers.Profile',
  'bluelatex.User.Controllers.Register',
  'bluelatex.User.Controllers.ResetPassword',
  'bluelatex.Latex.Directives.Preview',
  'bluelatex.Latex.Services.SyncTexParser'
]).config(['$routeProvider',
  function ($routeProvider) {
    /******************/
    /* Route settings */
    /******************/
    $routeProvider.when('/login', {
      templateUrl: 'partials/login.html',
      controller: 'LoginController',
      options: {
        name: 'login',
        connected: false,
        unconnected: true,
        title: 'Login'
      }
    });
    $routeProvider.when('/logout', {
      templateUrl: 'partials/logout.html',
      controller: 'LogoutController',
      options: {
        name: 'logout',
        connected: true,
        unconnected: false,
        title: 'Logout'
      }
    });
    $routeProvider.when('/register', {
      templateUrl: 'partials/register.html',
      controller: 'RegisterController',
      options: {
        name: 'register',
        connected: false,
        unconnected: true,
        title: 'Register'
      }
    });
    $routeProvider.when('/reset/?', {
      templateUrl: 'partials/reset.html',
      controller: 'ResetPasswordController',
      options: {
        name: 'reset',
        connected: false,
        unconnected: true,
        title: 'Login'
      }
    });
    $routeProvider.when('/:username/reset/:token/?', {
      templateUrl: 'partials/resetPassword.html',
      controller: 'ResetPasswordController',
      options: {
        name: 'resetPassword',
        connected: false,
        unconnected: true,
        title: 'Reset password'
      }
    });
    $routeProvider.when('/profile', {
      templateUrl: 'partials/profile.html',
      controller: 'ProfileController',
      options: {
        name: 'profile',
        connected: true,
        unconnected: false,
        title: 'Profile'
      }
    });
    $routeProvider.when('/papers', {
      templateUrl: 'partials/papers.html',
      controller: 'PapersController',
      options: {
        name: 'papers',
        connected: true,
        unconnected: false,
        title: 'Papers'
      }
    });
    $routeProvider.when('/paper/new', {
      templateUrl: 'partials/new_paper.html',
      controller: 'NewPaperController',
      options: {
        name: 'new_paper',
        connected: true,
        unconnected: false,
        title: 'New paper'
      }
    });
    $routeProvider.when('/paper/:id/edit', {
      templateUrl: 'partials/edit_paper.html',
      controller: 'EditPaperController',
      options: {
        name: 'edit_paper',
        connected: true,
        unconnected: false,
        title: 'Edit paper'
      }
    });
    $routeProvider.when('/paper/:id/?', {
      templateUrl: 'partials/paper.html',
      controller: 'InitPaperController',
      options: {
        name: 'paper',
        connected: true,
        unconnected: false,
        title: 'Paper'
      }
    });
    $routeProvider.when('/404/?', {
      templateUrl: 'partials/404.html',
      options: {
        name: 'new_paper',
        connected: true,
        unconnected: true,
        title: '404'
      }
    });
    $routeProvider.when('/', {
      redirectTo: '/papers',
      options: {}
    });

    $routeProvider.otherwise({
      redirectTo: '/404',
      options: {}
    });
  }
]).run(['$rootScope', '$location', '$route', '$window','$log','WindowActiveService',
  function ($rootScope, $location, $route, $window,$log,WindowActiveService) {
    $rootScope.loggedUser = null;
    var prev_page = null;
    $rootScope.$watch('loggedUser', function (value) {
      if (($rootScope.loggedUser == null || $rootScope.loggedUser.name == null) && $route.current != null && $route.current.$$route.options && !$route.current.$$route.options.unconnected) {
        // no logged user, we should be going to #login
        if ($route.current.$$route.options.name == "login") {
          // already going to #login, no redirect needed
        } else {
          prev_page = $location.path();
          // not going to #login, we should redirect now
          $location.path("/login");
        }
      } else if ($route.current != null && $route.current.$$route.options.connected == false && $rootScope.loggedUser != null  && $rootScope.loggedUser.name != null) {
        if (prev_page != null && prev_page != '/login') {
          $location.path(prev_page);
        } else {
          $location.path("/");
        }
      }
    });
    // register listener to watch route changes
    $rootScope.$on("$routeChangeStart", function (event, next, current) {
      if (($rootScope.loggedUser == null || $rootScope.loggedUser.name == null) && next.$$route != null && next.$$route.options && !next.$$route.options.unconnected) {
        // no logged user, we should be going to #login
        if (next.$$route.options.name == "login") {
          // already going to #login, no redirect needed
        } else {
          prev_page = $location.path();
          // not going to #login, we should redirect now
          $location.path("/login");
        }
      } else if (next.$$route != null && next.$$route.options && next.$$route.options.connected == false && $rootScope.loggedUser != null && $rootScope.loggedUser.name != null) {
        if (prev_page != null && prev_page != '/login') {
          $location.path(prev_page);
        } else {
          $location.path("/");
        }
      }
    });
    $rootScope.$on("$routeChangeStart", function (event, current, previous) {
      // change the title of the page
      if(current.$$route.options != null) {
        $rootScope.pageTitle = current.$$route.options.title;
      }
    });
  }
]);