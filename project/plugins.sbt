scalacOptions += "-deprecation"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "biz.aQute.bnd" % "bndlib" % "2.1.0"

libraryDependencies += "org.gnieh" %% "sohva-testing" % "0.4"

