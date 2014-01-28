'use strict';

angular.module('bluelatex.Shared.Directives.Autofocus', [])
  .directive('blAutofocus', function() {
     return function(scope, elem, attr) {
        elem[0].focus();
     };
  });