// Karma configuration
module.exports = function(config) {
  var configuration = {
    // base path, that will be used to resolve files and exclude
    basePath: '../../',

    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      'test/config/app.conf.js',
      'webapp/lib/angular/angular.min.js',
      'test/assets/angular-mocks.js',
      'webapp/lib/angular/*.min.js',
      'webapp/lib/*.min.js',
      'webapp/lib/angucomplete.js',
      'webapp/lib/GravatarDirective.js',
      'webapp/lib/angular*.js',
      'webapp/js/app.js',
      'webapp/js/utilities.js',
      'webapp/js/**/*.js',
      'test/spec/**/*.js'
    ],

    // list of files / patterns to exclude
    exclude: [],

    // web server port
    port: 8080,

    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,

    colors: true,

    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['Chrome', 'ChromeCanary'],

    customLaunchers: {
      Chrome_travis_ci: {
        base: 'Chrome',
        flags: ['--no-sandbox']
      }
    },

    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: true

  };
  if(process.env.TRAVIS){
      configuration.browsers = ['Chrome_travis_ci'];
  }
  config.set(configuration);
};