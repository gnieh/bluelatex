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
  }])
  .controller('LoginLogoutController', ['$scope','User','localize','$location', function ($scope, User,localize,$location) {
    var user = {};
    $scope.user = user;
    $scope.errors = [];

    $scope.logout = function () {
        $scope.errors = [];

        User.login(user.username, user.password).then(function(data) {
            $location.path( "/papers" );
        }, function(err) {
          switch(err.status){
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Login_Wrong_username_and_or_password_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {
            console.log(progress);
        });
    };

    $scope.login = function () {
        $scope.errors = [];

        User.login(user.username, user.password).then(function(data) {
            $location.path( "/papers" );
        }, function(err) {
          switch(err.status){
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Login_Wrong_username_and_or_password_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {
            console.log(progress);
        });
    };
  }]).controller('ResetController', ['$scope','$routeParams','User','localize','$location', function ($scope,$routeParams, User,localize,$location) {
    var user = {};
    $scope.user = user;
    $scope.errors = [];

    $scope.resetPassword = function () {
        $scope.errors = [];
        console.log(user);
        User.resetPassword($routeParams.username,$routeParams.token, user.new_password, user.new_password_2).then(function(data) {
          console.log(data);
          if(data.name != 'unable_to_reset'){
            $location.path( "/" );
          } else {
            $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
          }
        }, function(err) {
          switch(err.status){
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Login_Wrong_username_and_or_password_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {
            console.log(progress);
        });
    };
    $scope.reset = function () {
        $scope.errors = [];
        console.log(user);
        User.getPasswordToken(user.username).then(function(data) {
          console.log(data);
          if(data.name != 'unable_to_reset'){
            $location.path( "/" );
          } else {
            $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
          }
        }, function(err) {
          switch(err.status){
            case 404:
              $scope.errors.push(localize.getLocalizedString('_Reset_User_not_found'));
              break;
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Login_Wrong_username_and_or_password_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Login_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {
            console.log(progress);
        });
    };
  }]).controller('RegisterController', ['$scope','$routeParams','User','localize','$location', function ($scope,$routeParams, User,localize,$location) {
    var user = {};
    $scope.user = user;
    $scope.errors = [];

    $scope.register = function () {
        $scope.errors = [];

        User.register(user).then(function(data) {
            $location.path( "/papers" );
        }, function(err) {
          switch(err.status){
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Registration_Some_parameters_are_missing_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Registration_The_captcha_did_not_verify_'));
              break;
            case 409:
              $scope.errors.push(localize.getLocalizedString('_Registration_User_with_the_same_username_already_exists_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Registration_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Registration_Something_wrong_happened_'));
          }
        }, function (progress) {
            console.log(progress);
        });
    };

  }]).controller('ProfileController', ['$scope','User','localize','$location', function ($scope, User,localize,$location) {
    var user = {};
    $scope.user = user;
    $scope.errors = [];

    User.getInfo(user).then(function(data){
      user = data;
    }, function (err) {
      switch(err.status){
        case 500:
          $scope.errors.push(localize.getLocalizedString('_User_info_Something_wrong_happened_'));
          break;
        default:
          $scope.errors.push(localize.getLocalizedString('_User_info_Something_wrong_happened_'));
          console.log(err);
      }
    }, function(progress){});

    $scope.remove = function () {
        User.remove(user).then(function(data) {
            $location.path( "/login" );
        }, function(err) {
          switch(err.status){
            case 400:
              $scope.errors.push(localize.getLocalizedString('_Remove_user_Captcha_not_verify_or_user_not_authenticated_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Remove_user_The_captcha_did_not_verify_'));
              break;
            case 403:
              $scope.errors.push(localize.getLocalizedString('_Remove_user_The_user_still_owns_papers_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Remove_user_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Remove_user_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {});
    };

    $scope.edit = function () {
        User.save(user).then(function(data) {
            $scope.errors.message(localize.getLocalizedString('_Edit_profile_success_'));
        }, function(err) {
          switch(err.status){
            case 304:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_No_enough_data_'));
              break;
            case 401:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_User_must_be_authenticated_'));
              break;
            case 403:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_Not_authorized_to_modifiy_the_user_data_'));
              break;
            case 404:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_User_does_not_exist_'));
              break;
            case 409:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_No_revision_obsolete_revision_was_provided_in_the_request_'));
              break;
            case 500:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
              break;
            default:
              $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
              console.log(err);
          }
        }, function (progress) {});
    };

  }])
  .controller('papersController', [function() {

  }])
  .controller('MyCtrl2', [function() {

  }]);