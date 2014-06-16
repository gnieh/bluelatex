angular.module('bluelatex.Paper.Controllers.Papers', ['ngStorage','bluelatex.Paper.Services.Paper'])
  .controller('PapersController', ['$rootScope', '$scope', 'PaperService','$log','MessagesService','$localStorage',
    function ($rootScope, $scope, PaperService,$log,MessagesService,$localStorage) {
      // list of peper
      $scope.papers = [];

      // load settings from localStorage
      $scope.reverse = $localStorage.userPaperReverse;
      if($scope.reverse == null)
        $scope.reverse = false;

      $scope.$watch("reverse", function(value) {
        $localStorage.reverse = value;
      });

      $scope.predicate = $localStorage.userPaperPredicate;
      if($scope.predicate == null)
        $scope.predicate = 'title';
        
      $scope.$watch("predicate", function(value) {
        $localStorage.userPaperPredicate = value;
      });

      $scope.orderBy = function(predicate, reverse) {
        $scope.predicate = predicate;
        $scope.reverse = reverse;
      };

      $scope.display_style = $localStorage.userPaperStyle;
      if($scope.display_style == null)
        $scope.display_style = 'list';

      $scope.$watch("display_style", function(value) {
        $localStorage.userPaperStyle = value;
      });

      $scope.date_filter = 'all';
      $scope.role_filter = 'all';
      $scope.tag_filter = 'all';

      // get paper of a user
      PaperService.getUserPapers($rootScope.loggedUser).then(function (data) {
        $scope.papers = data;
      }, function (err) {
        MessagesService.clear();
        switch (err.status) {
        case 401:
          MessagesService.error('_List_Papers_Not_connected_',err);
          break;
        case 500:
          MessagesService.error('_List_Papers_Something_wrong_happened_',err);
          break;
        default:
          MessagesService.error('_List_Papers_Something_wrong_happened_',err);
        }
      });

      /***************/
      /* Date Filter */
      /***************/
      var dateFilterToday = function (paper) {
        var now = new Date();
        return paper.date.getDate() == now.getDate() &&
          paper.date.getMonth() == now.getMonth() &&
          paper.date.getFullYear() == now.getFullYear();
      };
      $scope.dateFilterToday = dateFilterToday;

      var dateFilterYesterday = function (paper) {
        var yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        return paper.date.getDate() == yesterday.getDate() &&
          paper.date.getMonth() == yesterday.getMonth() &&
          paper.date.getFullYear() == yesterday.getFullYear();
      };
      $scope.dateFilterYesterday = dateFilterYesterday;

      var dateFilterLWeek = function (paper) {
        var now = new Date();
        return paper.date.getWeek() == now.getWeek() - 1;
      };
      $scope.dateFilterLWeek = dateFilterLWeek;

      var dateFilterTWeek = function (paper) {
        var now = new Date();
        return paper.date.getWeek() == now.getWeek() &&
          paper.date.getFullYear() == now.getFullYear();
      };
      $scope.dateFilterTWeek = dateFilterTWeek;

      var dateFilterMonth = function (paper) {
        var now = new Date();
        return paper.date.getMonth() == now.getMonth() &&
          paper.date.getFullYear() == now.getFullYear();
      };
      $scope.dateFilterMonth = dateFilterMonth;
      var dateFilterYear = function (paper) {
        var now = new Date();
        return paper.date.getFullYear() == now.getFullYear();
      };
      $scope.dateFilterYear = dateFilterYear;

      $scope.dateFilter = function (paper) {
        switch ($scope.date_filter) {
        case 'all':
          return true;
        case 'today':
          return dateFilterToday(paper);
        case 'yesterday':
          return dateFilterYesterday(paper);
        case 'lweek':
          return dateFilterLWeek(paper);
        case 'tweek':
          return dateFilterTWeek(paper);
        case 'month':
          return dateFilterMonth(paper);
        case 'year':
          return dateFilterYear(paper);
        }
      };

      /***************/
      /* Role filter */
      /***************/
      var roleFilterAuthor = function (paper) {
        return paper.role == 'author';
      };

      $scope.roleFilterAuthor = roleFilterAuthor;
      var roleFilterReviewer = function (paper) {
        return paper.role == 'reviewer';
      };

      $scope.roleFilterReviewer = roleFilterReviewer;
      $scope.roleFilter = function (paper) {
        switch ($scope.role_filter) {
        case 'all':
          return true;
        case 'author':
          return roleFilterAuthor(paper);
        case 'reviewer':
          return roleFilterReviewer(paper);
        }
      };

      /**
      * Delete a paper
      */
      $scope.delete = function (paper) {
        if(!confirm("Are you sure you want to remove the paper: " + paper.title +"?")) return;
        PaperService.delete(paper.id).then(function (data) {
          if (data.response == true) {
            $scope.papers.splice($scope.papers.indexOf(paper), 1);
          }
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Delete_paper_User_must_be_authentified_',err);
            $rootScope.loggedUser = {};
            break;
          case 403:
            MessagesService.error('_Delete_paper_Authenticated_user_has_no_sufficient_rights_to_delete_the_paper_',err);
            break;
          case 500:
            MessagesService.error('_Delete_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Delete_paper_Something_wrong_happened_',err);
          }
        });
      };

      //action listener: action in the menu
      $scope.$on('handleAction', function (event, data) {
        if ($scope[data]) {
          $scope[data]();
        }
      });
    }
  ]);