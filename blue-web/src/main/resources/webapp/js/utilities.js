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
 
/**
* Get the week number of a date
*/
Date.prototype.getWeek = function() {
    var onejan = new Date(this.getFullYear(),0,1);
    return Math.ceil((((this - onejan) / 86400000) + onejan.getDay()+1)/7);
};
/**
* Transform json to HTTP POST parameters
*/
var jsonToPostParameters = function (json) {
    return Object.keys(json).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(json[k]);
    }).join('&');
};
/*
* Get the file name without the extension
*/
function getFileNameExtension(filename) {
    var parts = filename.split('.');
    return parts[parts.length - 1];
}
/**
* Get the file type of the file based on the extension
*/
function getFileType(filename) {
    var ext = getFileNameExtension(filename);
    switch (ext.toLowerCase()) {
      case 'jpg':
      case 'jpeg':
      case 'gif':
      case 'bmp':
      case 'png':
        return 'image';
      case 'm4v':
      case 'avi':
      case 'mpg':
      case 'mp4':
        return 'video';
      case 'pdf':
        return 'pdf';
      case 'txt':
        return 'text';
      case 'tex':
        return 'latex';
    }
    return 'text';
}
/**
* Clone an object
*/
function clone(obj) {
    // Handle the 3 simple types, and null or undefined
    if (null == obj || "object" != typeof obj) return obj;

    // Handle Date
    if (obj instanceof Date) {
        var copy = new Date();
        copy.setTime(obj.getTime());
        return copy;
    }

    // Handle Array
    if (obj instanceof Array) {
        var copy = [];
        for (var i = 0, len = obj.length; i < len; i++) {
            copy[i] = clone(obj[i]);
        }
        return copy;
    }

    // Handle Object
    if (obj instanceof Object) {
        var copy = {};
        for (var attr in obj) {
            if (obj.hasOwnProperty(attr)) copy[attr] = clone(obj[attr]);
        }
        return copy;
    }

    throw new Error("Unable to copy obj! Its type isn't supported.");
}
/**
* Get absolute position of an element in the dom
*/
function findPosX(obj) {
    var curleft = 0;
    if (obj.offsetParent) {
        while (1) {
            curleft+=obj.offsetLeft;
            if (!obj.offsetParent) {
                break;
            }
            obj=obj.offsetParent;
        }
    } else if (obj.x) {
        curleft+=obj.x;
    }
    return curleft;
};
/**
* Get absolute position of an element in the dom
*/
function findPosY(obj) {
    var curtop = 0;
    if (obj.offsetParent) {
        while (1) {
            curtop+=obj.offsetTop;
            if (!obj.offsetParent) {
                break;
            }
            obj=obj.offsetParent;
        }
    } else if (obj.y) {
        curtop+=obj.y;
    }
    return curtop;
};

/**
 * Returns scale factor for the canvas. It makes sense for the HiDPI displays.
 * @return {Object} The object with horizontal (sx) and vertical (sy)
                    scales. The scaled property is set to false if scaling is
                    not required, true otherwise.
 */
function getOutputScale(ctx) {
  var devicePixelRatio = window.devicePixelRatio || 1;
  var backingStoreRatio = ctx.webkitBackingStorePixelRatio ||
                          ctx.mozBackingStorePixelRatio ||
                          ctx.msBackingStorePixelRatio ||
                          ctx.oBackingStorePixelRatio ||
                          ctx.backingStorePixelRatio || 1;
  var pixelRatio = devicePixelRatio / backingStoreRatio;
  return {
    sx: pixelRatio,
    sy: pixelRatio,
    scaled: pixelRatio !== 1
  };
}

/*
* Convert pdf viewport to pixel
*/
var convertToViewportPoint  = function (x, y, dimension) {
    var transform = [
      dimension.scale,
      0,
      0,
      -dimension.scale,
      0,
      dimension.height
    ];

    var xt = x * transform[0] + y * transform[2] + transform[4];
    var yt = x * transform[1] + y * transform[3] + transform[5];
    return [xt, yt];
};

/**
* Create a random hex color
*/
function getRadomColor () {
  return (function(m,s,c){return (c ? arguments.callee(m,s,c-1) : '#') +
  s[m.floor(m.random() * s.length)];})(Math,'0123456789ABCDEF',5);
}

/**
* Define Object.keys if not available
*/
if(Object.keys == null) {
  Object.keys = function (obj) {
    var r = [];
    for (var k in obj) {
        if (!obj.hasOwnProperty(k)) 
            continue;
        r.push(k);
    }
    return r;
  }
}

/**
* Get the color of a string
*/
var stringToColour = function(str) {
    var golden_ratio_conjugate = 0.618033988749895;
    // str to hash
    for (var i = 0, hash = 0; i < str.length; hash = str.charCodeAt(i++) + ((hash << 5) - hash));

    hash = Math.abs(hash);
    hash /= Math.pow(10,(""+hash).length);
    hash = (hash+golden_ratio_conjugate)%1;
    return rgbToHex(HSVtoRGB(hash, 0.6, 0.95));
}

/*
* Convert hex color to rgb color
*/
function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

/*
* Convert rgb color to hex color
*/
function rgbToHex(rgb) {
    return "#" + ((1 << 24) + (rgb.r << 16) + (rgb.g << 8) + rgb.b).toString(16).slice(1);
}

function HSVtoRGB(h, s, v) {
    var r, g, b, i, f, p, q, t;
    if (h && s === undefined && v === undefined) {
        s = h.s, v = h.v, h = h.h;
    }
    i = Math.floor(h * 6);
    f = h * 6 - i;
    p = v * (1 - s);
    q = v * (1 - f * s);
    t = v * (1 - (1 - f) * s);
    switch (i % 6) {
        case 0: r = v, g = t, b = p; break;
        case 1: r = q, g = v, b = p; break;
        case 2: r = p, g = v, b = t; break;
        case 3: r = p, g = q, b = v; break;
        case 4: r = t, g = p, b = v; break;
        case 5: r = v, g = p, b = q; break;
    }
    return {
        r: Math.floor(r * 255),
        g: Math.floor(g * 255),
        b: Math.floor(b * 255)
    };
}