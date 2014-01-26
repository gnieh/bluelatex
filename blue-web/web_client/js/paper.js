angular.module('bluelatex.paper',[])
    .factory("Paper", function ($resource, $http, $upload, $q) {
      var paper =  $resource( apiRoot+"/papers/:paper_id", null, {
          "new": {
            method: "POST", headers:{'Content-Type':'application/x-www-form-urlencoded'},
            format: 'json',
            isArray: false,
            transformResponse: [function(data, headersGetter) {
              return {response: JSON.parse(data)};
            }].concat($http.defaults.transformResponse)
          },
          "delete": {method: "DELETE",
            transformResponse: [function(data, headersGetter) {
              return {response: JSON.parse(data)};
            }].concat($http.defaults.transformResponse)}
        }
      );
      var info =  $resource( apiRoot+"/papers/:paper_id/info", null, {
          "edit": {method: "PATCH",
            headers:{'Content-Type':'application/json-patch'},
            transformRequest: [function(data, headersGetter) {
              var header = headersGetter();
              header['If-Match'] = data.etag;
              return data.path_json;
            }].concat($http.defaults.transformRequest)
          },
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
          "get": {method: "GET",format: 'json',isArray: true}
        }
      );
      var resources =  $resource( apiRoot+"/papers/:paper_id/files/resources/:resource", null, {
        "get": {method: "GET",isArray: true,
          transformResponse: [function(data, headersGetter) {
            var array = [];
            data = JSON.parse(data);
            for (var i = 0; i < data.length; i++) {
              var resource = data[i]
              array.push({
                title: resource,
                name: resource.replace(/\.[^\.]+$/,''),
                type: getFileType(resource),
                extension: getFileNameExtension(resource)
              });
            }
            return array;
          }].concat($http.defaults.transformResponse)
        },
          "delete": {method: "DELETE",
            transformResponse: [function(data, headersGetter) {
              return {response: JSON.parse(data)};
            }].concat($http.defaults.transformResponse)}
        }
      );
      var upload = function (paper_id, file, resource) {
        var deferred = $q.defer();
        var promise = deferred.promise;
        $upload.upload({
          url: apiRoot+'/papers/'+paper_id+'/files/resources/'+resource,
          method: 'POST',
          // headers: {'headerKey': 'headerValue'}, withCredential: true,
          file: file,
          // file: $files, //upload multiple files, this feature only works in HTML5 FromData browsers
          /* set file formData name for 'Content-Desposition' header. Default: 'file' */
          //fileFormDataName: myFile, //OR for HTML5 multiple upload only a list: ['name1', 'name2', ...]
          /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
          //formDataAppender: function(formData, key, val){}
        }).progress(function(evt) {
          console.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
          deferred.notify(parseInt(100.0 * evt.loaded / evt.total));
        }).success(function(data, status, headers, config) {
          // file is uploaded successfully
          console.log(data);
          deferred.resolve({data: data, status: status,headers:headers, config:config});
        }).error(function (data, status) {
          deferred.reject({data:data, status: status});
        });
        return promise;
      }
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
        modify: function (paper, initial_paper) {
          var path_json = [];
          if(paper.title != initial_paper.title) {
            path_json.push({
              'op': 'replace',
              "path": "/title",
              "value": paper.title
            });
          }
          var reaplce_authors = false;
          for (var i = 0; i < paper.authors.length; i++) {
            var author = paper.authors[i];
            if(initial_paper.authors.indexOf(author)<0) {
              reaplce_authors = true;
              break;
            }
          }
          for (var i = 0; i < initial_paper.authors.length; i++) {
            var author = initial_paper.authors[i];
            if(paper.authors.indexOf(author)>=0) {
              reaplce_authors = true;
              break;
            }
          }
          if(reaplce_authors) {
            path_json.push({
              'op': 'replace',
              "path": "/authors",
              "value": paper.authors
            });
          }
          var reaplce_reviewers = false;
          for (var i = 0; i < paper.reviewers.length; i++) {
            var reviewer = paper.reviewers[i];
            if(initial_paper.reviewers.indexOf(reviewer)<0) {
              reaplce_reviewers = true;
              break;
            }
          }
          for (var i = 0; i < initial_paper.reviewers.length; i++) {
            var reviewer = initial_paper.reviewers[i];
            if(paper.reviewers.indexOf(reviewer)>=0) {
              reaplce_reviewers = true;
              break;
            }
          }
          if(reaplce_authors) {
            path_json.push({
              'op': 'replace',
              "path": "/reviewers",
              "value": paper.reviewers
            });
          }
          if(paper.branch != initial_paper.branch) {
            path_json.push({
              'op': 'replace',
              "path": "/branch",
              "value": paper.branch
            });
          }
          if(paper.cls != initial_paper.cls) {
            path_json.push({
              'op': 'replace',
              "path": "/cls",
              "value": paper.cls
            });
          }
          return info.edit({ paper_id: paper.id },
            {
              "etag": paper.etag,
              path_json: path_json
            }).$promise;
        },
        getResources: function (paper_id) {
          return resources.get({ paper_id: paper_id }).$promise;
        },
        getResourceUrl: function (paper_id, resource) {
          return apiRoot+'/papers/'+paper_id+'/files/resources/'+resource;
        },
        uploadResource: function (paper_id,resource, data) {
          return upload(paper_id, data, resource);
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
  }).controller('PaperController', ['$scope','localize','$location','ace','Paper','$routeParams','$upload', function ($scope,localize,$location, ace, Paper,$routeParams,$upload) {
    var paper_id = $routeParams.id;
    var getPaperInfo = function () {
      Paper.getInfo(paper_id).then(function (data) {
        $scope.paper = data;
        $scope.paper.etag = data.header.etag;
      }, function (error) {
        console.log(error);
      });
    };
    getPaperInfo();

    var getSynchronizedFiles = function () {
      Paper.getSynchronized(paper_id).then(function (data) {
        $scope.synchronizedFiles = data;
      }, function (error) {
        console.log(error);
      });
    };
    getSynchronizedFiles();

    var getResources = function () {
      Paper.getResources(paper_id).then(function (data) {
        $scope.resources = data;
      }, function (error) {
        console.log(error);
      });
    };
    getResources();

    var textLog= '';

    $scope.resources = [];
    $scope.paper = {};
    $scope.listType = 'files';
    $scope.mode = 'ace';
    $scope.logs = [];
    $scope.toc = [];
    $scope.content = '';
    //action listener: action in the menu
    $scope.$on('handleAction', function(event, data){
      if($scope[data]) {
        $scope[data]();
      }
    });

    $scope.switch_editor_mode = function () {
      $scope.mode = ($scope.mode == 'ace'?'text':'ace');
      if($scope.mode == 'ace') {
        ace.setContent($scope.content);
        ace.getEditor().focus();
      }
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

    $scope.new_file = {};

    $scope.onFileSelect = function($files) {
      console.log($files);
      if($files.length > 0) {
        var file = $files [0];
        $scope.new_file= {
          title: file.name,
          name: file.name.replace(/\.[^\.]+$/,''),
          type: getFileType(file.name),
          file: file,
          extension: getFileNameExtension(file.name)
        };
      }
    };

    $scope.uploadResource = function () {
      Paper.uploadResource(paper_id, $scope.new_file.title, $scope.new_file.file).then(function (data) {
        console.log(data);
        getResources();
        $scope.new_file = {};
      }, function (error) {
        switch(err.status){
          case 400:
            $scope.errors.push(localize.getLocalizedString('_Upload_resource_Some_parameters_are_missing_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Upload_resource_Wrong_username_and_or_password_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Upload_resource_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Upload_resource_Something_wrong_happened_'));
            console.log(err);
        }
        console.log(error);
      }, function (progress) {
        console.log(progress);
      });
    };

    $scope.cancelUploadResource = function () {
      $scope.new_file = {};
    };
    $scope.viewResource = function (resource) {

    };
    $scope.removeResource = function (resource) {
      Paper.removeResource(paper_id, resource.title).then(function (data) {
        getResources();
      }, function (error) {
        $scope.errors = [];
        switch(err.status){
          case 400:
            $scope.errors.push(localize.getLocalizedString('_Delete_resource_Some_parameters_are_missing_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Delete_resource_Wrong_username_and_or_password_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Delete_resource_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Delete_resource_Something_wrong_happened_'));
            console.log(err);
        }
        console.log(error);
      });
    };
    $scope.downloadResource = function (resource) {
      window.open(Paper.getResourceUrl(paper_id, resource.title));
    };
  }]).controller('NewPaperController', ['$scope','localize','$location','Paper', function ($scope,localize,$location, Paper) {
    var paper = {};

    $scope.paper = paper;

    $scope.create = function () {
      Paper.create(paper).then(function (data) {
        $location.path("/paper/"+data.response)
      }, function (err) {
        $scope.errors = [];
        switch(err.status){
          case 400:
            $scope.errors.push(localize.getLocalizedString('_New_paper_Some_parameters_are_missing_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_New_paper_Wrong_username_and_or_password_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_New_paper_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_New_paper_Something_wrong_happened_'));
            console.log(err);
        }
      });
    };

  }]).controller('EditPaperController', ['$scope','localize','$location','Paper','$routeParams', function ($scope,localize,$location, Paper, $routeParams) {
    var paper_id = $routeParams.id;
    var clone_paper = {};
    Paper.getInfo(paper_id).then(function (data) {
      $scope.paper = data;
      $scope.paper.etag = data.header.etag;
      $scope.paper.id = paper_id;
      clone_paper = clone($scope.paper);
    }, function (err) {
      $scope.errors = [];
      switch(err.status){
        case 400:
          $scope.errors.push(localize.getLocalizedString('_Edit_paper_Some_parameters_are_missing_'));
          break;
        case 401:
          $scope.errors.push(localize.getLocalizedString('_Edit_paper_Wrong_username_and_or_password_'));
          break;
        case 500:
          $scope.errors.push(localize.getLocalizedString('_Edit_paper_Something_wrong_happened_'));
          break;
        default:
          $scope.errors.push(localize.getLocalizedString('_Edit_paper_Something_wrong_happened_'));
          console.log(err);
      }
    });

    $scope.new_author = '';
    $scope.new_reviewer = '';

    $scope.removeAuthor = function (author) {
      $scope.paper.authors.splice($scope.paper.authors.indexOf(author), 1);
    };
    $scope.removeReviewer = function (reviewer) {
      $scope.paper.reviewers.splice($scope.paper.reviewers.indexOf(reviewer), 1);
    };

    $scope.addAuthor = function () {
      var author = $scope.new_author;
      if($scope.paper.authors.indexOf(author) < 0)
        $scope.paper.authors.push(author);
      $scope.new_author = '';
    };
    $scope.addReviewer = function () {
      var reviewer = $scope.new_reviewer;
      if($scope.paper.reviewers.indexOf(reviewer) < 0)
        $scope.paper.reviewers.push(reviewer);
      $scope.new_reviewer = '';
    };

    $scope.modify = function () {
      Paper.modify($scope.paper, clone_paper).then(function (data) {
        console.log(data);

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
  }]);