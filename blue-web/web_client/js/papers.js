angular.module('bluelatex.papers',[])
    .controller('PapersController', ['$rootScope','$scope','localize','User', function ($rootScope,$scope,localize, User) {
    $scope.reverse = false;
    $scope.predicate = 'title';
    var papers = [
      {
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my First Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my two Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my tree Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my fore Paper',
        date: new Date(new Date().setDate(new Date().getDate()-1)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my five Paper',
        date: new Date(new Date().setDate(new Date().getDate()-30)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my six Paper',
        date: new Date(new Date().setDate(new Date().getDate()-5)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my seven Paper',
        date: new Date(new Date().setDate(new Date().getDate()-2)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my First Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my First Paper',
        date: new Date(new Date().setDate(new Date().getDate()-365)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my First Paper',
        date: new Date(new Date().setDate(new Date().getDate()-100)),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my First Paper',
        date: new Date(new Date().setDate(new Date().getDate()-1)),
        authors: [
          'thomas', 'lui', 'elle'
          ]
      }
    ];

    User.getPapers($rootScope.loggedUser).then(function (data) {
      $scope.papers = [];
      for (var i = 0; i < data.length; i++) {
        data[i].date = new Date();
        data[i].authors = [];
        $scope.papers.push(data[i]);
      };
    }, function (err) {
      console.log(err);
    })
    $scope.papers = [];
    $scope.display_style = 'list';
    $scope.date_filter = 'all';

    $scope.role_filter = 'all';

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
      console.log("delete", paper)
    };

    //action listener: action in the menu
    $scope.$on('handleAction', function(event, data){
      if($scope[data]) {
        $scope[data]();
      }
    });

  }]);