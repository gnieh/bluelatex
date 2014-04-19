angular.module("bluelatex.User.Controllers.Login",['bluelatex.User.Services.Session','bluelatex.User.Services.User','ngStorage'])
  .controller('LoginController', ['$rootScope', '$scope', 'UserService','SessionService', '$location', '$sessionStorage', '$log','MessagesService',
    function ($rootScope, $scope, UserService,SessionService, $location, $sessionStorage, $log,MessagesService) {
      var user = {};
      $scope.user = user;

      $scope.login = function () {
        MessagesService.clear();
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
          switch (err.status) {
          case 400:
            MessagesService.error('_Login_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Login_Wrong_username_and_or_password_',err);
            break;
          case 500:
            MessagesService.error('_Login_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Login_Something_wrong_happened_',err);
          }
        });
      };
    }
  ]);