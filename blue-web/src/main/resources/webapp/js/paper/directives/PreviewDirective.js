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
 
angular.module('bluelatex.Latex.Directives.Preview', ['bluelatex.Paper.Services.Paper'])
  .directive('blPreview', [
    '$rootScope',
    'PaperService',
    '$q', 
    function($rootScope,
             PaperService,
             $q) {

    return {
      'require': 'blPreview',
      'scope': {
        'scale': '=scale',
        'type': '=previewtype',
        'pdf': '=pdf',
        'paperId': '=paperid',
        'synctex': '=synctex',
        'revision': '=revision',
        'currentPage': '=currentpage',
        'currentLine': '=currentline',
        'linePage': '=linepage',
        'currentFile': '=currentfile',
        'displaysynctexbox': '@'
      },
      'controller': function($scope) {
        var element;
        var viewport;
        var pdfDimension;

        $scope.hightlights = [];

        /**
        * Update the line hightlight
        */
        var updateHightlight = function(file, line) {
          $scope.hightlights = [];
          (function() {
            var deferred = $q.defer();
            setTimeout(function(file, line) {
              var hightlights = [];
              // check in the pdf is loaded
              if(!pdfDimension) return;
              // check of the synctex file is loaded
              if(!$scope.synctex) return;
              if(!$scope.synctex.blockNumberLine) return;
              if(!$scope.synctex.blockNumberLine[file]) return;
              if(!$scope.synctex.blockNumberLine[file][line]) return;
              if(!$scope.synctex.blockNumberLine[file][line][$scope.page]) return;
              // get all elements of the current line
              var elems = $scope.synctex.blockNumberLine[file][line][$scope.page];
              if(!elems.length || !elems[0]) return;

              var lines = [];
              var cLine = {
                positionFirst: elems.length - 1,
                minLeft: elems.length - 1,
                maxLeft: elems.length - 1,
                bottom: elems[elems.length - 1].bottom,
                height: elems[elems.length - 1].height
              };
              // Create the largest box
              for (var i = elems.length - 2; i >= 0; i--) {
                var e=elems[i];
                if(e.page != $scope.page) continue;
                if(e.parent.type == "vertical") continue;
                if(e.type=='k') continue;
                if(e.bottom!=cLine.bottom){
                  var toAdd = true;
                  for (var  j= 0; j < lines.length && toAdd; j++) {
                    if(cLine.bottom >= lines[j].bottom && 
                       cLine.height <= lines[j].height  &&
                       cLine.minLeft >= lines[j].minLeft && 
                       cLine.maxLeft <= lines[j].maxLeft) {
                      toAdd = false;
                    }
                  };
                  if(toAdd) {
                    lines.push(cLine);
                  }
                  cLine = {
                    positionFirst: i,
                    minLeft: i,
                    maxLeft: i,
                    bottom: elems[i].bottom,
                    height: elems[i].height
                  };
                  continue;
                }
                if(e.left < elems[cLine.minLeft].left) {
                  cLine.minLeft = i;
                  cLine.positionFirst = i;
                } else if(e.left > elems[cLine.maxLeft].left) {
                  cLine.maxLeft = i;
                }
                if(e.height>cLine.height) {
                  cLine.height=e.height;
                }
              }

              var toAdd = true;
              for (var i = 0; i < lines.length && toAdd; i++) {
                if(cLine.bottom > lines[i].bottom && 
                   cLine.height < lines[i].height  &&
                   cLine.minLeft > lines[i].minLeft && 
                   cLine.maxLeft < lines[i].maxLeft) {
                  toAdd = false;
                }
              };
              if(toAdd) {
                lines.push(cLine);
              }

              // display all hightlight lines
              for (var i = lines.length - 1; i >= 0; i--) {
                var line = lines[i];
                var minPosition = elems[line.minLeft].parent.elements.indexOf(elems[line.minLeft]);
                var maxPosition = elems[line.minLeft].parent.elements.indexOf(elems[line.maxLeft]);

                var left = elems[line.minLeft].left;
                if(minPosition <= 1) {
                  left = elems[line.minLeft].parent.left;
                } else if(elems[line.minLeft-1]){
                  left = elems[line.minLeft-1].left;
                }
                var width = elems[line.maxLeft].left-left;

                var offset = convertToViewportPoint($scope.synctex.offset.x, $scope.synctex.offset.y, pdfDimension);
                var s1 = convertToViewportPoint(left, line.bottom, pdfDimension);
                var s2 = convertToViewportPoint(width, line.height, pdfDimension);

                hightlights.push({
                  height: (pdfDimension.height-s2[1])+'px',
                  width: s2[0] + "px",
                  left: ($scope.synctex.offset.x + s1[0]) + "px",
                  top : ($scope.synctex.offset.y + pdfDimension.height-s1[1]-(pdfDimension.height-s2[1])) + 'px'
                });
              }
              deferred.resolve(hightlights);
            },0,file, line);
            return deferred.promise;
          })().then(function(hightlights) {
            $scope.hightlights = hightlights;
            $rootScope.$$phase || $rootScope.$apply();
          });
        };
        // update the hightlight when the current line change
        $scope.$watch('currentLine', function (line, oldLine) {
          if(!line || line == oldLine) return;
          updateHightlight($scope.currentFile.title, line);
        });

        function renderPdf(p) {
          p.getPage(parseInt($scope.page)).then(renderPage);
        }
        // update preview when the pdf change
        $scope.$watch('pdf', function(pdf) {
          if(pdf!=null)
            renderPdf(pdf);
        });
        // create the pdf preview
        function renderPage(page) {
          var parent = element[0];
          var ratio  = Number($scope.scale);
          if(!ratio)
            ratio  = parent.clientWidth/page.getViewport(1.0).width;

          var containerDiv = parent.getElementsByClassName('container')[0];
          var textLayerDiv = parent.getElementsByClassName('textLayer')[0];
          var canvas = parent.getElementsByTagName('canvas')[0];
          var hightlights = parent.getElementsByTagName('hightlights')[0];

          var viewport = page.getViewport(ratio);
          pdfDimension = {
            scale: ratio,
            height: viewport.height
          };
          if($scope.synctex)
            updateHightlight($scope.currentFile.title, $scope.currentLine);
          //Set the canvas height and width to the height and width of the viewport
          var context = canvas.getContext('2d');
          var outputScale = getOutputScale(context);

          containerDiv.style.height = viewport.height + 'px';
          containerDiv.style.width = viewport.width + 'px';
          canvas.width = (Math.floor(viewport.width) * outputScale.sx) | 0;
          canvas.height = (Math.floor(viewport.height) * outputScale.sy) | 0;
          textLayerDiv.style.width = canvas.width + 'px';
          textLayerDiv.style.height = canvas.height + 'px';

          textLayerDiv.innerHTML = '';
          
          var cssScale = 'scale(' + (1 / outputScale.sx) + ', ' +
              (1 / outputScale.sy) + ')';
          PDFJS.CustomStyle.setProp('transform', canvas, cssScale);
          PDFJS.CustomStyle.setProp('transformOrigin', canvas, '0% 0%');

          if (textLayerDiv) {
              PDFJS.CustomStyle.setProp('transform', textLayerDiv, cssScale);
              PDFJS.CustomStyle.setProp('transformOrigin', textLayerDiv, '0% 0%');
          }
          if(hightlights) {
              PDFJS.CustomStyle.setProp('transform', hightlights, cssScale);
              PDFJS.CustomStyle.setProp('transformOrigin', hightlights, '0% 0%');
          }

          context._scaleX = outputScale.sx;
          context._scaleY = outputScale.sy;
          if (outputScale.scaled) {
              context.scale(outputScale.sx, outputScale.sy);
          }
          /*          
          // render PDF and text layer
          page.getTextContent().then(function (textContent) {
            var textLayer = new TextLayerBuilder({
              textLayerDiv: textLayerDiv,
              viewport: viewport,
              pageIndex: 0
            });
            textLayer.setTextContent(textContent);

            var renderContext = {
              canvasContext: context,
              viewport: viewport
            };

            page.render(renderContext);
          });*/
          var renderContext = {
            canvasContext: context,
            viewport: viewport
          };

          page.render(renderContext);
        }
        // resize the preview 
        $scope.resize = function (e) {
          element = e;
          if($scope.pdf) {
            $scope.pdf.getPage($scope.page).then(renderPage);
          } else {
            var img = element[0].getElementsByTagName('img')[0];
            if(img) {
              element[0] = img.parentElement;
              pdfDimension = {
                scale: img.width/(img.naturalWidth*0.72),
                height: img.height
              };
              if($scope.synctex)
                updateHightlight($scope.currentFile.title, $scope.currentLine);
            }
          }
        };
        $scope.init = function(e) {
          element = e;
          element.on('click', getCurrentLine);
        };

        $scope.loadImage = function (e) {
          setTimeout(function () {
            var img = e[0].getElementsByTagName('img')[0];
            img.onload=function (event) {
              pdfDimension = {
                scale: img.width/(img.naturalWidth*0.72),
                height: img.height
              };
            };
          },500);
        };

        // find the line associate to the position of the click event
        var seuil = 2;
        var getCurrentLine = function(event) {
          (function() {
            var deferred = $q.defer();
            setTimeout(function(event) {
              if($scope.synctex == null) return;
              var x=event.layerX;
              var y=event.layerY;
              var target = event.target;
              while(target != event.currentTarget && target!=null && target!=document) {
                x+=target.offsetLeft;
                y+=target.offsetTop;
                target = target.parent;
              }
              for (var i = $scope.synctex.hBlocks.length - 1; i >= 0; i--) {
                var hBlock = $scope.synctex.hBlocks[i];

                var s1 = convertToViewportPoint(hBlock.left, hBlock.bottom, pdfDimension);
                var s2 = convertToViewportPoint(hBlock.width, hBlock.height, pdfDimension);

                var dim = {
                  height: (pdfDimension.height-s2[1]),
                  width: s2[0],
                  left: $scope.synctex.offset.x + s1[0],
                  top : $scope.synctex.offset.y + s2[1]-s1[1]
                };
                if($scope.page == hBlock.page &&
                    y <= dim.top + dim.height + seuil &&
                    y >= dim.top - seuil &&
                    x >= dim.left - seuil &&
                    x <= dim.left + dim.width + seuil ){
                  //console.log('('+y+','+x+')', '('+(dim.top - seuil)+','+(dim.left - seuil)+')', '('+(dim.top + dim.height + seuil)+','+(dim.left + dim.width + seuil)+')', hBlock);
                  for (var i = hBlock.elements.length - 1; i >= 1; i--) {
                    var e = hBlock.elements[i];
                    if(e.left >= x && hBlock.elements[i-1].left <= x ) {
                      $scope.currentLine = (i<=(hBlock.elements.length - 3)?hBlock.elements[i+1].line:e.line);
                      if($scope.currentFile.title != e.file.name) {
                        $scope.$parent.$parent.changeFileFromName(e.file.name, $scope.currentLine).then(function() {
                          deferred.resolve($scope.currentLine);
                        });
                      } else {
                        $scope.$parent.$parent.goToLine($scope.currentLine);
                        deferred.resolve($scope.currentLine);
                      }
                      return;
                    }
                  }
                  if(hBlock.elements[1]) {
                    $scope.currentLine = hBlock.elements[1].line;
                    if($scope.currentFile.title != hBlock.file.name) {
                      $scope.$parent.$parent.changeFileFromName(hBlock.file.name);
                      (function(line) {
                        setTimeout(function(arguments) {
                          $scope.$parent.$parent.goToLine(line);
                          deferred.resolve(line);
                        }, 750);
                      })($scope.currentLine);
                    
                    } else {
                      $scope.$parent.$parent.goToLine($scope.currentLine);
                      deferred.resolve($scope.currentLine);
                    }
                    return;
                  }
                  break;
                }
              }
            },0,event);
            return deferred.promise;
          })().then(function() {
            $rootScope.$$phase || $rootScope.$apply();
          });
        };

        $scope.getUrlImagePreview = function () {
          return PaperService.getPNGUrl($scope.paperId,$scope.page);
        };

        /* DEBUG */
        var createBlock = function (b, page){
          var block = document.createElement('div');
          var s1 = convertToViewportPoint(b.left, b.bottom, pdfDimension);
          var s2 = convertToViewportPoint(b.width, b.height, pdfDimension);

          block.style.top = pdfDimension.height-s1[1]-(pdfDimension.height-s2[1]) + 'px';
          block.style.left = s1[0]+ 'px';
          block.style.width =  s2[0] + 'px';
          block.style.height = (pdfDimension.height-s2[1])+'px';
          block.classList.add(b.type);
          block.classList.add('debug_block');

          var info = document.createElement('div');
          info.classList.add('info');
          info.innerHTML = '<span class="file">'+b.fileNumber+'</span><span class="line">'+b.line+'</span>';
          block.appendChild(info);

          element[0].getElementsByClassName('container')[0].appendChild(block);
        };

        var createElem = function (elm ,page){
          //if(elm.type!='x') return;
          var block = document.createElement('div');

          if(pdfDimension==null) {
            var img = element[0].getElementsByTagName('img')[0];
            pdfDimension = {
              scale: img.height/(img.naturalHeight*0.72),
              height: img.height
            };
          }

          var s1 = convertToViewportPoint(elm.left, elm.bottom, pdfDimension);
          var s2 = convertToViewportPoint(elm.width, elm.height, pdfDimension);

          block.style.top = pdfDimension.height-s1[1]-(pdfDimension.height-s2[1]) + 'px';

          if(elm.width == 0 || !elm.width ){
            block.style.left = s1[0]+ 'px';
            block.style.width =  '1px';
          }else {
            block.style.left = s1[0]-s2[0]+ 'px';
            block.style.width =  s2[0]+'px';
          }
          block.style.height = (pdfDimension.height-s2[1])+'px';
          block.classList.add('debug_block');
          block.classList.add(elm.type);

          var info = document.createElement('div');
          info.classList.add('info');
          info.innerHTML = '<span class="file">'+elm.fileNumber+'</span><span class="line">'+elm.line+'</span>';
          block.appendChild(info);
          element[0].getElementsByClassName('container')[0].appendChild(block);
        };

        $scope.removeBlocks = function () {
          var blocks = element[0].getElementsByClassName('debug_block');
          for (var i = blocks.length - 1; i >= 0; i--) {
            var block = blocks[i];
            block.parentNode.removeChild(block);
          }
        };

        var displayBlocks = function (blocks, page) {
          if(!isArray(blocks)) return;
          for (var j = blocks.length - 1; j >= 0; j--) {
            var block = blocks[j];
            //if(block.type!='v block')continue;
            createBlock(block,page);
            displayBlocks(block.blocks,page);
            for (var i = block.elements.length - 1; i >= 0; i--) {
              var elm = block.elements[i];
              createElem(elm,page);
            }
          }
        };

        $scope.displayPages = function (pages) {
          if(pdfDimension== null) return;
          $scope.removeBlocks();
          displayBlocks(pages[$scope.page].blocks,$scope.page);
        };

      },
      'link': function($scope, element, attrs, controller) {
        var ratio = 1;
        var page = attrs.page;
        var scale = attrs.scale;
        $scope.page = parseInt(attrs.page);

        if(attrs.previewType == 'image' ||  attrs.previewType == 'pdf' ) {
          $scope.type = attrs.previewType;
        }

        var timeoutIDscale = null;
        $scope.$watch('scale', function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        $scope.$watch('linePage', function (val, oldPage) {
          if(!val) return;
          if(val == page && val != $scope.currentPage && oldPage == $scope.currentPage) {
            var autoscroll = attrs.autoscroll;
            if(autoscroll == null) { 
              autoscroll = false; 
            }
            //if(autoscroll == "true") {
              element[0].parentElement.scrollTop = element[0].offsetTop - 10;
            //}
          }
        });

        attrs.$observe('scale', function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        $scope.$watch('displaysynctexbox', function(val){
          if(val == true || val == 'true') {
            if($scope.synctex)
              $scope.displayPages($scope.synctex.pages);
          } else {
            $scope.removeBlocks();
          }
        });

        var timeoutIDResize = null;
        $rootScope.$on('windowResize', function (event, data) {
          clearTimeout(timeoutIDResize);
          timeoutIDResize = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        $scope.init(element);
        if($scope.type == 'pdf') {
          //$scope.loadPDF(element);
        } else if($scope.type == 'image') {
          $scope.loadImage(element);
        }
      },
      'template': '<div class="container"><img src="{{getUrlImagePreview()}}&{{revision}}" ng-if="type==\'image\'"><canvas ng-if="type==\'pdf\'" height="0"></canvas><div class="textLayer" ng-if="type==\'pdf\'"></div><div class="hightlights" ng-if="synctex"><div class="hightlight_line" ng-repeat="hightlight in hightlights" style="height:{{hightlight.height}};width:{{hightlight.width}};left:{{hightlight.left}};top:{{hightlight.top}}"></div></div></div>'
    };
  }]).directive('whenScrolled', function() {
    return function($scope, elm, attr) {
        var raw = elm[0];
        var scrollTimeout;
        elm.bind('scroll', function() {
          clearTimeout(scrollTimeout);
          scrollTimeout = setTimeout(function () {
            var max = null;
            var witchMax = 0;
            var prev = null;
            for (var i = 0; i < raw.children.length; i++) {
              var c = raw.children[i];
              var top = c.offsetTop - raw.scrollTop;
              var visible = 0;
              if(top < 0) {
                visible = c.offsetHeight + top;
              } else {
                visible = raw.offsetHeight - top;
              }

              visible/=c.offsetHeight;
              if(visible >= max || max == null) {
                witchMax = i;
                max = visible;
                if(max >= 1) {
                  break;
                }
              } else if(prev > visible){
                break;
              }
              prev = visible;
            }
            $scope.$apply(function () {
              $scope.$parent.currentPage = (witchMax +1);
            });
          }, 10);
        });
    };
});