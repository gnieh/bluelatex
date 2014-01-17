angular.module('bluelatex.menu', [])
  .controller('menuController', function($rootScope, $scope,$route) {
    $rootScope.$watch('loggedUser', function(value) {
      if(value.name == null) {
        $scope.openUserMenu = false;
      }
      $scope.user= value;
    });
    $scope.$on( "$routeChangeSuccess", function(event, route) {
      $scope.menus = [];
      if(route.$$route == null) return;
      for (var i = 0; i < defaulfMenu.length; i++) {
        defaulfMenu[i].active = route.$$route.options.name == defaulfMenu[i].name;
        $scope.menus.push(defaulfMenu[i]);
      }
      $scope.options = [];
      var _options = optionsPage[route.$$route.options.name];
      if(_options!=null)
      for (var i = 0; i < _options.length; i++) {
        $scope.options.push(_options[i]);
      }
    });

    $scope.openUserMenu= false;

    var defaulfMenu = [
      {
        label: 'Papers',
        link: '#/papers',
        name: 'papers',
        connected: true,
        unconnected: false,
        active: false
      },
      {
        label: 'Login',
        link: '#/login',
        name: 'login',
        connected: false,
        unconnected: true,
        active: false
      },
      {
        label: 'Register',
        link: '#/register',
        name: 'register',
        connected: false,
        unconnected: true,
        active: false
      }
    ];
    var optionsPage = {
      'papers': [
        {
          label: 'New Paper',
          type: 'link',
          link: '#/paper/new',
          class: '',
          icon: null
        }
      ],
      'paper': [
        {
          label: 'Share',
          type: 'menu',
          class: '',
          icon: null,
          action: 'partials/paper_share_menu.html'
        },
        {
          label: 'Settings',
          type: 'menu',
          class: '',
          icon: null,
          action: 'partials/paper_settings.html'
        }
      ]
    };

    $scope.menus = [];
    $scope.options = [];

    $scope.action = function (option) {
      if(option.type == 'action') {
        $scope.$emit('handleTopAction', option.action);
      } else if(option.type == 'menu') {
        option.display = !option.display;
      }
      return false;
    }
  })
  .directive('blMenu', function() {
    return {
      templateUrl: 'partials/menu.html'
    };
  });