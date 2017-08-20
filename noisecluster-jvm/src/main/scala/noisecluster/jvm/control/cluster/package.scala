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
package noisecluster.jvm.control

import akka.actor.Address
import noisecluster.jvm.control.cluster.Messages._

import scala.concurrent.duration.FiniteDuration

package object cluster {
  val SourceActorNamePrefix: String = "source_"
  val TargetActorNamePrefix: String = "target_"

  /**
    * Container representing an action to be taken by a node after a specified (optional) amount of time.
    *
    * @param service the service to be affected by the action (audio, transport, application, host)
    * @param action  the action to take (start, stop, restart)
    * @param delay   the amount of time to wait before executing the action (set to None for immediate execution)
    */
  case class NodeAction(
    service: ServiceLevel,
    action: ServiceAction,
    delay: Option[FiniteDuration] = None
  )

  /**
    * Container representing a node's state.
    *
    * @param audio       the state of the audio service
    * @param transport   the state of the transport service
    * @param application the state of the application service
    * @param host        the state of the host service
    * @param volume      the host's master volume
    * @param muted       the host's muted state
    */
  case class NodeState(
    audio: ServiceState,
    transport: ServiceState,
    application: ServiceState,
    host: ServiceState,
    volume: Int,
    muted: Boolean
  )

  /**
    * Container representing a node's state and the timestamp of when the state was recorded.
    *
    * @param state      the node state
    * @param lastUpdate the state timestamp
    */
  case class NodeInfo(
    state: NodeState,
    lastUpdate: java.time.LocalDateTime
  )

  /**
    * Container representing a cluster member's status.
    *
    * @param address the member's address
    * @param roles   the roles the member has
    * @param status  the member's status
    */
  case class MemberInfo(
    address: Address,
    roles: Seq[String],
    status: String
  )

  /**
    * Container representing the cluster's state as seen by a source.
    *
    * @param localSource     the state of the local node
    * @param localAddress    the address of the local node
    * @param targets         the state of all registered targets (name -> node info)
    * @param targetAddresses the addresses of all targets (name -> address)
    * @param pings           the number of pings sent by the local node
    * @param pongs           the number of pongs received by the local node
    * @param leaderAddress   the address of the cluster leader
    * @param members         cluster members info
    */
  case class ClusterState(
    localSource: NodeState,
    localAddress: Address,
    targets: Map[String, Option[NodeInfo]],
    targetAddresses: Map[String, Address],
    pings: Int,
    pongs: Int,
    leaderAddress: Option[Address],
    members: Seq[MemberInfo]
  )

  /**
    * Creates a list of cluster messages to be executed based on the requested service action.
    *
    * @param service the affected service
    * @param action  the action to take
    * @return the list of cluster messages
    */
  def getMessagesForServiceAction(service: ServiceLevel, action: ServiceAction): Seq[Messages.ControlMessage] = {
    service match {
      case ServiceLevel.Audio =>
        action match {
          case ServiceAction.Start => Seq(StartAudio())
          case ServiceAction.Stop => Seq(StopAudio())
          case ServiceAction.Restart => Seq(StopAudio(), StartAudio())
        }

      case ServiceLevel.Transport =>
        action match {
          case ServiceAction.Start => Seq(StartTransport())
          case ServiceAction.Stop => Seq(StopTransport())
          case ServiceAction.Restart => Seq(StopTransport(), StartTransport())
        }

      case ServiceLevel.Application =>
        action match {
          case ServiceAction.Start => Seq.empty
          case ServiceAction.Stop => Seq(StopApplication(restart = false))
          case ServiceAction.Restart => Seq(StopApplication(restart = true))
        }

      case ServiceLevel.Host =>
        action match {
          case ServiceAction.Start => Seq.empty
          case ServiceAction.Stop => Seq(StopHost(restart = false))
          case ServiceAction.Restart => Seq(StopHost(restart = true))
        }
    }
  }
}
