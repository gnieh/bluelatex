/**
* The controller for latex paper 
*/
angular.module('bluelatex.Paper.Controllers.LatexPaper', ['angularFileUpload','bluelatex.Paper.Directives.Toc','bluelatex.Paper.Services.Ace','bluelatex.Paper.Services.Paper','bluelatex.Paper.Services.Ace','bluelatex.Latex.Services.SyncTexParser','bluelatex.Paper.Services.Latex','bluelatex.Shared.Services.WindowActive', 'MobWrite','bluelatex.Paper'])
  .controller('LatexPaperController', ['$rootScope','$scope', 'localize', '$http', '$location', 'AceService', 'PaperService', '$routeParams', '$upload', '$log','MessagesService','SyncTexParserService','$document','WindowActiveService','LatexService','MobWriteService','AceMobWriteClient','$q',
    function ($rootScope,$scope, localize, $http, $location, AceService, PaperService, $routeParams, $upload, $log,MessagesService,SyncTexParserService,$document,WindowActiveService, LatexService,MobWriteService,AceMobWriteClient, $q) {
      $scope.paperId = $routeParams.id;
      var peerId = MobWriteService.syncUsername;
      var pageActive = true;

      $scope.currentLine = 0;
      $scope.currentPage = 0;
      $scope.linePage = 0;
      $scope.page = 0;
      
      // list of resources
      $scope.resources = [];
      // list of synchronosed files
      $scope.synchronizedFiles = [];

      // the compiler info
      $scope.compiler = {};
      // the paper info
      $scope.paper = {};
      // data of the current page
      $scope.currentFile = {};

      // synctex data
      $scope.synctex = null;
      // display synctex debug data
      $scope.displaySyncTexBox = false;
      // latex compiler logs
      $scope.logs = [];
      // the number of page
      $scope.totalPage = 0;

      // table of contents of the current paper
      $scope.toc = [];
      // the content of the document
      $scope.content = '';
      // the data of the now synchronozed file
      $scope.new_file = {};
      
      // url of different data
      $scope.zipURL = PaperService.getZipUrl($scope.paperId);
      $scope.pdfURL = PaperService.getPDFUrl($scope.paperId);
      $scope.logURL = PaperService.getLogUrl($scope.paperId);
      $scope.urlPaper = PaperService.getPaperUrlRoot($scope.paperId);
      
      // the type of preview (pdf or image)
      $scope.previewType = "pdf";
      // the scale factor
      $scope.scale = "auto";
      $scope.listType = 'files';
      $scope.autoscroll=true;

      // the pdf data
      $scope.pdf = null;
      $scope.revision=Math.random();

      /**************/
      /* Exit Paper */
      /**************/
      var exitPaper = function () {
        pageActive = false;
        WindowActiveService.removeObserverCallback(windowStatusCallback);
        stopMobWrite();
        PaperService.leavePaper($scope.paperId, peerId);
      };
      
      window.onbeforeunload = function () {
        exitPaper();
      };

      /**
      * Exit paper on controller destroy
      */
      $scope.$on("$destroy", function(){
        exitPaper();
      });

      /************/
      /* MobWrite */
      /************/
      /**
      * Start mobWrite
      */
      var initMobWrite = function () {
        return MobWriteService.share({paper_id: $scope.paperId,file:$scope.currentFile.title}).then(function (){
          displayAnnotation();
          $scope.toc = LatexService.parseTOC(AceService.getContent());
          AceService.getEditor().focus();
        })
      };

      /**
      * Stop sharing file
      */
      var stopMobWrite = function () {
        MobWriteService.unload_();
        MobWriteService.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title});
      };

      /**
      * Download and parse SyncTex file
      */
      var getSyncTex = function () {
        PaperService.getSynctex($scope.paperId).then(function (data) {
          $scope.synctex = SyncTexParserService.parse(data);
          $rootScope.$$phase || $rootScope.$apply();
        });
      };

      /*
      * DEBUG
      * Display synctex debug data
      */
      $scope.displaySyncTex = function() {
        if($scope.displaySyncTexBox == false || $scope.displaySyncTexBox == 'false') {
          $scope.displaySyncTexBox = true;
        } else {
          $scope.displaySyncTexBox = false;
        }
      };

      /**
      * Get the number of page of the paper
      */
      var getPages = function () {
        PaperService.getPages($scope.paperId).then(function (data) {
          $scope.totalPage = data.response;
          if($scope.currentPage == 0) {
            $scope.currentPage = 1;
            $scope.linePage = 1;
            $scope.page = 1;
          }
        });
      };

      /********/
      /* Logs */
      /********/
      /*
      * Download the log file
      */
      var getLog = function () {
        PaperService.getLog($scope.paperId).then(function (data) {
          $scope.logs = LatexParser.parse(data,{});
          displayAnnotation();
        });
      };
      /*
      * Display compiler annotations
      */
      var displayAnnotation = function() {
        AceService.getSession().setAnnotations([]);
        var annotations = [];
        if($scope.logs.all == null) return;
        for (var i = 0; i < $scope.logs.all.length; i++) {
          var error = $scope.logs.all[i];
          if(error.filename != $scope.currentFile.title) continue;
          annotations.push({
            row: error.line - 1,
            column: 1,
            text: error.message,
            type: (error.level=="error")?"error":'warning' // also warning and information
          });
        }
        AceService.getSession().setAnnotations(annotations);
      };

      /**
      * Get the list of synchronised file
      */
      var getSynchronizedFiles = function () {
        return PaperService.getSynchronized($scope.paperId).then(function (data) {
          $scope.synchronizedFiles = data;
          for (var i = 0; i < $scope.synchronizedFiles.length; i++) {
            if($scope.synchronizedFiles[i].title == $scope.paperId + ".tex") {
              $scope.currentFile = $scope.synchronizedFiles[i];
              break;
            }
          }
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_synchronized_resource_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_synchronized_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_synchronized_resource_Something_wrong_happened_',err);
          }
        });
      };

      $scope.new_file_extension = '.tex';

      $scope.newFile = function(filename) {
        PaperService.newSynchronizedFile($rootScope.loggedUser, $scope.paperId, filename).then(function() {
          $scope.new_file_name = '';
          getSynchronizedFiles();
          getResources();
          $scope.resources = [];
          $scope.synchronizedFiles = [];
        });
      };

      $scope.removeSynchronisedFile = function(file) {
        if(file == $scope.currentFile) {
          MobWriteService.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title});
        }
        PaperService.deleteSynchronizedFile($rootScope.loggedUser, $scope.paperId, file.title).then(function() {
          $scope.synchronizedFiles = [];
          getSynchronizedFiles().then(function(){
            initMobWrite();
          });
        });
      };

      /**
      * Get the list of resources
      */
      var getResources = function () {
        PaperService.getResources($scope.paperId).then(function (data) {
          $scope.resources = data;
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_resources_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_resources_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_resources_Something_wrong_happened_',err);
          }
        });
      };

      /**
      * User selects a file to upload
      */
      $scope.onFileSelect = function ($files) {
        if ($files.length > 0) {
          var file = $files[0];
          $scope.new_file = {
            title: file.name,
            name: file.name.replace(/\.[^\.]+$/, ''),
            type: getFileType(file.name),
            file: file,
            extension: getFileNameExtension(file.name)
          };
        }
      };

      /**
      * Upload a resource
      */
      $scope.uploadResource = function () {
        PaperService.uploadResource($scope.paperId, $scope.new_file.title, $scope.new_file.file).then(function (data) {
          if(data.data == true) {
            getResources();
            $scope.new_file = {};
          } else
            MessagesService.error('_Upload_resource_Some_parameters_are_missing_');
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Upload_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Upload_resource_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Upload_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Upload_resource_Something_wrong_happened_',err);
          }
        });
      };

      /**
      * Cancel an upload
      */
      $scope.cancelUploadResource = function () {
        $scope.new_file = {};
      };

      /**
      * View a resource
      */
      $scope.viewResource = function (resource) {
        if(resource.type == "image") {
          $scope.displayResourceViewer = true;
          $scope.resourceURL = PaperService.getResourceUrl($scope.paperId,resource.title);
        }
      };

      $scope.closeResourceViewer = function() {
        $scope.displayResourceViewer = false;
      };

      $scope.insertResource = function(resource) {
        var text = resource.title;
        if(resource.type == "image") {
          text = '\\includegraphics{'+resource.title+'}\n';
        }
        AceService.getSession().insert(AceService.getEditor().selection.getCursor(),text);
        AceService.getEditor().focus();
      };

      /**
      * Remove a resource
      */
      $scope.removeResource = function (resource) {
        PaperService.removeResource($scope.paperId, resource.title).then(function (data) {
          if(data.response == true) {
            getResources();
            $scope.synchronizedFiles = [];
            getSynchronizedFiles();
          } else
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_');
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Delete_resource_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
          }
        });
      };

      /**
      * The the paper infos
      */
      var getPaperInfo = function (callback) {
        PaperService.getInfo($scope.paperId).then(function (data) {
          $scope.paper = data;
          if(callback) callback($scope.paper);
        }, function (error) {
          MessagesService.clear();
          switch (error.status) {
          case 404:
            MessagesService.error('_Get_info_paper_Paper_not_found_',err);
            break;
          case 401:
            MessagesService.error('_Get_info_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
          }
        });
      };
      /************/
      /* Compiler */
      /************/
      /**
      * Get the paper compiler info
      */
      var getCompilerInfo = function() {
        PaperService.getPaperCompiler($scope.paperId).then(function (data) {
          $scope.compiler = data;
          $scope.newcompiler = JSON.parse(JSON.stringify(data));
        }, function (error) {
          MessagesService.clear();
          MessagesService.error('_Get_compiler_Unable_to_get_compiler_info_');
        });
      }; 
      /*
      * Modify compiler options
      */
      $scope.modifyCompiler = function () {
        if($scope.compiler.interval != $scope.newcompiler.interval || 
           $scope.compiler.synctex != $scope.newcompiler.synctex || 
           $scope.compiler.compiler != $scope.newcompiler.compiler) {
          PaperService.editPaperCompiler($scope.paperId, $scope.newcompiler, $scope.compiler).then(function () {
            getCompilerInfo();
          });
        } 
      };

      var getCompilers = function() {
        PaperService.getCompilers().then(function (data) {
          $scope.compilers = data;
        }, function (error) {
          MessagesService.clear();
          MessagesService.error('_Get_compilers_Unable_to_get_compiler_list_');
        });
      };
      getCompilers();

      /**************/
      /* Navigation */
      /**************/

      /*
      * Change the current file
      */
      $scope.changeFile = function (file, line) {
        if($scope.currentFile == file) return;
        stopMobWrite();
        MobWriteService.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title});
        $scope.currentFile = file;
        $scope.content = '';
        AceService.setContent($scope.content);
        AceService.getEditor().focus();
        return initMobWrite().then(function (data) {
          if(line) {
            $scope.goToLine(line);
          }
        });
      };
      /**
      * Change the current file with the name of the file
      */
      $scope.changeFileFromName = function(filename, line) {
        if($scope.currentFile.title == filename) return;
        for (var i = $scope.synchronizedFiles.length - 1; i >= 0; i--) {
          var syncFile = $scope.synchronizedFiles[i];
          if(syncFile.title == filename) {
            return $scope.changeFile(syncFile, line);
          }
        }
      };
      /**
      * Go to the next page
      */
      $scope.nextPage = function () {
        $scope.changePage(parseInt($scope.linePage)+1);
      };

      /**
      * Go to the previous page
      */
      $scope.prevPage = function () {
        $scope.changePage(parseInt($scope.currentPage)-1);
      };

      /**
      * Go to the page: page
      */
      $scope.changePage = function (page) {
        if(page > 0 && page <= $scope.totalPage ) {
          $scope.linePage = page;

          for(var i in $scope.synctex.blockNumberLine) {
            if($scope.synctex.blockNumberLine[i][page]) {
              $scope.goToLine(parseInt(i));
              return;
            }
          }
        }
      };

      $scope.$watch('currentPage', function(value) {
        $scope.currentPage = parseInt(value);
      });

      /**
      * Go to the line: line
      */
      $scope.goToLine = function (line) {
        AceService.goToLine(line);
        if(!$scope.synctex) return;
        if(!$scope.synctex.blockNumberLine[$scope.currentFile.title]) return;
        if(!$scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]) return;
        var pages = Object.keys($scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]);
        if(pages.length < 0) return;
        $scope.linePage = pages[0];
      };

      // disable compilation when page page is not active
      var windowStatusCallback = function(windowActive) {
        if(windowActive == true) {
          if(pageActive == false) {
            pageActive = true;
            $scope.compile();
          }
        } else {
          pageActive = false;
        }
      };
      WindowActiveService.registerObserverCallback(windowStatusCallback);

      var parsePDF = function() {
        if($scope.previewType == "pdf") {
          PDFJS.getDocument($scope.pdfURL+"?"+$scope.revision).then(function(pdf) {
            $scope.pdf = pdf;
            $rootScope.$$phase || $rootScope.$apply();
          });
        }
      };
      
      getPages();
      parsePDF();

      /**
      * Compile the paper
      */
      var compileActive = false;
      $scope.compile = function () {
        if(!pageActive) return;
        if(compileActive) return;

        compileActive = true;
        PaperService.subscribePaperCompiler($scope.paperId).then(function (data) {
          compileActive = false;
          getLog();
          if(data.response == true) {
            $scope.revision++;
            parsePDF();
            getSyncTex();
            getPages();
          }
          $scope.compile();
        }, function (err) {
          compileActive = false;
          switch (err.status) {
          // no change
          case 304:
            $scope.compile();
            break;
          // not conneted
          case 401:
            MessagesService.error('_Not_connected_',err);            
            break;
          // other bugs
          default:
            getLog();
            setTimeout(function() {
              $scope.compile();
            }, 3000);
          }
        });
      };

      /*****************/
      /* It's all text */ 
      /*****************/
      $scope.itsalltextClass = (document.querySelector(".centerCol .itsalltext") && document.querySelector(".centerCol .itsalltext").id)?'':'hidden';
      var itsalltext_inited = false;
      var init_itsalltext = function () {
        if(itsalltext_inited) return;
        var area = $document[0].querySelector(".itsalltext");
        if (area.addEventListener) {
          area.addEventListener('change', function() {
            $scope.content = this.value;
            AceService.setContent($scope.content);
            AceService.getEditor().focus();
          }, false);
        } else if (area.attachEvent) {
          area.attachEvent('onpropertychange', function() {
            $scope.content = this.value;
            AceService.setContent($scope.content);
            AceService.getEditor().focus();
          });
        }
        itsalltext_inited = true;
      };

      $scope.$watch("content", function (value) {
        if(value && itsalltext_inited){
          $document[0].querySelector(".itsalltext").value = value;
          $scope.openItsalltext();
        }
      });

      $scope.openItsalltext = function () {
        if($document[0].querySelector(".itsalltext + img")){
          init_itsalltext();
          $document[0].querySelector(".itsalltext + img").click();
        }else
          MessagesService.warning("_Install_itsalltext_");
      };

      var langTools = ace.require("ace/ext/language_tools");

      var texCmds = null;
      var texCompleter = {
        getCompletions: function(editor, session, pos, prefix, callback) {
          var usrCommands = LatexService.parseCommands($scope.content);
          if(texCmds == null ){
            $http.get("resources/texCmds.json").then(function(cmds) {
              texCmds = cmds.data;
              callback(null, texCmds.concat(usrCommands));
            });
          } else {
            callback(null, texCmds.concat(usrCommands));
          }
        }
      };
      langTools.addCompleter(texCompleter);

      /**
      * Load ACE editor
      */
      $scope.aceLoaded = function (_editor) {
        AceService.aceLoaded(_editor, function () {
          _editor.commands.addCommand({
              name: "compile",
              bindKey: {win: "Ctrl-S", mac: "Command-S"},
              exec: function(editor) {
                $scope.compile();
              }
          });
          AceService.getEditor().selection.on("changeCursor", function(){
            $scope.currentLine = parseInt(_editor.selection.getCursor().row)+1;
            $rootScope.$$phase || $rootScope.$apply();
            if(!$scope.synctex) return;
            if(!$scope.synctex.blockNumberLine[$scope.currentFile.title]) return;
            if(!$scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]) return;
            var pages = Object.keys($scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]);
            if(pages.length < 0) return;
            $scope.linePage = pages[0];
          });
          AceService.getEditor().setOptions({
              enableBasicAutocompletion: true
          });

          var promiseJoin = PaperService.joinPaper($scope.paperId,peerId).then(function () {
            $scope.compile();
          });

          $q.all([
            getSynchronizedFiles(),
            promiseJoin
          ]).then(function () {
            initMobWrite();
          });
          getSyncTex();
          getCompilerInfo();
          getResources();

          setTimeout(function () {
            $scope.itsalltextClass = (document.querySelector(".centerCol .itsalltext") && document.querySelector(".centerCol .itsalltext").id)?'':'hidden';
            $rootScope.$$phase || $rootScope.$apply();
          },1500);
          getLog();
          AceService.getSession().on("change", function () {
            $scope.toc = LatexService.parseTOC($scope.content);
          });
          _editor.focus();
        });
      };

      $scope.aceChanged = function (e) {};

      /*
      * Download a resource
      */
      $scope.downloadResource = function (resource) {
        window.open(PaperService.getResourceUrl($scope.paperId, resource.title));
      };

      $scope.downloadLog = function () {
        window.open($scope.logURL);
      };
      $scope.downloadPDF = function () {
        window.open($scope.pdfURL);
      };
      $scope.downloadZip = function () {
        window.open($scope.zipURL);
      };

      //action listener: action in the menu
      $scope.$on('handleAction', function (event, data) {
        if ($scope[data]) {
          $scope[data]();
        }
      });

      $scope.range = function(n) {
        return new Array(parseInt(n));
      };
    }
  ]);