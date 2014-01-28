angular.module("bluelatex.User.Services.Session", ["ngResource", 'bluelatex.Shared.Services.Configuration'])
  .factory("SessionService", ['$resource', '$http', '$log', 'apiRootUrl',
    function ($resource, $http, $log, apiRootUrl) {
      var session = $resource(apiRootUrl + "/session", null, {
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