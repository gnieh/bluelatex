'use strict';

/* Controllers */

angular.module('bluelatex.controller', ['bluelatex.User'])
  .controller('MainController', ['$scope','User','$route','$location','$routeParams','ace', function ($scope, User,$route,$location,$routeParams,ace) {
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
  }]);