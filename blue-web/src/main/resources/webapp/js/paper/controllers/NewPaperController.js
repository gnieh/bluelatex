angular.module('bluelatex.Paper.Controllers.NewPaper', ['bluelatex.Shared.Services.Configuration'])
  .controller('NewPaperController', ['$scope', '$location', 'PaperService', '$log','MessagesService',
    function ($scope, $location, PaperService, $log,MessagesService) {
      var paper = {};

      $scope.paper = paper;

      /* Hard-coded for now, but will be dynamically loaded  later */
      $scope.templates = {
        "article": {
          "name": "Article",
          "scope": "Global"
        },
        "llncs": {
          "name": "Lecture Notes in Computer Science",
          "scope": "Global"
        },
        "sigproc-sp": {
          "name": "Strict Adherence to SIGS style",
          "scope": "Global"
        },
        "sig-alternate": {
          "name": "SIGS Tighter Alternate",
          "scope": "Global"
        }
      };

      /*
      * Save the new paper
      */
      $scope.create = function () {
        PaperService.create(paper).then(function (data) {
          $location.path("/paper/" + data.id);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_New_paper_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_New_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_New_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_New_paper_Something_wrong_happened_',err);
          }
        });
      };

    }
  ]);
