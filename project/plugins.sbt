scalacOptions += "-deprecation"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += Resolver.typesafeRepo("releases")

libraryDependencies += "biz.aQute.bnd" % "bndlib" % "2.2.0"

libraryDependencies += "org.gnieh" %% "sohva-testing" % "0.4"

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")