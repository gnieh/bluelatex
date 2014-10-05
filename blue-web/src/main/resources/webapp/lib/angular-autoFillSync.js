/**
 * autoFillSync
 * autoFillSync directive for AngularJS
 * By Ben Lesh 
 * From http://stackoverflow.com/questions/14965968/angularjs-browser-autofill-workaround-by-using-a-directive
 */

angular.module('autoFillSync', [] )
  .directive('autoFillSync', function($timeout) {
   return {
      require: 'ngModel',
      link: function(scope, elem, attrs, ngModel) {
          var origVal = elem.val();
          $timeout(function () {
              var newVal = elem.val();
              if(ngModel.$pristine && origVal !== newVal) {
                  ngModel.$setViewValue(newVal);
              }
          }, 500);
      }
   }
});