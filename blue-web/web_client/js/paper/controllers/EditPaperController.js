angular.module('bluelatex.Paper.Controllers.EditPaper', ['bluelatex.Paper.Services.Paper','bluelatex.User.Services.User','angucomplete'])
  .controller('EditPaperController', ['$scope', 'localize', '$location', 'PaperService','UserService', '$routeParams', '$log','MessagesService',
    function ($scope, localize, $location, PaperService,UserService, $routeParams, $log,MessagesService) {
      var paper_id = $routeParams.id;
      var clone_paper = {};

      $scope.users = [];
      $scope.new_author = '';
      $scope.new_reviewer = '';
      $scope.new_tag = '';

      /**
      * Get paper info
      */
      var getPaperInfo = function (paper_id, callback) {
        PaperService.getInfo(paper_id).then(function (data) {
          $scope.paper = data;
          $scope.paper.etag = data.header.etag;
          $scope.paper.id = paper_id;
          clone_paper = clone($scope.paper);
          if(callback) {
            callback($scope.paper);
          }
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Get_info_paper_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Get_info_paper_Wrong_username_and_or_password_',err);
            break;
          case 404:
            MessagesService.error('_Get_info_paper_Paper_not_found_',err);
            break;
          case 500:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
          }
          // redirect the user
        });
      };

      getPaperInfo(paper_id, function () {
        getUsers();
      });
      /**
      * Get the list of all users
      */
      var getUsers = function (search, callback) {
        UserService.getUsers(search).then(function (users) {
          $scope.users = users;
          if(callback) callback();
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_users_not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_users_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_users_Something_wrong_happened_',err);
          }
        });
      };
      /**
      * Remove an author
      */
      $scope.removeAuthor = function (author) {
        $scope.paper.authors.splice($scope.paper.authors.indexOf(author), 1);
      };
      /**
      * Remove a reviewer
      */
      $scope.removeReviewer = function (reviewer) {
        $scope.paper.reviewers.splice($scope.paper.reviewers.indexOf(reviewer), 1);
      };
      /**
      * Remove a tag
      */
      $scope.removeTag = function (tag) {
        $scope.paper.tags.splice($scope.paper.tags.indexOf(tag), 1);
      };
      /**
      * Add a new author
      */
      $scope.addAuthor = function () {
        var author = $scope.new_author;
        if(!author.title) return;
        if ($scope.paper.authors.indexOf(author.title) < 0)
          $scope.paper.authors.push(author.title);
        $scope.new_author = '';
      };
      /**
      * Add a new reviewer
      */
      $scope.addReviewer = function () {
        var reviewer = $scope.new_reviewer;
        if(!reviewer.title)return;
        if ($scope.paper.reviewers.indexOf(reviewer.title) < 0)
          $scope.paper.reviewers.push(reviewer.title);
        $scope.new_reviewer = '';
      };
      /**
      * Add a new tag
      */
      $scope.addTag = function () {
        var tag = $scope.new_tag;
        if ($scope.paper.tags.indexOf(tag) < 0)
          $scope.paper.tags.push(tag);
        $scope.new_tag = '';
      };
      /**
      * Modify the paper
      */
      $scope.modify = function () {
        PaperService.modify($scope.paper, clone_paper).then(function (data) {
          $location.path("/papers");
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Edit_paper_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Edit_paper_Wrong_username_and_or_password_',err);
            break;
          case 404:
            MessagesService.error('_Edit_paper_Paper_not_found_',err);
            break;
          case 500:
            MessagesService.error('_Edit_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Edit_paper_Something_wrong_happened_',err);
          }
        });
      };
    }
  ]);