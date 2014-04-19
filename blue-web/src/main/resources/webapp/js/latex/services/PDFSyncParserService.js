angular.module("bluelatex.Latex.Services.PDFSyncParser", ['bluelatex.Shared.Services.Configuration'])
  .factory("PDFSyncParserService", ['$log','$q','$http',
    function ($log,$q,$http) {
      var parsePDFSyncUrl = function (pdfsync) {
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
        var currentPage = 0;
        var currentFile = null;
        var latexLines = {};
        var pdfBlocks = {};
        var blockNumberLine = {};
        var files = [];

        var pdfsyncObject = {};
        var lineArray = pdfsyncBody.split("\n");
        pdfsyncObject.title = lineArray[0];
        pdfsyncObject.version = lineArray[1].replace('version ','');

        for (var i = 2; i < lineArray.length; i++) {
          var line = lineArray[i];
          var parameters = line.split(' ');
          switch(line[0]){
            case '(':
              currentFile=line.replace('(','');
              files.push(currentFile);
              break;
            case ')':
              currentFile=null;
              break;
            case 'l':
              if(latexLines[parameters[2]] == null)
                latexLines[parameters[2]] = [];
              blockNumberLine[parameters[1]] = parameters[2];
              latexLines[parameters[2]].push({
                blockNumber: parameters[1],
                file: currentFile,
                page: currentPage,
                columnNumber: parameters[3]
              });
              break;
            case 's':
              currentPage=parameters[1];
            break;
            case 'p':
              pdfBlocks[parameters[1]] = {
                xPosition: parameters[2],
                yPosition: parameters[3],
                page: currentPage
              };
            break;
          }
        }
        for (var i in pdfBlocks) {
          if(!pdfBlocks.hasOwnProperty(i)) continue;
          var block = pdfBlocks[i];
          var l = blockNumberLine[i];
          for (var j = latexLines[l].length - 1; j >= 0; j--) {
            if(latexLines[l][j].blockNumber == i){
              latexLines[l][j].block = block;
              latexLines[l][j].page = block.page;
              break;
            }
          }
        }

        pdfsyncObject.latexLines = latexLines;
        pdfsyncObject.numberPage = currentPage;
        pdfsyncObject.files = files;
        pdfsyncObject.pdfBlocks = pdfBlocks;
        pdfsyncObject.blockNumberLine = blockNumberLine;
        return pdfsyncObject;
      };
      return {
        parse: parsePDFSync,
        parseUrl: parsePDFSyncUrl
      };
    }
  ]);