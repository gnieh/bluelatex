scalacOptions += "-deprecation"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "biz.aQute.bnd" % "bndlib" % "2.2.0"

libraryDependencies += "org.gnieh" %% "sohva-testing" % "0.4"

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

