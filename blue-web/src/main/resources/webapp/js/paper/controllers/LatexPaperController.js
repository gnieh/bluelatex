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
 
/**
* The controller for latex paper 
*/
angular.module('bluelatex.Paper.Controllers.LatexPaper', [
  'angularFileUpload',
  'ngDialog',
  'bluelatex.Paper.Directives.Toc',
  'bluelatex.Paper.Services.Ace',
  'bluelatex.Paper.Services.Paper',
  'bluelatex.Paper.Services.Ace',
  'bluelatex.Latex.Services.SyncTexParser',
  'bluelatex.Paper.Services.Latex',
  'bluelatex.Shared.Services.WindowActive',
  'MobWrite',
  'bluelatex.Paper'])
  .controller('LatexPaperController', 
    ['$rootScope','$scope','localize','$http','$location','AceService','PaperService','$routeParams','MessagesService','SyncTexParserService','$document','WindowActiveService','LatexService','MobWriteService','AceMobWriteClient','$q','compilation_type','ngDialog',
    function (
      $rootScope, 
      $scope,
      localize,
      $http,
      $location,
      AceService,
      PaperService,
      $routeParams,
      MessagesService,
      SyncTexParserService,
      $document,
      WindowActiveService,
      LatexService,
      MobWriteService,
      AceMobWriteClient,
      $q,
      compilation_type,
      ngDialog) {
      $scope.paperId = $routeParams.id;
      var peerId = MobWriteService.syncUsername;
      var pageActive = true;

      $scope.currentLine = 0;
      $scope.currentPage = 0;
      $scope.linePage = 1;
      $scope.page = 1;
      
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
        AceMobWriteClient.message({ type: 'leave', 'user': $rootScope.loggedUser.name });
        stopMobWrite();
        PaperService.leavePaper($scope.paperId, peerId);
      };
      
      window.onbeforeunload = function () {
        return localize.getLocalizedString('_Exit_paper_confirm_');
      };

      window.onunload = function () {
        exitPaper();
      };

      /**
      * Exit paper on controller destroy
      */
      $scope.$on("$destroy", function(){
        exitPaper();
        window.onunload = window.onbeforeunload = null;
      });

      /************/
      /* MobWrite */
      /************/
      /**
      * Start mobWrite
      */
      var initMobWrite = function () {
        AceMobWriteClient.message({ type: 'cursor', 'user': $rootScope.loggedUser.name });
        return MobWriteService.share({paper_id: $scope.paperId,file:$scope.currentFile.title}).then(function (){
          displayAnnotation();
          $scope.toc = LatexService.parseTOC(AceService.getContent());
          AceService.getEditor().focus();
        });
      };

      /**
      * Stop sharing file
      */
      var stopMobWrite = function () {
        if(AceService.getContent() != '') {
          MobWriteService.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title});
        }
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
            if($scope.synchronizedFiles[i].title == "main.tex") {
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
        if(!confirm(localize.getLocalizedString('_Delete_synchronized_file_confirm_', file.title))) return;
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
        if(!confirm(localize.getLocalizedString('_Delete_resource_confirm_', resource.title))) return;
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
        var deferred = $q.defer();
        if($scope.currentFile == file) return;
        MobWriteService.unshare({paper_id: $scope.paperId,file:$scope.currentFile.title}).then(function(data) {
          $scope.currentFile = file;
          $scope.content = '';
          AceService.setContent($scope.content);
          AceService.getEditor().focus();
          displayCursors();
          initMobWrite().then(function (data) {
            if(line) {
              $scope.goToLine(line);
            }
            deferred.resolve(data);
          }, function(err) {
            deferred.reject(err);
          });
        }, function(err) {
          deferred.reject(err);
        });
        return deferred.promise;
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
        page = parseInt(page);
        if(page > 0 && page <= $scope.totalPage ) {
          $scope.linePage = page;
        }
      };

      $scope.$watch('currentPage', function(val) {
        $scope.page = val;
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
        if(pages.length < 1) return;
        $scope.linePage = pages[0];
      };

      // disable compilation when page page is not active
      var windowStatusCallback = function(windowActive) {
        if(windowActive == true) {
          if(pageActive == false) {
            // give the focus to the editor when the window is active
            AceService.getEditor().focus();
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
          PDFJS.getDocument($scope.pdfURL+"?"+$scope.revision,
            null, null, function(progress) {
              $scope.pdfProgress = (progress.loaded / progress.total)*100;
              if($scope.pdfProgress == 100) {
                $scope.pdfProgress = 0;
              }
              $scope.$$phase || $scope.$apply();
          }).then(function(pdf) {
            $scope.pdf = pdf;
            $rootScope.$$phase || $rootScope.$apply();
          }, function(error) {
            console.error(error);
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
        var deferred = $q.defer();
        if(!pageActive){
          deferred.reject("page active");
          return deferred.promise;
        } 
        if(compileActive) {
          deferred.reject("compile in progress");
          return deferred.promise;
        }

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
          if(compilation_type === 'background') {
            $scope.compile();
          }
          deferred.resolve(data);
        }, function (err) {
          deferred.reject(err);
          compileActive = false;
          switch (err.status) {
          // no change
          case 304:
            if(compilation_type === 'background') {
              $scope.compile();
            }
            break;
          // not conneted
          case 401:
            MessagesService.error('_Not_connected_',err);            
            break;
          // other bugs
          default:
            getLog();
            if(compilation_type === 'background') {
              setTimeout(function() {
                $scope.compile();
              }, 3000);
            }
          }
        });
        return deferred.promise;
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
          // list all commands created by the user
          var usrCommands = LatexService.parseCommands(AceService.getContent());
          // list all labels used by the user
          var labels = LatexService.parseLabels(AceService.getContent());
          usrCommands = usrCommands.concat(labels);

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
      /**
       * Share
       */
      $scope.share = function () {
        $scope.paperUrl = document.location.toLocaleString();
        ngDialog.open({ 
          template: 'partials/paper/share_popup.html',
          scope: $scope,
          closeByDocument: true,
          closeByEscape: true,
          controller: 'EditPaperController',
          className: 'ngdialog-theme-plain',
        });
      };

      $scope.cancelShare = function () {
        ngDialog.close();
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
                $scope.compileInProgress = true;
                $scope.$$phase || $scope.$apply();
                MobWriteService.synchronize().then(function() {
                  $scope.compile().finally(function() {
                    $scope.compileInProgress = false;
                    $scope.$$phase || $scope.$apply();
                  });
                });
              }
          });
          AceService.getEditor().selection.on("changeCursor", function(){
            AceMobWriteClient.message({ type: 'cursor', 'user': $rootScope.loggedUser.name });
            $scope.currentLine = parseInt(_editor.selection.getCursor().row)+1;
            $rootScope.$$phase || $rootScope.$apply();
            if(!$scope.synctex) return;
            if(!$scope.synctex.blockNumberLine[$scope.currentFile.title]) return;
            if(!$scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]) return;
            var pages = Object.keys($scope.synctex.blockNumberLine[$scope.currentFile.title][$scope.currentLine]);
            if(pages.length < 1) return;
            $scope.linePage = pages[0];
          });
          AceService.getEditor().setOptions({
              enableBasicAutocompletion: true,
              enableSnippets: true
          });
          // enable autocompletation
          _editor.completers[1] = texCompleter;
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
            displayCursors();
            $scope.toc = LatexService.parseTOC(AceService.getContent());
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
        $scope.downloadOptions = false;
      };
      $scope.downloadPDF = function () {
        window.open($scope.pdfURL);
        $scope.downloadOptions = false;
      };
      $scope.downloadZip = function () {
        window.open($scope.zipURL);
        $scope.downloadOptions = false;
      };

      //action listener: action in the menu
      $rootScope.$on('handleMenuAction', function (event, data) {
        if ($scope[data]) {
          $scope[data]();
        }
      });

      $scope.connectedUsers = {};
      var userStyle = document.createElement('style');
      userStyle.type = 'text/css';
      document.getElementsByTagName('head')[0].appendChild(userStyle);
      var createUserStyle = function () {
        
        userStyle.innerHTML = '';
        var userKeys = Object.keys($scope.connectedUsers);
        for(var user in $scope.connectedUsers) {
          userStyle.innerHTML += '.'+$scope.connectedUsers[user].class+'Color {color: '+$scope.connectedUsers[user].color+'!important;}';
          userStyle.innerHTML += '.'+$scope.connectedUsers[user].class+'Bg {background-color: '+$scope.connectedUsers[user].color+'!important;}';
        }
      };

      var displayCursors = function () {
        var Range = ace.require('ace/range').Range;
        for(var user in $scope.connectedUsers) {
          for(var peer in $scope.connectedUsers[user].peer){
            if($scope.connectedUsers[user].peer[peer].range) {
              AceService.getSession().removeMarker($scope.connectedUsers[user].peer[peer].range);
              $scope.connectedUsers[user].peer[peer].range == null;
            }
            if($scope.connectedUsers[user].peer[peer].file == $scope.currentFile.title) {
              var cursor = $scope.connectedUsers[user].peer[peer].getPosition();
              var cursorClass= "ace_cursor "+$scope.connectedUsers[user].class+"Color";
              if(cursor.start.row != cursor.end.row || cursor.start.column != cursor.end.column) {
                cursorClass= "ace_selection "+$scope.connectedUsers[user].class+"Bg";
              }
              var range = new Range(cursor.start.row,cursor.start.column,cursor.end.row,cursor.end.column + 1);
              $scope.connectedUsers[user].peer[peer].range = AceService.getSession().addMarker(range, cursorClass, "line"); 
            }
          }
        }
      };
      //action listener: action in the menu
      $rootScope.$on('MobWriteMessage', function (event, message) {
        if(message.json.type == 'leave') {
          if(!$scope.connectedUsers[message.json.user]) return;
          if($scope.connectedUsers[message.json.user].peer[message.from]){
            AceService.getSession().removeMarker($scope.connectedUsers[message.json.user].peer[message.from].range);
            delete $scope.connectedUsers[message.json.user].peer[message.from];
          }
          if(Object.keys($scope.connectedUsers[message.json.user].peer).length == 0) {
            delete $scope.connectedUsers[message.json.user];
          }
        } else if(message.json.type == "cursor") {
          if(!$scope.connectedUsers[message.json.user]) {
            var color = stringToColour(message.json.user);
            var rgb = hexToRgb(color);
            var fColor = Math.round(((rgb.r * 299) + (rgb.g * 587) + (rgb.b * 114)) /1000) > 128?'black':'white';
            $scope.connectedUsers[message.json.user] = {
              peer: {},
              color: color,
              name: message.json.user,
              class: message.json.user.replace(/\./g,'').replace(/ /g,'-'),
              forground: fColor
            };
          }
          if($scope.connectedUsers[message.json.user].peer[message.from]){
            AceService.getSession().removeMarker($scope.connectedUsers[message.json.user].peer[message.from].range);
          } else {
            AceMobWriteClient.message({ type: 'cursor', 'user': $rootScope.loggedUser.name });
          }
          $scope.connectedUsers[message.json.user].peer[message.from] = {
            getPosition: message.json.getPosition,
            range: null,
            file: message.filename
          };
          createUserStyle();

          displayCursors();
        }
      });

      $scope.range = function(n) {
        return new Array(parseInt(n));
      };
    }
  ]);
