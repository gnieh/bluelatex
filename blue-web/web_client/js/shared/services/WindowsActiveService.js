'use strict';

angular.module('bluelatex.Shared.Services.WindowActive', [])
  .factory("WindowActiveService", ['$document', '$window',
    function ($document, $window) {

      var observerCallbacks = [];

      //register an observer
      var registerObserverCallback = function(callback){
        observerCallbacks.push(callback);
      };

      //call this when you know 'foo' has been changed
      var notifyObservers = function(){
        angular.forEach(observerCallbacks, function(callback){
          callback();
        });
      };

      var windowActive = true;
      var hidden = "hidden";
      // Standards:
      if (hidden in $document)
        $document.addEventListener("visibilitychange", onchange);
      else if ((hidden = "mozHidden") in $document)
        $document.addEventListener("mozvisibilitychange", onchange);
      else if ((hidden = "webkitHidden") in $document)
        $document.addEventListener("webkitvisibilitychange", onchange);
      else if ((hidden = "msHidden") in $document)
        $document.addEventListener("msvisibilitychange", onchange);
      // IE 9 and lower:
      else if ('onfocusin' in $document)
        $document.onfocusin = $document.onfocusout = onchange;
      // All others:
      else
        $window.onpageshow = $window.onpagehide = $window.onfocus = $window.onblur = onchange;

      function onchange(evt) {
        var v = 'visible',
          h = 'hidden',
          evtMap = {
            focus: v,
            focusin: v,
            pageshow: v,
            blur: h,
            focusout: h,
            pagehide: h
          };

        evt = evt || $window.event;
        if (evt.type in evtMap) {
          windowActive = evtMap[evt.type] == v;
        } else {
          windowActive = this[hidden];
        }
        notifyObservers();
      }
      return {
        isActiveWindow: function () {
          return windowActive;
        },
        registerObserverCallback: registerObserverCallback
      };
    }
  ]);