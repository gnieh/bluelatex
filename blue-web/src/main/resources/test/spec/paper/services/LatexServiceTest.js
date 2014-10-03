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

describe('LaTeXService', function() {
  var module, factorty;
  // get the app
  beforeEach(function() {
    module = angular.mock.module('bluelatex');
  });

  it("LatexService should be registered", inject(function(LatexService) {
    expect(LatexService).toBeDefined();
  }));

  it("Parse TOC of a null text must return an empty array", inject(function(LatexService) {
    LatexService.parseTOC(null);
  }));

  it("Parse TOC of an empty text must return an empty array", inject(function(LatexService) {
    LatexService.parseTOC('');
  }));
  
  it("Parse TOC of an text which contains section should return the TOC", inject(function(LatexService) {
    var latexText = '\\part{}\
    \\chapter{Chapter}\
    \\section{Section}\
    \\subsection*{SubSection}\
    \\subsubsection{Subsubsection}\
    \\paragraph{paragraph}\
    \\subparagraph{Subparagraph}';
    var toc = LatexService.parseTOC(latexText);
    expect(toc).toEqual([{  
      type:'chapter',
      level:1,
      ignore:false,
      title:'Chapter',
      line:1
    },
    {  
      type:'section',
      level:2,
      ignore:false,
      title:'Section',
      line:1
    },
    {  
      type:'subsection',
      level:3,
      ignore:true,
      title:'SubSection',
      line:1
    },
    {  
      type:'subsubsection',
      level:4,
      ignore:false,
      title:'Subsubsection',
      line:1
    },
    {  
      type:'paragraph',
      level:5,
      ignore:false,
      title:'paragraph',
      line:1
    },
    {  
      type:'subparagraph',
      level:6,
      ignore:false,
      title:'Subparagraph',
      line:1
    }
    ]); 
  }));

  it("Parse command of a null text must return an empty array", inject(function(LatexService) {
    var commands = LatexService.parseCommands(null);
    expect(commands).toEqual([]);
  }));

  it("Parse command of an empty text must return an empty array", inject(function(LatexService) {
    var commands = LatexService.parseCommands('');
    expect(commands).toEqual([]);
  }));
  
  it("Parse command of an text which contains new commands should return an array of commands", inject(function(LatexService) {
    var latexText = 'My beautiful test\n\
    \\newcommand{\\reporttitle}{{\\BlueLaTeX} Web Client User Manual}\n\
    continue here';
    var commands = LatexService.parseCommands(latexText);
    expect(commands).toEqual([{value: '\\reporttitle', meta: 'userCmd'}]);
  }));
  
  it("Parse command of an text which doesn't contain command should return an empty array", inject(function(LatexService) {
    var latexText = '\\newccommand{\\reporttitle}{{\\BlueLaTeX} Web Client User Manual}';
    var commands = LatexService.parseCommands(latexText);
    expect(commands).toEqual([]);
  }));

  it("Parse label of a null text must return an empty array", inject(function(LatexService) {
    LatexService.parseLabels(null);
  }));
  
  it("Parse label of an empty text must return an empty array", inject(function(LatexService) {
    LatexService.parseLabels('');
  }));

  it("Parse label of an text which contains new commands should return an array of labels", inject(function(LatexService) {
    var latexText = '\\label{label content}';
    var labels = LatexService.parseLabels(latexText);
    expect(labels).toEqual([{value: 'label content', meta: 'label'}]);
  }));

  it("Parse command of an text which doesn't contain label should return an empty array", inject(function(LatexService) {
    latexText = '\\newcommand{\\reporttitle}{{\\BlueLaTeX} Web Client User Manual}';
    labels = LatexService.parseLabels(latexText);
    expect(labels).toEqual([]);
  }));
})