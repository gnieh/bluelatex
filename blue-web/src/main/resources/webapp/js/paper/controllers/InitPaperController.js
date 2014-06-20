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
angular.module('bluelatex.Paper.Controllers.InitPaper', ['bluelatex.Paper.Services.Paper'])
  .controller('InitPaperController', ['$rootScope','$scope', 'PaperService', '$routeParams','MessagesService','$q',
    function ($rootScope,$scope, PaperService, $routeParams,MessagesService,$q) {
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
          switch (error.status) {
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
      * Get the paper info and load the correct page
      */
      getPaperInfo().then(function(paper) {
        // change the title of the page
        $rootScope.pageTitle = "Paper - " + paper.name;

        // change the type of paper
        $scope.paperType = "latex";
        //$scope.paperType = "markdown";

        // change the status of the page
        if(paper.authors.indexOf($rootScope.loggedUser.name) >= 0) {
          $scope.status = "author";
        } else if(paper.reviewers.indexOf($rootScope.loggedUser.name) >= 0) {
          $scope.status = "reviewer";
        } else {
          $scope.status = "error";
        }
      });
    }
  ]);
