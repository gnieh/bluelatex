#!/usr/bin/env node

var util = require('util'),
    http = require('http'),
    httpProxy = require('http-proxy'),
    express = require('express'),
    path = require('path');

var DEFAULT_PORT = 8000;
var port = 8080;

var app = express();

// all environments
// configure Express
app.configure(function() {
    app.set('port', process.env.PORT || DEFAULT_PORT);
    app.set('env', 'development');
    app.use(express.compress());
    app.use(express.static(path.join(__dirname, 'web_client')));
    app.use(express.cookieParser('myGoodPasswordNeverKnowIt'));
    app.use(express.bodyParser());
    app.use(express.session({ secret: 'myGoodPasswordNeverKnowIt' }));

    app.use(express.favicon(path.join(__dirname, 'web_client') + '/favicon.ico'));
    app.use(express.json());
    app.use(express.urlencoded());
    app.use(express.methodOverride());
});

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

httpProxy.createServer(function (req, res, proxy) {
    if(req.url.search("/api")>=0) {
      req.url = req.url.replace("/api","");
      proxy.proxyRequest(req, res, { host: 'bluelatex.cloudapp.net', port: 18080 });
    }else
      proxy.proxyRequest(req, res, { host: '127.0.0.1', port: app.get('port') });
}).listen(port);

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + port);
});
