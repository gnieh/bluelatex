angular.module("bluelatex.User.Controllers.ResetPassword",['bluelatex.User.Services.User'])
  .controller('ResetPasswordController', ['$scope', '$routeParams', 'UserService', '$location', '$log','MessagesService',
    function ($scope, $routeParams, UserService, $location, $log,MessagesService) {
      var user = {};
      $scope.user = user;
      $scope.requesting = false;

      MessagesService.clear();
      /**
      * Change the password
      */
      $scope.resetPassword = function () {
        $scope.requesting = true;
        MessagesService.clear();
        UserService.resetPassword($routeParams.username, $routeParams.token, user.new_password, user.new_password_2).then(function (data) {
          if (data.name != 'unable_to_reset') {
            MessagesService.messageSession('_Reset_Password_changed_');
            $location.path("/login");
          } else {
            MessagesService.error('_Reset_password_Something_wrong_happened_');
          }
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
          case 400:
            MessagesService.error('_Reset_password_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Reset_password_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Reset_password_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Reset_password_Something_wrong_happened_',err);
          }
        }).finally(function() {
          $scope.requesting = false;
        });
      };
      
      /**
      * Ask a reset password token
      */
      $scope.reset = function () {
        $scope.requesting = true;
        MessagesService.clear();
        UserService.getPasswordToken(user.username).then(function (data) {
          console.log(data);
          if (data.response == true) {
            MessagesService.messageSession('_Reset_Wait_email_confirm_request_');
            $location.path("/login");
          } else {
            MessagesService.error('_Login_Some_parameters_are_missing_');
          }
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
          case 404:
            MessagesService.error('_Reset_User_not_found',err);
            break;
          case 400:
            MessagesService.error('_Reset_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Reset_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Reset_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Reset_Something_wrong_happened_',err);
          }
        }).finally(function() {
          $scope.requesting = false;
        });
      };
    }
  ]);