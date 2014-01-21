Date.prototype.getWeek = function() {
    var onejan = new Date(this.getFullYear(),0,1);
    return Math.ceil((((this - onejan) / 86400000) + onejan.getDay()+1)/7);
}
var jsonToPostParameters = function (json) {
    return Object.keys(json).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(json[k])
    }).join('&')
}
function getFileNameExtension(filename) {
    var parts = filename.split('.');
    return parts[parts.length - 1];
}
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
    return 'other';
}