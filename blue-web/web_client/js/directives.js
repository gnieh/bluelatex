'use strict';

/* Directives */

angular.module('myApp.directives', [])
  .directive('appVersion', ['version', function(version) {
    return function(scope, elm, attrs) {
      elm.text(version);
    };
  }]).directive('blToc', ['$compile',function($compile) {
    var updateTOC = function (elm, toc,$scope) {
        if(toc == null) return;
        var current = null;
        var currentlevel = -1;
        elm.text('');
        for (var i = 0; i < toc.length; i++) {
            var line = toc[i];
            //create a new ul/ol
            if(current == null){
                current = elm.context;
                var l = document.createElement(line.level<3?'ol':'ul');
                current.appendChild(l);
                current = l;
            } else if(line.restart == true) {
                var j = currentlevel;
                for(; j >= line.level; j--) {
                    current = current.parentElement;
                }
                var l = document.createElement('ul');
                current.appendChild(l);
                current = l;
            }else if(currentlevel < line.level) {
                var j = line.level;
                for(; j>currentlevel; j--) {
                    var t = document.createElement(j<3?'ol':'ul');
                    current.appendChild(t);
                    current = t;
                }
            } else if(currentlevel > line.level) {
                var j = currentlevel;
                for(; j > line.level; j--) {
                    current = current.parentElement;
                }
            }
            currentlevel = line.level;
            //create li
            var li = document.createElement('li');
            var a = document.createElement('a');
            a.setAttribute('ng-click','goToLine('+line.line+')');
            a.innerHTML = line.title;
            li.appendChild(a);
            current.appendChild(li);
        };
        var a_input = angular.element($compile(elm.html())($scope))
        elm.html(a_input);
    };
    return function(scope, elm, attrs) {
        scope.$watch('toc', function(value) {
            updateTOC(elm,value,scope);
        });
        updateTOC(elm,scope.toc,scope);
    };
  }])
