\BlueLaTeX
==========

The \BlueLaTeX server and web client that allows people to collaboratively write LaTeX documents with real-time synchronization.

<http://www.publications.li/blue> runs \BlueLaTeX.

\BlueLaTeX is free open-source software, which offers you several advantages.
It is still under heavy development but our goals are the following:
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

Developers
----------

The \BlueLaTeX server is developed in [Scala](http://scala-lang.org/) and uses [sbt](http://www.scala-sbt.org/) as build tool and dependency manager.
You will also need to install [java](http://java.com) (version 1.6 or higher), [couchdb](http://couchdb.apache.org/) (version 1.2 or higher) and [jsvc](http://commons.apache.org/proper/commons-daemon/jsvc.html) to start the test server.
Once all requirements are installed, a good starting point is to start sbt by typing `sbt` in your shell. You will get a running sbt console from which you can execute some command.
Try to type `compile` for example and it will compile the entire project.

If you want to start a test server, just type `blueStart` in the sbt console. To stop it (it is quite easy to infer actually...), type `blueStop`.

The project is structured as follows:
 - `blue-commons` contains the commons utilities and registsers global services. This includes logging service, configuration loader, actor system, ...
 - `blue-core` contains the core server features, such as the Http server, the core Rest Api to manage users, sessions and paper synchronization,
 - `blue-compile` contains the compilation server features,
 - `blue-sync` contains the scala implementation of the synchronization server,
 - `blue-mobwrite` contains the mobwrite binding to provide the synchronization implementation that simply delegates all requests to a running mobwrite daemon,
 - `blue-launcher` contains only the blue daemon launcher class (used to launch the test server),
 - `blue-test` contains the high level scenarii,
 - `src` contains the test configuration and data used by the test server and the test scenarii.

If you have troubles or want to ask us questions, join us on IRC on channel `#bluelatex` on freenode.

