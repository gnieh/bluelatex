angular.module("bluelatex.User.Controllers.Logout",['bluelatex.User.Services.Session','bluelatex.User.Services.User'])
  .controller('LogoutController', ['$rootScope', 'SessionService','UserService', '$location','MessagesService',
    function ($rootScope, SessionService,UserService, $location,MessagesService) {
      MessagesService.clear();
      /*
      * Logout the user
      */
      SessionService.logout().then(function (data) {
        $rootScope.loggedUser = {};
        UserService.clearCache();
        $location.path("/login");
      }, function (err) {
        switch (err.status) {
        case 401:
          MessagesService.error('_Logout_Not_connected_',err);
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