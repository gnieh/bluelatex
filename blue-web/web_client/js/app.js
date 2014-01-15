'use strict';

// Declare app level module which depends on filters, and services
angular.module('bluelatex', [
  'localization',
  'ngRoute',
  'myApp.filters',
  'myApp.services',
  'myApp.directives',
  'bluelatex.User',
  'bluelatex.controller',
  'bluelatex.paper',
  'bluelatex.papers',
  'bluelatex.menu',
  'ui.ace'
]).config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/login', {templateUrl: 'partials/login.html', controller: 'LoginLogoutController', options:{
    name: 'login',
    private: false,
    title: 'Login'
  }});
  $routeProvider.when('/logout', {controller: 'LoginLogoutController',options:{
    name: 'logout', private: false, title: 'Logout'
  }});
  $routeProvider.when('/register', {templateUrl: 'partials/register.html', controller: 'RegisterController',options:{
    name: 'register', private: false, title: 'Register'
  }});
  $routeProvider.when('/reset/?', {templateUrl: 'partials/reset.html', controller: 'ResetController',options:{
    name: 'reset', private: false, title: 'Login'
  }});
  $routeProvider.when('/:username/reset/:token/?', {templateUrl: 'partials/resetPassword.html', controller: 'ResetController',options:{
     name: 'resetPassword', private: false, title: 'Rset password'
  }});
  $routeProvider.when('/profil', {templateUrl: 'partials/partial2.html', controller: 'MyCtrl2',options:{
    name: 'profile', private: true, title: 'Profil'
  }});
  $routeProvider.when('/papers', {templateUrl: 'partials/papers.html', controller: 'PapersController',options:{
    name: 'papers', private: false, title: 'Papers'
  }});
  $routeProvider.when('/paper/:id/?', {templateUrl: 'partials/paper.html', controller: 'PaperController',options:{
    name: 'paper', private: false, title: 'Paper'
  }});
  $routeProvider.otherwise({redirectTo: '/papers'});
}]).run( function($rootScope, $location) {
    // register listener to watch route changes
    $rootScope.$on( "$routeChangeStart", function(event, next, current) {
      if ( $rootScope.loggedUser == null && next.$$route != null && next.$$route.options.private) {
        // no logged user, we should be going to #login
        if ( next.$$route.options.name == "login" ) {
          // already going to #login, no redirect needed
        } else {
          // not going to #login, we should redirect now
          $location.path( "/login" );
        }
      } else if ( next.$$route!= null && next.$$route.options.private == false && $rootScope.loggedUser != null) {
        $location.path( "/" );
      }
    });
});