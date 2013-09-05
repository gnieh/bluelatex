package blue

import sbt._
import Keys._
import java.net.Socket

trait Server {
  this: BlueBuild =>

  lazy val launcher = Project(id = "launcher",
    base = file("blue-launcher")) settings(
      name := "blue-launcher",
      organization := "org.gnieh",
      version := blueVersion,
      autoScalaLibrary := false,
      crossPaths := false,
      javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked"),
      libraryDependencies := Seq(
        "org.apache.felix" % "org.apache.felix.main" % "4.2.1",
        "commons-daemon" % "commons-daemon" % "1.0.15"
      )
    )

  val BlueServer = config("blue-server")

  val blueStart: TaskKey[Boolean] =
    TaskKey[Boolean]("blue-start", "starts a \\BlueLaTeX test environment")

  val blueStop: TaskKey[Boolean] =
    TaskKey[Boolean]("blue-stop", "stops \\BlueLaTeX test environment")

  val blueServerSettings: Seq[Project.Setting[_]] =
    Seq(
      blueStartTask,
      blueStopTask
    )

  private def blueStartTask = blueStart <<= (pack, packageBin in (launcher, Compile), update in launcher) map { (pack, jar, deps) =>
    val jars = for {
      c <- deps.configurations
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file
    val cp = ((jar +: jars).map(_.getCanonicalPath)).mkString(":")
    Fork.java(None, Seq("-cp", cp, "org.gnieh.blue.launcher.Main"), Some(pack), StdoutOutput)
    true
  }

  private def blueStopTask = blueStop := {
    val s = new Socket("localhost", 8911)
    s.getOutputStream.write("stop".getBytes("ISO-8859-1"))
    s.close
    true
  }

}
