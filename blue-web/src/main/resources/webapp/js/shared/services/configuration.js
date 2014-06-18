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
 
angular.module('bluelatex.Configuration',[])
  // the rest api root
  .constant('api_prefix','')
  .constant('recaptcha_public_key', null)
  .factory("ConfigurationService", ['$http','$q','api_prefix','recaptcha_public_key', function ($http, $q,api_prefix,recaptcha_public_key) {
  	return {
  		getConfiguration: function  () {
  		  var deferred = $q.defer();
        $http({method:'get',url:  "configuration"}).then(function (data) {
          var configuration = data.data;
          api_prefix = configuration['api_prefix'];
          recaptcha_public_key = configuration['recaptcha_public_key'];
          deferred.resolve(configuration);
        }, function (error) {
          deferred.reject(error);
        }, function (progress) {
          deferred.notify(progress);
        });
        return deferred.promise;
  		}
  	}
  }]);
