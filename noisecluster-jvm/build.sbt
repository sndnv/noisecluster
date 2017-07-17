import sbt.Keys._

name in ThisBuild := "noisecluster"
licenses in ThisBuild := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage in ThisBuild := Some(url("https://github.com/sndnv/noisecluster"))

scalaVersion in ThisBuild := "2.12.2"

lazy val akkaVersion = "2.5.3"

lazy val noisecluster = (project in file("."))
  .settings(
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    libraryDependencies ++= Seq(
      "io.aeron"           % "aeron-all"          % "1.0.5",
      "com.typesafe.akka" %% "akka-actor"         % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"       % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
    )
  )
