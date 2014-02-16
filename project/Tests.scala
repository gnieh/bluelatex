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
        testOptions in IntegrationTest <+= (couchPort in bluelatex, couchAdmin in bluelatex, couchPassword in bluelatex) map { (port, admin, password) =>
          Tests.Argument(s"-DcouchPort=$port", s"-Dadmin=$admin", s"-Dpassword=$password")
        },
        test in Scenario <<= (blueStop in bluelatex) dependsOn ((test in IntegrationTest) dependsOn (blueStart in bluelatex))
      )
    ) dependsOn(blueCommon)

  lazy val testDeps = Seq(
    "org.subethamail" % "subethasmtp" % "3.1.7",
    "org.scala-stm" %% "scala-stm" % "0.7",
    "org.scalatest" %% "scalatest" % "2.0" % "scenario,test,it"
  )

}
