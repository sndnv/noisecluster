import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt.Keys._

name in ThisBuild := "noisecluster"
licenses in ThisBuild := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage in ThisBuild := Some(url("https://github.com/sndnv/noisecluster"))

scalaVersion in ThisBuild := "2.12.2"

lazy val akkaVersion = "2.5.3"

lazy val noisecluster = (project in file("."))
  .settings(SbtMultiJvm.multiJvmSettings)
  .settings(
    crossScalaVersions := Seq("2.11.11", "2.12.2"),
    libraryDependencies ++= Seq(
      "io.aeron"           % "aeron-client"             % "1.3.0",
      "io.aeron"           % "aeron-driver"             % "1.3.0",
      "com.typesafe.akka" %% "akka-actor"               % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster"             % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools"       % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit"  % akkaVersion % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.3"     % Test
    ),
    compile in MultiJvm := ((compile in MultiJvm) triggeredBy (compile in Test)).value,
    executeTests in Test := {
      val testResults = (executeTests in Test).value
      val multiNodeResults = (executeTests in MultiJvm).value

      Tests.Output(
        if (testResults.overall.id < multiNodeResults.overall.id) {
          multiNodeResults.overall
        } else {
          testResults.overall
        },
        testResults.events ++ multiNodeResults.events,
        testResults.summaries ++ multiNodeResults.summaries
      )
    },
    logBuffered in Test := false,
    parallelExecution in Test := false
  )
  .configs(MultiJvm)
