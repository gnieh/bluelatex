angular.module('bluelatex.Paper.Services.Paper', ["ngResource",'jmdobry.angular-cache','bluelatex.Shared.Services.Configuration'])
  .factory("PaperService", ['$resource', '$http', '$upload', '$q', '$angularCacheFactory', '$log','apiRootUrl',
    function ($resource, $http, $upload, $q, $angularCacheFactory, $log,apiRootUrl) {
      var _dataCache = $angularCacheFactory('paperCache', {
        maxAge: 300000, // items expire after an hour
        storageMode: 'localStorage',
        deleteOnExpire: 'aggressive',
        verifyIntegrity: true
      });

      var paper = $resource(apiRootUrl + "/papers/:paper_id", null, {
        "new": {
          method: "POST",
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          format: 'json',
          isArray: false,
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        },
        "delete": {
          method: "DELETE",
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        }
      });
      var info = $resource(apiRootUrl + "/papers/:paper_id/info", null, {
        "edit": {
          method: "PATCH",
          headers: {
            'Content-Type': 'application/json-patch'
          },
          transformRequest: [
            function (data, headersGetter) {
              var header = headersGetter();
              header['If-Match'] = data.etag;
              return data.path_json;
            }
          ].concat($http.defaults.transformRequest)
        },
        "get": {
          method: "GET",
          transformResponse: [
            function (data, headersGetter) {
              data = JSON.parse(data);
              data.header = headersGetter();
              return data;
            }
          ].concat($http.defaults.transformResponse)
        }
      });
      var synchronizedFile = $resource(apiRootUrl + "/papers/:paper_id/files/synchronized", null, {
        "get": {
          method: "GET",
          format: 'json',
          isArray: true
        }
      });
      var resources = $resource(apiRootUrl + "/papers/:paper_id/files/resources/:resource", null, {
        "get": {
          method: "GET",
          isArray: true,
          transformResponse: [
            function (data, headersGetter) {
              var array = [];
              data = JSON.parse(data);
              for (var i = 0; i < data.length; i++) {
                var resource = data[i];
                array.push({
                  title: resource,
                  name: resource.replace(/\.[^\.]+$/, ''),
                  type: getFileType(resource),
                  extension: getFileNameExtension(resource)
                });
              }
              return array;
            }
          ].concat($http.defaults.transformResponse)
        },
        "delete": {
          method: "DELETE",
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        }
      });
      var upload = function (paper_id, file, resource) {
        var deferred = $q.defer();
        var promise = deferred.promise;
        $upload.upload({
          url: apiRootUrl + '/papers/' + paper_id + '/files/resources/' + resource,
          method: 'POST',
          // headers: {'headerKey': 'headerValue'}, withCredential: true,
          file: file,
          // file: $files, //upload multiple files, this feature only works in HTML5 FromData browsers
          /* set file formData name for 'Content-Desposition' header. Default: 'file' */
          //fileFormDataName: myFile, //OR for HTML5 multiple upload only a list: ['name1', 'name2', ...]
          /* customize how data is added to formData. See #40#issuecomment-28612000 for example */
          //formDataAppender: function(formData, key, val){}
        }).progress(function (evt) {
          $log.log('percent: ' + parseInt(100.0 * evt.loaded / evt.total));
          deferred.notify(parseInt(100.0 * evt.loaded / evt.total));
        }).success(function (data, status, headers, config) {
          // file is uploaded successfully
          deferred.resolve({
            data: data,
            status: status,
            headers: headers,
            config: config
          });
        }).error(function (data, status) {
          deferred.reject({
            data: data,
            status: status
          });
        });
        return promise;
      };
      return {
        create: function (p) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          paper.new({}, jsonToPostParameters(p)).$promise.then(function (data) {
            p.id = data.response;
            _dataCache.put('/papers/' + p.id, p);
            deferred.resolve(p);
          }, function (error) {
            $log.error(error);
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        delete: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          paper.delete({
            paper_id: paper_id
          }).$promise.then(function (data) {
            _dataCache.remove('/papers/' + paper_id);
            deferred.resolve(data);
          }, function (error) {
            $log.error(error);
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        getInfo: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          if (_dataCache.get('/papers/' + paper_id))
            deferred.resolve(_dataCache.get('/papers/' + paper_id));
          else {
            info.get({
              paper_id: paper_id
            }).$promise.then(function (data) {
              _dataCache.put('/papers/' + paper_id, data);
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
        modify: function (paper, initial_paper) {
          var path_json = [];
          if (paper.title != initial_paper.title) {
            path_json.push({
              'op': 'replace',
              "path": "/title",
              "value": paper.title
            });
          }
          var reaplce_authors = false;
          for (var i = 0; i < paper.authors.length; i++) {
            var author = paper.authors[i];
            if (initial_paper.authors.indexOf(author) < 0) {
              reaplce_authors = true;
              break;
            }
          }
          for (var i = 0; i < initial_paper.authors.length; i++) {
            var author = initial_paper.authors[i];
            if (paper.authors.indexOf(author) < 0) {
              reaplce_authors = true;
              break;
            }
          }
          if (reaplce_authors) {
            path_json.push({
              'op': 'replace',
              "path": "/authors",
              "value": paper.authors
            });
          }
          var reaplce_reviewers = false;
          for (var i = 0; i < paper.reviewers.length; i++) {
            var reviewer = paper.reviewers[i];
            if (initial_paper.reviewers.indexOf(reviewer) < 0) {
              reaplce_reviewers = true;
              break;
            }
          }
          for (var i = 0; i < initial_paper.reviewers.length; i++) {
            var reviewer = initial_paper.reviewers[i];
            if (paper.reviewers.indexOf(reviewer) < 0) {
              reaplce_reviewers = true;
              break;
            }
          }
          if (reaplce_reviewers) {
            path_json.push({
              'op': 'replace',
              "path": "/reviewers",
              "value": paper.reviewers
            });
          }
          var reaplce_tags = false;
          for (var i = 0; i < paper.tags.length; i++) {
            var tag = paper.tags[i];
            if (initial_paper.tags.indexOf(tag) < 0) {
              reaplce_tags = true;
              break;
            }
          }
          for (var i = 0; i < initial_paper.tags.length; i++) {
            var tag = initial_paper.tags[i];
            if (paper.tags.indexOf(tag) < 0) {
              reaplce_tags = true;
              break;
            }
          }
          if (reaplce_tags) {
            path_json.push({
              'op': 'replace',
              "path": "/tags",
              "value": paper.tags
            });
          }
          if (paper.branch != initial_paper.branch) {
            path_json.push({
              'op': 'replace',
              "path": "/branch",
              "value": paper.branch
            });
          }
          if (paper.cls != initial_paper.cls) {
            path_json.push({
              'op': 'replace',
              "path": "/cls",
              "value": paper.cls
            });
          }
          var deferred = $q.defer();
          var promise = deferred.promise;
          info.edit({
            paper_id: paper.id
          }, {
            "etag": paper.etag,
            path_json: path_json
          }).$promise.then(function (data) {
            _dataCache.remove('/papers/' + paper.id);
            deferred.resolve(data);
          }, function (error) {
            $log.error(error);
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        getResources: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          if (_dataCache.get('/resources/' + paper_id))
            deferred.resolve(_dataCache.get('/resources/' + paper_id));
          else {
            resources.get({
              paper_id: paper_id
            }).$promise.then(function (data) {
              _dataCache.put('/resources/' + paper_id, data);
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
        getResourceUrl: function (paper_id, resource) {
          return apiRootUrl + '/papers/' + paper_id + '/files/resources/' + resource;
        },
        uploadResource: function (paper_id, resource, data) {
          return upload(paper_id, data, resource);
        },
        removeResource: function (paper_id, resource) {
          return resources.delete({
            paper_id: paper_id,
            resource: resource
          }).$promise;
        },
        getSynchronized: function (paper_id) {
          return synchronizedFile.get({
            paper_id: paper_id
          }).$promise;
        },
        getZipUrl: function (paper_id) {
          return apiRootUrl + "/papers/" + paper_id + ".zip";
        }
      };
    }
  ]);