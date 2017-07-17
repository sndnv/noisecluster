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
package noisecluster.control

import akka.actor.{Actor, ActorLogging, ActorSystem, Address, AddressFromURIString, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

class ClusterService(
  private val systemName: String,
  private val actorProvider: String,
  private val localPort: Int,
  private val clusterHost: String,
  private val clusterPort: Int,
  private val handlers: LocalHandlers
) {
  private val isMaster = localPort == clusterPort

  private val config = ConfigFactory.load()
    .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef(actorProvider))
    .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(localPort))

  private val system = ActorSystem(systemName, config)
  private val cluster = Cluster(system)
  private val messenger = system.actorOf(ClusterMessenger.props(handlers))
  private val clusterAddress = if(isMaster) {
    cluster.selfAddress
  } else {
    AddressFromURIString(s"akka.tcp://$systemName@$clusterHost:$clusterPort")
  }

  cluster.join(clusterAddress)

  def startTransport(): Unit = {
    //TODO
  }

  def stopTransport(): Unit = {
    //TODO
  }

  def restart(level: ServiceLevel): Unit = {
    //TODO
  }

  def terminate(): Unit = {
    //TODO - notify all nodes
    cluster.leave(cluster.selfAddress)
    system.terminate()
  }
}

object ClusterService {

}