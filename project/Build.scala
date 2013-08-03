package blue

import sbt._
import Keys._
import xerial.sbt.Pack._
import aQute.bnd.osgi._

import java.io.File

object BlueBuild extends Build {

  val blueVersion = "0.7-SNAPSHOT"

  lazy val bndDir: SettingKey[File] =
    SettingKey[File]("bnd-dir", "the directory containing the BND descriptors")

  lazy val bluelatex = (Project(id = "bluelatex",
    base = file(".")) settings (
    resolvers in ThisBuild += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers in ThisBuild += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    organization in ThisBuild := "org.gnieh",
    name := "bluelatex",
    version in ThisBuild := blueVersion,
    scalaVersion in ThisBuild := "2.10.2",
    autoCompilerPlugins in ThisBuild := true,
    compileOptions,
    libraryDependencies ++= blueDependencies,
    // fork jvm when running
    fork in run := true)
    settings(packSettings: _*)
    settings(bndDir <<= baseDirectory(new File(_, "bnd")))
    settings(packLibJars <<= (bndDir, target, packLibJars) map { case (bnddir, target, jars) => jars map osgify(bnddir, target) })
    settings(pack: _*)
  ) aggregate(common, http, compile, mobwrite, sync)

  /* make an OSGi bundle out of a jar file */
  def osgify(bnddir: File, target: File)(file: File): File = {
    val jar = new Jar(file)
    if(jar.getManifest == null || jar.getBsn == null) {
      // it is not an OSGi bundle, wrap it
      // extract the name and version from filename
      file.getName match {
        case library(name, version) =>
          //look if there is some bnd descriptor for this jar
          val descriptor = new File(bnddir, name + ".bnd")
          val wrapper = new Analyzer
          wrapper.setJar(jar)
          if(descriptor.exists) {
            wrapper.setProperties(descriptor)
          } else {
            wrapper.setImportPackage("*;resolution:=optional");
            wrapper.setExportPackage("*");
          }
          wrapper.setBundleVersion(version.replaceAll("-SNAPSHOT", "").replace('-', '.'))
          val m = wrapper.calcManifest
          if(wrapper.isOk) {
            jar.setManifest(m)
            val tmpdir = new File(target, "bnd")
            tmpdir.mkdirs
            val newFile = new File(tmpdir, file.getName)
            wrapper.save(newFile, true)
            newFile
          } else {
            file
          }
        case _ =>
          file
      }
    } else {
      // it is already an OSGi bundle, return the file
      file
    }
  }

  lazy val library =
    """([^_]+)(?:_[0-9](?:.[0-9]+)+)?-([0-9]+(?:.[0-9]+)*(?:-\w+)*).jar""".r

  lazy val compileOptions = scalacOptions in ThisBuild ++=
      Seq("-deprecation", "-feature")

  lazy val blueDependencies = Seq(
    "org.apache.felix" % "org.apache.felix.main" % "4.2.1" % "runtime"
  )

  lazy val pack = Seq(packMain := Map(
    "blue-server-start" -> "org.apache.felix.main.Main",
    "blue-server-stop" -> "gnieh.blue.BlueServerStop")
  )

  lazy val common =
    (Project(id = "blue-common", base = file("blue-common"))
      settings (
        libraryDependencies ++= commonDependencies
      )
    )

  lazy val commonDeps = Seq(
    "com.jsuereth" %% "scala-arm" % "1.3",
    "org.osgi" % "org.osgi.core" % "4.3.0" % "provided",
    "org.osgi" % "org.osgi.compendium" % "4.3.0" % "provided"
  )

  lazy val nonOsgoDeps = Seq(
  )

  lazy val commonDependencies = commonDeps ++ Seq(
    "org.gnieh" %% "sohva-client" % "0.3",
    "org.gnieh" %% "tiscaf" % "0.8-SNAPSHOT",
    "commons-io" % "commons-io" % "1.4",
    "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7",
    "com.typesafe" % "config" %"1.0.1",
    "com.typesafe.akka" %% "akka-osgi" % "2.2.0",
    "org.apache.pdfbox" % "pdfbox" % "1.8.2" exclude("commons-logging", "commons-logging"),
    "ch.qos.logback" % "logback-classic" % "1.0.10",
    "commons-beanutils" % "commons-beanutils" % "1.8.3" exclude("commons-logging", "commons-logging"),
    "commons-collections" % "commons-collections" % "3.2.1",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "3.0.0.201306101825-r",
    "javax.mail" % "mail" % "1.4.6",
    "org.fusesource.scalate" %% "scalate-core" % "1.6.1"
  )

  lazy val mobwrite =
    (Project(id = "blue-mobwrite",
      base = file("blue-mobwrite"))
      settings (
        libraryDependencies ++= commonDeps
      )
    ) dependsOn(common)

  lazy val http =
    (Project(id = "blue-http",
      base = file("blue-http"))
      settings (
        libraryDependencies ++= commonDeps
      )
    ) dependsOn(common, mobwrite, compile)

  lazy val compile =
    (Project(id = "blue-compile",
      base = file("blue-compile"))
      settings (
        libraryDependencies ++= commonDeps
      )
    ) dependsOn(common, mobwrite)

  lazy val sync =
    (Project(id = "blue-sync",
      base = file("blue-sync"))
      settings (
        libraryDependencies ++= commonDeps
      )
    ) dependsOn(common)

}
