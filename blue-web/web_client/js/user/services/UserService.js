angular.module("bluelatex.User.Services.User", ["ngResource", 'jmdobry.angular-cache', 'bluelatex.Shared.Services.Configuration'])
  .factory("UserService", ['$resource', '$http', '$log', '$angularCacheFactory', '$q', 'apiRootUrl',
      function ($resource, $http, $log, $angularCacheFactory, $q, apiRootUrl) {
        // userCache
        var _dataCache = $angularCacheFactory('userCache', {
          maxAge: 300000,
          storageMode: 'localStorage',
          deleteOnExpire: 'aggressive',
          verifyIntegrity: true
        });

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
        var papers = $resource(apiRootUrl + "/users/:username/papers", {
          username: "@username"
        }, {
          "get": {
            method: "GET",
            isArray: true,
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        });
        var password = $resource(apiRootUrl + "/users/:username/reset", {
          username: "@username"
        }, {
          "getToken": {
            method: "get"
          },
          "reset": {
            'method': 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        });
        var info = $resource(apiRootUrl + "/users/:username/info", {
          username: "@username"
        }, {
          "get": {
            method: "get",
            format: 'json',
            transformResponse: [

              function (data, headersGetter) {
                data = JSON.parse(data);
                data.header = headersGetter();
                return data;
              }
            ].concat($http.defaults.transformResponse)
          },
          "modify": {
            'method': 'PATCH',
            transformRequest: [

              function (data, headersGetter) {
                $log.log(data, headersGetter);
                return data;
              }
            ].concat($http.defaults.transformRequest)
          }
        });
        var register = $resource(apiRootUrl + "/users", null, {
          "register": {
            method: "POST",
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        });
        var removeUser = $resource(apiRootUrl + "/users/:username", {
          username: "@username"
        }, {
          "remove": {
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
          getPapers: function (user) {
            var deferred = $q.defer();
            var promise = deferred.promise;
            if (_dataCache.get('/papers/' + user.name)) deferred.resolve(_dataCache.get('/papers/' + user.name));
            else {
              papers.get({
                username: user.name
              }).$promise.then(function (data) {
                _dataCache.put('/papers/' + user.name, data);
                deferred.resolve(data);
              }, function (error) {
                $log.error(error);
                deferred.reject(error);
              }, function (progress) {
                deferred.notify(progress);
              });
            }
            return promise;
          },
          getPasswordToken: function (username) {
            return password.getToken({
              username: username
            }).$promise;
          },
          resetPassword: function (username, reset_token, new_password1, new_password2) {
            return password.reset({
              username: username
            }, jsonToPostParameters({
              reset_token: reset_token,
              new_password1: new_password1,
              new_password2: new_password2
            })).$promise;
          },
          getInfo: function (user) {
            var deferred = $q.defer();
            var promise = deferred.promise;
            if (_dataCache.get('/users/' + user.username)) deferred.resolve(_dataCache.get('/users/' + user.username));
            else {
              info.get({
                username: user.username
              }).$promise.then(function (data) {
                _dataCache.put('/users/' + user.username, data);
                deferred.resolve(data);
              }, function (error) {
                $log.error(error);
                deferred.reject(error);
              }, function (progress) {
                deferred.notify(progress);
              });
            }
            return promise;
          },
          save: function (user) {
            var deferred = $q.defer();
            var promise = deferred.promise;
            info.modify({
              username: user.name
            }, user).$promise.then(function (data) {
              _dataCache.remove('/users/' + user.name);
              deferred.resolve(data);
            }, function (error) {
              $log.error(error);
              deferred.reject(error);
            }, function (progress) {
              deferred.notify(progress);
            });
            return promise;
          },
          register: function (user) {
            return register.register({}, jsonToPostParameters(user)).$promise;
          },
          remove: function (user) {
            var deferred = $q.defer();
            var promise = deferred.promise;
            removeUser.remove({
              username: user.username
            }, user).$promise.then(function (data) {
              _dataCache.remove('/users/' + user.username);
              deferred.resolve(data);
            }, function (error) {
              $log.error(error);
              deferred.reject(error);
            }, function (progress) {
              deferred.notify(progress);
            });
            return promise;
          }
        };
      }
    ]);