angular.module('bluelatex.papers',[])
    .controller('PapersController', ['$scope','localize', function ($scope,localize) {
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
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my five Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my six Paper',
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      },{
        id: 'id',
        icon: '//writelatex.s3.amazonaws.com/qkmscjgckstd/page/4e97a487a6e1525e2c7d548b98caa06447003d8c.jpeg',
        title: 'my seven Paper',
        date: new Date(),
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
        date: new Date(),
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
        date: new Date(),
        authors: [
          'thomas', 'lui'
          ]
      }
    ];

    $scope.papers = papers;
    $scope.display_style = 'list';

    //action listener: action in the menu
    $scope.$on('handleAction', function(event, data){
      if($scope[data]) {
        $scope[data]();
      }
    });

  }]);