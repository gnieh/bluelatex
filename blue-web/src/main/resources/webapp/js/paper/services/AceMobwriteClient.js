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
 
angular.module('bluelatex.Paper', ['MobWrite','bluelatex.Paper.Services.Ace'])
  .factory("AceMobWriteClient", [
    '$rootScope',
    'MobWriteService',
    'MobWriteConfig',
    'AceService',
    'config',
    '$log',
    function ($rootScope,
              MobWriteService,
              MobWriteConfig,
              AceService,
              config,
              $log) {

      var api_prefix = config.api_prefix;

      var message = null;
      /**
      * Constructor of shared object representing a text field.
      * @param {Node} node A textarea, text or password input.
      * @constructor
      */
      var shareAceObj = function(paper) {
        // Call our prototype's constructor.
        MobWriteService.shareObj.apply(this, [paper.file]);
        MobWriteConfig.syncGateway = api_prefix + "/papers/" + paper.paper_id + "/" + (MobWriteConfig.messageType=='json'?'sync':'q');

        this.paperId = paper.paper_id;
        this.file = paper.file;
      };

      // The textarea shared object's parent is a shareObj.
      shareAceObj.prototype = new MobWriteService.shareObj('');


      /**
       * Retrieve the user's text.
       * @return {string} Plaintext content.
       */
      shareAceObj.prototype.getClientText = function() {
        return shareAceObj.normalizeLinebreaks_(AceService.getContent());
      };


      /**
       * Set the user's text.
       * @param {string} text New text
       */
      shareAceObj.prototype.setClientText = function(text) {
        AceService.getSession().setValue(text);
      };


      /**
       * Modify the user's plaintext by applying a series of patches against it.
       * @param {Array.<patch_obj>} patches Array of Patch objects.
       */
      shareAceObj.prototype.patchClientText = function(patches) {
        // Set some constants which tweak the matching behaviour.
        // Maximum distance to search from expected location.
        this.dmp.Match_Distance = 1000;
        // At what point is no match declared (0.0 = perfection, 1.0 = very loose)
        this.dmp.Match_Threshold = 0.6;

        var oldClientText = this.getClientText();
        var simpleDiffer = new MobWriteService.SimpleDiffer();
        var newClientText = this.patch_apply_(patches, oldClientText, simpleDiffer);

        // Set the new text only if there is a change to be made.
        if (oldClientText != newClientText) {
          var splittedText = oldClientText.split('\n');
          var simpleDiff = simpleDiffer.getSimpleDiff();

          for (var i = 0, mutation; mutation = simpleDiff[i]; i++) {
            if (mutation.type == 'insert') {
              // convert text offset to row, Column
              var offset = convertOffetToRomColumn(mutation.start, splittedText);
              
              // get the current scroll position
              var scrollTop = AceService.getSession().getScrollTop();
              // get the height of a line
              var lineHeight = AceService.getEditor().renderer.lineHeight;
              // get the number of screen lines
              var sreenLengthBefore = AceService.getSession().getScreenLength();
              // convert the scroll position to screen line number
              var scrollLineNumber = scrollTop / lineHeight;
              // convert screen position to text position
              var textPosition = AceService.getSession().screenToDocumentPosition(Math.ceil(scrollLineNumber), 0);

              // insert the new text
              AceService.getSession().insert(offset, mutation.text);

              // get the number of screen lines
              var sreenLengthAfter = AceService.getSession().getScreenLength();
              // if the text is inserted before the scroll position
              if(offset.row <= textPosition.row) {
                // performs the number of screen line added
                var lengthFiff = sreenLengthAfter - sreenLengthBefore;
                // convert number of line to height
                var scrollToAdd = lengthFiff * lineHeight;
                // change the scroll position
                AceService.getSession().setScrollTop(scrollTop + scrollToAdd);
              } else {
                // conserves the scroll position
                AceService.getSession().setScrollTop(scrollTop);
              }
            } else if (mutation.type == 'delete') {
              // convert text offset to row, Column
              var offset = convertOffetToRomColumn(mutation.start, mutation.end, splittedText);
              var Range = ace.require('ace/range').Range;
              var range = new Range(offset.start.row,offset.start.column,offset.end.row,offset.end.column);

              // get the current scroll position
              var scrollTop = AceService.getSession().getScrollTop();
              // get the height of a line
              var lineHeight = AceService.getEditor().renderer.lineHeight;
              // get the number of screen lines
              var sreenLengthBefore = AceService.getSession().getScreenLength();
              // convert the scroll position to screen line number
              var scrollLineNumber = scrollTop / lineHeight;
              // convert screen position to text position
              var textPosition = AceService.getSession().screenToDocumentPosition(Math.ceil(scrollLineNumber), 0);

              AceService.getSession().remove(range);
              
              // get the number of screen lines
              var sreenLengthAfter = AceService.getSession().getScreenLength();
              // if the text is inserted before the scroll position
              if(offset.start.row <= textPosition.row) {
                // performs the number of screen line added
                var lengthFiff = sreenLengthAfter - sreenLengthBefore;
                // convert number of line to height
                var scrollToAdd = lengthFiff * lineHeight;
                // change the scroll position
                AceService.getSession().setScrollTop(scrollTop + scrollToAdd);
              } else {
                // conserves the scroll position
                AceService.getSession().setScrollTop(scrollTop);
              }
            }
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
      shareAceObj.prototype.patch_apply_ = function(patches, text,
                                                    simpleDiffer) {
        if (patches.length == 0) {
          return text;
        }

        // Deep copy the patches so that no changes are made to originals.
        patches = this.dmp.patch_deepCopy(patches);
        var nullPadding = this.dmp.patch_addPadding(patches);
        var nullPaddingLength = nullPadding.length;
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
            if (MobWriteConfig.debug) {
              $log.warn('Patch failed: ' + patches[x]);
            }
            // Subtract the delta for this failed patch from subsequent patches.
            delta -= patches[x].length2 - patches[x].length1;
          } else {
            // Found a match.  :)
            if (MobWriteConfig.debug) {
              $log.info('Patch OK.');
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
              if (MobWriteConfig.debug) {
                $log.warn('Patch contents mismatch: ' + patches[x]);
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
                  simpleDiffer.applyInsert(
                      start_loc + index2 - nullPaddingLength,
                      mod[1]);
                } else if (mod[0] === DIFF_DELETE) {  // Deletion
                  var del_start = start_loc + index2;
                  var del_end = start_loc + this.dmp.diff_xIndex(diffs,
                      index1 + mod[1].length);
                  text = text.substring(0, del_start) + text.substring(del_end);
                  simpleDiffer.applyDelete(
                      del_start - nullPadding.length,
                      del_end - nullPadding.length);
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


      var convertRowColumn = function (cursor, content) {
        var splittedContent = content.split('\n');
        var count = cursor.column;
        for(var i =0 ; i < cursor.row; i++) {
          // +1 for the \n
          count += splittedContent[i].length + 1;
        }
        return count;
      };

      /**
       * Record information regarding the current cursor.
       * @return {Object?} Context information of the cursor.
       * @private
       */
      shareAceObj.prototype.captureCursor_ = function() {
        var padLength = this.dmp.Match_MaxBits / 2;  // Normally 16.
        var text = this.getClientText();
        var cursor = {};

        var range = AceService.getEditor().selection.getRange();

        selectionStart = convertRowColumn(range.start, text);
        selectionEnd = convertRowColumn(range.end, text);

        cursor.scrollTop  = AceService.getSession().getScrollTop();
        cursor.scrollLeft = AceService.getSession().getScrollLeft();

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
      shareAceObj.prototype.restoreCursor_ = function(cursor) {
        var position = this.getCursorPosition(cursor);

        var Range = ace.require('ace/range').Range;

        // Restore selection.
        AceService.getSession().selection.setRange(new Range(position.start.row,position.start.column,position.end.row,position.end.column));

        // Restore scrollbar locations
        AceService.getSession().setScrollTop(cursor.scrollTop);
        AceService.getSession().setScrollLeft(cursor.scrollLeft);
      };

      /**
       * Attempt to get the cursor's location.
       * @param {Object} cursor Context information of the cursor.
       */
      shareAceObj.prototype.getCursorPosition = function (cursor) {
        // Set some constants which tweak the matching behaviour.
        // Maximum distance to search from expected location.
        this.dmp.Match_Distance = 1000;
        // At what point is no match declared (0.0 = perfection, 1.0 = very loose)
        this.dmp.Match_Threshold = 0.9;

        var padLength = this.dmp.Match_MaxBits / 2;  // Normally 16.
        var newText = this.getClientText();

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
        return convertOffetToRomColumn(cursorStartPoint, cursorEndPoint, splittedText);
      };

      var convertOffetToRomColumn = function(offsetStart, offsetEnd, textArray) {
        if(arguments.length == 2) {
          textArray = offsetEnd;
          offsetEnd = null;
        }

        var startFound = false;
        var count = 0;

        var startRow = 0;
        var startCol = 0;

        var endRow = 0;
        var endCol = 0;

        for (var i = 0; i < textArray.length; i++) {
          if(!startFound && count + textArray[i].length + 1 > offsetStart) {
            startRow = i;
            startCol = offsetStart - count;
            startFound = true;
          }
          if(offsetEnd != null && count + textArray[i].length+1 > offsetEnd) {
            endRow = i;
            endCol = offsetEnd - count;
            break;
          }
          // +1 for \n character
          count += textArray[i].length + 1;
        }

        if(offsetEnd == null) {
          return {
            row: startRow,
            column: startCol
          }
        }

        return {
          start: {
            row: startRow,
            column: startCol
          },
          end: {
            row: endRow,
            column: endCol
          }
        }
      }

      shareAceObj.prototype.messages = function () {
        var json = message;
        if(json == null) return;

        if(json.type == 'cursor') {
          json.cursor = this.captureCursor_();
        }
        message = null;
        return {
          from: MobWriteService.syncUsername,
          filename: this.file,
          json: json
        };
      };
      var _cursors = [];

      shareAceObj.prototype.onMessage = function (message) {
        if(message.json.type == "cursor") {
          var self = this;
          message.json.getPosition = function () {
            return self.getCursorPosition(message.json.cursor); 
          } 
        }
        $rootScope.$broadcast('MobWriteMessage', message);
      };

      /**
       * Ensure that all linebreaks are LF
       * @param {string} text Text with unknown line breaks
       * @return {string} Text with normalized linebreaks
       * @private
       */
      shareAceObj.normalizeLinebreaks_ = function(text) {
        return text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
      };


      /**
       * Handler to accept text fields as elements that can be shared.
       * If the element is a textarea, text or password input, create a new
       * sharing object.
       * @param {*} node Object or ID of object to share.
       * @return {Object?} A sharing object or null.
       */
      shareAceObj.shareHandler = function(node) {
        if(node.file && node.paper_id) {
          return new shareAceObj(node);
        }
        return null;
      };


      // Register this shareHandler with MobWrite.
      MobWriteService.shareHandlers.push(shareAceObj.shareHandler);

      return {
        shareAceObj: shareAceObj,
        message: function(m) {
          message = m;
        }
      };
    }
  ]);