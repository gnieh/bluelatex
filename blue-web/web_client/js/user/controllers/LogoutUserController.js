angular.module("bluelatex.User.Controllers.Logout",['bluelatex.User.Services.Session','ngStorage'])
  .controller('LogoutController', ['$rootScope', '$scope', 'SessionService', 'localize', '$location', '$sessionStorage', '$log',
    function ($rootScope, $scope, SessionService, localize, $location, $sessionStorage, $log) {
      SessionService.logout().then(function (data) {
        $rootScope.loggedUser = {};
        delete $sessionStorage.username;
        delete $sessionStorage.password;
        $location.path("/login");
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
    }
  ]);