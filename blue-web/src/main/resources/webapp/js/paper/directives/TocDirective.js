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
        for (var i = 0; i < toc.length; i++) {
          var line = toc[i];
          //create a new ul/ol
          if (current == null) {
            var l = $document[0].createElement(line.level < 3 ? 'ol' : 'ul');
            current = l;
            top = current;
          } else if (line.restart == true) {
            var j = currentlevel;
            for (; j >= line.level; j--) {
              current = current.parentElement;
            }
            var l = $document[0].createElement('ul');
            current.appendChild(l);
            current = l;
          } else if (currentlevel < line.level) {
            var j = line.level;
            for (; j > currentlevel; j--) {
              var t = $document[0].createElement(j < 3 ? 'ol' : 'ul');
              current.appendChild(t);
              current = t;
            }
          } else if (currentlevel > line.level) {
            var j = currentlevel;
            for (; j > line.level; j--) {
              current = current.parentElement;
            }
          }

          currentlevel = line.level;

          //create a new li
          var li = $document[0].createElement('li');
          var a = $document[0].createElement('a');
          a.setAttribute('ng-click', 'goToLine(' + line.line + ')');
          a.innerHTML = line.title;
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