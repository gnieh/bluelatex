angular.module('bluelatex.Paper.Controllers.Paper', ['angularFileUpload','bluelatex.Paper.Directives.Toc','bluelatex.Paper.Services.Ace','bluelatex.Paper.Services.Paper','bluelatex.Paper.Services.Ace'])
  .controller('PaperController', ['$scope', 'localize', '$location', 'AceService', 'PaperService', '$routeParams', '$upload', '$log','MessagesService',
    function ($scope, localize, $location, AceService, PaperService, $routeParams, $upload, $log,MessagesService) {
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

      $scope.resources = [];
      $scope.paper = {};
      $scope.listType = 'files';
      $scope.mode = 'ace';
      $scope.logs = [];
      $scope.toc = [];
      $scope.content = '';
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
      $scope.goToLine = function (line) {
        AceService.goToLine(line);
      };

      $scope.aceLoaded = function (_editor) {
        AceService.aceLoaded(_editor, function () {
          $scope.toc = AceService.getToc();
          AceService.getSession().on("change", function () {
            $scope.toc = AceService.getToc();
          });
          _editor.focus();
        });
      };

      $scope.aceChanged = function (e) {};

      $scope.new_file = {};

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