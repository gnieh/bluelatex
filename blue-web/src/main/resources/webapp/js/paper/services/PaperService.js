angular.module('bluelatex.Paper.Services.Paper', ["ngResource",'jmdobry.angular-cache','bluelatex.Shared.Services.Configuration'])
  .factory("PaperService", ['$resource', '$http', '$upload', '$q', '$angularCacheFactory', '$log','apiRootUrl',
    function ($resource, $http, $upload, $q, $angularCacheFactory, $log,apiRootUrl) {
      // create a cache
      var _dataCache = $angularCacheFactory('paperCache', {
        maxAge: 300000, // items expire after an hour
        storageMode: 'localStorage',
        deleteOnExpire: 'aggressive',
        verifyIntegrity: true
      });
      // join a paper
      var join = $resource(apiRootUrl + "/papers/:paper_id/join", null, {
          "join": {
            method: "post",
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            transformResponse: [
              function (data, headersGetter) {
                return {
                  response: JSON.parse(data)
                };
              }
            ].concat($http.defaults.transformResponse)
          }
      });
      // leave a paper
      var leave = $resource(apiRootUrl + "/papers/:paper_id/part", null, {
          "leave": {
            method: "post",
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            transformResponse: [
              function (data, headersGetter) {
                console.log(data);
                return {
                  response: JSON.parse(data)
                };
              }
            ].concat($http.defaults.transformResponse)
          }
      });
      var compiler = $resource(apiRootUrl + "/papers/:paper_id/compiler", null, {
        // get compiler settings
        "get": {
          method: "get",
          transformResponse: [
            function (data, headersGetter) {
              data = JSON.parse(data);
              var headers = headersGetter();
              data.etag = headers.etag;
              return data;
            }
          ].concat($http.defaults.transformResponse)
        },
        // long polling compilation
        "subscribe": {
          method: "post",
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        },
        // change compiler settings
        "modify": {
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
        }
      });

      // get the list of supported compilers
      var compilers = $resource(apiRootUrl + "/compilers", null, {
        "get": {
          method: "get",
          isArray: true,
          transformResponse: [
            function (data) {
              var array = [];
              data = JSON.parse(data);
              for (var i = 0; i < data.length; i++) {
                array.push({
                  name: data[i]
                });
              }
              return array;
            }
          ].concat($http.defaults.transformResponse)
        }
      });

      // get the number of page of the paper
      var pages = $resource(apiRootUrl + "/papers/:paper_id/compiled/pages", null, {
        "get": {
          method: "get",
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        }
      });

      var paper = $resource(apiRootUrl + "/papers/:paper_id", null, {
        // create a new paper
        "new": {
          method: "POST",
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          },
          responseType: 'text',
          isArray: false,
          transformResponse: [
            function (data, headersGetter) {
              return {
                response: JSON.parse(data)
              };
            }
          ].concat($http.defaults.transformResponse)
        },
        // delete a paper
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
        // modify paper information
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
        // get paper information
        "get": {
          method: "GET",
          transformResponse: [
            function (data, headersGetter) {
              data = JSON.parse(data);
              var header = headersGetter();
              data.etag = header.etag;
              return data;
            }
          ].concat($http.defaults.transformResponse)
        }
      });
      var synchronizedFile = $resource(apiRootUrl + "/papers/:paper_id/files/synchronized", null, {
        // get the list of synchronized file
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
        }
      });
      var resources = $resource(apiRootUrl + "/papers/:paper_id/files/resources/:resource", null, {
        // get the resource list
        "get": {
          method: "GET",
          isArray: true,
          transformResponse: [
            function (data, headersGetter) {
              var array = [];
              data = JSON.parse(data);
              for (var i = 0; i < data.length; i++) {
                var resource = decodeURIComponent(data[i]);
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
        // delete a resource
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
      // get all paper that the user has access
      var userPapers = $resource(apiRootUrl + "/users/:username/papers", {
          username: "@username"
        }, {
          "get": {
            method: "GET",
            isArray: true,
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          }
        });
      // upload a new resource
      var upload = function (paper_id, file, resource) {
        var deferred = $q.defer();
        var promise = deferred.promise;
        $upload.upload({
          url: apiRootUrl + '/papers/' + paper_id + '/files/resources/' + resource,
          method: 'POST',
          // headers: {'headerKey': 'headerValue'}, withCredential: true,
          file: file,
          fileFormDataName: resource,
        }).progress(function (evt) {
          deferred.notify(parseInt(100.0 * evt.loaded / evt.total));
        }).success(function (data, status, headers, config) {
          // file is uploaded successfully
          deferred.resolve({
            data: JSON.parse(data),
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
            _dataCache.remove('/userPapers');
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
            _dataCache.remove('/userPapers');
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
              delete data.$promise;
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
        modify: function (paper, oldpaper) {
          var etag = paper.etag;
          var path_json = jsonpatch.compare(oldpaper, paper);
          var deferred = $q.defer();
          var promise = deferred.promise;
          _dataCache.remove('/papers/' + paper.id);
          info.edit({
            paper_id: paper.id
          }, {
            "etag": etag,
            path_json: path_json
          }).$promise.then(function (data) {            
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
          _dataCache.remove('/resources/' + paper_id);
          return upload(paper_id, data, resource);
        },
        removeResource: function (paper_id, resource) {
          _dataCache.remove('/resources/' + paper_id);
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
        newSynchronizedFile: function (user,paper_id,filename) {
          var deferred = $q.defer();
          $http({method:'post',url: apiRootUrl + "/papers/"+paper_id+"/q", data: 'u:'+user.name+'\nF:0:'+filename+'\nr:1:'}).then(function (data) {
            deferred.resolve(data.data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return deferred.promise;
        },
        deleteSynchronizedFile: function (user,paper_id,filename) {
          var deferred = $q.defer();
          $http({method:'post',url: apiRootUrl + "/papers/"+paper_id+"/q", data: 'u:'+user.name+'\nn:'+filename+'\n'}).then(function (data) {
            deferred.resolve(data.data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return deferred.promise;
        },
        getZipUrl: function (paper_id) {
          return apiRootUrl + "/papers/" + paper_id + "/zip";
        },
        getPDFUrl: function (paper_id) {
          return apiRootUrl + "/papers/" + paper_id + "/compiled/pdf";
        },
        getLogUrl: function (paper_id) {
          return apiRootUrl + "/papers/" + paper_id + "/compiled/log";
        },
        getPNGUrl: function (paper_id,page) {
          return apiRootUrl + "/papers/" + paper_id;
          return apiRootUrl + "/papers/" + paper_id + "/compiled/png?page="+page;
        },
        getPaperUrlRoot: function (paper_id) {
          return apiRootUrl + "/papers/" + paper_id;
        },
        downloadRessource: function (file_name) {
          var deferred = $q.defer();
          $http({method: 'GET', url: file_name}).then(function(returnData){
            deferred.resolve(returnData.data);
          }, function (error) {
            $log.error(error);
            deferred.reject(error);
          });
          return deferred.promise;
        },
        getCompilers: function () {
          var deferred = $q.defer();
          var promise = deferred.promise;
          if (_dataCache.get('/compilers'))
            deferred.resolve(_dataCache.get('/compilers'));
          else {
            compilers.get().$promise.then(function (data) {
              _dataCache.put('/compilers', data);
              deferred.resolve(data);
            }, function (error) {
              deferred.reject(error);
            }, function (progress) {
              deferred.notify(progress);
            });
          }
          return promise;
        },
        getPaperCompiler: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          compiler.get({paper_id: paper_id}).$promise.then(function (data) {
            delete data.$promise;
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        subscribePaperCompiler: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          compiler.subscribe({paper_id: paper_id},{}).$promise.then(function (data) {
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        joinPaper: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          join.join({paper_id: paper_id},{}).$promise.then(function (data) {
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        leavePaper: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          leave.leave({paper_id: paper_id},{}).$promise.then(function (data) {
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        editPaperCompiler: function (paper_id, newvalue, oldvalue) {
          var deferred = $q.defer();

          var path_json = jsonpatch.compare(oldvalue, newvalue);

          compiler.modify({
            paper_id: paper_id
          }, {
            "etag": oldvalue.etag,
            path_json: path_json
          }).$promise.then(function (data) {
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return deferred.promise;
        },
        getSynctex: function (paper_id) {
          var deferred = $q.defer();
          $http({method:'get',url: apiRootUrl + "/papers/"+paper_id+"/synctex"}).then(function (data) {
            deferred.resolve(data.data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return deferred.promise;
        },
        getLog: function (paper_id) {
          var deferred = $q.defer();
          $http({method:'get',url: apiRootUrl + "/papers/"+paper_id+"/compiled/log"}).then(function (data) {
            deferred.resolve(data.data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return deferred.promise;
        },
        getPages: function (paper_id) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          pages.get({paper_id: paper_id}).$promise.then(function (data) {
            deferred.resolve(data);
          }, function (error) {
            deferred.reject(error);
          }, function (progress) {
            deferred.notify(progress);
          });
          return promise;
        },
        getUserPapers: function (user) {
          var deferred = $q.defer();
          var promise = deferred.promise;
          if (_dataCache.get('/userPapers')) deferred.resolve(_dataCache.get('/userPapers'));
          else {
            userPapers.get({
              username: user.name
            }).$promise.then(function (data) {
              _dataCache.put('/userPapers', data);
              deferred.resolve(data);
            }, function (error) {
              deferred.reject(error);
            }, function (progress) {
              deferred.notify(progress);
            });
          }
          return promise;
        },
        clearCache: function() {
          $angularCacheFactory.clearAll();
        }
      };
    }
  ]);