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

  val blueStart =
    taskKey[Boolean]("starts a \\BlueLaTeX test environment")

  val blueStop =
    taskKey[Boolean]("stops \\BlueLaTeX test environment")

  val blueServerSettings: Seq[Def.Setting[_]] =
    Seq(
      blueStartTask,
      blueStopTask
    )

  private def blueStartTask = blueStart <<= (streams, bluePack, packageBin in (launcher, Compile), update in launcher) map { (out, pack, jar, deps) =>
    val jars = for {
      c <- deps.configurations
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file
    val cp = ((jar +: jars).map(_.getCanonicalPath)).mkString(":")
    val javaHome = System.getProperty("java.home")
    val user = System.getProperty("user.name")
    // TODO make it configurable for other platforms
    val process = Process(
      Seq(
        "jsvc",
        "-cwd", pack.getCanonicalPath,
        "-java-home", javaHome,
        "-cp", cp,
        "-user", user,
        "-pidfile", "/tmp/bluelatex.pid",
        "-outfile", "/tmp/bluelatex.out",
        "-errfile", "/tmp/bluelatex.err",
        "org.gnieh.blue.launcher.Main"),
      Some(pack)
    )
    process ! new Logger(out) == 0
  }

  private def blueStopTask = blueStop <<= (streams, bluePack, packageBin in (launcher, Compile), update in launcher) map { (out, pack, jar, deps) =>
    val jars = for {
      c <- deps.configurations
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file
    val cp = ((jar +: jars).map(_.getCanonicalPath)).mkString(":")
    val javaHome = System.getProperty("java.home")
    val user = System.getProperty("user.name")
    // TODO make it configurable for other platforms
    val process = Process(
      Seq(
        "jsvc",
        "-cwd", pack.getCanonicalPath,
        "-java-home", javaHome,
        "-cp", cp,
        "-user", user,
        "-pidfile", "/tmp/bluelatex.pid",
        "-outfile", "/tmp/bluelatex.out",
        "-errfile", "/tmp/bluelatex.err",
        "-stop",
        "org.gnieh.blue.launcher.Main"),
      None
    )
    process ! new Logger(out) == 0
  }

  private class Logger(out: TaskStreams) extends ProcessLogger {

    def buffer[T](f: =>T): T = f

    def error(s: =>String): Unit =
      out.log.error(s)

    def info(s: => String):Unit =
      out.log.info(s)

  }

}
