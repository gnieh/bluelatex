'use strict';

/* Controllers */

angular.module('bluelatex.controller', ['bluelatex.User'])
  .controller('MainController', ['$rootScope','$scope','User','$route','$location','$routeParams','ace', function ($rootScope,$scope, User,$route,$location,$routeParams,ace) {
    $scope.$on( "$routeChangeSuccess", function(event, route) {
      $scope.currentRoute = route;
    });
    $scope.$route = $route;
    $scope.$location = $location;
    $scope.$routeParams = $routeParams;
    $scope.ace = ace;

    $scope.$on('handleTopAction', function(event, data){
      $scope.$broadcast('handleAction', data);
    });

    User.getSession().then(function (data) {
      $rootScope.loggedUser = {
        username: data.name,
        name: data.name,
         roles: data.roles
      };
      User.getInfo($rootScope.loggedUser).then(function (data) {
        $rootScope.loggedUser.first_name = data.first_name;
        $rootScope.loggedUser.last_name = data.last_name;
        $rootScope.loggedUser.email = data.email;
        $rootScope.loggedUser.etag = data.header.etag
      }, function (err) {
        console.log(err);
      }, function (progress) {
        // body...
      });
    }, function (error) {
      console.log(error);
    });

  }]);