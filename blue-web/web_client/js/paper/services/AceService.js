angular.module('bluelatex.Paper.Services.Ace', ['ngStorage','ui.ace'])
  .factory("AceService", ['$localStorage','$log',
    function ($localStorage,$log) {
      var content;
      var _session;
      var _editor;
      var _renderer;

      var aceSettings = {
        fontSize: '12px',
        readOnly: false,
        useWrapMode: true,
        showGutter: true,
        theme: 'ace/theme/textmate',
        mode: 'ace/mode/latex',
        modeName: 'latex',
        softWrap: 'free',
        keyBinding: 'ace',
        fullLineSelection: true,
        highlightActiveLine: true,
        showInvisibles: false,
        showIndentGuides: true,
        showPrintMargin: true,
        useSoftTab: true,
        highlightSelectedWord: true,
        enableBehaviours: false,
        fadeFoldWidgets: false,
        incrementalSearch: false
      };
      if ($localStorage.aceSettings == null) {
        $localStorage.aceSettings = aceSettings;
      } else {
        aceSettings = $localStorage.aceSettings;
      }

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
        $log.log(aceSettings.mode);
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

      var goToLine = function (line) {
        _editor.gotoLine(line);
      };

      var aceLoaded = function (_e, callback) {
        // Editor part
        _editor = _e;
        _session = _editor.getSession();
        _renderer = _editor.renderer;
        content = _session.getValue();

        // Options
        _session.setUndoManager(new ace.UndoManager());

        // Events
        _editor.on("changeSession", function () {
          _session = _editor.getSession();
        });

        _session.on("change", function () {
          content = _session.getValue();
        });
        loadSettings();
        callback(_editor);
      };

      var getToc = function () {
        var toc = [];
        var keys = ['chapter', 'section', 'subsection', 'subsubsection', 'paragraph'];
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
        getToc: getToc,
        aceSettings: aceSettings
      };
    }
  ]);