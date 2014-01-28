angular.module('bluelatex.Paper.Controllers.NewPaper', ['bluelatex.Shared.Services.Configuration'])
  .controller('NewPaperController', ['$scope', 'localize', '$location', 'PaperService', '$log',
    function ($scope, localize, $location, PaperService, $log) {
      var paper = {};

      $scope.paper = paper;

      $scope.create = function () {
        PaperService.create(paper).then(function (data) {
          $location.path("/paper/" + data.response);
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
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
            $log.error(err);
          }
        });
      };

    }
  ]);