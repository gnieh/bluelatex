\BlueLaTeX [![Build Status](https://travis-ci.org/gnieh/bluelatex.png?branch=master)](https://travis-ci.org/gnieh/bluelatex)
==========

\BlueLaTeX is an open-source server and web client that allows people to collaboratively write LaTeX documents in a browser with real-time synchronization. It has a true open-source philosophy and process and welcomes contributions. 

\BlueLaTeX is an official Mozilla Science project:  <http://www.mozillascience.org/projects/gnieh-bluelatex/>

Known public instances:

* <http://demo.bluelatex.org> 

Our goals are the following:
 - as a user:
   - you can have your own instance, running on your server, and keep your data at home,
   - it offers you a clear Restful API which allows for interoperability, to integrate the service with your favorite \LaTeX editor,
   - it is designed to be part of a distributed system, so you can scale up by adding more instance
   - you can share it as your want (according to the licence, of course).
 - as a developer:
   - it uses a convergent synchronization protocol based on mobwrite, designed to be easy to distribute,
   - it is implemented in a modular (but not too modular) way, so that you can easily add new features,
   - you can modify it to your need, and integrate it in your own solution (according to the licence, of course).

We are actively looking for contributors, please contact us and send pull requests if you are interested!

Repository Layout
-----------------

\BlueLaTeX is composed of several modules that are distributed over several folders:
 - `blue-common` contains the commons utilities and registers global services. This includes the Http server, the logging service, configuration loader, actor system, ...
 - `blue-core` contains the core server features, such as the core Rest Api to manage users, sessions and paper synchronization,
 - `blue-compile` contains the compilation server features,
 - `blue-sync` contains the scala implementation of the synchronization server,
 - `blue-mobwrite` contains the mobwrite binding to provide the synchronization implementation that simply delegates all requests to a running mobwrite daemon,
 - `blue-launcher` contains only the blue daemon launcher class (used to launch the test server),
 - `blue-test` contains the high level scenarios,
 - `blue-web` contains the web client for \BlueLaTeX,
 - `src` contains the test configuration and data used by the test server and the test scenarios.

Building \BlueLaTeX
-------------------

\BlueLaTeX uses [sbt](http://scala-sbt.org) as build system and dependency manager. Building \BlueLaTeX can be done by simply typing `sbt compile` in a console.

For more details, if you have problems to install dependencies or to build and package \BlueLaTeX, please refer to [the documentation](http://bluelatex.gnieh.org/developers/)
