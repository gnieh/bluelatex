'use strict';
/**
* A directive bl-gravatar witch display the gravatar of email
*/
angular.module('bluelatex.Shared.Directives.Gravatar', ['angular-md5','gdi2290.gravatar-filter'])
  .directive('blGravatar', [function () {
    return {
      scope: {
        email: '=email'
      },
      template: '<img src="https://secure.gravatar.com/avatar/{{email | gravatar}}?s=200&d=mm">'
    };
  }]);