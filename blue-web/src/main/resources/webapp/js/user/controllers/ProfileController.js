/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
angular.module("bluelatex.User.Controllers.Profile",['bluelatex.User.Services.User','reCAPTCHA'])
  .controller('ProfileController', [
    '$rootScope',
    '$scope',
    'UserService',
    '$location',
    '$log',
    'MessagesService',
    'localize',
    'reCAPTCHA',
    'config',
    function ($rootScope,
              $scope,
              UserService,
              $location,
              $log,
              MessagesService,
              localize,
              reCAPTCHA,
              config) {
      $scope.requesting = false;
      var user;

      reCAPTCHA.setOptions({
         theme: 'clean'
      });

      $scope.displayCaptcha = config.recaptcha_public_key != null;
      reCAPTCHA.setPublicKey(config.recaptcha_public_key);

      /**
      * Get the user data
      */
      UserService.getInfo($rootScope.loggedUser).then(function(data) {
        $scope.user = data;
        user = clone($scope.user);
      });
      /**
      * delete the user
      */
      $scope.delete = function () {      
        $scope.requesting = true;
        if(!confirm(localize.getLocalizedString('_Delete_account_confirm_'))) {
          $scope.requesting = false;
          return;
        }
        MessagesService.clear();
        if($scope.displayCaptcha) {
          $scope.user.recaptcha_response_field = reCAPTCHA.response();
          $scope.user.recaptcha_challenge_field = reCAPTCHA.challenge();
        }
        UserService.delete($scope.user).then(function (data) {
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
        UserService.getPasswordToken(user.name).then(function (data) {
          console.log(data);
          if (data.response == true) {
            MessagesService.messageSession('_Reset_Wait_email_confirm_request_');
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
      /**
      * Edit the user profile
      */
      $scope.edit = function () {
        $scope.requesting = true;
        MessagesService.clear();
        UserService.save($scope.user,user).then(function (data) {
          MessagesService.messageSession('_Edit_profile_success_',data);
          UserService.getInfo($rootScope.loggedUser).then(function(data) {
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
        }).finally(function() {
          $scope.requesting = false;
        });
      };
    }
  ]);
