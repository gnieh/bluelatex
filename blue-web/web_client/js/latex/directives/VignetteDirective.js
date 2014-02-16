angular.module('bluelatex.Latex.Directives.Vignette', [])
  .directive('blVignette', ['$window', function($window) {
    return {
      'require': 'blVignette',
      'scope': {
        'page': "@",
        'scale': "=scale",
        'type': "=type",
        'url': "=url",
        'synctex': "=synctex",
        'current': "=current"
      },
      'controller': function($scope) {
        var pdf = null;
        var element;
        var viewport;
        var pdfDimension;

        $scope.hightlights = [{
          'height': "10px",
          'width': "200px",
          'left': "150px",
          'bottom': "25px"
        }];

        $scope.$watch("currentLine", function (line) {
          $scope.hightlights = [];
          if(!pdfDimension) return;
          if(!$scope.synctex) return;
          if(!$scope.synctex.blockNumberLine) return;
          if(!$scope.synctex.blockNumberLine[line]) return;
          var elems = $scope.synctex.blockNumberLine[line];

          if(!isArray(elems) || !elems[0]) return;

          var lines = [];
          var currentLine = {
            positionFirst: 0,
            minLeft: 0,
            maxLeft: 0,
            bottom: elems[0].bottom,
            height: elems[0].height
          };

          for (var i = elems.length - 1; i >= 1; i--) {
            var e=elems[i];
            if(e.page != $scope.page) continue;
            if(e.type!='x') continue;
            if(e.bottom!=currentLine.bottom){
              lines.push(currentLine);
              currentLine = {
                positionFirst: i,
                minLeft: i,
                maxLeft: i,
                bottom: elems[i].bottom,
                height: elems[i].height
              };
              continue;
            }
            if(e.left < elems[currentLine.minLeft].left) {
              currentLine.minLeft = i;
              currentLine.positionFirst = i - currentLine.positionFirst;
            } else if(e.left > elems[currentLine.maxLeft].left) {
              currentLine.maxLeft = i;
            }
            if(e.height>currentLine.height) {
              currentLine.height=e.height;
            }
          }
          lines.push(currentLine);

          for (var i = lines.length - 1; i >= 0; i--) {
            var line = lines[i];
            var minPosition = elems[line.minLeft].parent.elements.indexOf(elems[line.minLeft]);
            var maxPosition = elems[line.minLeft].parent.elements.indexOf(elems[line.maxLeft]);
            var max = elems[line.minLeft].parent.elements.length -1;

            var left = elems[line.minLeft].left;
            if(minPosition <= 1) {
              left = elems[line.minLeft].parent.left;
            } else if(elems[line.minLeft-1]){
              left = elems[line.minLeft-1].left;
            }
            var width = elems[line.maxLeft].left-left;
            if(maxPosition<max && elems[line.maxLeft-1]) {
              width = elems[line.maxLeft-1].left-left;
            }

            var s1 = convertToViewportPoint(left, line.bottom, pdfDimension);
            var s2 = convertToViewportPoint(width, line.height, pdfDimension);

            $scope.hightlights.push({
              height: (pdfDimension.height-s2[1])+"px",
              width: s2[0] + "px",
              left: s1[0] + "px",
              bottom: s1[1] + "px"
            });
          }
        });

        function loadPdf(pdfURI) {
            PDFJS.getDocument(pdfURI).then(renderPdf);
        }

        function loadImage (imageURI) {

        }

        function renderPdf(p) {
          pdf = p;
          pdf.getPage(1).then(renderPage);
        }

        function renderPage(page) {
          var parent = element[0];
          var ratio  = Number($scope.scale);
          console.log(element.width,parent.clientWidth);
          if(!ratio)
            ratio  = parent.clientWidth/page.getViewport(1.0).width;

          var textLayerDiv = parent.getElementsByClassName('textLayer')[0];
          var canvas = parent.getElementsByTagName('canvas')[0];
          var hightlights = parent.getElementsByTagName('hightlights')[0];

          var viewport = page.getViewport(ratio);
          pdfDimension = {
            scale: ratio,
            height: viewport.height
          };
          //Set the canvas height and width to the height and width of the viewport
          var context = canvas.getContext("2d");
          canvas.height = viewport.height;
          canvas.width = viewport.width;
          parent.style.height = viewport.height+'px';
          //parent.style.width = viewport.width+'px';

          textLayerDiv.innerHTML = '';

          //The following few lines of code set up scaling on the context if we are on a HiDPI display
          var outputScale = getOutputScale(context);
          if (outputScale.scaled ) {
              var cssScale = 'scale(' + (1 / outputScale.sx) + ', ' +
                  (1 / outputScale.sy) + ')';
              CustomStyle.setProp('transform', canvas, cssScale);
              CustomStyle.setProp('transformOrigin', canvas, '0% 0%');

              if (textLayerDiv) {
                  CustomStyle.setProp('transform', textLayerDiv, cssScale);
                  CustomStyle.setProp('transformOrigin', textLayerDiv, '0% 0%');
              }
              if(hightlights) {
                  CustomStyle.setProp('transform', hightlights, cssScale);
                  CustomStyle.setProp('transformOrigin', hightlights, '0% 0%');
              }
          }

          context._scaleX = outputScale.sx;
          context._scaleY = outputScale.sy;
          if (outputScale.scaled) {
              context.scale(outputScale.sx, outputScale.sy);
          }
          page.getTextContent().then(function (textContent) {
              var textLayer = new TextLayerBuilder({
                  textLayerDiv: textLayerDiv,
                  pageIndex: 0
              });

              textLayer.setTextContent(textContent);

              var renderContext = {
                  canvasContext: context,
                  viewport: viewport,
                  textLayer: textLayer
              };
              page.render(renderContext);
          });
        }

        $scope.resize = function (e) {
          element = e;
          if(pdf)
            pdf.getPage(1).then(renderPage);
        };

        $scope.loadPDF = function (e) {
          element = e;
          loadPdf($scope.url + '_' + $scope.page);
        };
      },
      'link': function($scope, element, attrs, controller) {
        var ratio = 1;
        var page = attrs.page;
        var scale = attrs.scale;

        var timeoutIDscale = null;
        $scope.$watch("scale", function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });
        attrs.$observe('scale', function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        attrs.$observe("line",function (value) {
          $scope.currentLine = value;
          console.log('line', value);
        });

        element.on('mousemove', function(event) {
          // Prevent default dragging of selected content
          event.preventDefault();

        });

        var timeoutIDResize = null;
        $window.onresize = function () {
          console.log("resize");
          clearTimeout(timeoutIDResize);
          timeoutIDResize = setTimeout(function () {
            $scope.resize(element);
          },500);
        };

        $scope.loadPDF(element);
      },
      'template': '<canvas></canvas><div class="textLayer"></div><div class="hightlights"><div class="hightlight_line" ng-repeat="hightlight in hightlights" style="height:{{hightlight.height}};width:{{hightlight.width}};left:{{hightlight.left}};bottom:{{hightlight.bottom}}"></div></div>'
    };
  }])
  .directive('blVignette2',['$document','$window', function($document,$window) {
    return function($scope, elem, attr) {
      var seuil = 1;

      var ratio = 1;
      var page = attr.page;
      var synctex = null;
      var scale = attr.scale;

      attr.$observe('scale', function(val){
        scale = val;
        // TODO: update scale
      });


      if(attr.type == 'pdf') {
        $scope.$watch('block', function (value) {
          for (var i = elem[0].getElementsByClassName('cursor').length - 1; i >= 0; i--) {
            elem[0].removeChild(elem[0].getElementsByClassName('cursor')[i]);
          }
          if(value != null) {

            if(value[0].page == page) {
              var x = value[0].block.xPosition* ratio;
              var y = value[0].block.yPosition* ratio;

              var cursor = $document[0].createElement("div");
              cursor.style.position = 'absolute';
              cursor.classList.add("cursor");
              cursor.style.top = (value[0].block.yPosition/65536)*ratio+"pt";
              cursor.style.left = (value[0].block.xPosition/65536)*ratio+"pt";
              cursor.style.width = "12px";
              cursor.style.height = "12px";
              cursor.style.background='red';
              elem[0].appendChild(cursor);
              return;
            }
          }
        });

        elem[0].onmousemove = function (event) {
          if($scope.synctex == null) return;
          var x=event.x-findPosX(elem[0]);
          var y=event.y-findPosY(elem[0]);
          for (var i = $scope.synctex.hBlocks.length - 1; i >= 0; i--) {
            var hBlock = $scope.synctex.hBlocks[i];
            if(page == hBlock.page &&
                y <= viewport.height - hBlock.bottom + seuil &&
                y >= viewport.height - hBlock.bottom - hBlock.height - seuil &&
                x >= hBlock.left - seuil &&
                x <= hBlock.left+hBlock.width + seuil ){
              //console.log('('+event.offsetY+','+event.offsetX+')', '('+(viewport.height-hBlock.bottom-hBlock.height - seuil)+','+(hBlock.left - seuil)+')', '('+(viewport.height-hBlock.bottom+seuil)+','+(hBlock.left+hBlock.width - seuil)+')', hBlock);
              for (var i = hBlock.elements.length - 1; i >= 1; i--) {
                var element = hBlock.elements[i];
                if(element.left >= x && hBlock.elements[i-1].left <= x ) {
                  $scope.currentElem = (i!=(hBlock.elements.length - 3)?hBlock.elements[i+1]:element);
                  $scope.$apply();
                  return;
                }
              }
              if(hBlock.elements[1]) {
                $scope.currentElem = hBlock.elements[1];
                $scope.$apply();
                return;
              }
              break;
            }
          }
          $scope.currentElem = {};
          $scope.$apply();
        };

        loadPdf(attr.url+'_'+page);
      }
    };
  }]);