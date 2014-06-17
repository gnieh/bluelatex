'use strict';
/**
* The main controller of the website, this controller handle window resize event
* and retransmit events launched by the menu
*/
angular.module('bluelatex.Shared.Controllers.Main', ['bluelatex.User.Services.User','bluelatex.User.Services.Session','bluelatex.Paper.Services.Ace','bluelatex.Configuration','bluelatex.Shared.Services.WindowActive'])
  .controller('MainController', ['$rootScope', '$scope','$window', 'UserService','SessionService', '$route', '$location', '$routeParams', 'AceService', '$log','WindowActiveService','ConfigurationService',
    function ($rootScope, $scope,$window, UserService,SessionService, $route, $location, $routeParams, AceService, $log,WindowActiveService,ConfigurationService) {
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
        $rootScope.$broadcast('windowResize', event);
      };

      //
      $scope.$on("$routeChangeSuccess", function (event, route) {
        $scope.currentRoute = route;
      });

      var getConfiguration =  function() {
        return ConfigurationService.getConfiguration();
      }

      // login the user with data present in sessionStorage
      var getUserSession = function () {
        if ($rootScope.loggedUser == null || $rootScope.loggedUser.name ) {
          SessionService.getSession().then(function (data) {
            UserService.getInfo({
              name: data.name
            }).then(function (data) {
              $rootScope.loggedUser = data;
              $rootScope.$$phase || $rootScope.$apply();
            }, function (error) {
              $rootScope.loggedUser = {};
            });
          }, function (error) {
            $rootScope.loggedUser = {};
            $rootScope.$$phase || $rootScope.$apply();
          });
        }
      };

      //check user session every  minute
      var interval = 1 /* min */ * 60 /* sec */ * 1000 /* nano-sec*/;
      var intervalId = setInterval(getUserSession, interval);
      getConfiguration().then(function (configuration) {
        getUserSession();
      });

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