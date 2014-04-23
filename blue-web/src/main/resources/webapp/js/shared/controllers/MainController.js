'use strict';
/**
* The main controller of the website, this controller handle window resize event
* and retransmit events launched by the menu
*/
angular.module('bluelatex.Shared.Controllers.Main', ['bluelatex.User.Services.User','bluelatex.User.Services.Session','ngStorage','bluelatex.Paper.Services.Ace'])
  .controller('MainController', ['$rootScope', '$scope','$window', 'UserService','SessionService', '$route', '$location', '$routeParams', 'AceService', '$sessionStorage', '$log','WindowActiveService',
    function ($rootScope, $scope,$window, UserService,SessionService, $route, $location, $routeParams, AceService, $sessionStorage, $log,WindowActiveService) {
      // give access to current route
      $scope.$route = $route;
      // give access to current url
      $scope.$location = $location;
      // give access to current path parameters
      $scope.$routeParams = $routeParams;
      // give access to aceService
      $scope.ace = AceService;

      // retransmits window resize event
      $window.onresize = function (event) {
        $scope.$broadcast('windowResize', event);
      };

      // transfert the menu event
      $scope.$on('handleTopAction', function (event, data) {
        $scope.$broadcast('handleAction', data);
      });

      //
      $scope.$on("$routeChangeSuccess", function (event, route) {
        $scope.currentRoute = route;
      });

      // login the user with data present in sessionStorage
      var getUserSession = function () {
        if ($sessionStorage.username && $sessionStorage.password) {
          SessionService.login($sessionStorage.username, $sessionStorage.password).then(function () {
            UserService.getInfo({
              username: $sessionStorage.username
            }).then(function (data) {
              $rootScope.loggedUser = {
                name: $sessionStorage.username,
                first_name: data.first_name,
                last_name: data.last_name,
                email: data.email,
                etag: data.header.etag
              };
            }, function (error) {
              $rootScope.loggedUser = {};
            });
          });
        }
      };

      //check user session every  minute
      var interval = 60 * 1000;
      var intervalId = setInterval(getUserSession, interval);
      getUserSession();

      // don't check session when the page is not active
      WindowActiveService.registerObserverCallback(function () {
        var data = WindowActiveService.isActiveWindow();
        if (data == false) {
          clearInterval(intervalId);
          intervalId = null;
        } else if (intervalId == null) {
          intervalId = setInterval(getUserSession, interval);
          getUserSession();
        }
      });
    }
  ]);