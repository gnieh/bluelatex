angular.module('bluelatex.Paper.Services.Latex', [])
  .factory("LatexService", ['$log','$q','$http',
    function ($log,$q,$http) {
      
      /**
      * Create the table of content
      */
      var parseTOC = function (content) {
        var toc = [];
        // search different section
        var keys = ['part','chapter', 'section', 'subsection', 'subsubsection', 'paragraph','subparagraph'];
        var regex = '\\\\(' + keys.join('|') + ')(\\*)?{([^}]+)}';
        var reg = new RegExp(regex, "gi");
        var astring = content.split('\n');

        for (var i = 0; i < astring.length; i++) {
          var number = i + 1;
          var line = astring[i];
          var result;
          while ((result = reg.exec(line)) !== null) {
            var type = (result[1]);
            toc.push({
              type: type,
              level: keys.indexOf(type),
              restart: result[2] == '*',
              title: result[3],
              line: number
            });
          }
        }
        return toc;
      };
      /**
      * Search new latex commands
      */
      var parseCommands = function(content) {
        //\newcommand*{\private}{../w4f5bc79f34884cbb}
        var commands = [];
        var regex = '\\\\newcommand\*{([^}]+)}';
        var reg = new RegExp(regex, "gi");
        var result;
        while ((result = reg.exec(content)) !== null) {
          var type = (result[1]);

          commands.push({
            "value":result[1],
            "meta":"userCmd"
          });
        }
        return commands;
      };

      return {
        parseTOC: parseTOC,
        parseCommands: parseCommands
      };
    }
  ]);