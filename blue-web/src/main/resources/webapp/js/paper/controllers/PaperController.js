angular.module('bluelatex.Paper.Controllers.Paper', ['angularFileUpload','bluelatex.Paper.Directives.Toc','bluelatex.Paper.Services.Ace','bluelatex.Paper.Services.Paper','bluelatex.Paper.Services.Ace','bluelatex.Latex.Services.SyncTexParser','bluelatex.Shared.Services.WindowActive'])
  .controller('PaperController', ['$rootScope','$scope', 'localize', '$location', 'AceService', 'PaperService', '$routeParams', '$upload', '$log','MessagesService','SyncTexParserService','$document','WindowActiveService',
    function ($rootScope,$scope, localize, $location, AceService, PaperService, $routeParams, $upload, $log,MessagesService,SyncTexParserService,$document,WindowActiveService) {
      var paper_id = $routeParams.id;
      var pageActive = true;
      $scope.paperId = paper_id;
      $scope.pageViewport = {};

      $scope.currentLine = 0;
      $scope.currentPage = 0;

      $scope.resources = [];
      $scope.paper = {};
      $scope.listType = 'files';
      $scope.mode = 'ace';
      $scope.logs = [];
      $scope.toc = [];
      $scope.content = '';
      $scope.new_file = {};
      $scope.synctex = null;
      $scope.zipURL = PaperService.getZipUrl(paper_id);
      $scope.pdfURL = PaperService.getPDFUrl(paper_id);
      $scope.logURL = PaperService.getLogUrl(paper_id);
      $scope.currentFile = {};
      $scope.status = "load";
      $scope.warnings = [];
      $scope.errors = [];

      $scope.displaySyncTexBox = true;

      $scope.vignetteType = "pdf";
      $scope.urlPaper = PaperService.getPaperUrlRoot(paper_id);
      $scope.scale = "auto";
      $scope.totalPage = 0;

      $scope.revision=Math.random();

      /**
      * Download and parse SyncTex file
      */
      var getSyncTex = function () {
        PaperService.getSynctex(paper_id).then(function (data) {
          $scope.synctex = SyncTexParserService.parse(data);
          console.log($scope.synctex);
          $rootScope.$$phase || $rootScope.$apply();
        });
      };

      /**
      * Exit paper
      */
      var exitPaper = function () {
        pageActive = false;
        if($scope.paper.authors &&
           $scope.paper.authors.indexOf($rootScope.loggedUser.name) >= 0) {
          stopMobWrite();
          PaperService.leavePaper(paper_id);
        }
      };

      window.onbeforeunload = function (event) {
        exitPaper();
      };

      /**
      * Start mobWrite
      */
      var initMobWrite = function () {
        mobwrite.syncUsername = $rootScope.loggedUser.name;
        mobwrite.share({paper_id: $scope.paperId,file:$scope.currentFile.title});
      };

      /**
      * Stop sharing file
      */
      var stopMobWrite = function () {
        if(mobwrite) {
          mobwrite.unload_();
          mobwrite.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title});
        }
      };

      /**
      * Get the number of page of the paper
      */
      var getPages = function () {
        PaperService.getPages(paper_id).then(function (data) {
          $scope.totalPage = data.response;
          if($scope.currentPage == 0)
            $scope.currentPage = 1;
        });
      };

      var displayAnnotation = function() {
        AceService.getSession().setAnnotations([]);
        var annotations = [];
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

      /*
      * Download the log file
      */
      var getLog = function () {
        PaperService.getLog(paper_id).then(function (data) {
          $scope.logs = LatexParser.parse(data,{});
          $scope.warnings = $scope.logs.warnings;
          $scope.errors = $scope.logs.errors;
          displayAnnotation();
        });
      };

      /**
      * Get the list of synchronised file
      */
      var getSynchronizedFiles = function (callback) {
        PaperService.getSynchronized(paper_id).then(function (data) {
          $scope.synchronizedFiles = data;
          for (var i = 0; i < $scope.synchronizedFiles.length; i++) {
            if($scope.synchronizedFiles[i].title == $scope.paperId + ".tex") {
              $scope.currentFile = $scope.synchronizedFiles[i];
              break;
            }
          }
          if(callback) callback();
        }, function (error) {
          MessagesService.clear();
          switch (error.status) {
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

      /**
      * Download Tex file
      */
      var getTexfile = function (callback) {
        var file_name = PaperService.getResourceUrl(paper_id,paper_id+".tex");
        PaperService.downloadRessource(file_name).then(function (data) {
          $scope.content = data;

          if(callback) {
            callback();
          }
        }, function (error) {
          // body...
        });
      };

      /**
      * Update Tex file
      */
      var updateTexfile = function () {
        var file_name = PaperService.getResourceUrl(paper_id+".tex");
        PaperService.uploadResource(paper_id,paper_id+".tex", $scope.content).then(function (data) {

        }, function (error) {

        });
      };

      /**
      * Get the list of resources
      */
      var getResources = function () {
        PaperService.getResources(paper_id).then(function (data) {
          $scope.resources = data;
        }, function (error) {
          MessagesService.clear();
          switch (error.status) {
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
      * Close connection on leaving
      */
      $scope.$on('$locationChangeStart', function (event, next, current) {
        exitPaper();
      });

      /**
      * The the paper infos
      */
      var updateTexInterval = 0;
      var getPaperInfo = function (callback) {
        PaperService.getInfo(paper_id).then(function (data) {
          getPages();
          $scope.paper = data;
          $scope.paper.etag = data.header.etag;
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
      getPaperInfo(function(paper) {
        if($scope.paper.authors.indexOf($rootScope.loggedUser.name) >= 0) {
          $scope.status = "author";
          PaperService.joinPaper(paper_id).then(function (data) {
            getSynchronizedFiles(function () {
              initMobWrite();
            });
          });
          getCompilerInfo();
          getResources();
          getSyncTex();
          $scope.compile();
        } else if($scope.paper.reviewers.indexOf($rootScope.loggedUser.name) >= 0) {
          $scope.status = "reviewer";
        } else {
          $scope.status = "error";
        }
      });

      var getCompilerInfo = function() {
        PaperService.getPaperCompiler(paper_id).then(function (data) {
          $scope.compiler = data;
        }, function (error) {
          MessagesService.clear();
          MessagesService.error('_Get_compiler_Unable_to_get_compiler_info_');
        });
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

      $scope.displaySyncTex = function() {
        $scope.displaySyncTexBox = !$scope.displaySyncTexBox;
      };

      //action listener: action in the menu
      $scope.$on('handleAction', function (event, data) {
        if ($scope[data]) {
          $scope[data]();
        }
      });

      $scope.changeFile = function (file) {
        if($scope.currentFile == file) return;
        stopMobWrite();
        $scope.currentFile = file;
        $scope.content = '';
        AceService.setContent($scope.content);
        initMobWrite();
        setTimeout(displayAnnotation, 1000);
      };

      $scope.changeFileFromName = function(filename) {
        if($scope.currentFile.title == filename) return;
        for (var i = $scope.synchronizedFiles.length - 1; i >= 0; i--) {
          var syncFile = $scope.synchronizedFiles[i];
          if(syncFile.title == filename) {
            $scope.changeFile(syncFile);
          }
        }
      };

      WindowActiveService.registerObserverCallback(function(windowActive) {
        if(windowActive == true) {
          pageActive = true;
          $scope.compile();
        } else {
          pageActive = false;
        }
      });

      /**
      * Compile the paper
      */
      var compileActive = false;
      $scope.compile = function () {
        if(!pageActive) return;
        if(compileActive) return;

        compileActive = true;
        PaperService.subscribePaperCompiler(paper_id).then(function (data) {
          compileActive = false;
          getLog();
          if(data.response == true) {
            getSyncTex();
            getPages();
            $scope.revision++;
          }
          $scope.compile();
        }, function (error) {
          compileActive = false;
          setTimeout(function() {
            getLog();
            $scope.compile();
          }, 3000);
        });
      };

      /**
      * Go to the next page
      */
      $scope.nextPage = function () {
        $scope.changePage(parseInt($scope.currentPage)+1);
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
        if(page > 0 && page <=$scope.totalPage ) {
          $scope.currentPage = page;

          for(var i in $scope.synctex.blockNumberLine) {
            if($scope.synctex.blockNumberLine[i][0].page == page) {
              $scope.goToLine(i);
              return;
            }
          }
        }
      };
      /**
      * Go to the line: line
      */
      $scope.goToLine = function (line) {
        AceService.goToLine(line);
        $scope.currentLine = line;
        if(!$scope.synctex) return;
        if(!$scope.synctex.blockNumberLine[$scope.currentFile.title]) return;
        if(!$scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]) return;
        $scope.currentPage = $scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine][0].page;
      };
      $scope.new_file_extension = '.tex';

      $scope.newFile = function(filename) {
        PaperService.newSynchronizedFile($rootScope.loggedUser, paper_id, filename).then(function() {
          $scope.new_file_name = '';
          getSynchronizedFiles();
          getResources();
          $scope.resources = [];
          $scope.synchronizedFiles = [];
        });
      };

      $scope.removeSynchronisedFile = function(file) {
        PaperService.deleteSynchronizedFile($rootScope.loggedUser, paper_id, file.title).then(function() {
          getSynchronizedFiles();
          $scope.synchronizedFiles = [];
        });
      };

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
          _editor.selection.on("changeCursor", function(){
            $scope.$apply(function() {
              $scope.currentLine = _editor.selection.getCursor().row+1;
              if(!$scope.synctex) return;
              if(!$scope.synctex.blockNumberLine[$scope.currentFile.title]) return;
              if(!$scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]) return;
              $scope.currentPage = $scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine][0].page;
            });
          });
          setTimeout(function () {
            $scope.itsalltextClass = (document.querySelector(".centerCol .itsalltext") && document.querySelector(".centerCol .itsalltext").id)?'':'hidden';
            $scope.$apply();
          },1500);
          getLog();
          setTimeout(displayAnnotation, 1000);
          $scope.toc = AceService.getToc();
          AceService.getSession().on("change", function () {
            $scope.toc = AceService.getToc();
          });
          _editor.focus();
        });
      };

      $scope.aceChanged = function (e) {};

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
        PaperService.uploadResource(paper_id, $scope.new_file.title, $scope.new_file.file).then(function (data) {
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
        }, function (progress) {
          $log.debug(progress);
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
          $scope.resourceURL = PaperService.getResourceUrl(paper_id,resource.title);
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
        PaperService.removeResource(paper_id, resource.title).then(function (data) {
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

      /*
      * Download a resource
      */
      $scope.downloadResource = function (resource) {
        window.open(PaperService.getResourceUrl(paper_id, resource.title));
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

      $scope.range = function(n) {
        return new Array(parseInt(n));
      };
    }
  ]);