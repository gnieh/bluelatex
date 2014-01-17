'use strict';

/* Directives */

angular.module('myApp.directives', [])
  .directive('appVersion', ['version', function(version) {
    return function(scope, elm, attrs) {
      elm.text(version);
    };
  }]).directive('blGravatar', ['md5', function (md5) {
    return {
      scope: {
        email: '=email'
      },
      template: '<img src="https://secure.gravatar.com/avatar/{{email | gravatar}}?s=200&d=mm">'
    };
  }]);