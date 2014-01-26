angular.module('bluelatex.papers',[])
    .controller('PapersController', ['$rootScope','$scope','localize','User','Paper', function ($rootScope,$scope,localize, User, Paper) {
    $scope.reverse = false;
    $scope.predicate = 'title';


    User.getPapers($rootScope.loggedUser).then(function (data) {
      $scope.papers = [];
      for (var i = 0; i < data.length; i++) {
        data[i].date = new Date();
        data[i].authors = [];
        $scope.papers.push(data[i]);
      };
    }, function (err) {
      $scope.errors = [];
      switch(err.status){
        case 401:
          $scope.errors.push(localize.getLocalizedString('_List_Papers_User_must_be_authentified_'));
          break;
        case 500:
          $scope.errors.push(localize.getLocalizedString('_List_Papers_Something_wrong_happened_'));
          break;
        default:
          $scope.errors.push(localize.getLocalizedString('_List_Papers_Something_wrong_happened_'));
          console.log(err);
      }
    })
    $scope.papers = [];
    $scope.tags = ['A tag','CV'];
    $scope.display_style = 'list';
    $scope.date_filter = 'all';
    $scope.role_filter = 'all';
    $scope.tag_filter = 'all';

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
    var dateFilterYear= function (paper) {
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

    $scope.delete = function (paper) {
      Paper.delete(paper.id).then(function (data) {
        if(data.response == true) {
          $scope.papers.splice($scope.papers.indexOf(paper), 1);
        }
      }, function (err) {
        $scope.errors = [];
        switch(err.status){
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Delete_paper_User_must_be_authentified_'));
            $rootScope.loggedUser = {};
            break;
          case 403:
            $scope.errors.push(localize.getLocalizedString('_Delete_paper_Authenticated_user_has_no_sufficient_rights_to_delete_the_paper_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Delete_paper_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Delete_paper_Something_wrong_happened_'));
            console.log(err);
        }
      })
    };

    //action listener: action in the menu
    $scope.$on('handleAction', function(event, data){
      if($scope[data]) {
        $scope[data]();
      }
    });

  }]);