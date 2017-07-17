import sbt.Keys._

name in ThisBuild := "ve"
licenses in ThisBuild := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage in ThisBuild := Some(url("https://github.com/sndnv/noisecluster"))

scalaVersion in ThisBuild := "2.12.2"

lazy val ve = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "io.aeron" % "aeron-all" % "1.0.5",
      "noisecluster" %% "noisecluster" % "0.0.1-SNAPSHOT"
    )
  )