package blue

import sbt._
import Keys._

trait Tests {
  this: BlueBuild =>

  val Scenario = config("scenario") extend(IntegrationTest)

  lazy val testing =
    (Project(id = "blue-test",
      base = file("blue-test"))
      configs(IntegrationTest, Scenario)
      settings(
        inConfig(Scenario)(Defaults.itSettings): _*
      )
      settings(
        inConfig(IntegrationTest)(Defaults.itSettings): _*
      )
      settings(
        libraryDependencies ++= testDeps,
        fork in IntegrationTest := true,
        parallelExecution in IntegrationTest := false,
        test in Scenario <<= ((test in IntegrationTest) dependsOn (blueStart in bluelatex))
      )
    ) dependsOn(core)

  lazy val testDeps = Seq(
    "org.subethamail" % "subethasmtp" % "3.1.7",
    "org.scala-stm" %% "scala-stm" % "0.7",
    "org.scalatest" %% "scalatest" % "2.0.M6" % "scenario,test,it"
  )

}
