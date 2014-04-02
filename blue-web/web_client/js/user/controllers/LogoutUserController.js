angular.module("bluelatex.User.Controllers.Logout",['bluelatex.User.Services.Session','bluelatex.User.Services.User','ngStorage'])
  .controller('LogoutController', ['$rootScope', 'SessionService','UserService', '$location', '$sessionStorage','MessagesService',
    function ($rootScope, SessionService,UserService, $location, $sessionStorage,MessagesService) {
      MessagesService.clear();
      SessionService.logout().then(function (data) {
        $rootScope.loggedUser = {};
        delete $sessionStorage.username;
        delete $sessionStorage.password;
        UserService.clearCache();
        $location.path("/login");
      }, function (err) {
        switch (err.status) {
        case 400:
          MessagesService.error('_Logout_Some_parameters_are_missing_',err);
          break;
        case 401:
          MessagesService.error('_Logout_Wrong_username_and_or_password_',err);
          break;
        case 500:
          MessagesService.error('_Logout_Something_wrong_happened_',err);
          break;
        default:
          MessagesService.error('_Logout_Something_wrong_happened_',err);
        }
      });
    }
  ]);