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
 
angular.module('bluelatex.Paper.Services.Ace', ['ngStorage','ui.ace','bluelatex.Configuration'])
  .factory("AceService", ['$localStorage','$log','api_prefix','$q','$http',
    function ($localStorage,$log,api_prefix,$q,$http) {
      var content;
      var _session;
      var _editor;
      var _renderer;

      var _cursors = [];

      // ace default settings
      var aceSettings = {
        fontSize: '12px',
        readOnly: false,
        useWrapMode: true,
        showGutter: true,
        theme: 'ace/theme/textmate',
        mode: 'ace/mode/latex',
        modeName: 'latex',
        softWrap: 'free',
        keyBinding: 'emacs',
        fullLineSelection: true,
        highlightActiveLine: true,
        showInvisibles: false,
        showIndentGuides: true,
        showPrintMargin: false,
        useSoftTab: true,
        highlightSelectedWord: true,
        enableBehaviours: false,
        fadeFoldWidgets: false,
        incrementalSearch: true
      };
      // load ace settings from local storage
      if ($localStorage.aceSettings == null) {
        $localStorage.aceSettings = aceSettings;
      } else {
        aceSettings = $localStorage.aceSettings;
      }

      // change ace settings
      var loadSettings = function () {
        $localStorage.aceSettings = aceSettings;

        _editor.setTheme(aceSettings.theme);
        _editor.setFontSize(aceSettings.fontSize);
        _editor.setReadOnly(aceSettings.readOnly);
        _editor.setKeyboardHandler(aceSettings.keyBinding);
        _editor.setSelectionStyle(aceSettings.fullLineSelection ? 'line' : 'text');
        _editor.setHighlightActiveLine(aceSettings.highlightActiveLine);
        _editor.setShowInvisibles(aceSettings.showInvisibles);
        _editor.setDisplayIndentGuides(aceSettings.showIndentGuides);

        _editor.renderer.setShowPrintMargin(aceSettings.showPrintMargin);
        _editor.setHighlightSelectedWord(aceSettings.highlightSelectedWord);
        _editor.session.setUseSoftTabs(aceSettings.useSoftTab);
        _editor.setBehavioursEnabled(aceSettings.enableBehaviours);
        _editor.setFadeFoldWidgets(aceSettings.fadeFoldWidgets);

        _editor.session.setMode(aceSettings.mode);
        _editor.session.modeName = aceSettings.modeName;

        _renderer.setShowGutter(aceSettings.showGutter);

        switch (aceSettings.softWrap) {
        case "off":
          _session.setUseWrapMode(false);
          _renderer.setPrintMarginColumn(80);
          break;
        case "free":
          _session.setUseWrapMode(true);
          _session.setWrapLimitRange(null, null);
          _renderer.setPrintMarginColumn(80);
          break;
        default:
          _session.setUseWrapMode(true);
          var col = parseInt(aceSettings.softWrap, 10);
          _session.setWrapLimitRange(col, col);
          _renderer.setPrintMarginColumn(col);
        }
      };
      // go to a specific line and give focus to ace
      var goToLine = function (line) {
        _editor.gotoLine(line);
        _editor.focus();
      };

      var aceLoaded = function (_e, callback) {
        // Editor part
        _editor = _e;

        _session = _editor.getSession();
        _renderer = _editor.renderer;
        content = _session.getValue();

        // add undo support
        _session.setUndoManager(new ace.UndoManager());
        _editor.setOptions({
            spellcheck: true
        });

        // Event when the session change
        _editor.on("changeSession", function () {
          _session = _editor.getSession();
        });
        // When the content change
        _session.on("change", function () {
          content = _session.getValue();
        });
        loadSettings();
        callback(_editor);
      };

      return {
        goToLine: goToLine,
        getContent: function () {
          return content;
        },
        setContent: function (c) {
          return _session.setValue(c);
        },
        getEditor: function () {
          return _editor;
        },
        getSession: function () {
          return _editor.getSession();
        },
        loadSettings: loadSettings,
        aceLoaded: aceLoaded,
        aceSettings: aceSettings,
        getCommands: function () {
          return ace.commands;
        }
      };
    }
  ]);