package blue

import sbt._
import Keys._

object PackOsgi {

  lazy val pack = TaskKey[File]("pack", "create a distributable package of \\BlueLaTeX")

  private def packTask = pack <<=

}
