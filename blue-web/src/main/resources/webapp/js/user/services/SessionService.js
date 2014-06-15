angular.module("bluelatex.User.Services.Session", ["ngResource", 'bluelatex.Configuration'])
  .factory("SessionService", ['$resource', '$http', '$log', 'api_prefix',
    function ($resource, $http, $log, api_prefix) {
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