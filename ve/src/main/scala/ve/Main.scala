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
package ve

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import noisecluster.jvm.control.cluster.{NodeAction, TargetService}
import noisecluster.jvm.control.{ServiceAction, ServiceLevel}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    val baseConfig = ConfigFactory.load()
    val appConfig = baseConfig.getConfig("noisecluster.ve")

    val clusterSystemName = appConfig.getString("control.systemName")
    val clusterHost = appConfig.getString("control.cluster.host")
    val clusterPort = appConfig.getInt("control.cluster.port")
    val clusterAddress = s"akka.tcp://$clusterSystemName@$clusterHost:$clusterPort"

    val localHost = appConfig.getString("control.local.host")
    val localPort = appConfig.getInt("control.local.port")
    val lastSourceDownAction: Option[NodeAction] = if(appConfig.hasPath("control.local.actions.lastSourceDown")) {
      val lastSourceDownConfig = appConfig.getConfig("control.local.actions.lastSourceDown")
      Some(
        NodeAction(
          lastSourceDownConfig.getString("service").toLowerCase match {
            case "audio" => ServiceLevel.Audio
            case "transport" => ServiceLevel.Transport
            case "application" => ServiceLevel.Application
            case "host" => ServiceLevel.Host
            case param => throw new IllegalArgumentException(s"Node action service [$param] is not supported")
          },
          lastSourceDownConfig.getString("action").toLowerCase match {
            case "start" => ServiceAction.Start
            case "stop" => ServiceAction.Stop
            case "restart" => ServiceAction.Restart
            case param => throw new IllegalArgumentException(s"Node action [$param] is not supported")
          },
          if(lastSourceDownConfig.hasPath("delay")) {
            Some(lastSourceDownConfig.getInt("delay").seconds)
          } else {
            None
          }
        )
      )
    } else {
      None
    }

    val clusterConfig = baseConfig
      .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(localPort))
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(localHost))
      .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(Seq(clusterAddress).asJava))
      .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Seq("target").asJava))

    implicit val ec = ExecutionContext.Implicits.global
    implicit val system = ActorSystem(clusterSystemName)

    val service = new ApplicationService(appConfig)
    val control = new TargetService(
      clusterSystemName,
      appConfig.getString("control.messengerName"),
      service.localHandlers,
      lastSourceDownAction,
      overrideConfig = Some(clusterConfig)
    )

    sys.addShutdownHook {
      service.shutdown()
    }
  }
}
