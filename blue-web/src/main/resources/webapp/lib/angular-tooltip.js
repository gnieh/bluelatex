(function(angular) {
  'use strict';

  var module = angular.module('ngTooltip', ['ng']),
      extend = angular.extend;

  module.provider('$tooltip', function() {
    // Default template for tooltips.
    var defaultTemplateUrl = 'template/ng-tooltip.html'
    this.setDefaultTemplateUrl = function(templateUrl) {
      defaultTemplateUrl = templateUrl;
    };

    var defaultTetherOptions = {
      attachment: 'top middle',
      targetAttachment: 'bottom middle'
    };
    this.setDefaultTetherOptions = function(options) {
      extend(defaultTetherOptions, options);
    };

    this.$get = function($rootScope, $animate, $compile, $templateCache) {
      return function(options) {
        options = options || {};
        options = extend({ templateUrl: defaultTemplateUrl }, options);
        options.tether = extend({}, defaultTetherOptions, options.tether || {});

        var template = options.template || $templateCache.get(options.templateUrl),
            scope    = options.scope || $rootScope.$new(),
            target   = options.target,
            elem     = $compile(template)(scope),
            tether;

        /**
         * Attach a tether to the tooltip and the target element.
         */
        function attachTether() {
          new Tether(extend({
            element: elem[0],
            target: target[0]
          }, options.tether));
        };

        /**
         * Detach the tether.
         */
        function detachTether() {
          if (tether) {
            tether.destroy();
          }
        };

        /**
         * Open the tooltip
         */
        function open() {
          $animate.enter(elem, null, target);
          attachTether();
        };

        /**
         * Close the tooltip
         */
        function close() {
          $animate.leave(elem);
          detachTether();
        };

        // Close the tooltip when the scope is destroyed.
        scope.$on('$destroy', close);

        return {
          open: open,
          close: close
        };
      };
    }
  });

  module.provider('$tooltipDirective', function() {

    /**
     * Returns a factory function for building a directive for tooltips.
     *
     * @param {String} name - The name of the directive.
     */
    this.$get = function($tooltip) {
      return function(name, options) {
        return {
          restrict: 'EA',
          scope: {
            content:  '@' + name,
            tether:  '=?' + name + 'Tether'
          },
          link: function(scope, elem, attrs) {
            var tooltip = $tooltip(extend({
              target: elem,
              scope: scope
            }, options, { tether: scope.tether }));

            /**
             * Toggle the tooltip.
             */
            elem[0].onmouseover = function() { scope.$apply(tooltip.open); }; 
            elem[0].onmouseout = function() { scope.$apply(tooltip.close); }; 
          }
        };
      };
    };
  });

  module.directive('ngTooltip', function($tooltipDirective) {
    return $tooltipDirective('ngTooltip');
  });

  module.run(function($templateCache) {
    $templateCache.put('template/ng-tooltip.html', '<div class="tooltip">{{content}}</div>');
  });

})(angular);