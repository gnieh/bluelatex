angular.module('bluelatex.Paper.Controllers.EditPaper', ['bluelatex.Paper.Services.Paper'])
  .controller('EditPaperController', ['$scope', 'localize', '$location', 'PaperService', '$routeParams', '$log',
    function ($scope, localize, $location, PaperService, $routeParams, $log) {
      var paper_id = $routeParams.id;
      var clone_paper = {};

      PaperService.getInfo(paper_id).then(function (data) {
        $scope.paper = data;
        $scope.paper.etag = data.header.etag;
        $scope.paper.id = paper_id;
        clone_paper = clone($scope.paper);
      }, function (err) {
        $scope.errors = [];
        switch (err.status) {
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
          $log.error(err);
        }
      });

      $scope.new_author = '';
      $scope.new_reviewer = '';
      $scope.new_tag = '';

      $scope.removeAuthor = function (author) {
        $scope.paper.authors.splice($scope.paper.authors.indexOf(author), 1);
      };
      $scope.removeReviewer = function (reviewer) {
        $scope.paper.reviewers.splice($scope.paper.reviewers.indexOf(reviewer), 1);
      };
      $scope.removeTag = function (tag) {
        $scope.paper.tags.splice($scope.paper.tags.indexOf(tag), 1);
      };

      $scope.addAuthor = function () {
        var author = $scope.new_author;
        if ($scope.paper.authors.indexOf(author) < 0)
          $scope.paper.authors.push(author);
        $scope.new_author = '';
      };
      $scope.addReviewer = function () {
        var reviewer = $scope.new_reviewer;
        if ($scope.paper.reviewers.indexOf(reviewer) < 0)
          $scope.paper.reviewers.push(reviewer);
        $scope.new_reviewer = '';
      };
      $scope.addTag = function () {
        var tag = $scope.new_tag;
        if ($scope.paper.tags.indexOf(tag) < 0)
          $scope.paper.tags.push(tag);
        $scope.new_tag = '';
      };

      $scope.modify = function () {
        PaperService.modify($scope.paper, clone_paper).then(function (data) {
          $log.log(data);
          $location.path("/papers");
        }, function (error) {
          $log.error(error);
        });
      };
    }
  ]);