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
 
angular.module("bluelatex.User.Controllers.Register",['bluelatex.User.Services.User','reCAPTCHA'])
  .controller('RegisterController', ['$scope', 'UserService', '$location', '$log','MessagesService','ConfigurationService','reCAPTCHA',
    function ($scope, UserService, $location, $log,MessagesService,ConfigurationService,reCAPTCHA) {
      $scope.user = {};
      $scope.requesting = false;

      reCAPTCHA.setOptions({
         theme: 'clean'
      });

      ConfigurationService.getConfiguration().then(function(data) {
        $scope.displayCaptcha = data.recaptcha_public_key != null;
        reCAPTCHA.setPublicKey(data.recaptcha_public_key);
      });
      
      /**
      * Create a new user
      */
      $scope.register = function () {
        $scope.requesting = true;
        if($scope.displayCaptcha) {
          $scope.user.recaptcha_response_field = reCAPTCHA.response();
          $scope.user.recaptcha_challenge_field = reCAPTCHA.challenge();
        }
        console.log($scope.user);
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
