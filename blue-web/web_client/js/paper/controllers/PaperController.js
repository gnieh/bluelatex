angular.module('bluelatex.Paper.Controllers.Paper', ['angularFileUpload','bluelatex.Paper.Directives.Toc','bluelatex.Paper.Services.Ace','bluelatex.Paper.Services.Paper','bluelatex.Paper.Services.Ace','bluelatex.Latex.Services.SyncTexParser'])
  .controller('PaperController', ['$scope', 'localize', '$location', 'AceService', 'PaperService', '$routeParams', '$upload', '$log','MessagesService','SyncTexParserService',
    function ($scope, localize, $location, AceService, PaperService, $routeParams, $upload, $log,MessagesService,SyncTexParserService) {
      $scope.pageViewport = {};
      $scope.currentElem = {};
      $scope.resources = [];
      $scope.paper = {};
      $scope.listType = 'debug';
      $scope.mode = 'ace';
      $scope.logs = [];
      $scope.toc = [];
      $scope.content = '';
      $scope.new_file = {};
      $scope.synctex = null;
      $scope.current = {line: 0};

      $scope.vignetteType = "pdf";
      $scope.urlPaper = "/ressources/latex_example";
      $scope.scale = "auto";
      $scope.currentPage = 1;
      $scope.totalPage = 1;

      var paper_id = $routeParams.id;
      var getPaperInfo = function () {
        PaperService.getInfo(paper_id).then(function (data) {
          $scope.paper = data;
          $scope.paper.etag = data.header.etag;
        }, function (error) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Delete_resource_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
          }
        });
      };
      getPaperInfo();

      var getSynchronizedFiles = function () {
        PaperService.getSynchronized(paper_id).then(function (data) {
          $scope.synchronizedFiles = data;
        }, function (error) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Delete_resource_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
          }
        });
      };
      getSynchronizedFiles();

      var getResources = function () {
        PaperService.getResources(paper_id).then(function (data) {
          $scope.resources = data;
        }, function (error) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Delete_resource_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
          }
        });
      };
      getResources();

      var getTexfile = function (file_name) {
        PaperService.getTexfile(file_name).then(function (data) {
          $scope.content = data;
        }, function (error) {
          // body...
        });
      };
      getTexfile('ressources/latex_example.tex');



      var createBlock = function (b, page){
        var previews = preview.getElementsByClassName('preview_page_container');
        var block = document.createElement('div');

        block.style.bottom = b.bottom + 'px';
        block.style.left = b.left + 'px';
        block.style.width = b.width + 'px';
        block.style.height = b.height + 'px';
        block.classList.add(b.type);
        block.classList.add('debug_block');

        var info = document.createElement('div');
        info.classList.add('info');
        info.innerHTML = '<span class="file">'+b.fileNumber+'</span><span class="line">'+b.line+'</span>';
        block.appendChild(info);

        previews[page-1].appendChild(block);
      };

      var createElem = function (elm ,page){
        //if(elm.type!='x') return;
        var previews = preview.getElementsByClassName('preview_page_container');
        var block = document.createElement('div');
        block.style.bottom = elm.bottom + 'px';
        block.style.left = elm.left+ 'px';
        block.style.width =  '1px';
        block.style.height = elm.height+'px';
        block.classList.add('debug_block');
        block.classList.add(elm.type);

        var info = document.createElement('div');
        info.classList.add('info');
        info.innerHTML = '<span class="file">'+elm.fileNumber+'</span><span class="line">'+elm.line+'</span>';
        block.appendChild(info);
        previews[page-1].appendChild(block);
      };

      var removeBlocks = function () {
        var blocks = preview.getElementsByClassName('debug_block');
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

      var displayPages = function (pages) {
        removeBlocks();
        for(var i in pages) {
          var page = pages[i];
          displayBlocks(page.blocks,i);
        }
      };

      SyncTexParserService.parseUrl('ressources/latex_example.synctex').then(function (data) {
        $scope.synctex = data;
      });

      $scope.$watch('pageViewport', function (value) {
        if(value.scale == null) return;

      });
      $scope.$watch('displaySyncTexBox', function (value) {
        if(value)
          displayPages($scope.synctex.pages);
        else
          removeBlocks();
      });

      //action listener: action in the menu
      $scope.$on('handleAction', function (event, data) {
        if ($scope[data]) {
          $scope[data]();
        }
      });

      $scope.switch_editor_mode = function () {
        $scope.mode = ($scope.mode == 'ace' ? 'text' : 'ace');
        if ($scope.mode == 'ace') {
          AceService.setContent($scope.content);
          AceService.getEditor().focus();
        }
      };

      $scope.compile = function () {

      };

      $scope.nextPage = function () {
        if($scope.currentPage != $scope.totalPage) {
          $scope.currentPage++;
        }
      };
      $scope.prevPage = function () {
        if($scope.currentPage != 1) {
          $scope.currentPage--;
        }
      };
      $scope.goToLine = function (line) {
        AceService.goToLine(line);
        $scope.current.line = line;
      };

      var removeLineHightlight = function () {
        var blocks = preview.getElementsByClassName('hightlight_line');
        for (var i = blocks.length - 1; i >= 0; i--) {
          var block = blocks[i];
          block.parentNode.removeChild(block);
        }
      };
      var createCurrrentLineHightlight = function (left, bottom, width,height, page) {
        var previews = preview.getElementsByClassName('preview_page_container');
        if(!previews[page-1]) return;

        var block = document.createElement('div');

        block.style.bottom = bottom + 'px';
        block.style.left = left + 'px';
        block.style.width = width + 'px';
        block.style.height = height + 'px';
        block.classList.add('hightlight_line');
        previews[page-1].appendChild(block);
      };

      $scope.aceLoaded = function (_editor) {
        AceService.aceLoaded(_editor, function () {
          $scope.toc = AceService.getToc();
          AceService.getSession().on("change", function () {
            $scope.toc = AceService.getToc();
          });
          _editor.selection.on("changeCursor", function(){
            $scope.$apply(function() {
              $scope.current.line = _editor.selection.getCursor().row+1;
            });
          });
          _editor.focus();
        });
      };

      $scope.aceChanged = function (e) {};

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

      $scope.uploadResource = function () {
        PaperService.uploadResource(paper_id, $scope.new_file.title, $scope.new_file.file).then(function (data) {
          if(data.response == true) {
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
            MessagesService.error('_Upload_resource_Wrong_username_and_or_password_',err);
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

      $scope.cancelUploadResource = function () {
        $scope.new_file = {};
      };
      $scope.viewResource = function (resource) {

      };
      $scope.removeResource = function (resource) {
        PaperService.removeResource(paper_id, resource.title).then(function (data) {
          if(data.response == true) {
            getResources();
            $scope.new_file = {};
          } else
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_');
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Delete_resource_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Delete_resource_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_resource_Something_wrong_happened_',err);
          }
        });
      };
      $scope.downloadResource = function (resource) {
        window.open(PaperService.getResourceUrl(paper_id, resource.title));
      };
    }
  ]);