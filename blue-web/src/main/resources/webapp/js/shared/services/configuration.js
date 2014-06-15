angular.module('bluelatex.Configuration',[])
  // the rest api root
  .constant('api_prefix','')
  .constant('recaptcha_public_key', null)
  .factory("ConfigurationService", ['$http','$q','api_prefix','recaptcha_public_key', function ($http, $q,api_prefix,recaptcha_public_key) {
  	return {
  		getConfiguration: function  () {
  		  var deferred = $q.defer();
        $http({method:'get',url:  "configuration"}).then(function (data) {
          var configuration = data.data;
          api_prefix = configuration['api_prefix'];
          recaptcha_public_key = configuration['recaptcha_public_key'];
          deferred.resolve(configuration);
        }, function (error) {
          deferred.reject(error);
        }, function (progress) {
          deferred.notify(progress);
        });
        return deferred.promise;
  		}
  	}
  }]);
