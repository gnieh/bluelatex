package blue

import sbt._
import Keys._

import aQute.bnd.osgi._
import scala.io.Source

trait Pack {
  this: BlueBuild =>

  val couchPort =
    settingKey[Int]("the port CouchDB is listening to")

  val couchAdmin =
    settingKey[String]("the CouchDB administrator name")

  val couchPassword =
    settingKey[String]("the CouchDB administrator password")

  val couchConf =
    settingKey[(Int, String, String)]("the CouchDB configuration")

  val blueBndDir =
    settingKey[File]("the directory containing the BND descriptors")

  val blueScriptDir =
    settingKey[File]("the directory containing the start/stop scripts")

  val blueConfDir =
    settingKey[File]("the directory containing the configuration")

  val blueTemplateDir =
    settingKey[File]("the directory containing the templates")

  val blueClassDir =
    settingKey[File]("the directory containing the TeX classes")

  val blueDesignDir =
    settingKey[File]("the directory containing the couchdb design documents")

  val blueProjectBundles =
    taskKey[Seq[File]]("the project bundle files")

  val blueDepBundles =
    taskKey[Seq[File]]("the project dependencies as bundles")

  val bluePack =
    taskKey[File]("packs the OSGi application")

  val packSettings: Seq[Def.Setting[_]] =
    Seq(
      couchPort := 15984,
      couchAdmin := "admin",
      couchPassword := "admin",
      couchConf <<= (couchPort, couchAdmin, couchPassword)((_, _, _)),
      blueBndDir <<= baseDirectory(_ / "bnd"),
      blueScriptDir <<= sourceDirectory(_ / "main" / "script"),
      blueConfDir <<= sourceDirectory(_ / "main" / "configuration"),
      blueTemplateDir <<= sourceDirectory(_ / "main" / "templates"),
      blueClassDir <<= sourceDirectory(_ / "main" / "classes"),
      blueDesignDir <<= sourceDirectory(_ / "main" / "designs"),
      blueProjectBundles <<= (thisProjectRef, buildStructure) flatMap { (project, structure) =>
        getFromSelectedProjects(packageBin in Runtime)(project, structure, Seq("bluelatex"))
      },
      blueDepBundles <<= (thisProjectRef, buildStructure) flatMap { (project, structure) =>
        getFromSelectedProjects(update)(project, structure, Seq()) map (_ flatMap wrapReport)
      },
      bluePackTask
    )

  def wrapReport(report: UpdateReport): Seq[File] =
    for {
      c <- report.configurations
      if c.configuration == "runtime"
      m <- c.modules
      (artifact, file) <- m.artifacts
      if DependencyFilter.allPass(c.configuration, m.module, artifact)
    } yield file

  /* make an OSGi bundle out of a jar file */
  def osgify(bnddir: File, target: File)(file: File): File = {
    val jar = new Jar(file)
      // extract the name and version from filename
    file.getName match {
      case library(name, version) =>
        //look if there is some bnd descriptor for this jar
        val descriptor = new File(bnddir, name + ".bnd")
        // it is not an OSGi bundle, wrap it
        if(descriptor.exists || jar.getManifest == null || jar.getBsn == null) {
          val wrapper = new Analyzer
          wrapper.setJar(jar)
          if(descriptor.exists) {
            wrapper.setProperties(descriptor)
          } else {
            wrapper.setImportPackage("*;resolution:=optional");
            wrapper.setExportPackage("*;version=" + version.replaceAll("-SNAPSHOT", "").replace('-', '.'));
            wrapper.setBundleSymbolicName(name)
          }
          if(wrapper.getBundleVersion() == null)
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
        } else {
          // it is already an OSGi bundle, return the file
          file
        }
      case _ =>
        file
    }
  }

  lazy val library =
    """([^_]+)(?:_[0-9](?:.[0-9]+)+)?-([0-9]+(?:.[0-9]+)*(?:-\w+)*).jar""".r


  def getFromSelectedProjects[T](targetTask: TaskKey[T])(currentProject: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[T]] = {
    def allProjectRefs(currentProject: ProjectRef): Seq[ProjectRef] = {
      def isExcluded(p: ProjectRef) = exclude.contains(p.project)
      val children = Project.getProject(currentProject, structure).toSeq.flatMap(_.aggregate)
      (currentProject +: (children flatMap (allProjectRefs(_)))) filterNot (isExcluded)
    }

    val projects = allProjectRefs(currentProject)
    projects.flatMap(p => targetTask in p get structure.data).join
  }

  private def bluePackTask =
    bluePack <<= (blueBndDir, blueProjectBundles, blueDepBundles, blueScriptDir, blueConfDir, couchConf, blueTemplateDir, blueClassDir, blueDesignDir, streams, target) map {
      (bnddir, projectJars, depJars, scriptdir, confdir, couchconf, templatedir, classdir, designdir, out, target) =>
        // create the directories
        val packDir = target / "pack"
        val bundleDir = packDir / "bundle"
        val binDir = packDir / "bin"
        val lastPacked = projectJars.map(_.lastModified).max

        if(!packDir.exists || (packDir.exists && packDir.lastModified < lastPacked)) {

          val projectBundles = projectJars map osgify(bnddir, target)
          val depBundles = depJars map osgify(bnddir, target)

          if(packDir.exists)
            IO.delete(packDir)
          bundleDir.mkdirs
          binDir.mkdirs

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
        }

        out.log.info("copy configuration")
        IO.copyDirectory(confdir, packDir / "conf")
        // set the couchdb port in configuration
        val couchport = couchconf._1
        val couchadmin = couchconf._2
        val couchpassword = couchconf._3
        sed.replaceAll(packDir / "conf" / "org.gnieh.blue.common" / "application.conf", "\\$couchPort", couchport.toString)
        sed.replaceAll(packDir / "conf" / "org.gnieh.blue.common" / "application.conf", "\\$couchAdmin", couchadmin)
        sed.replaceAll(packDir / "conf" / "org.gnieh.blue.common" / "application.conf", "\\$couchPassword", couchpassword)

        out.log.info("copy templates")
        IO.copyDirectory(templatedir, packDir / "data" / "templates")

        (packDir / "data").mkdir

        out.log.info("copy classes")
        IO.copyDirectory(classdir, packDir / "data" / "classes")

        out.log.info("create paper dir")
        (packDir / "data" / "papers").mkdir

        out.log.info("copy designs")
        IO.copyDirectory(designdir, packDir / "data" / "designs")

        packDir
    }

}
