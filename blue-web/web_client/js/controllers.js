'use strict';

/* Controllers */

angular.module('bluelatex.controller', ['bluelatex.User'])
  .controller('MainController', ['$rootScope','$scope','User','$route','$location','$routeParams','ace','$sessionStorage', function ($rootScope,$scope, User,$route,$location,$routeParams,ace,$sessionStorage) {
    $scope.$on( "$routeChangeSuccess", function(event, route) {
      $scope.currentRoute = route;
    });
    $scope.$errors = [];
    $scope.$messages = [];
    $scope.$route = $route;
    $scope.$location = $location;
    $scope.$routeParams = $routeParams;
    $scope.ace = ace;

    $scope.$on('handleTopAction', function(event, data){
      $scope.$broadcast('handleAction', data);
    });
    var getUserSession = function () {
      if($sessionStorage.username && $sessionStorage.password) {
        User.login($sessionStorage.username, $sessionStorage.password).then(function () {
          User.getInfo({username: $sessionStorage.username}).then(function (data) {
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
            $rootScope.loggedUser = {};
          });
        });
      }
    };

    //check user session every  minute
    var interval = 60*1000;
    var intervalId = intervalId = setInterval(getUserSession, interval);
    getUserSession();
    $scope.$on('windowActive', function(event, data){
      if(data==false) {
        clearInterval(intervalId);
        intervalId = null;
      } else if(intervalId == null){
        intervalId = setInterval(getUserSession, interval);
        getUserSession();
      }
    });
  }]);