angular.module("bluelatex.Latex.Services.SyncTexParser", [])
  .factory("SyncTexParserService", ['$log','$q','$http',
    function ($log,$q,$http) {
      var unit = 65781.76;
      var parseSyncTexUrl = function (pdfsync) {
        var deferred = $q.defer();
        $http({method: 'GET', url: pdfsync}).then(function(returnData){
          deferred.resolve(parsePDFSync(returnData.data));
        }, function (error) {
          $log.error(error);
          deferred.reject(error);
        });
        return deferred.promise;
      };

      var parsePDFSync = function (pdfsyncBody) {
        var numberPages = 0;
        var currentPage = {};
        var currentElement = {};

        var latexLines = {};
        var blockNumberLine = {};
        var hBlocks = [];

        var files = {};
        var pages = {};
        var pdfsyncObject = {};

        var lineArray = pdfsyncBody.split("\n");

        pdfsyncObject.version = lineArray[0].replace('SyncTeX Version:','');

        for (var i = 1; i < lineArray.length; i++) {
          var line = lineArray[i];
          //input files
          match = line.match(/Input:([0-9]+):(.+)/);
          if(match) {
            files[match[1]] = match[2];
            continue;
          }
          //offset
          match = line.match(/X Offset:([0-9]+)/);
          if(match) {
            pdfsyncObject.offsetX = match[1];
            continue;
          }
          match = line.match(/Y Offset:([0-9]+)/);
          if(match) {
            pdfsyncObject.offsetY = match[1];
            continue;
          }
          //new page
          match = line.match(/\{([0-9]+)/);
          if(match) {
            currentPage = {
              page: match[1],
              blocks: [],
              type: 'page',
            };
            if(currentPage.page > numberPages) {
              numberPages = currentPage.page;
            }
            currentElement = currentPage;
            continue;
          }
          // close page
          match = line.match(/\}([0-9]+)/);
          if(match) {
            pages[match[1]] = currentPage;
            currentPage = null;
            continue;
          }

          // new V block
          match = line.match(/\[([0-9]+),([0-9]+):([0-9]+),([0-9]+):([0-9]+),([0-9]+),([0-9]+)/);
          if(match) {
            var s1 = [match[3]/unit,match[4]/unit];
            var s2 = [match[5]/unit,match[6]/unit];
            var block = {
              type: 'vertical',
              parent: currentElement,
              fileNumber: parseInt(match[1]),
              file: files[match[1]],
              line: parseInt(match[2]),
              left: s1[0],
              bottom: s1[1],
              width: s2[0],
              height: s2[1],
              depth: parseInt(match[7]),
              blocks: [],
              elements: [],
              page: currentPage.page
            };
            currentElement = block;
            continue;
          }
          // close V block
          match = line.match(/\]/);
          if(match) {
            currentElement.parent.blocks.push(currentElement);
            currentElement = currentElement.parent;
            continue;
          }
          // new H block
          match = line.match(/\(([0-9]+),([0-9]+):([0-9]+),([0-9]+):([0-9]+),([0-9]+),([0-9]+)/);
          if(match) {
            var s1 = [match[3]/unit,match[4]/unit];
            var s2 = [match[5]/unit,match[6]/unit];
            var block = {
              type: 'horizontal',
              parent: currentElement,
              fileNumber: parseInt(match[1]),
              file: files[match[1]],
              line: parseInt(match[2]),
              left: s1[0],
              bottom: s1[1],
              width: s2[0],
              height: s2[1],
              blocks: [],
              elements: [],
              page: currentPage.page
            };
            hBlocks.push(block);
            currentElement = block;
            continue;
          }
          // close H block
          match = line.match(/\)/);
          if(match) {
            currentElement.parent.blocks.push(currentElement);
            currentElement = currentElement.parent;
            continue;
          }

          // new element
          match = line.match(/(.)([0-9]+),([0-9]+):([0-9]+),([0-9]+)(:?([0-9]+))?/);
          if(match) {
            if(match[1]!='x') continue;
            var s1 = [match[4]/unit,match[5]/unit];
            if(match[7])
              var s2 = [match[7]/unit,0];
            var elem = {
              type: match[1],
              parent: currentElement,
              fileNumber: parseInt(match[2]),
              file: files[match[2]],
              line: parseInt(match[3]),
              left: s1[0],
              bottom: s1[1],
              height: currentElement.height,
              to: ((s2)?s2[1]:null),
              page: currentPage.page
            };
            if(blockNumberLine[elem.line] == null) {
              blockNumberLine[elem.line]= [];
            }
            blockNumberLine[elem.line].push(elem);
            currentElement.elements.push(elem);
            continue;
          }
        }
        pdfsyncObject.files = files;
        pdfsyncObject.pages = pages;
        pdfsyncObject.blockNumberLine = blockNumberLine;
        pdfsyncObject.hBlocks = hBlocks;
        pdfsyncObject.numberPages = numberPages;
        return pdfsyncObject;
      };
      return {
        parse: parsePDFSync,
        parseUrl: parseSyncTexUrl
      };
    }
  ]);