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

  case class NodeAction(
    service: ServiceLevel,
    action: ServiceAction,
    delay: Option[FiniteDuration] = None
  )

  case class NodeState(
    audio: ServiceState,
    transport: ServiceState,
    application: ServiceState,
    host: ServiceState,
    volume: Int,
    muted: Boolean
  )

  case class NodeInfo(
    state: NodeState,
    lastUpdate: java.time.LocalDateTime
  )

  case class MemberInfo(
    address: Address,
    roles: Seq[String],
    status: String
  )

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
