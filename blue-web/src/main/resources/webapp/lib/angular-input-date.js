(function(window, angular) {
    'use strict';

    var inputDateFormat = 'yyyy-MM-dd';

    /**
     * Converts string representation of date to a Date object.
     *
     * @param dateString
     * @returns {Date|null}
     */
    function parseDateString(dateString) {
        if ('undefined' === typeof dateString || '' === dateString) {
            return null;
        }

        var parts = dateString.split('-');
        if (3 !== parts.length) {
            return null;
        }
        var year = parseInt(parts[0], 10);
        var month = parseInt(parts[1], 10);
        var day = parseInt(parts[2], 10);

        if (month < 1 || year < 1 || day < 1) {
            return null;
        }

        return new Date(year, (month - 1), day);
    }

    /**
     * Converts DateTime object to Date object.
     *
     * I.e. truncates time part.
     * @param dateTime
     * @constructor
     */
    function ExtractDate(dateTime) {
        return new Date(
            dateTime.getUTCFullYear(),
            dateTime.getUTCMonth(),
            dateTime.getUTCDate()
        );
    }

    angular.module('ngInputDate', ['ng'])
        .factory('inputDate', function() {
            return {
                ExtractDate: ExtractDate
            };
        })
        .directive('input', ['dateFilter', function(dateFilter) {
            return {
                restrict: 'E',
                require: '?ngModel',
                link: function(scope, element, attrs, ngModel) {
                    if (
                           'undefined' !== typeof attrs.type
                        && 'date' === attrs.type
                        && ngModel
                    ) {
                        ngModel.$formatters.push(function(modelValue) {
                            return dateFilter(modelValue, inputDateFormat);
                        });

                        ngModel.$parsers.push(function(viewValue) {
                            return parseDateString(viewValue);
                        });
                    }
                }
            }
        }])
    ;

})(window, angular);