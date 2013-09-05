package blue

import sbt._
import Keys._

trait Tests {
  this: BlueBuild =>

  lazy val BlueTest = config("blue-test") extend(Test)

  lazy val test =
    (Project(id = "blue-test",
      base = file("blue-test"))
      configs(BlueTest)
      settings(
        inConfig(BlueTest)(Defaults.testSettings): _*
      )
      settings(
        libraryDependencies ++= testDeps,
        fork in BlueTest := true,
        parallelExecution in BlueTest := false,
        baseDirectory in BlueTest <<= target(_ / "pack"),
        testOptions in BlueTest <+= baseDirectory map (basedir => Tests.Argument("-Dbundle.dir=" + new File(basedir, "bundle").getCanonicalPath))
      )
    ) dependsOn(core)

  lazy val testDeps = Seq(
    "org.subethamail" % "subethasmtp" % "3.1.7",
    "org.scala-stm" %% "scala-stm" % "0.7",
    "org.gnieh" %% "sohva-testing" % "0.3" % "blue-test",
    "org.scalatest" %% "scalatest" % "2.0.M5b" % "blue-test,test"
  )

}
