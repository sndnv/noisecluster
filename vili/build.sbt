import sbt.Keys._

name in ThisBuild := "vili"
licenses in ThisBuild := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage in ThisBuild := Some(url("https://github.com/sndnv/noisecluster"))

scalaVersion in ThisBuild := "2.12.2"

lazy val vili = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      guice,
      "com.interelgroup" %% "core3" % "2.1.0",
      "org.webjars" % "jquery" % "3.0.0",
      "noisecluster" %% "noisecluster" % "0.0.1-SNAPSHOT"
    )
  )
  .enablePlugins(PlayScala)

//loads the Play project at sbt startup
onLoad in Global := (Command.process("project vili", _: State)) compose (onLoad in Global).value
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")
