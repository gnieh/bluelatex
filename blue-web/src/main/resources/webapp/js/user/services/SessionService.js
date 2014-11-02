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
 
angular.module("bluelatex.User.Services.Session", ["ngResource"])
  .factory("SessionService", [
    '$resource',
    '$http',
    'config',
    function ($resource,
              $http,
              config) {

      var api_prefix = config.api_prefix;

      var session = $resource(api_prefix + "/session", null, {
        "login": {
          method: "POST",
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          transformResponse: [

            function (data, headersGetter) {
              if (data == 'true') return {
                response: JSON.parse(data)
              };
              return data;
            }
          ].concat($http.defaults.transformResponse)
        },
        "get": {
          method: "GET"
        },
        "logout": {
          'method': 'DELETE'
        }
      });

      return {
        login: function (username, password) {
          return session.login({}, jsonToPostParameters({
            "username": username,
            'password': password
          })).$promise;
        },
        logout: function () {
          return session.logout().$promise;
        },
        getSession: function () {
          return session.get().$promise;
        },
      };
    }
  ]);