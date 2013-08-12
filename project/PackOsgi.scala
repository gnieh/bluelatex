package blue

import sbt._
import Keys._
import aQute.bnd.osgi._

trait PackOsgi {

  val Osgi = config("osgi")

  val bndDir: SettingKey[File] =
    SettingKey[File]("bnd-dir", "the directory containing the BND descriptors")

  val scriptDir: SettingKey[File] =
    SettingKey[File]("script-dir", "the directory containing the start/stop scripts")

  val confDir: SettingKey[File] =
    SettingKey[File]("configuration-dir", "the directory containing the configuration")

  val projectBundles: TaskKey[Seq[File]] =
    TaskKey[Seq[File]]("project-bundles", "the project bundle files")

  val depBundles: TaskKey[Seq[File]] =
    TaskKey[Seq[File]]("dependency-bundles", "the project dependencies as bundles")

  val pack: TaskKey[File] =
    TaskKey[File]("pack", "packs the OSGi application")

  val packSettings: Seq[Project.Setting[_]] =
    Seq(
      bndDir <<= baseDirectory(_ / "bnd"),
      scriptDir <<= sourceDirectory(_ / "main" / "script"),
      confDir <<= sourceDirectory(_ / "main" / "configuration"),
      projectBundles <<= (bndDir, target, thisProjectRef, buildStructure) flatMap { (bnddir, target, project, structure) =>
        getFromSelectedProjects(packageBin.task in Runtime)(project, structure, Seq()) map (_ map osgify(bnddir, target))
      },
      depBundles <<= (bndDir, target, thisProjectRef, buildStructure) flatMap { (bnddir, target, project, structure) =>
        getFromSelectedProjects(update.task)(project, structure, Seq()) map (_ flatMap wrapReport(bnddir, target))
      },
      packTask
    )

  def wrapReport(bnddir: File, target: File)(report: UpdateReport): Seq[File] =
    for {
      c <- report.configurations
      if c.configuration == "runtime"
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield osgify(bnddir, target)(file)

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
            wrapper.setExportPackage("*;version=" + version.replaceAll("-SNAPSHOT", ""));
            wrapper.setBundleSymbolicName(name)
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


  def getFromSelectedProjects[T](targetTask: SettingKey[Task[T]])(currentProject: ProjectRef, structure: Load.BuildStructure, exclude: Seq[String]): Task[Seq[T]] = {
    def allProjectRefs(currentProject: ProjectRef): Seq[ProjectRef] = {
      def isExcluded(p: ProjectRef) = exclude.contains(p.project)
      val children = Project.getProject(currentProject, structure).toSeq.flatMap(_.aggregate)
      (currentProject +: (children flatMap (allProjectRefs(_)))) filterNot (isExcluded)
    }

    val projects = allProjectRefs(currentProject)
    projects.flatMap(p => targetTask in p get structure.data).join
  }

  private def packTask = pack <<= (projectBundles, depBundles, scriptDir, confDir, streams, target) map {
    (projectBundles, depBundles, scriptdir, confdir, out, target) =>
      // create the directories
      val packDir = new File(target, "pack")
      val bundleDir = new File(packDir, "bundle")
      val binDir = new File(packDir, "bin")
      val confDir = new File(packDir, "conf")

      if(packDir.exists)
        IO.delete(packDir)
      bundleDir.mkdirs
      binDir.mkdirs
      confDir.mkdirs

      // copy the bundles to the bundle directory
      out.log.info("copy bundles to " + bundleDir.getCanonicalPath)
      for(bundle <- projectBundles ++ depBundles) {
        IO.copyFile(bundle, bundleDir / bundle.getName)
      }

      out.log.info("copy scripts")
      for(script <- IO.listFiles(scriptdir)) {
        IO.copyFile(script, binDir / script.getName)
        (binDir / script.getName).setExecutable(true, false)
      }

      out.log.info("copy configuration")
      for(f <- IO.listFiles(confdir)) {
        IO.copyFile(f, confDir / f.getName)
      }

      packDir
  }

}
