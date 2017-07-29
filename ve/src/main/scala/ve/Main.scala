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
import noisecluster.jvm.control.cluster.TargetService

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

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
      Some(clusterConfig)
    )

    sys.addShutdownHook {
      service.shutdown()
    }
  }
}
