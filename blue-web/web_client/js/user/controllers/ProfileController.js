angular.module("bluelatex.User.Controllers.Profile",['bluelatex.User.Services.User'])
  .controller('ProfileController', ['$rootScope', '$scope', 'UserService', 'localize', '$location', '$log',
    function ($rootScope, $scope, UserService, localize, $location, $log) {
      $scope.user = {
        name: $rootScope.loggedUser.name,
        first_name: $rootScope.loggedUser.first_name,
        last_name: $rootScope.loggedUser.last_name,
        email: $rootScope.loggedUser.email,
        roles: $rootScope.loggedUser.roles
      };
      $scope.editProfile = function () {
        UserService.save(save).then(function (data) {
          $location.path("/papers");
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
          case 400:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Some_parameters_are_missing_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Wrong_username_and_or_password_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
            $log.error(err);
          }
        });
      };

      $scope.remove = function () {
        UserService.remove(user).then(function (data) {
          $location.path("/login");
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
          case 400:
            $scope.errors.push(localize.getLocalizedString('_Remove_user_Captcha_not_verify_or_user_not_authenticated_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Remove_user_The_captcha_did_not_verify_'));
            break;
          case 403:
            $scope.errors.push(localize.getLocalizedString('_Remove_user_The_user_still_owns_papers_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Remove_user_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Remove_user_Something_wrong_happened_'));
            $log.error(err);
          }
        });
      };

      $scope.edit = function () {
        UserService.save(user).then(function (data) {
          $scope.messages.push(localize.getLocalizedString('_Edit_profile_success_'));
        }, function (err) {
          $scope.errors = [];
          switch (err.status) {
          case 304:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_No_enough_data_'));
            break;
          case 401:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_User_must_be_authenticated_'));
            break;
          case 403:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Not_authorized_to_modifiy_the_user_data_'));
            break;
          case 404:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_User_does_not_exist_'));
            break;
          case 409:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_No_revision_obsolete_revision_was_provided_in_the_request_'));
            break;
          case 500:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
            break;
          default:
            $scope.errors.push(localize.getLocalizedString('_Edit_profile_Something_wrong_happened_'));
            $log.error(err);
          }
        });
      };
    }
  ]);