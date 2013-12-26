var apiRoot = '/api';

var jsonToPostParameters = function (json) {
    return Object.keys(json).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(json[k])
    }).join('&')
}
angular.module("bluelatex.User", ["ngResource"])
  .factory("User", function ($resource, $http) {
    var session =  $resource( apiRoot+"/session", null, {
            "login": {method: "POST", headers:{'Content-Type':'application/x-www-form-urlencoded'}},
            "get": {method: "GET"},
            "logout": {'method': 'DELETE'}
        }
    );
    var papers =  $resource( apiRoot+"/users/:username/papers", {username: "@username" }, {
            "get": {method: "GET"}
        }
    );
    var password =  $resource( apiRoot+"/users/:username/reset", {username: "@username" }, {
            "getToken": {method: "get"},
            "reset": {'method': 'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}}
        }
    );
    var info = $resource( apiRoot+"/users/:username/info", {username: "@username" }, {
            "get": {method: "get"},
            "modify": {'method': 'PATCH', headers:{'Content-Type':'application/x-www-form-urlencoded'}}
        }
    );
    var register = $resource( apiRoot+"/users", null, {
            "register": {method: "POST", headers:{'Content-Type':'application/x-www-form-urlencoded'}}
        }
    );
    var removeUser = $resource( apiRoot+"/users/:username", {username: "@username" }, {
            "remove": {'method': 'DELETE'}
        }
    );
    return {
        login: function(username, password) {
            console.log("login");

            return session.login({},jsonToPostParameters({
                "username":username,'password':password
            })).$promise;
        },
        logout: function() {
            return session.logout().$promise;
        },
        getSession: function() {
            return session.get().$promise;
        },
        getPapers: function(user) {
            return papers.get({ username: user.username }).$promise;
        },
        getPasswordToken: function() {
            return password.getToken().$promise;
        },
        resetPassword: function() {
            return password.reset().$promise;
        },
        getInfo: function(user) {
            return info.get({ username: user.username }).$promise;
        },
        save: function(user) {
            return info.modify({ username: user.username }, jsonToPostParameters(user)).$promise;
        },
        register: function(user) {
            return register.register({},jsonToPostParameters(user)).$promise
        },
        remove: function(user) {
            return removeUser.remove({ username: user.username }).$promise
        }
    };
});