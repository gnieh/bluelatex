angular.module("bluelatex.User.Controllers.Profile",['bluelatex.User.Services.User'])
  .controller('ProfileController', ['$rootScope', '$scope', 'UserService', '$location', '$log','MessagesService',
    function ($rootScope, $scope, UserService, $location, $log,MessagesService) {

      UserService.getInfo($rootScope.loggedUser).then(function(data) {
        $scope.user = data;
        $scope.$apply();
      });
 
      $scope.remove = function () {
        MessagesService.clear();
        UserService.remove($scope.user).then(function (data) {
          $rootScope.loggedUser = {};
          $location.path("/login");
        }, function (err) {
          switch (err.status) {
          case 400:
            MessagesService.error('_Remove_user_Captcha_not_verify_or_user_not_authenticated_',err);
            break;
          case 401:
            MessagesService.error('_Remove_user_The_captcha_did_not_verify_',err);
            break;
          case 403:
            MessagesService.error('_Remove_user_The_user_still_owns_papers_',err);
            break;
          case 500:
            MessagesService.error('_Remove_user_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Remove_user_Something_wrong_happened_',err);
          }
        });
      };

      $scope.edit = function () {
        MessagesService.clear();
        UserService.save($scope.user,$rootScope.loggedUser).then(function (data) {
          MessagesService.messageSession('_Edit_profile_success_',data);
          UserService.getInfo($rootScope.loggedUser.name).then(function(data) {
            $rootScope.loggedUser = data;
          });
          $location.path("/papers");
        }, function (err) {
          switch (err.status) {
          case 304:
            MessagesService.error('_Edit_profile_No_enough_data_',err);
            break;
          case 401:
            MessagesService.error('_Edit_profile_User_must_be_authenticated_',err);
            break;
          case 403:
            MessagesService.error('_Edit_profile_Not_authorized_to_modifiy_the_user_data_',err);
            break;
          case 404:
            MessagesService.error('_Edit_profile_User_does_not_exist_',err);
            break;
          case 409:
            MessagesService.error('_Edit_profile_No_revision_obsolete_revision_was_provided_in_the_request_',err);
            break;
          case 500:
            MessagesService.error('_Edit_profile_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Edit_profile_Something_wrong_happened_',err);
          }
        });
      };
    }
  ]);