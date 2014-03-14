angular.module('bluelatex.Paper.Services.Ace', ['ngStorage','ui.ace','bluelatex.Shared.Services.Configuration'])
  .factory("AceService", ['$localStorage','$log','apiRootUrl',
    function ($localStorage,$log,apiRootUrl) {
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
        _editor.focus();
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

      var initMobWrite = function () {
        /**
        * Constructor of shared object representing a text field.
        * @param {Node} node A textarea, text or password input.
        * @constructor
        */
        mobwrite.shareAceObj = function(paper) {
          // Call our prototype's constructor.
          mobwrite.debug=true;
          mobwrite.shareObj.apply(this, [paper.file]);
          mobwrite.syncGateway = apiRootUrl + "/papers/" + paper.paper_id + "/q";
          this.file = paper.file;
          this.element = paper.file;
        };

        // The textarea shared object's parent is a shareObj.
        mobwrite.shareAceObj.prototype = new mobwrite.shareObj('');


        /**
         * Retrieve the user's text.
         * @return {string} Plaintext content.
         */
        mobwrite.shareAceObj.prototype.getClientText = function() {
          return mobwrite.shareAceObj.normalizeLinebreaks_(content);
        };


        /**
         * Set the user's text.
         * @param {string} text New text
         */
        mobwrite.shareAceObj.prototype.setClientText = function(text) {
          _session.setValue(text);
        };


        /**
         * Modify the user's plaintext by applying a series of patches against it.
         * @param {Array.<patch_obj>} patches Array of Patch objects.
         */
        mobwrite.shareAceObj.prototype.patchClientText = function(patches) {
          // Set some constants which tweak the matching behaviour.
          // Maximum distance to search from expected location.
          this.dmp.Match_Distance = 1000;
          // At what point is no match declared (0.0 = perfection, 1.0 = very loose)
          this.dmp.Match_Threshold = 0.6;

          var oldClientText = this.getClientText();
          var cursor = this.captureCursor_();
          // Pack the cursor offsets into an array to be adjusted.
          // See http://neil.fraser.name/writing/cursor/
          var offsets = [];
          if (cursor) {
            offsets[0] = cursor.startOffset;
            if ('endOffset' in cursor) {
              offsets[1] = cursor.endOffset;
            }
          }
          var newClientText = this.patch_apply_(patches, oldClientText, offsets);
          // Set the new text only if there is a change to be made.
          if (oldClientText != newClientText) {
            this.setClientText(newClientText);
            if (cursor) {
              // Unpack the offset array.
              cursor.startOffset = offsets[0];
              if (offsets.length > 1) {
                cursor.endOffset = offsets[1];
                if (cursor.startOffset >= cursor.endOffset) {
                  cursor.collapsed = true;
                }
              }
              this.restoreCursor_(cursor);
            }
          }
        };


        /**
         * Merge a set of patches onto the text.  Return a patched text.
         * @param {Array.<patch_obj>} patches Array of patch objects.
         * @param {string} text Old text.
         * @param {Array.<number>} offsets Offset indices to adjust.
         * @return {string} New text.
         */
        mobwrite.shareAceObj.prototype.patch_apply_ =
            function(patches, text, offsets) {
          if (patches.length == 0) {
            return text;
          }

          // Deep copy the patches so that no changes are made to originals.
          patches = this.dmp.patch_deepCopy(patches);
          var nullPadding = this.dmp.patch_addPadding(patches);
          text = nullPadding + text + nullPadding;

          this.dmp.patch_splitMax(patches);
          // delta keeps track of the offset between the expected and actual location
          // of the previous patch.  If there are patches expected at positions 10 and
          // 20, but the first patch was found at 12, delta is 2 and the second patch
          // has an effective expected position of 22.
          var delta = 0;
          for (var x = 0; x < patches.length; x++) {
            var expected_loc = patches[x].start2 + delta;
            var text1 = this.dmp.diff_text1(patches[x].diffs);
            var start_loc;
            var end_loc = -1;
            if (text1.length > this.dmp.Match_MaxBits) {
              // patch_splitMax will only provide an oversized pattern in the case of
              // a monster delete.
              start_loc = this.dmp.match_main(text,
                  text1.substring(0, this.dmp.Match_MaxBits), expected_loc);
              if (start_loc != -1) {
                end_loc = this.dmp.match_main(text,
                    text1.substring(text1.length - this.dmp.Match_MaxBits),
                    expected_loc + text1.length - this.dmp.Match_MaxBits);
                if (end_loc == -1 || start_loc >= end_loc) {
                  // Can't find valid trailing context.  Drop this patch.
                  start_loc = -1;
                }
              }
            } else {
              start_loc = this.dmp.match_main(text, text1, expected_loc);
            }
            if (start_loc == -1) {
              // No match found.  :(
              if (mobwrite.debug) {
                window.console.warn('Patch failed: ' + patches[x]);
              }
              // Subtract the delta for this failed patch from subsequent patches.
              delta -= patches[x].length2 - patches[x].length1;
            } else {
              // Found a match.  :)
              if (mobwrite.debug) {
                window.console.info('Patch OK.');
              }
              delta = start_loc - expected_loc;
              var text2;
              if (end_loc == -1) {
                text2 = text.substring(start_loc, start_loc + text1.length);
              } else {
                text2 = text.substring(start_loc, end_loc + this.dmp.Match_MaxBits);
              }
              // Run a diff to get a framework of equivalent indices.
              var diffs = this.dmp.diff_main(text1, text2, false);
              if (text1.length > this.dmp.Match_MaxBits &&
                  this.dmp.diff_levenshtein(diffs) / text1.length >
                  this.dmp.Patch_DeleteThreshold) {
                // The end points match, but the content is unacceptably bad.
                if (mobwrite.debug) {
                  window.console.warn('Patch contents mismatch: ' + patches[x]);
                }
              } else {
                var index1 = 0;
                var index2;
                for (var y = 0; y < patches[x].diffs.length; y++) {
                  var mod = patches[x].diffs[y];
                  if (mod[0] !== DIFF_EQUAL) {
                    index2 = this.dmp.diff_xIndex(diffs, index1);
                  }
                  if (mod[0] === DIFF_INSERT) {  // Insertion
                    text = text.substring(0, start_loc + index2) + mod[1] +
                           text.substring(start_loc + index2);
                    for (var i = 0; i < offsets.length; i++) {
                      if (offsets[i] + nullPadding.length > start_loc + index2) {
                        offsets[i] += mod[1].length;
                      }
                    }
                  } else if (mod[0] === DIFF_DELETE) {  // Deletion
                    var del_start = start_loc + index2;
                    var del_end = start_loc + this.dmp.diff_xIndex(diffs,
                        index1 + mod[1].length);
                    text = text.substring(0, del_start) + text.substring(del_end);
                    for (var i = 0; i < offsets.length; i++) {
                      if (offsets[i] + nullPadding.length > del_start) {
                        if (offsets[i] + nullPadding.length < del_end) {
                          offsets[i] = del_start - nullPadding.length;
                        } else {
                          offsets[i] -= del_end - del_start;
                        }
                      }
                    }
                  }
                  if (mod[0] !== DIFF_DELETE) {
                    index1 += mod[1].length;
                  }
                }
              }
            }
          }
          // Strip the padding off.
          text = text.substring(nullPadding.length, text.length - nullPadding.length);
          return text;
        };


        /**
         * Record information regarding the current cursor.
         * @return {Object?} Context information of the cursor.
         * @private
         */
        mobwrite.shareAceObj.prototype.captureCursor_ = function() {
          // TODO use ACE api
          var padLength = this.dmp.Match_MaxBits / 2;  // Normally 16.
          var text = content;
          var cursor = {};
          /*try {
            var selectionStart = this.element.selectionStart;
            var selectionEnd = this.element.selectionEnd;
          } catch (e) {
            // No cursor; the element may be "display:none".
            return null;
          }*/
          cursor.range = _editor.selection.getRange();
          cursor.cursor = _editor.selection.getCursor();

          var splittedContent = content.split('\n');
          var count = cursor.range.start.column;
          for(var i =0 ; i<cursor.range.start.row; i++) {
            count+=splittedContent[i].length;
          }
          selectionStart = count;
          count = cursor.range.end.column;
          for(var i =0 ; i<cursor.range.end.row; i++) {
            count+=splittedContent[i].length;
          }
          selectionEnd = count;

          cursor.scrollTop = _editor.session.getScrollTop();
          cursor.scrollLeft = _editor.session.getScrollLeft();

          cursor.startPrefix = text.substring(selectionStart - padLength, selectionStart);
          cursor.startSuffix = text.substring(selectionStart, selectionStart + padLength);
          cursor.startOffset = selectionStart;
          cursor.collapsed = (selectionStart == selectionEnd);
          if (!cursor.collapsed) {
            cursor.endPrefix = text.substring(selectionEnd - padLength, selectionEnd);
            cursor.endSuffix = text.substring(selectionEnd, selectionEnd + padLength);
            cursor.endOffset = selectionEnd;
          }
          return cursor;
        };


        /**
         * Attempt to restore the cursor's location.
         * @param {Object} cursor Context information of the cursor.
         * @private
         */
        mobwrite.shareAceObj.prototype.restoreCursor_ = function(cursor) {
          // Set some constants which tweak the matching behaviour.
          // Maximum distance to search from expected location.
          this.dmp.Match_Distance = 1000;
          // At what point is no match declared (0.0 = perfection, 1.0 = very loose)
          this.dmp.Match_Threshold = 0.9;

          var padLength = this.dmp.Match_MaxBits / 2;  // Normally 16.
          var newText = content;

          // Find the start of the selection in the new text.
          var pattern1 = cursor.startPrefix + cursor.startSuffix;
          var pattern2, diff;
          var cursorStartPoint = this.dmp.match_main(newText, pattern1,
              cursor.startOffset - padLength);
          if (cursorStartPoint !== null) {
            pattern2 = newText.substring(cursorStartPoint,
                                         cursorStartPoint + pattern1.length);
            //alert(pattern1 + '\nvs\n' + pattern2);
            // Run a diff to get a framework of equivalent indicies.
            diff = this.dmp.diff_main(pattern1, pattern2, false);
            cursorStartPoint += this.dmp.diff_xIndex(diff, cursor.startPrefix.length);
          }

          var cursorEndPoint = null;
          if (!cursor.collapsed) {
            // Find the end of the selection in the new text.
            pattern1 = cursor.endPrefix + cursor.endSuffix;
            cursorEndPoint = this.dmp.match_main(newText, pattern1,
                cursor.endOffset - padLength);
            if (cursorEndPoint !== null) {
              pattern2 = newText.substring(cursorEndPoint,
                                           cursorEndPoint + pattern1.length);
              //alert(pattern1 + '\nvs\n' + pattern2);
              // Run a diff to get a framework of equivalent indicies.
              diff = this.dmp.diff_main(pattern1, pattern2, false);
              cursorEndPoint += this.dmp.diff_xIndex(diff, cursor.endPrefix.length);
            }
          }

          // Deal with loose ends
          if (cursorStartPoint === null && cursorEndPoint !== null) {
            // Lost the start point of the selection, but we have the end point.
            // Collapse to end point.
            cursorStartPoint = cursorEndPoint;
          } else if (cursorStartPoint === null && cursorEndPoint === null) {
            // Lost both start and end points.
            // Jump to the offset of start.
            cursorStartPoint = cursor.startOffset;
          }
          if (cursorEndPoint === null) {
            // End not known, collapse to start.
            cursorEndPoint = cursorStartPoint;
          }

          var splittedText = newText.split('\n');
          var count = 0;
          var startRow = 0;
          var startCol = 0;
          var startFound = false;

          var endRow = 0;
          var endCol = 0;

          for (var i = 0; i < splittedText.length; i++) {
            if(count+splittedText[i].length > cursorStartPoint && !startFound) {
              startRow = i;
              startCol = cursorStartPoint -count;
              startFound = true;
            }
            if(count+splittedText[i].length > cursorEndPoint) {
              endRow = i;
              endCol = cursorEndPoint -count;
              break;
            }
            count+=splittedText[i].length;
          }
          var Range = ace.require('ace/range').Range;

          // Restore selection.
          _editor.selection.setRange(new Range(startRow,startCol,endRow,endCol));

          // Restore scrollbar locations
          _editor.session.setScrollTop(cursor.scrollTop);
          _editor.session.setScrollLeft(cursor.scrollLeft);
        };


        /**
         * Ensure that all linebreaks are LF
         * @param {string} text Text with unknown line breaks
         * @return {string} Text with normalized linebreaks
         * @private
         */
        mobwrite.shareAceObj.normalizeLinebreaks_ = function(text) {
          return text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
        };


        /**
         * Handler to accept text fields as elements that can be shared.
         * If the element is a textarea, text or password input, create a new
         * sharing object.
         * @param {*} node Object or ID of object to share.
         * @return {Object?} A sharing object or null.
         */
        mobwrite.shareAceObj.shareHandler = function(node) {
          return new mobwrite.shareAceObj(node);
        };


        // Register this shareHandler with MobWrite.
        mobwrite.shareHandlers.push(mobwrite.shareAceObj.shareHandler);
      };

      if(window.mobwrite != null) {
        initMobWrite();
      }

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
        aceSettings: aceSettings,
        getCommands: function () {
          return ace.commands;
        }
      };
    }
  ]);