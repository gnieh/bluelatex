angular.module("bluelatex.User.Controllers.Login",['bluelatex.User.Services.Session','bluelatex.User.Services.User','ngStorage'])
  .controller('LoginController', ['$rootScope', '$scope', 'UserService','SessionService', 'localize', '$location', '$sessionStorage', '$log',
    function ($rootScope, $scope, UserService,SessionService, localize, $location, $sessionStorage, $log) {
      var user = {};
      $scope.user = user;

      $scope.login = function () {
        SessionService.login(user.username, user.password).then(function (data) {
          if (data.response == true) {
            UserService.getInfo(user).then(function (data) {
              $sessionStorage.username = user.username;
              $sessionStorage.password = user.password;
              $rootScope.loggedUser = {
                name: data.name,
                first_name: data.first_name,
                last_name: data.last_name,
                email: data.email,
                etag: data.header.etag
              };
              $location.path("/");
            }, function (err) {
              $log.error(err);
            });
          } else {

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
            $log.log(err);
          }
        });
      };
    }
  ]);