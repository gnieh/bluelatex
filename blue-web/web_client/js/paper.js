angular.module('bluelatex.paper',[])
    .factory("Paper", function ($resource, $http) {
      var paper =  $resource( apiRoot+"/papers/:paper_id", null, {
          "new": {
            method: "POST", headers:{'Content-Type':'application/x-www-form-urlencoded'},
            format: 'json',
            isArray: false,
            transformResponse: [function(data, headersGetter) {
              if(data = 'true')
              return {response: data};
              return data;
            }].concat($http.defaults.transformResponse)
          },
          "delete": {method: "DELETE"}
        }
      );
      var info =  $resource( apiRoot+"/papers/:paper_id/info", null, {
          "edit": {method: "PATCH", headers:{'Content-Type':'application/x-www-form-urlencoded'}},
          "get": {
            method: "GET",
            transformResponse: [function(data, headersGetter) {
              data = JSON.parse(data);
              data.header = headersGetter();
              return data;
            }].concat($http.defaults.transformResponse)
          }
        }
      );
      var synchronizedFile =  $resource( apiRoot+"/papers/:paper_id/files/synchronized", null, {
          "get": {method: "GET"}
        }
      );
      var resources =  $resource( apiRoot+"/papers/:paper_id/files/resources/:resource", null, {
          "get": {method: "GET"},
          "upload": {method: "POST"},
          "delete": {method: "DELETE"}
        }
      );
      var resources =  $resource( apiRoot+"/papers/:paper_id/files/resources/:resource", null, {
          "get": {method: "GET"},
          "upload": {method: "POST"},
          "delete": {method: "DELETE"}
        }
      );
      return {
        create: function (p) {
          return paper.new({},jsonToPostParameters(p)).$promise;
        },
        delete: function(paper_id) {
          return paper.delete({ paper_id: paper_id }).$promise;
        },
        getInfo: function (paper_id) {
          return info.get({ paper_id: paper_id }).$promise;
        },
        modify: function (paper) {
          return info.edit({ paper_id: paper.id },jsonToPostParameters(paper)).$promise;
        },
        getResources: function (paper_id) {
          return resources.get({ paper_id: paper_id }).$promise;
        },
        getResource: function (paper_id, resource) {
          return resources.get({ paper_id: paper_id, resource: resource }).$promise;
        },
        uploadResource: function (paper_id,resource, data) {
          return resources.upload({ paper_id: paper_id, resource: resource },jsonToPostParameters(data)).$promise;
        },
        removeResource: function (paper_id,resource) {
          return resources.delete({ paper_id: paper_id, resource: resource }).$promise;
        },
        getSynchronized: function (paper_id) {
          return synchronizedFile.get({ paper_id: paper_id }).$promise;
        },
        getZipUrl: function (paper_id) {
          return apiRoot+"/papers/"+paper_id+".zip";
        }
      };
    }).factory("ace", function () {
      var content;
      var _session;
      var _editor;
      var _renderer;

      var aceSettings = {
        fontSize: '12px',
        readOnly: false,
        useWrapMode : true,
        showGutter: true,
        theme:'ace/theme/textmate',
        mode: 'ace/mode/latex',
        modeName: 'latex',
        softWrap: 'free',
        keyBinding: 'ace',
        fullLineSelection: true,
        highlightActiveLine: true,
        showInvisibles: false,
        showIndentGuides: true,
        showPrintMargin: true,
        useSoftTab: true,
        highlightSelectedWord: true,
        enableBehaviours: false,
        fadeFoldWidgets: false,
        incrementalSearch: false
      };
      if(localStorage.aceSettings == null){
        localStorage.aceSettings = angular.toJson(aceSettings);
      } else {
        aceSettings = angular.fromJson(localStorage.aceSettings);
      }

      var loadSettings = function () {
        localStorage.aceSettings = angular.toJson(aceSettings);

        _editor.setTheme(aceSettings.theme);
        _editor.setFontSize(aceSettings.fontSize);
        _editor.setReadOnly(aceSettings.readOnly);
        _editor.setKeyboardHandler(aceSettings.keyBinding);
        _editor.setSelectionStyle(aceSettings.fullLineSelection?'line':'text');
        _editor.setHighlightActiveLine(aceSettings.highlightActiveLine);
        _editor.setShowInvisibles(aceSettings.showInvisibles);
        _editor.setDisplayIndentGuides(aceSettings.showIndentGuides);

        _editor.renderer.setShowPrintMargin(aceSettings.showPrintMargin);
        _editor.setHighlightSelectedWord(aceSettings.highlightSelectedWord);
        _editor.session.setUseSoftTabs(aceSettings.useSoftTab);
        _editor.setBehavioursEnabled(aceSettings.enableBehaviours);
        _editor.setFadeFoldWidgets(aceSettings.fadeFoldWidgets);

        _editor.session.setMode(aceSettings.mode);
        console.log(aceSettings.mode);
        _editor.session.modeName = aceSettings.modeName;

        _renderer.setShowGutter(aceSettings.showGutter);

        switch (aceSettings.softWrap) {
          case "off":
              _session.setUseWrapMode(false);
              _renderer.setPrintMarginColumn(80);
              break;
          case "free":
              _session.setUseWrapMode(true);
              _session.setWrapLimitRange(null, null);
              _renderer.setPrintMarginColumn(80);
              break;
          default:
              _session.setUseWrapMode(true);
              var col = parseInt(aceSettings.softWrap, 10);
              _session.setWrapLimitRange(col, col);
              _renderer.setPrintMarginColumn(col);
        }
      };

      var goToLine = function (line) {
        _editor.gotoLine(line);
      };

      var aceLoaded = function(_e, callback) {
        // Editor part
        _editor = _e;
        _session = _editor.getSession();
        _renderer = _editor.renderer;
        content = _session.getValue();

        // Options
        _session.setUndoManager(new ace.UndoManager());

        // Events
        _editor.on("changeSession", function(){
          _session = _editor.getSession();
        });

        _session.on("change", function(){
          content = _session.getValue();
        });
        loadSettings();
        callback(_editor);
      };

      var getToc = function () {
        var toc = [];
        var keys = ['chapter','section','subsection','subsubsection','paragraph'];
        var regex = '\\\\('+keys.join('|')+')(\\*)?\{([^}]+)\}';
        var reg = new RegExp(regex,"gi");
        var astring = content.split('\n');

        for (var i = 0; i < astring.length; i++) {
          var number = i+1;
          var line = astring[i];
          var result;
          while ((result = reg.exec(line)) !== null) {
            var type = (result[1]);
            toc.push({
              type: type,
              level: keys.indexOf(type),
              restart: result[2]=='*',
              title: result[3],
              line: number
            });
          }
        };
        return toc;
      }

      return {
          goToLine: goToLine,
          getContent: function () { return content;},
          setContent: function (c) { return _session.setValue(c);},
          getEditor: function () {
            return _editor;
          },
          getSession: function () {
            return _editor.getSession();
          },
          loadSettings: loadSettings,
          aceLoaded: aceLoaded,
          getToc: getToc,
          aceSettings: aceSettings
      }
  }).controller('PaperController', ['$scope','localize','$location','ace','Paper','$routeParams', function ($scope,localize,$location, ace, Paper,$routeParams) {
    var paper_id = $routeParams.id;
    Paper.getInfo(paper_id).then(function (data) {
      $scope.paper = data;
      $scope.paper.etag = data.header.etag;
    }, function (error) {
      console.log(error);
    });
    Paper.getSynchronized(paper_id).then(function (data) {
      $scope.paper = data;
    }, function (error) {
      console.log(error);
    });

    var paper = {};
    var textLog= '';


    $scope.mode = 'ace';
    $scope.logs = [];
    $scope.toc = [];
    $scope.listType = 'files';

    //action listener: action in the menu
    $scope.$on('handleAction', function(event, data){
      if($scope[data]) {
        $scope[data]();
      }
    });

    $scope.switch_editor_mode = function () {
      $scope.mode = ($scope.mode == 'ace'?'text':'ace');
    }

    $scope.compile = function () {
      textLog = '';
      var pdftex = new PDFTeX();
      pdftex.set_TOTAL_MEMORY(80*1024*1024).then(function() {
        //pdftex.FS_createLazyFile('/', 'snowden.jpg', 'snowden.jpg', true, true);

        var appendOutput = function(msg) {
            textLog += "\r\n" + msg;
        }
        pdftex.on_stdout = pdftex.on_stderr = appendOutput;

        var start_time = new Date().getTime();
        pdftex.compile(ace.getContent()).then(function(pdf_dataurl) {
          var end_time = new Date().getTime();
          console.info("Execution time: " + (end_time-start_time)/1000+' sec');
          console.log(pdf_dataurl);
          var logs = LatexParser.parse(textLog,{});
          var annotations = [];
          for (var i = 0; i < logs.all.length; i++) {
            var error = logs.all[i];
            annotations.push({
              row: error.line - 1,
              column: 1,
              text: error.message,
              type: (error.level=="error")?"error":'warning' // also warning and information
            });
          }
          ace.getSession().setAnnotations(annotations);
          if(pdf_dataurl === false)
            return;
        });
      });
    };
    $scope.goToLine = function (line) {
      ace.goToLine(line);
    }

    $scope.aceLoaded = function (_editor) {
        ace.aceLoaded(_editor, function() {
            $scope.toc = ace.getToc();
            ace.getSession().on("change", function(){
              $scope.toc = ace.getToc();
            });
            _editor.focus();
        });
    };

    $scope.aceChanged = function(e) {
    };

  }]).controller('NewPaperController', ['$scope','localize','$location','Paper', function ($scope,localize,$location, Paper) {
    var paper = {};

    $scope.paper = paper;

    $scope.create = function () {
      Paper.create(paper).then(function (data) {
        console.log(data);
        if(data.response == 'true') {
          $location.path("/papers")
        } else {
          $scope.errors.push(data);
        }
      }, function (error) {
        console.log(error);
      });
    };

  }]).controller('EditPaperController', ['$scope','localize','$location','Paper','$routeParams', function ($scope,localize,$location, Paper, $routeParams) {
    var paper_id = $routeParams.id;
    Paper.getInfo(paper_id).then(function (data) {
      $scope.paper = data;
      $scope.paper.etag = data.header.etag;
    }, function (error) {
      console.log(error);
    });

    $scope.create = function () {
      Paper.create(paper).then(function (data) {
        console.log(data);
        if(data.response == 'true') {
          $location.path("/papers")
        } else {
          $scope.errors.push(data);
        }
      }, function (error) {
        console.log(error);
      });
    };

  }]).directive('blToc', ['$compile',function($compile) {
    var updateTOC = function (elm, toc,$scope) {
        if(toc == null) return;
        var top = null;
        var current = null;
        var currentlevel = -1;
        elm.text('');
        for (var i = 0; i < toc.length; i++) {
            var line = toc[i];
            //create a new ul/ol
            if(current == null){
                var l = document.createElement(line.level<3?'ol':'ul');
                current = l;
                top = current;
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
        }
        if(toc.length == 0) {
          elm.html("No table of contents");
        } else {
          angular.element(elm).append($compile(top)($scope));
        }
    };
    return function(scope, elm, attrs) {
        scope.$watch('toc', function(value) {
            updateTOC(elm,value,scope);
        });
        updateTOC(elm,scope.toc,scope);
    };
  }])