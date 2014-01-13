angular.module('bluelatex.menu', [])
  .controller('menuController', function($scope,$route) {
    var defaulfMenu = [
      {
        label: 'Login',
        link: '#/login',
        name: 'login'
      },
      {
        label: 'Register',
        link: '#/register',
        name: 'register'
      },
      {
        label: 'Papers',
        link: '#/papers',
        name: 'papers'
      },
      {
        label: 'Profil',
        link: '#/profil.html',
        name: 'profil'
      }
    ];
    var options = [];
    var menus = [];

    for (var i = 0; i < defaulfMenu.length; i++) {
      options.push(defaulfMenu[i]);
    };
    $scope.menus = menus;
    $scope.options = options;

    $scope.addMenu = function (m) {
      menus.push(m);
    };
    $scope.addOption = function (o) {
      options.push(o);
    };
    $scope.addOptions = function (o) {
      for (var i = 0; i < o.length; i++) {
        options.push(o[i]);
      };
    };
  })
  .directive('blMenu', function() {
    return {
      templateUrl: 'partials/menu.html'
    };
  }).directive('blOptions', function() {
    return {
      templateUrl: 'partials/options.html'
    };
  });