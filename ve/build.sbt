import sbt.Keys._

name in ThisBuild := "ve"
licenses in ThisBuild := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage in ThisBuild := Some(url("https://github.com/sndnv/noisecluster"))

scalaVersion in ThisBuild := "2.12.2"

lazy val ve = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.aeron" % "aeron-client" % "1.3.0",
      "io.aeron" % "aeron-driver" % "1.3.0",
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.3",
      "noisecluster" %% "noisecluster" % "1.0.0"
    ),
    mainClass in assembly := Some("ve.Main"),
    assemblyMergeStrategy in assembly := {
      case "application.conf" => MergeStrategy.discard
      case "logback.xml"      => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
