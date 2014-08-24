package blue

import sbt._
import Keys._

import complete.DefaultParsers._
import complete.Parser

import sbtassembly.Plugin.AssemblyKeys.assembly

trait Distrib {
  this: BlueBuild =>

  lazy val blueDistInstallDir =
    settingKey[String]("the path where the packaged distribution is installed")

  lazy val blueDistConfDir =
    settingKey[String]("the path where the configuration will be located after installation")

  lazy val blueDistDataDir =
    settingKey[String]("the path where the data will be stored after installation")

  lazy val blueDistLogDir =
    settingKey[String]("the path where the log file is created after installation")

  lazy val blueDistUser =
    settingKey[String]("the name of the system user under which \\BlueLaTeX is running")

  val blueDistribSettings: Seq[Def.Setting[_]] =
    Seq(
      blueDistInstallDir <<= version(v => s"/opt/bluelatex-$v"),
      blueDistConfDir := "/etc/opt/bluelatex",
      blueDistDataDir := "/var/opt/bluelatex",
      blueDistLogDir := "/var/log/bluelatex",
      blueDistUser := "bluelatex",
      commands ++= Seq(mkDistribution)
    )

  lazy val blueDist = Project(id = "blue-dist",
    base = file("blue-dist")) settings(blueDistribSettings: _*)

  private def distribParam(params: DistribParameters) =
    token(for(_ <- OptSpace ~ "--no-systemd")
      yield params.copy(systemd = false)) |
    token(for(_ <- OptSpace ~ "--no-jsvc")
      yield params.copy(jsvc = false))

  private def distribParams(params: DistribParameters): Parser[DistribParameters] =
    (for {
      params1 <- distribParam(params)
      params2 <- distribParams(params1)
    } yield params2) |
    success(params)

  def mkDistribution = Command("makeDistribution")(_ => distribParams(DistribParameters())) {
    case (state, params) =>
      // first pack the bluelatex project
      Project.runTask(bluePack in bluelatex, state) match {
        case Some((state, Value(p))) =>

          val extracted = Project.extract(state)
          import extracted._

          val st = for {
            (state, Value(packaged)) <- Project.runTask(assembly in blueLauncher, state)
            targetDir <- target in blueDist get structure.data
            blueVersion <- version in bluelatex get structure.data
          } yield {

            // move the packed files to the new working location
            val packed = targetDir / s"bluelatex-$blueVersion"
            IO.delete(packed)
            IO.move(p, packed)

            IO.copyFile(packaged, packed / "bin" / packaged.getName)

            // pack the default configuration and installation script
            for(source <- sourceDirectory in blueDist get structure.data) {
              IO.copyDirectory(source / "configuration", packed / "conf", overwrite = true)
              replaceKeys(packed / "conf", state)
              IO.copyFile(source / "shell" / "install.sh", packed / "install.sh")
              (packed / "install.sh").setExecutable(true)
              replaceKeys(packed / "install.sh", state)
            }

            // pack the systemd unit if required
            if(params.systemd)
              systemdDist(packed, state)

            // packe the jsvc scripts if required
            if(params.jsvc)
              jsvcDist(packed, state)

            // create the tarball and zipball of the distribution
            Process(s"tar zcvf bluelatex-$blueVersion.tgz ${packed.getName}", Some(targetDir)).!
            Process(s"zip -r bluelatex-$blueVersion.zip ${packed.getName}", Some(targetDir)).!

            state
          }

          st.getOrElse(state)

        case _ =>
          state
      }
  }

  def replaceKeys(file: File, state: State): Unit = {
    val extracted = Project.extract(state)
    import extracted._

    for {
      installDir <- blueDistInstallDir in blueDist get structure.data
      confDir <- blueDistConfDir in blueDist get structure.data
      dataDir <- blueDistDataDir in blueDist get structure.data
      logDir <- blueDistLogDir in blueDist get structure.data
      user <- blueDistUser in blueDist get structure.data
    } {
      if(file.isDirectory) {
        for(f <- IO.listFiles(file))
          replaceKeys(f, state)
      } else {
        sed.replaceAll(file, "\\$install_dir", installDir)
        sed.replaceAll(file, "\\$conf_dir", confDir)
        sed.replaceAll(file, "\\$data_dir", dataDir)
        sed.replaceAll(file, "\\$log_dir", logDir)
        sed.replaceAll(file, "\\$blue_user", user)
      }
    }
  }

  def systemdDist(packed: File, state: State): Unit = {

    val extracted = Project.extract(state)
    import extracted._

    // get the source directory for the launcher project
    for(source <- sourceDirectory in blueDist get structure.data) {
      // and copy the systemd unit and replace keys by their values
      val unitDir = packed / "init" / "systemd"
      val unitFile = unitDir / "bluelatex.service"
      unitDir.mkdirs
      IO.copyFile(source / "systemd" / "bluelatex.service", unitFile)
      replaceKeys(unitFile, state)
    }

  }

  def jsvcDist(packed: File, state: State): Unit = {

    val extracted = Project.extract(state)
    import extracted._

    val jsvcDir = packed / "init" / "jsvc"
    jsvcDir.mkdirs

    for {
      source <- sourceDirectory in blueDist get structure.data
      script <- IO.listFiles(source / "jsvc")
    } {
      val scriptTarget =  jsvcDir / script.getName
      IO.copyFile(script, scriptTarget)
      replaceKeys(scriptTarget, state)
    }

  }

}

case class DistribParameters(jsvc: Boolean = true, systemd: Boolean = true, minify: Boolean = false)
