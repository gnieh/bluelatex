angular.module("bluelatex.User.Controllers.Register",['bluelatex.User.Services.User'])
  .controller('RegisterController', ['$scope', 'UserService', 'localize', '$location', '$log',
    function ($scope, UserService, localize, $location, $log) {
      $scope.user = {};

      $scope.register = function () {
        UserService.register($scope.user).then(function (data) {
          $location.path("/login");
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
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
            $log.error(err);
          }
        });
      };
    }
  ]);