'use strict';
// A directive witch give the focus on an element
angular.module('bluelatex.Shared.Directives.Autofocus', [])
  .directive('blAutofocus', function() {
     return function(scope, elem, attr) {
        elem[0].focus();
     };
  });