'use strict';

/* Services */


// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', []).
  value('version', '0.1')
  .factory("windowActiveService",['$scope',function ($scope) {
    var windowActive = false;
    var hidden = "hidden";
    // Standards:
    if (hidden in document)
        document.addEventListener("visibilitychange", onchange);
    else if ((hidden = "mozHidden") in document)
        document.addEventListener("mozvisibilitychange", onchange);
    else if ((hidden = "webkitHidden") in document)
        document.addEventListener("webkitvisibilitychange", onchange);
    else if ((hidden = "msHidden") in document)
        document.addEventListener("msvisibilitychange", onchange);
    // IE 9 and lower:
    else if ('onfocusin' in document)
        document.onfocusin = document.onfocusout = onchange;
    // All others:
    else
        window.onpageshow = window.onpagehide
            = window.onfocus = window.onblur = onchange;

    function onchange (evt) {
        var v = 'visible', h = 'hidden',
          evtMap = {
              focus:v, focusin:v, pageshow:v, blur:h, focusout:h, pagehide:h
          };

        evt = evt || window.event;
        if (evt.type in evtMap) {
          windowActive = evtMap[evt.type] == v;
          $scope.$broadcast('windowActive', windowActive);
        } else {
          windowActive = this[hidden];
          $scope.$broadcast('windowActive', windowActive);
        }
    }
    return {
      isActiveWindow: function () {
        return windowActive;
      }
    }
  }]);
