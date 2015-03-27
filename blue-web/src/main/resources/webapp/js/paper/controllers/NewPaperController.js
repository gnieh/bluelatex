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

angular.module('bluelatex.Paper.Controllers.NewPaper', [])
  .controller('NewPaperController', [
    '$scope',
    '$location',
    'PaperService',
    'MessagesService',
    function ($scope, 
              $location,
              PaperService,
              MessagesService) {
      
      var paper = {
        template: "article",
        visibility: "private",
        paper_title: '',
        paper_name: ''
      };

      $scope.saving=false;

      $scope.paper = paper;

      $scope.visibilities = {
        "public": "public",
        "private": "private"
      };

      /* Hard-coded for now, but will be dynamically loaded later */
      $scope.templates = {
        "article": {
          "name": "Article",
          "scope": "Built-in"
        },
        "report": {
          "name": "Report",
          "scope": "Built-in"
        },
        "book": {
          "name": "Book",
          "scope": "Built-in"
        },
        "beamer": {
          "name": "Beamer",
          "scope": "Built-in"
        },
        "llncs": {
          "name": "Lecture Notes in Computer Science",
          "scope": "Global"
        },
        "sigproc-sp": {
          "name": "Strict Adherence to SIGS style",
          "scope": "Global"
        },
        "sig-alternate": {
          "name": "SIGS Tighter Alternate",
          "scope": "Global"
        }
      };

      /*
      * Save the new paper
      */
      $scope.create = function () {
        $scope.saving=true;

        PaperService.create(paper).then(function (data) {
          $location.path("/paper/" + data.id);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_New_paper_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_New_paper_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_New_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_New_paper_Something_wrong_happened_',err);
          }
        }).finally(function () {
          $scope.saving=false;
        });
      };
      // autocomplete the title with the content of the paper name
      $scope.$watch('paper.paper_name', function(newValue, oldValue) {
        if($scope.paper.paper_title == null ||  $scope.paper.paper_title == '' || $scope.paper.paper_title == oldValue) {
          $scope.paper.paper_title = newValue;
        }
        //$scope.$$phase || $scope.$apply();
      });
      // autocomplete the title with the content of the paper title
      /*$scope.$watch('paper.paper_title', function(newValue, oldValue) {
        if($scope.paper.paper_name == null ||  $scope.paper.paper_name == '' || $scope.paper.paper_name == oldValue) {
          $scope.paper.paper_name = newValue;
        }
      });*/
    }
  ]);
