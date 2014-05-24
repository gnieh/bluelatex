Contributing
============

We are happy to accept contributions from everybody.
However we would like to keep \BlueLaTeX maintainable and make the patch integration process as easy as possible, thus here are some rules that we would like you to respect.

Getting Started
---------------

The first steps to contribute are as follows:
 - Make sure you have a [Github account](https://github.com/signup/free)?
 - Submit a ticket for your issue, assuming one does not already exist.
   - Clearly describe the issue including steps to reproduce when it is a bug.
   - Make sure you fill in the earliest version that you know has the issue.
 - Fork the repository on Github.

What Should Come With a Contribution?
-------------------------------------

If you are implementing a new feature (for example, if you extend the Rest API), or if you fix a bug, your contribution must come with high level test scenarii.
To do so, just use the scenario framework provided by us in the [blue-test](https://github.com/gnieh/bluelatex/tree/master/blue-test/) project.
You can find sample scenarii [here](https://github.com/gnieh/bluelatex/tree/master/blue-test/src/it/scala/gnieh/blue/scenario).
The provided scenarii must cover the implemented feature (positive and robustness cases).

If you are fixing a bug, there was probably no scenario to test it, thus the new scenario is really welcome with the fix!

If you need to add unit tests, this is to be done in the impacted subproject, not in `blue-test`.

Of course, the best thing to do if you have questions is to ask us before starting to work, either on our [developer mailing-list](http://lists.gnieh.org/mailman/listinfo/blue-dev) or on the IRC channel `#bluelatex` on `freenode`.

Making Changes
--------------

 - Create a topic branch from where you want to base your work,
   - This is usually the master branch,
   - Only target release branches if you are certain your fix must be on that
    branch,
   - To quickly create a topic branch based on master; `git branch
    wip-my-contribution master` then checkout the new branch with `git
    checkout wip-my-contribution`.  Please avoid working directly on the
    `master` branch,
 - Make commits of logical units,
 - Check for unnecessary whitespace with `git diff --check` before committing,
 - Make sure your commit messages are in the proper format.

```
    [XXX #99] Make the example in CONTRIBUTING imperative and concrete

    Without this patch applied the example commit message in the CONTRIBUTING
    document is not a concrete example.  This is a problem because the
    contributor is left to imagine what the commit message should look like
    based on a description rather than an example.  This patch fixes the
    problem by making the example concrete and imperative.

    The first line is a real life imperative statement with a ticket number
    from our issue tracker with `XXX` being the kind of change this pull
    request addresses:
     - `new` for a new feature
     - `fix` if this fixes a problem
     - `imp` if it improves an existing feature
     - `del` if it deletes some existing stuff
    The body describes the behavior without the patch,
    why this is a problem, and how the patch fixes the problem when applied.
```

 - Make sure you read and understand and agree with the [Developer Certificate of Origin](http://developercertificate.org/) before contributing to this project,
 - Make sure you applied the [commit message guidelines](http://git-scm.com/book/ch5-2.html#Commit-Guidelines) we are using for this project,
 - Make sure you have added the necessary tests for your changes as described in the previous section,
 - Make sure you added your full name in the CONTRIBUTORS file with the correct year,
 - Run _all_ the tests to assure nothing else was accidentally broken.

