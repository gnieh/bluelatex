angular.module('bluelatex.menu', [])
  .controller('menuController', function($scope,$route) {
    $scope.links = [
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
  })
  .directive('blMenu', function() {
    return {
      templateUrl: 'partials/menu.html'
    };
  });