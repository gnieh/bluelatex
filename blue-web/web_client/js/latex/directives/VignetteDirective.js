angular.module('bluelatex.Latex.Directives.Vignette', ['bluelatex.Paper.Services.Paper'])
  .directive('blVignette', ['PaperService', function(PaperService) {
    return {
      'require': 'blVignette',
      'scope': {
        'page': "@",
        'scale': "=scale",
        'type': "=vignettetype",
        'paperId': "=paperid",
        'synctex': "=synctex",
        'revision': "=revision",
        'currentPage': "=currentpage",
        'currentLine': "=currentline"
      },
      'controller': function($scope) {
        var pdf = null;
        var element;
        var viewport;
        var pdfDimension;

        $scope.hightlights = [];

        var updateHightlight = function(line) {
          $scope.hightlights = [];
          $scope.$apply();
          if(!pdfDimension) return;
          if(!$scope.synctex) return;
          if(!$scope.synctex.blockNumberLine) return;
          if(!$scope.synctex.blockNumberLine[line]) return;
          var elems = $scope.synctex.blockNumberLine[line];

          if(!isArray(elems) || !elems[0]) return;

          var lines = [];
          var cLine = {
            positionFirst: 0,
            minLeft: 0,
            maxLeft: 0,
            bottom: elems[0].bottom,
            height: elems[0].height
          };

          for (var i = elems.length - 1; i >= 0; i--) {
            var e=elems[i];
            if(e.page != $scope.page) continue;
            //if(e.type!='x') continue;
            if(e.bottom!=cLine.bottom){
              lines.push(cLine);
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
              cLine.positionFirst = i - cLine.positionFirst;
            } else if(e.left > elems[cLine.maxLeft].left) {
              cLine.maxLeft = i;
            }
            if(e.height>cLine.height) {
              cLine.height=e.height;
            }
          }
          lines.push(cLine);

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
              top :pdfDimension.height-s1[1]-(pdfDimension.height-s2[1]) + 'px'
            });
          }
          $scope.$apply();
        };

        $scope.$watch("currentLine", function (line) {
          if(!line) return;
          updateHightlight(line);
        });
        $scope.$watch("revision", function () {
          if(pdf)
            $scope.loadPDF(element);
        });
        function loadPdf(pdfURI) {
            PDFJS.getDocument(pdfURI).then(renderPdf);
        }

        function loadImage (imageURI) {

        }

        function renderPdf(p) {
          pdf = p;
          pdf.getPage($scope.page).then(renderPage);
        }

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
          updateHightlight($scope.currentLine);
          //Set the canvas height and width to the height and width of the viewport
          var context = canvas.getContext("2d");
          containerDiv.style.height = viewport.height+"px";
          containerDiv.style.width = viewport.width+"px";
          canvas.height = viewport.height;
          canvas.width = viewport.width;

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
          if(pdf) {
            pdf.getPage($scope.page).then(renderPage);
          } else {
            var img = element[0].getElementsByTagName("img")[0];
            if(img) {
              element[0] = img.parentElement;
              pdfDimension = {
                scale: img.width/(img.naturalWidth*0.72),
                height: img.height
              };
              updateHightlight($scope.currentLine);
            }
          }
        };

        $scope.loadPDF = function (e) {
          element = e;
          element.on('click', getCurrentLine);
          loadPdf(PaperService.getPDFUrl($scope.paperId,$scope.page)+"?"+$scope.revision);
        };
        $scope.loadImage = function (e) {
          element = e;
          element.on('click', getCurrentLine);
          setTimeout(function () {
            var img = element[0].getElementsByTagName("img")[0];
            img.onload=function (event) {
              pdfDimension = {
                scale: img.width/(img.naturalWidth*0.72),
                height: img.height
              };
            };
          },500);
        };
        var seuil = 2;
        var getCurrentLine = function(event) {
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
              left: s1[0],
              top : s2[1]-s1[1]
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
                  $scope.currentLine = (i!=(hBlock.elements.length - 3)?hBlock.elements[i+1].line:e.line);
                  $scope.$parent.$parent.goToLine($scope.currentLine);
                  $scope.$apply();
                  return;
                }
              }
              if(hBlock.elements[1]) {
                $scope.currentLine = hBlock.elements[1].line;
                $scope.$parent.$parent.goToLine(hBlock.elements[1].line);
                $scope.$apply();
                return;
              }
              break;
            }
          }
        };

        $scope.getUrlVignette = function () {
          return PaperService.getPNGUrl($scope.paperId,$scope.page);
        };
      },
      'link': function($scope, element, attrs, controller) {
        var ratio = 1;
        var page = attrs.page;
        var scale = attrs.scale;

        if(attrs.vignettetype == "image" ||  attrs.vignettetype == "pdf" ) {
          $scope.type = attrs.vignettetype;
        }

        var timeoutIDscale = null;
        $scope.$watch("scale", function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        $scope.$watch("currentPage", function (val, oldPage) {
          if(!val)return;
          if(val == page && val != oldPage) {
            element[0].parentElement.scrollTop = element[0].offsetTop -10;
          }
        });

        attrs.$observe("scale", function(val){
          if(!val)return;
          clearTimeout(timeoutIDscale);
          timeoutIDscale = setTimeout(function () {
            $scope.resize(element);
          },500);
        });

        var timeoutIDResize = null;
        $scope.$on("windowResize", function (event, data) {
          clearTimeout(timeoutIDResize);
          timeoutIDResize = setTimeout(function () {
            $scope.resize(element);
          },500);
        });


        if($scope.type == "pdf") {
          $scope.loadPDF(element);
        } else if($scope.type == "image") {
          $scope.loadImage(element);
        }
      },
      'template': '<div class="container"><img src="{{getUrlVignette()}}&{{revision}}" ng-if="type==\'image\'"><canvas ng-if="type==\'pdf\'"></canvas><div class="textLayer" ng-if="type==\'pdf\'"></div><div class="hightlights" ng-if="synctex"><div class="hightlight_line" ng-repeat="hightlight in hightlights" style="height:{{hightlight.height}};width:{{hightlight.width}};left:{{hightlight.left}};top:{{hightlight.top}}"></div></div></div>'
    };
  }]);