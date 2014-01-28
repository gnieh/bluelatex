angular.module('bluelatex.Paper.Controllers.NewPaper', ['bluelatex.Shared.Services.Configuration'])
  .controller('NewPaperController', ['$scope', 'localize', '$location', 'PaperService', '$log','MessagesService',
    function ($scope, localize, $location, PaperService, $log,MessagesService) {
      var paper = {};

      $scope.paper = paper;

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
            MessagesService.error('_New_paper_Wrong_username_and_or_password_',err);
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