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
 
/**
* Controller for the edit paper page
*/
angular.module('bluelatex.Paper.Controllers.EditPaper', ['bluelatex.Paper.Services.Paper','bluelatex.User.Services.User','angucomplete'])
  .controller('EditPaperController', ['$scope', 'localize', '$location', 'PaperService','UserService', '$routeParams', '$log','MessagesService','$q',
    function ($scope, localize, $location, PaperService,UserService, $routeParams, $log,MessagesService, $q) {
      /**
      * Paper id
      */
      var paperId = $routeParams.id;

      var clone_paper = {};
      var clone_paperRoles = {};

      $scope.users = [];
      $scope.newAuthor = '';
      $scope.newReviewer = '';
      $scope.newTag = '';

      $scope.saving=false;

      /**
      * Get paper info
      */
      var getPaperInfo = function (paperId) {
        var deferred = $q.defer();
        PaperService.getInfo(paperId).then(function (data) {
          $scope.paper = data;
          $scope.paper.id = paperId;
          clone_paper = clone($scope.paper);
          deferred.resolve($scope.paper);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_info_paper_Not_connected_',err);
            break;
          case 404:
            MessagesService.error('_Get_info_paper_Paper_not_found_',err);
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
      * Get paper roles
      */
      var getPaperRoles = function (paperId) {
        var deferred = $q.defer();
        PaperService.getRoles(paperId).then(function (data) {
          $scope.paperRoles = data;
          $scope.paperRoles.id = paperId;
          clone_paperRoles = clone($scope.paperRoles);
          deferred.resolve($scope.paperRoles);
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_roles_paper_Not_connected_',err);
            break;
          case 404:
            MessagesService.error('_Get_roles_paper_Paper_not_found_',err);
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

      // Get paper info
      getPaperInfo(paperId).then(function (paper) {
        getPaperRoles(paperId);
        // get all users for autocompletion
        getUsers();
      });

      /**
      * Get the paper compiler info
      */
      var getCompilerInfo = function() {
        return PaperService.getPaperCompiler(paperId).then(function (data) {
          $scope.compiler = data;
          $scope.compilerSettings = JSON.parse(JSON.stringify(data));
        }, function (error) {
          MessagesService.clear();
          MessagesService.error('_Get_compiler_Unable_to_get_compiler_info_');
        });
      }; 
      getCompilerInfo();

      /*
      * Modify compiler options
      */
      var modifyCompiler = function () {
        if($scope.compiler.interval != $scope.compilerSettings.interval || 
           $scope.compiler.synctex != $scope.compilerSettings.synctex || 
           $scope.compiler.compiler != $scope.compilerSettings.compiler) {
          return PaperService.editPaperCompiler(paperId, $scope.compilerSettings, $scope.compiler).then(function () {
            getCompilerInfo();
          });
        } 
      };

      var getCompilers = function() {
        PaperService.getCompilers().then(function (data) {
          $scope.compilers = data;
        }, function (error) {
          MessagesService.clear();
          MessagesService.error('_Get_compilers_Unable_to_get_compiler_list_');
        });
      };
      getCompilers();


      /**
      * Get the list of all users
      */
      var getUsers = function (search, callback) {
        UserService.getUsers(search).then(function (users) {
          $scope.users = users;
          if(callback) callback();
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 401:
            MessagesService.error('_Get_users_Not_connected_',err);
            break;
          case 500:
            MessagesService.error('_Get_users_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Get_users_Something_wrong_happened_',err);
          }
        });
      };
      /**
      * Remove an author
      */
      $scope.removeAuthor = function (author) {
        $scope.paperRoles.authors.splice($scope.paperRoles.authors.indexOf(author), 1);
      };
      /**
      * Remove a reviewer
      */
      $scope.removeReviewer = function (reviewer) {
        $scope.paperRoles.reviewers.splice($scope.paperRoles.reviewers.indexOf(reviewer), 1);
      };
      /**
      * Remove a tag
      */
      $scope.removeTag = function (tag) {
        $scope.paper.tags.splice($scope.paper.tags.indexOf(tag), 1);
      };
      /**
      * Add a new author
      */
      $scope.addAuthor = function () {
        var author = $scope.newAuthor;
        if(!author.title) return;
        if ($scope.paperRoles.authors.indexOf(author.title) < 0)
          $scope.paperRoles.authors.push(author.title);
        $scope.newAuthor = '';
        $scope.$$phase || $scope.$apply();
      };
      /**
      * Add a new reviewer
      */
      $scope.addReviewer = function () {
        var reviewer = $scope.newReviewer;
        if(!reviewer.title)return;
        if ($scope.paperRoles.reviewers.indexOf(reviewer.title) < 0)
          $scope.paperRoles.reviewers.push(reviewer.title);
        $scope.newReviewer = '';
        $scope.$$phase || $scope.$apply();
      };
      /**
      * Add a new tag
      */
      $scope.addTag = function () {
        var tag = $scope.newTag;
        if ($scope.paper.tags.indexOf(tag) < 0)
          $scope.paper.tags.push(tag);
        $scope.newTag = '';
      };
      /**
      * Modify paper data
      */
      $scope.modify = function () {
        $scope.saving=true;
        var deferred = $q.defer();
        var promises = [];

        var promisePaper = PaperService.modify($scope.paper, clone_paper).then(function (data) {
          $location.path("/papers");
        }, function (err) {
          MessagesService.clear();
          switch (err.status) {
          case 400:
            MessagesService.error('_Edit_paper_Some_parameters_are_missing_',err);
            break;
          case 401:
            MessagesService.error('_Edit_paper_Not_connected_',err);
            break;
          case 404:
            MessagesService.error('_Edit_paper_Paper_not_found_',err);
            break;
          case 500:
            MessagesService.error('_Edit_paper_Something_wrong_happened_',err);
            break;
          default:
            MessagesService.error('_Edit_paper_Something_wrong_happened_',err);
          }
        });

        var promiseRoles = $scope.modifyPaperRole();
        var promiseCompiler = modifyCompiler();

        promises.push(promisePaper);
        promises.push(promiseRoles);
        promises.push(promiseCompiler);

        return $q.all(promises).finally(function () {
          $scope.saving = false;
        });
      };

      $scope.modifyPaperRole = function() {
        return PaperService.modifyRoles($scope.paperRoles, clone_paperRoles);
      }
    }
  ]);
