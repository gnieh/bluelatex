/*
* A directive used to display messages
*/
angular.module('bluelatex.Shared.Directives.Messages', [])
  .directive('blMessages', function() {
    return {
      templateUrl: 'partials/messages.html'
    };
  });