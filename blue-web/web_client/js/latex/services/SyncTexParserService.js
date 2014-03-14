angular.module("bluelatex.Latex.Services.SyncTexParser", [])
  .factory("SyncTexParserService", ['$log','$q','$http',
    function ($log,$q,$http) {
      // convert Latx unit
      var unit = 65781.76;


      var parseSyncTex = function (pdfsyncBody) {
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

        var inputPatern = /Input:([0-9]+):(.+)/;
        var offsetPatern = /(X|Y) Offset:([0-9]+)/;
        var openPagePatern = /\{([0-9]+)/;
        var closePagePatern = /\}([0-9]+)/;
        var verticalBlockPatern = /\[([0-9]+),([0-9]+):([0-9]+),([0-9]+):([0-9]+),([0-9]+),([0-9]+)/;
        var closeVerticalBlockPatern = /\]/;
        var horizontalBlockPatern = /\(([0-9]+),([0-9]+):([0-9]+),([0-9]+):([0-9]+),([0-9]+),([0-9]+)/;
        var closeHorizontalBlockPatern = /\)/;
        var elementBlockPatern = /(.)([0-9]+),([0-9]+):([0-9]+),([0-9]+)(:?([0-9]+))?/;

        for (var i = 1; i < lineArray.length; i++) {
          var line = lineArray[i];

          //input files
          match = line.match(inputPatern);
          if(match) {
            files[match[1]] = match[2];
            continue;
          }

          //offset
          match = line.match(offsetPatern);
          if(match) {
            pdfsyncObject['offset'+match[1]] = match[2];
            continue;
          }

          //new page
          match = line.match(openPagePatern);
          if(match) {
            currentPage = {
              page: match[1],
              blocks: [],
              type: 'page'
            };
            if(currentPage.page > numberPages) {
              numberPages = currentPage.page;
            }
            currentElement = currentPage;
            continue;
          }

          // close page
          match = line.match(closePagePatern);
          if(match) {
            pages[match[1]] = currentPage;
            currentPage = null;
            continue;
          }

          // new V block
          match = line.match(verticalBlockPatern);
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
          match = line.match(closeVerticalBlockPatern);
          if(match) {
            currentElement.parent.blocks.push(currentElement);
            currentElement = currentElement.parent;
            continue;
          }

          // new H block
          match = line.match(horizontalBlockPatern);
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
          match = line.match(closeHorizontalBlockPatern);
          if(match) {
            currentElement.parent.blocks.push(currentElement);
            currentElement = currentElement.parent;
            continue;
          }

          // new element
          match = line.match(elementBlockPatern);
          if(match) {
            var type = match[1];
            var fileNumber = parseInt(match[2]);
            var lineNumber = parseInt(match[3]);
            var left = match[4]/unit;
            var bottom = match[5]/unit;
            var width = (match[7])?match[7]/unit:null;

            var elem = {
              type: type,
              parent: currentElement,
              fileNumber: fileNumber,
              file: files[fileNumber],
              line: lineNumber,
              left: left,
              bottom: bottom,
              height: currentElement.height,
              width: width,
              page: currentPage.page
            };

            if(blockNumberLine[lineNumber] == null) {
              blockNumberLine[lineNumber]= [];
            }
            blockNumberLine[lineNumber].push(elem);
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
        parse: parseSyncTex
      };
    }
  ]);