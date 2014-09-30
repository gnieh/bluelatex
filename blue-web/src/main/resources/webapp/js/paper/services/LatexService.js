/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
angular.module('bluelatex.Paper.Services.Latex', [])
  .factory("LatexService", ['$log','$q','$http',
    function ($log,$q,$http) {
      
      /**
      * Create the table of content
      */
      var parseTOC = function (content) {
        var toc = [];
        if(content == null) {
          return content;
        }
        // search different section
        var keys = ['part','chapter', 'section', 'subsection', 'subsubsection', 'paragraph','subparagraph'];
        var regex = '\\\\(' + keys.join('|') + ')(\\*)?{([^}]+)}';
        var reg = new RegExp(regex, "gi");
        var astring = content.split('\n');

        for (var i = 0; i < astring.length; i++) {
          var number = i + 1;
          var line = astring[i];
          // ignore commented line
          if(line.charAt(0) == '%'){
            continue;
          }
          var result;
          while ((result = reg.exec(line)) !== null) {
            var type = (result[1]);
            toc.push({
              type: type,
              level: keys.indexOf(type),
              ignore: result[2] == '*',
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

      /**
      * Search label
      */
      var parseLabels = function(content) {
        //\newcommand*{\private}{../w4f5bc79f34884cbb}
        var commands = [];
        var regex = '\\\\label\*{([^}]+)}';
        var reg = new RegExp(regex, "gi");
        var result;
        while ((result = reg.exec(content)) !== null) {
          var type = (result[1]);

          commands.push({
            "value":result[1],
            "meta":"label"
          });
        }
        return commands;
      };

      return {
        parseTOC: parseTOC,
        parseCommands: parseCommands,
        parseLabels: parseLabels
      };
    }
  ]);