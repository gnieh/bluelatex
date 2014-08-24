angular.module('bluelatex.Shared.Directives.tooltip', ['ngTooltip'])
.directive('i18nTooltip',[ '$tooltip', 'localize',
  function($tooltip, localize) {
  return {
    restrict: 'EA',
    scope: { 
      content: '@i18nTooltip',
    },
    link: function(scope, elem, attr) {
      /**
      * Translate the message
      */
      var getMessageLocalized = function (m) {
        var tempMessage = localize.getLocalizedString(m);
        if(tempMessage == '' || tempMessage == null) {
          tempMessage = m.replace(/_/g,' ').trim();
        }
        return tempMessage;
      };

      var tooltip = $tooltip({  
        target: elem,
        scope: scope,
        tether: {
          attachment: 'middle left',
          targetAttachment: 'middle right',
          constraints: [
            {
              to: document.getElementById('page'),
              attachment: 'together',
              pin: true
            }
          ]
        }
      });

      var displayTimeout = null;
      elem[0].onmouseover = function() { 
        displayTimeout = setTimeout(function () {
          scope.content = getMessageLocalized(scope.content);
          scope.$apply(tooltip.open); 
        }, 250);
      }; 
      elem[0].onmouseout = function() { 
        clearTimeout(displayTimeout);
        scope.$apply(tooltip.close); 
      };
    }
  };
}]);