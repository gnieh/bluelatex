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
angular.module('bluelatex.Paper.Controllers.InitPaper', 
  ['bluelatex.Paper.Services.Paper'])
  .controller('InitPaperController', 
    ['$rootScope',
     '$scope',
     'PaperService',
     '$routeParams',
     'MessagesService',
     '$q',
    function ($rootScope,
              $scope,
              PaperService,
              $routeParams,
              MessagesService,
              $q) {

      var paperId = $routeParams.id;
      // the status of the page
      $scope.status = "load";
      // the type of paper (latex or markdown)
      $scope.paperType = "";

      /**
      * Get the paper infos
      */
      var getPaperInfo = function () {
        var deferred = $q.defer();
        PaperService.getInfo(paperId).then(function (data) {
          deferred.resolve(data);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 404:
            MessagesService.error('_Get_info_paper_Paper_not_found_',err);
            break;
          case 401:
            MessagesService.error('_Get_info_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_info_paper_Something_wrong_happened_',err);
          }
          deferred.reject(err);
        });
        return deferred.promise;
      };

      /**
      * Get the user paper role
      */
      var getUserPaperRole = function () {
        var deferred = $q.defer();
        PaperService.getRoles(paperId).then(function (roles) {
          // not connected user
          var role = "anonymous";
          // connected user
          if ($rootScope.loggedUser.name != null) {
            role = "other";

            if(roles.authors.indexOf($rootScope.loggedUser.name) >= 0) {
              role = "author";
            } else if(roles.reviewers.indexOf($rootScope.loggedUser.name) >= 0) {
              role = "reviewer";
            }
          }
          deferred.resolve(role);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 404:
            MessagesService.error('_Get_roles_paper_Paper_not_found_',err);
            break;
          case 401:
            MessagesService.error('_Get_roles_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_roles_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_roles_paper_Something_wrong_happened_',err);
          }
          deferred.reject(err);
        });
        return deferred.promise;
      };

      /**
      * Get the user paper permissions
      */
      var getUserPaperPermissions = function () {
        var deferred = $q.defer();
        PaperService.getPermissions(paperId).then(function (permissions) {
          getUserPaperRole().then(function(role) {
            deferred.resolve(permissions[role]);
          }, deferred.reject);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 404:
            MessagesService.error('_Get_permissions_paper_Paper_not_found_',err);
            break;
          case 401:
            MessagesService.error('_Get_permissions_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_permissions_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_permissions_paper_Something_wrong_happened_',err);
          }
          deferred.reject(err);
        });
        return deferred.promise;
      };

      /**
      * Get the paper info and load the correct page
      */
      getPaperInfo().then(function(paper) {
        // change the title of the page
        $rootScope.pageTitle = "Paper - " + paper.name;

        getUserPaperPermissions().then(function (permissions) {
          // change the type of paper
          $scope.paperType = "latex";
          if (permissions.indexOf("view") != -1) {
            $scope.status = "author";
          } else {
            $scope.paperType = "";
            $scope.status = "error";
          }
        })
      });
    }
  ]);
