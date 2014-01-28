angular.module("bluelatex.User.Controllers.ResetPassword",['bluelatex.User.Services.User'])
  .controller('ResetPasswordController', ['$scope', '$routeParams', 'UserService', 'localize', '$location', '$log',
    function ($scope, $routeParams, UserService, localize, $location, $log) {
      var user = {};
      $scope.user = user;

      $scope.resetPassword = function () {
        UserService.resetPassword($routeParams.username, $routeParams.token, user.new_password, user.new_password_2).then(function (data) {
          $log.log(data);
          if (data.name != 'unable_to_reset') {
            $location.path("/");
          } else {
            $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
          }
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
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
            $log.error(err);
          }
        });
      };

      $scope.reset = function () {
        UserService.getPasswordToken(user.username).then(function (data) {
          if (data.name != 'unable_to_reset') {
            $location.path("/");
          } else {
            $scope.errors.push(localize.getLocalizedString('_Login_Some_parameters_are_missing_'));
          }
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
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
            $log.error(err);
          }
        });
      };
    }
  ]);