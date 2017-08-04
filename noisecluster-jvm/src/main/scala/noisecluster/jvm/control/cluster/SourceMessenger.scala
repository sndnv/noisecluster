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
package noisecluster.jvm.control.cluster

import java.time.LocalDateTime

import akka.actor.{Address, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import akka.cluster.pubsub.DistributedPubSubMediator._
import noisecluster.jvm.control._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class SourceMessenger(
  private val pingInterval: FiniteDuration,
  private val localHandlers: LocalHandlers
)(implicit ec: ExecutionContext) extends Messenger(localHandlers) {
  private var targets = Map.empty[String, Option[NodeInfo]]
  private var targetsByAddress = Map.empty[Address, String]
  private var pingsSent: Int = 0
  private var pongsReceived: Int = 0
  private var audioStoppedBySystem: Boolean = true

  private val pingSchedule = context.system.scheduler.schedule(pingInterval, pingInterval) {
    targets.keys.foreach {
      targetName =>
        mediatorRef ! Send(s"/user/$targetName", Messages.Ping(), localAffinity = false)
        pingsSent += 1
    }
  }

  override def postStop(): Unit = {
    pingSchedule.cancel()
    super.postStop()
  }

  addReceiver {
    case SourceMessenger.ForwardMessage(target, message) =>
      target match {
        case Some(targetName) =>
          if (targets.contains(targetName)) {
            mediatorRef ! Send(s"/user/$targetName", message, localAffinity = false)
          } else {
            log.error("Failed to send message [{}] to unregistered target node [{}]", message, targetName)
          }

        case None =>
          targets.keys.foreach {
            targetName =>
              mediatorRef ! Send(s"/user/$targetName", message, localAffinity = false)
          }
      }

    case SourceMessenger.GetClusterState() =>
      val members = clusterRef.state.members.map {
        member =>
          MemberInfo(member.address, member.roles.toSeq, member.status.toString)
      }

      sender ! ClusterState(
        getLocalState,
        clusterRef.selfAddress,
        targets.map {
          case (name, info) =>
            (name.split(TargetActorNamePrefix).last, info)
        },
        targetsByAddress.map {
          case (address, name) =>
            (name.split(TargetActorNamePrefix).last, address)
        },
        pingsSent,
        pongsReceived,
        clusterRef.state.leader,
        members.toSeq
      )

    //Cluster Management
    case CurrentClusterState(existingMembers, _, _, _, _) =>
      existingMembers
        .filter(member => member.status == MemberStatus.Up && member.hasRole("target"))
        .foreach {
          target =>
            context.actorSelection(s"${target.address}/user/$TargetActorNamePrefix*") ! Messages.RegisterSource()
            log.info("Registering with target [{}]", target.address)
        }

    case MemberUp(member) =>
      if (member.hasRole("target")) {
        context.actorSelection(s"${member.address}/user/$TargetActorNamePrefix*") ! Messages.RegisterSource()
        log.info("Registering with target [{}]", member.address)
      }

    case MemberRemoved(member, _) =>
      if (member.hasRole("target")) {
        val targetAddress = member.address
        val targetName = targetsByAddress(targetAddress)

        if (!targets.contains(targetName)) {
          log.warning("Received [MemberRemoved] event for unregistered target node [{}]", targetName)
        }
        else {
          targets -= targetName
          targetsByAddress -= targetAddress

          if (targets.isEmpty) {
            self ! Messages.StopTransport(restart = false)
            self ! Messages.StopAudio(restart = false)
            audioStoppedBySystem = true
          }
        }
      }

    case Messages.RegisterTarget() =>
      val targetName = sender.path.name
      val targetAddress = sender.path.address

      if (targets.contains(targetName)) {
        log.warning("Registration message received for already registered target node [{}] with address [{}]", targetName, targetAddress)
      }
      else {
        targets += targetName -> None
        targetsByAddress += targetAddress -> targetName
        log.info("Registered target [{}] with address [{}]", targetName, targetAddress)

        if (targets.size == 1) {
          if (audioStoppedBySystem) {
            self ! Messages.StartAudio(None)
            self ! Messages.StartTransport()
            audioStoppedBySystem = false
          }
        }
      }

    case message: Messages.Pong =>
      val targetName = sender.path.name
      if (targets.contains(targetName)) {
        targets += targetName -> Some(NodeInfo(message.state, LocalDateTime.now()))
        pongsReceived += 1
        log.debug("Received state update [{}] from target [{}] with address [{}]", message, targetName, sender.path.address)
      } else {
        log.warning("Received status update [{}] from an unregistered target node [{}]", message, sender.path)
      }
  }
}

object SourceMessenger {

  case class ForwardMessage(target: Option[String], message: Messages.ControlMessage)

  case class GetClusterState()

  def props(pingInterval: FiniteDuration, localHandlers: LocalHandlers)(implicit ec: ExecutionContext): Props = Props(classOf[SourceMessenger], pingInterval, localHandlers, ec)
}
