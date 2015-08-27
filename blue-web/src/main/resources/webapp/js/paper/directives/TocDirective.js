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
 
// a directive used to dipslay a table of content
angular.module('bluelatex.Paper.Directives.Toc', [])
  .directive('blToc', ['$compile','$document',
    function ($compile,$document) {
      var updateTOC = function (elm, toc, $scope) {
        if (toc == null) return;
        var top = null;
        var current = null;
        var currentlevel = -1;
        var lastLi;
        var lastLine = 0;
        elm.text('');
        var numbers = {};
        for (var i = 0; i < toc.length; i++) {
          var line = toc[i];
          //create a new ul/ol
          if (current == null) {
            var l = $document[0].createElement('ul');
            current = l;
            top = current;
          } else if (line.ignore == true) {
            var j = currentlevel;
            for (; j >= line.level &&  current.parentElement != null; j--) {
              current = current.parentElement;
            }
             numbers[line.level] = 0;
            var l = $document[0].createElement('ul');
            if(current.parentElement == null && currentlevel == line.level) {
              angular.element(elm).append($compile(top)($scope));
              current = l;
              top = current;
            } else {
              current.appendChild(l);  
              current = l;
            }
          } else if (currentlevel < line.level) {
            var j = line.level;
            for (; j > currentlevel; j--) {
              // removes the handled levels
              delete numbers[j];
              var t = $document[0].createElement('ul');
              current.appendChild(t);
              current = t;
            }
          } else if (currentlevel > line.level) {
            var j = currentlevel;
            for (; j > line.level &&  current.parentElement != null; j--) {
              current = current.parentElement;
            }
          }

          currentlevel = line.level;
          if(!numbers[currentlevel]) {
            numbers[currentlevel] = 1;
          } else {
            numbers[currentlevel] ++;
          }
          var lineNumber = "";
          for (var j = 1; j <= currentlevel; j++) {
            if(numbers[j]) {
              lineNumber += numbers[j] + ".";
            }
          }

          //create a new li
          var li = $document[0].createElement('li');
          var a = $document[0].createElement('a');
          a.setAttribute('ng-click', 'goToLine(' + line.line + ')');
          a.innerHTML = lineNumber + " " + line.title;
          li.appendChild(a);
          current.appendChild(li);

          if(!lastLi) lastLi = li;
          if(lastLine <=$scope.currentLine  &&  line.line > $scope.currentLine) {
            lastLi.setAttribute('class', 'current');
          } else if(i==toc.length-1 && line.line <= $scope.currentLine) {
            li.setAttribute('class', 'current');
          }
          lastLine = line.line;
          lastLi = li;
        }
        if (toc.length == 0) {
          elm.html("No table of contents");
        } else {
          angular.element(elm).append($compile(top)($scope));
        }
      };
      return function (scope, elm, attrs) {
        // update table of content view when data is changed
        scope.$watch('toc', function (value) {
          updateTOC(elm, value, scope);
        });
        scope.$watch('currentLine', function (value) {
          updateTOC(elm, scope.toc, scope);
        });
        updateTOC(elm, scope.toc, scope);
      };
    }
  ]);