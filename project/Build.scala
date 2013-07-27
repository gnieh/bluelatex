package blue

import sbt._
import Keys._
import xerial.sbt.Pack._
import com.typesafe.sbt.osgi.SbtOsgi._

object BlueBuild extends Build {

  val blueVersion = "0.7-SNAPSHOT"

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
    settings(pack: _*)
  ) aggregate(common, http, compile, mobwrite, sync)

  lazy val compileOptions = scalacOptions in ThisBuild ++=
      Seq("-deprecation", "-feature")

  lazy val blueDependencies = Seq(
    "org.apache.felix" % "org.apache.felix.main" % "4.2.1" % "runtime"
  )

  lazy val pack = Seq(packMain in (Compile, packageBin) := Map(
    "blue-server-start" -> "gnieh.blue.BlueServer",
    "blue-server-stop" -> "gnieh.blue.BlueServerStop")
  )

  lazy val common =
    (Project(id = "blue-common", base = file("blue-common"))
      settings (
        libraryDependencies ++= commonDependencies
      )
      settings(osgiSettings: _*)
      settings(
        OsgiKeys.bundleActivator := Some("gnieh.blue.impl.BlueActivator"),
        OsgiKeys.bundleSymbolicName := "org.gnieh.blue.common",
        OsgiKeys.exportPackage := Seq(
          "gnieh.blue",
          "gnieh.blue.util"
        ),
        OsgiKeys.privatePackage := Seq(
          "gnieh.blue.impl"
        ),
        OsgiKeys.additionalHeaders := Map(
          "Bundle-Name" -> "\\BlueLaTeX Server Core"
        )
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
      settings(osgiSettings: _*)
      settings (
        libraryDependencies ++= commonDeps,
        OsgiKeys.bundleSymbolicName := "org.gnieh.blue.mobwrite",
        OsgiKeys.bundleActivator := Some("gnieh.blue.mobwrite.impl.MobwriteActivator"),
        OsgiKeys.exportPackage := Seq(
          "gnieh.blue.mobwrite"
        ),
        OsgiKeys.privatePackage := Seq(
          "gnieh.blue.mobwrite.impl"
        ),
        OsgiKeys.additionalHeaders := Map(
          "Provide-Capability" -> "org.gnieh.blue.sync"
        )
      )
    ) dependsOn(common)

  lazy val http =
    (Project(id = "blue-http",
      base = file("blue-http"))
      settings(osgiSettings: _*)
      settings (
        libraryDependencies ++= commonDeps,
        OsgiKeys.bundleSymbolicName := "org.gnieh.blue.http",
        OsgiKeys.bundleActivator := Some("gnieh.blue.mobwrite.impl.HttpActivator"),
        OsgiKeys.exportPackage := Seq(
          "gnieh.blue.http"
        ),
        OsgiKeys.privatePackage := Seq(
          "gnieh.blue.http.impl"
        )
      )
    ) dependsOn(common, mobwrite, compile)

  lazy val compile =
    (Project(id = "blue-compile",
      base = file("blue-compile"))
      settings(osgiSettings: _*)
      settings (
        libraryDependencies ++= commonDeps,
        OsgiKeys.bundleSymbolicName := "org.gnieh.blue.compile",
        OsgiKeys.bundleActivator := Some("gnieh.blue.compile.impl.CompileActivator"),
        OsgiKeys.exportPackage := Seq(
          "gnieh.blue.compile"
        ),
        OsgiKeys.privatePackage := Seq(
          "gnieh.blue.http.compile"
        )
      )
    ) dependsOn(common, mobwrite)

  lazy val sync =
    (Project(id = "blue-sync",
      base = file("blue-sync"))
      settings(osgiSettings: _*)
      settings (
        libraryDependencies ++= commonDeps,
        OsgiKeys.bundleSymbolicName := "org.gnieh.blue.sync",
        OsgiKeys.bundleActivator := Some("gnieh.blue.sync.impl.SyncServerActivator"),
        OsgiKeys.exportPackage := Seq(
          "gnieh.blue.sync"
        ),
        OsgiKeys.privatePackage := Seq(
          "gnieh.blue.sync.impl"
        ),
        OsgiKeys.additionalHeaders := Map(
          "Provide-Capability" -> "org.gnieh.blue.sync"
        )
      )
    ) dependsOn(common)

}
