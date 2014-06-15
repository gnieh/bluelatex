angular.module("bluelatex.User.Controllers.Register",['bluelatex.User.Services.User'])
  .controller('RegisterController', ['$scope', 'UserService', '$location', '$log','MessagesService',
    function ($scope, UserService, $location, $log,MessagesService) {
      $scope.user = {};
      $scope.requesting = false;

      $scope.register = function () {
        $scope.requesting = true;
        UserService.register($scope.user).then(function (data) {
          MessagesService.messageSession('_Registration_Success_');
          $location.path("/login");
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Registration_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Registration_The_captcha_did_not_verify_',err);
            break;
          case 409:
            MessagesService.error('_Registration_User_with_the_same_username_already_exists_',err);
            break;
          case 500:
          default:
            MessagesService.error('_Registration_Something_wrong_happened_',err);
          }
        }).finally(function() {
          $scope.requesting = false;
        });
      };
    }
  ]);