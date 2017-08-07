/**
  * Copyright 2017 https://github.com/sndnv
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package noisecluster_multi_jvm.test.specs.control.cluster

import akka.remote.testconductor.RoleName
import akka.remote.testkit._
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import noisecluster.jvm.control.cluster._
import noisecluster.jvm.control.{LocalHandlers, ServiceState}
import noisecluster.jvm.test.utils._
import org.scalatest.concurrent.Eventually
import org.scalatest.{AsyncWordSpecLike, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ServiceTestConfig extends MultiNodeConfig {
  val systemName: String = "testClusterSpec"
  val clusterPort: Int = 20001

  val baseServiceConfig: Config = ConfigFactory.load()
    .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(Seq(s"akka.tcp://$systemName@localhost:$clusterPort").asJava))
    .withValue("akka.cluster.seed-node-timeout", ConfigValueFactory.fromAnyRef("15s"))

  val sourceNode: RoleName = role(s"source1")
  val targetNode1: RoleName = role(s"target1")
  val targetNode2: RoleName = role(s"target2")

  def getLocalNodeRoles(node: RoleName): Seq[String] = {
    node match {
      case x if x == sourceNode => Seq("source")
      case x if x == targetNode1 => Seq("target")
      case x if x == targetNode2 => Seq("target")
    }
  }

  var calls_total: Int = 0
  var calls_startAudio: Int = 0
  var calls_stopAudio: Int = 0
  var calls_startTransport: Int = 0
  var calls_stopTransport: Int = 0
  var calls_stopApplication: Int = 0
  var calls_restartApplication: Int = 0
  var calls_stopHost: Int = 0
  var calls_restartHost: Int = 0
  var calls_setVolume: Int = 0
  var calls_toggleMute: Int = 0

  val testHandlers: LocalHandlers = new LocalHandlers {
    override def startAudio(): Future[Boolean] = {
      calls_total += 1
      calls_startAudio += 1
      Future.successful(true)
    }

    override def startTransport(): Future[Boolean] = {
      calls_total += 1
      calls_startTransport += 1
      Future.successful(true)
    }

    override def stopApplication(restart: Boolean): Future[Boolean] = {
      calls_total += 1
      if (restart) calls_restartApplication += 1
      else calls_stopApplication += 1

      Future.successful(true)
    }

    override def stopAudio(): Future[Boolean] = {
      calls_total += 1
      calls_stopAudio += 1

      Future.successful(true)
    }

    override def stopHost(restart: Boolean): Future[Boolean] = {
      calls_total += 1
      if (restart) calls_restartHost += 1
      else calls_stopHost += 1

      Future.successful(true)
    }

    override def stopTransport(): Future[Boolean] = {
      calls_total += 1
      calls_stopTransport += 1

      Future.successful(true)
    }

    override def setHostVolume(level: Int): Future[Boolean] = {
      calls_total += 1
      calls_setVolume += 1

      Future.successful(true)
    }

    override def muteHost(): Future[Boolean] = {
      calls_total += 1
      calls_toggleMute += 1

      Future.successful(true)
    }

    override def unmuteHost(): Future[Boolean] = {
      calls_total += 1
      calls_toggleMute += 1

      Future.successful(true)
    }
  }
}

class ServiceSpec extends MultiNodeSpec(ServiceTestConfig) with AsyncWordSpecLike with Matchers with Eventually {
  override def initialParticipants: Int = roles.size

  import ServiceTestConfig._

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val timeout: Timeout = 5.seconds
  private val pingInterval: FiniteDuration = 3.seconds

  private val service: Service =
    myself match {
      case x if x == sourceNode =>
        new SourceService(
          systemName,
          myself.name,
          pingInterval,
          testHandlers,
          Some(
            baseServiceConfig
              .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(clusterPort))
              .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Seq("source").asJava))
          )
        )

      case x if x == targetNode1 =>
        new TargetService(
          systemName,
          myself.name,
          testHandlers,
          Some(
            baseServiceConfig
              .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Seq("target").asJava))
          )
        )

      case x if x == targetNode2 =>
        new TargetService(
          systemName,
          myself.name,
          testHandlers,
          Some(
            baseServiceConfig
              .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Seq("target").asJava))
          )
        )
    }

  enterBarrier("setup")

  waitUntil(what = "all nodes are active", waitTimeMs = 1000, waitAttempts = 15) {
    service.activeSources == 1 && service.activeTargets == (initialParticipants - 1)
  }

  enterBarrier("cluster-join")

  "A cluster Service" should {
    "successfully have targets accept commands from a source" in {
      if (myself == sourceNode) {
        val sourceService = service.asInstanceOf[SourceService]
        sourceService.forwardMessage(targetNode1.name, Messages.StartAudio())
        sourceService.forwardMessage(targetNode2.name, Messages.StopAudio())
        sourceService.forwardMessage(Messages.StopAudio())
        sourceService.forwardMessage(targetNode1.name, Messages.StartTransport())
        sourceService.forwardMessage(targetNode2.name, Messages.StartTransport())
        sourceService.forwardMessage(Messages.StopTransport())
        sourceService.forwardMessage(targetNode1.name, Messages.StopApplication(restart = false))
        sourceService.forwardMessage(targetNode2.name, Messages.StopApplication(restart = true))
        sourceService.forwardMessage(Messages.StopHost(restart = true))
        sourceService.forwardMessage(Messages.SetHostVolume(level = 24))
        sourceService.forwardMessage(Messages.MuteHost())
        sourceService.forwardMessage(Messages.UnmuteHost())
      }

      runOn(targetNode1, targetNode2) {
        waitUntil(what = "all messages have been processed", waitTimeMs = 1000, waitAttempts = 15) {
          calls_total == 9
        }
      }

      enterBarrier("post-test-setup-01")

      myself match {
        case x if x == sourceNode =>
          calls_startAudio should be(0)
          calls_stopAudio should be(0)
          calls_startTransport should be(0)
          calls_stopTransport should be(0)
          calls_stopApplication should be(0)
          calls_restartApplication should be(0)
          calls_stopHost should be(0)
          calls_restartHost should be(0)
          calls_setVolume should be(0)
          calls_toggleMute should be(0)

        case x if x == targetNode1 =>
          calls_startAudio should be(1)
          calls_stopAudio should be(1)
          calls_startTransport should be(1)
          calls_stopTransport should be(1)
          calls_stopApplication should be(1)
          calls_restartApplication should be(0)
          calls_stopHost should be(0)
          calls_restartHost should be(1)
          calls_setVolume should be(1)
          calls_toggleMute should be(2)

        case x if x == targetNode2 =>
          calls_startAudio should be(0)
          calls_stopAudio should be(2)
          calls_startTransport should be(1)
          calls_stopTransport should be(1)
          calls_stopApplication should be(0)
          calls_restartApplication should be(1)
          calls_stopHost should be(0)
          calls_restartHost should be(1)
          calls_setVolume should be(1)
          calls_toggleMute should be(2)
      }
    }

    "successfully have targets report to a source" in {
      myself match {
        case x if x == sourceNode =>
          waitUntil(what = "a few status messages have been exchanged", waitTimeMs = 1000, waitAttempts = 15) {
            service.asInstanceOf[SourceService].getClusterState.await.pongs >= 4
          }

          service.asInstanceOf[SourceService].getClusterState.map {
            state =>
              state.localSource.audio should be(ServiceState.Stopped)
              state.localSource.transport should be(ServiceState.Stopped)
              state.localSource.application should be(ServiceState.Active)
              state.localSource.host should be(ServiceState.Active)

              state.targets.keys should contain(s"$TargetActorNamePrefix${targetNode1.name}")
              state.targets.keys should contain(s"$TargetActorNamePrefix${targetNode2.name}")

              state.pings should be(state.pongs)
          }

        case x if x == targetNode1 => //do nothing

        case x if x == targetNode2 => //do nothing
      }

      enterBarrier("post-test-03")
      succeed
    }
  }
}

class ServiceMultiJvmSourceNode extends ServiceSpec

class ServiceMultiJvmTargetNode1 extends ServiceSpec

class ServiceMultiJvmTargetNode2 extends ServiceSpec
