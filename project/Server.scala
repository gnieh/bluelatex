package blue

import sbt._
import Keys._
import java.net.Socket
import gnieh.sohva.Configuration
import gnieh.sohva.testing._
import scala.util.Properties

trait Server {
  this: BlueBuild =>

  val BlueServer = config("blue-server")

  val javaHome = System.getProperty("java.home")
  val user = System.getProperty("user.name")

  private def defaultStartJsvcOptions(cp: String) =
    List(
      "-wait", "10",
      "-java-home", javaHome,
      "-cp", cp,
      "-user", user,
      "-pidfile", "/tmp/bluelatex.pid",
      "-outfile", "/tmp/bluelatex.out",
      "-errfile", "/tmp/bluelatex.err"
    )

  private def defaultStartPrunOptionsOptions(cp: String) =
    Nil

  private def defaultStartOptions(deps: UpdateReport, jar: File): List[String] = {
    val jars = for {
      c <- deps.configurations
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file
    val cp = ((jar +: jars).map(_.getCanonicalPath)).mkString(":")
    if(Properties.isWin) defaultStartPrunOptionsOptions(cp) else defaultStartJsvcOptions(cp)
  }

  private def defaultStopOptions(deps: UpdateReport, jar: File): List[String] = {
    val jars = for {
      c <- deps.configurations
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file
    val cp = ((jar +: jars).map(_.getCanonicalPath)).mkString(":")
    if(Properties.isWin) defaultStartPrunOptionsOptions(cp) else (defaultStartJsvcOptions(cp) ::: List("-stop"))
  }

  lazy val blueLauncher = Project(id = "launcher",
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

  val launchExe =
    settingKey[String]("the command to launch the server")

  val startOptions =
    taskKey[List[String]]("the options to pass when starting the server")

  val stopOptions =
    taskKey[List[String]]("the options to pass when stopping the server")

  val couchdb =
    settingKey[CouchInstance]("the CouchDB instance")

  val blueStart =
    taskKey[Unit]("starts a \\BlueLaTeX test environment")

  val blueStop =
    taskKey[Unit]("stops \\BlueLaTeX test environment")

  val blueServerSettings: Seq[Def.Setting[_]] =
    Seq(
      couchdb <<= target(t => new CouchInstance(t / "couchdb", false, true, "1.4.0", Configuration(Map("log" -> Map("level" -> "debug"))))),
      launchExe := (if(Properties.isWin) "prunsrv" else "jsvc"),
      startOptions <<= (update in blueLauncher, packageBin in (blueLauncher, Compile)) map (defaultStartOptions _),
      stopOptions <<= (update in blueLauncher, packageBin in (blueLauncher, Compile)) map defaultStopOptions _,
      blueConfDir in BlueServer <<= sourceDirectory(_ / "configuration"),
      blueStartTask,
      blueStopTask
    )

  private def blueStartTask = blueStart <<= (launchExe, startOptions, couchdb, streams, bluePack, packageBin in (blueLauncher, Compile), update in blueLauncher) map {
    (exe, options, couchdb, out, pack, jar, deps) =>

    if(!Properties.isWin)
      couchdb.start()

    val process = Process(
      (exe :: options ::: List("org.gnieh.blue.launcher.Main")).toSeq,
      Some(pack)
    )
    process ! new Logger(out)
    println("started")
  }

  private def blueStopTask = blueStop <<= (launchExe, stopOptions, couchdb, streams, packageBin in (blueLauncher, Compile), update in blueLauncher) map {
    (exe, options, couchdb, out, jar, deps) =>

    if(!Properties.isWin)
      couchdb.stop()

    val process = Process(
      (exe :: options ::: List("org.gnieh.blue.launcher.Main")).toSeq,
      None
    )
    process ! new Logger(out)
    println("stopped")
  }

  private class Logger(out: TaskStreams) extends ProcessLogger {

    def buffer[T](f: =>T): T = f

    def error(s: =>String): Unit =
      out.log.error(s)

    def info(s: => String):Unit =
      out.log.info(s)

  }

}
