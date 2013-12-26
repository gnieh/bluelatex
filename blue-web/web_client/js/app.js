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
  'bluelatex.menu',
  'ui.ace'
]).config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/login', {templateUrl: 'partials/login.html', controller: 'LoginController', name: 'login', private: false});
  $routeProvider.when('/logout', {templateUrl: 'partials/partial2.html', controller: 'MyCtrl2', name: 'logout', private: false});
  $routeProvider.when('/register', {templateUrl: 'partials/register.html', controller: 'RegisterController', name: 'register', private: false});
  $routeProvider.when('/profile', {templateUrl: 'partials/partial2.html', controller: 'MyCtrl2', name: 'profile', private: true});
  $routeProvider.when('/papers', {templateUrl: 'partials/partial1.html', controller: 'MyCtrl1', name: 'papers', private: true});
  $routeProvider.when('/paper/:id/?', {templateUrl: 'partials/paper.html', controller: 'PaperController', name: 'paper', private: false});
  $routeProvider.otherwise({redirectTo: '/papers'});
}]).run( function($rootScope, $location) {
    // register listener to watch route changes
    $rootScope.$on( "$routeChangeStart", function(event, next, current) {
      if ( $rootScope.loggedUser == null && next.$$route.private == true) {
        // no logged user, we should be going to #login
        if ( next.$$route.name == "login" ) {
          // already going to #login, no redirect needed
        } else {
          // not going to #login, we should redirect now
          $location.path( "/login" );
        }
      } else if ( next.$$route.private == false && $rootScope.loggedUser != null) {
        $location.path( "/" );
      }
    });
});