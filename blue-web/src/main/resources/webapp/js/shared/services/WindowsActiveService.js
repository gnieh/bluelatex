/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
'use strict';
/**
* A simple module that launch event when the page is out of fucus et on focus
*/
angular.module('bluelatex.Shared.Services.WindowActive', [])
  .factory("WindowActiveService", ['$document', '$window',
    function ($document, $window) {

      var observerCallbacks = [];

      //register an observer
      var registerObserverCallback = function(callback){
        observerCallbacks.push(callback);
      };

      var removeObserverCallback = function(callback){
        observerCallbacks.splice(observerCallbacks.indexOf(callback), 1);
      };

      //call this when you know 'foo' has been changed
      var notifyObservers = function(){
        angular.forEach(observerCallbacks, function(callback){
          callback(windowActive);
        });
      };

      var windowActive = true;
      var hidden = "hidden";

      //register event
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
        registerObserverCallback: registerObserverCallback,
        removeObserverCallback: removeObserverCallback
      };
    }
  ]);