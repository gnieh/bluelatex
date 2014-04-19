/*
* A directive used to display the menu
*/
angular.module('bluelatex.Shared.Directives.Menu', [])
  .directive('blMenu', function() {
    return {
      templateUrl: 'partials/menu.html'
    };
  });