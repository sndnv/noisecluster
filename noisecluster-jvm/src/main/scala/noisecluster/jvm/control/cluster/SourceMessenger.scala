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
import akka.pattern.pipe
import noisecluster.jvm.control._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Source messenger responsible for handling target registrations, message forwarding and status updates.
  *
  * @param pingInterval the time between target status requests
  * @param localHandlers the local system handlers to use
  */
class SourceMessenger(
  private val pingInterval: FiniteDuration,
  private val localHandlers: LocalHandlers
)(implicit ec: ExecutionContext) extends Messenger(localHandlers) {
  private var targets = Map.empty[String, Option[NodeInfo]]
  private var targetsByAddress = Map.empty[Address, String]
  private var pingsSent: Int = 0
  private var pongsReceived: Int = 0

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

  //adds source-specific behaviour
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

      getLocalState.map {
        localState =>
          ClusterState(
            localState,
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
      } pipeTo sender

    //Cluster Management
    case CurrentClusterState(existingMembers, _, _, _, _) =>
      //registers the local source with all existing targets in the cluster
      existingMembers
        .filter(member => member.status == MemberStatus.Up && member.hasRole("target"))
        .foreach {
          target =>
            context.actorSelection(s"${target.address}/user/$TargetActorNamePrefix*") ! Messages.RegisterSource()
            log.info("Registering with target [{}]", target.address)
        }

    case MemberUp(member) =>
      if (member.hasRole("target")) {
        //registers with the newly joined target
        context.actorSelection(s"${member.address}/user/$TargetActorNamePrefix*") ! Messages.RegisterSource()
        log.info("Registering with target [{}]", member.address)
      }

    case MemberRemoved(member, _) =>
      if (member.hasRole("target")) {
        //removes the exiting target
        if (!targetsByAddress.contains(member.address)) {
          log.warning("Received [MemberRemoved] event for unregistered target node [{}]", member.address)
        }
        else {
          targets -= targetsByAddress(member.address)
          targetsByAddress -= member.address
        }
      }

    case Messages.RegisterTarget() =>
      //registers the target with the local source
      val targetName = sender.path.name
      val targetAddress = sender.path.address

      if (targets.contains(targetName)) {
        log.warning("Registration message received for already registered target node [{}] with address [{}]", targetName, targetAddress)
      }
      else {
        targets += targetName -> None
        targetsByAddress += targetAddress -> targetName
        log.info("Registered target [{}] with address [{}]", targetName, targetAddress)
      }

    case message: Messages.Pong =>
      //records the target's status update
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

  /**
    * Message used for forwarding control messages to targets.
    *
    * @param target a list of targets to forward the message to (set to None to forward to all registered targets)
    * @param message the message to forward
    */
  case class ForwardMessage(target: Option[String], message: Messages.ControlMessage)

  /**
    * Message for retrieving the current cluster state, as seen by the local source.
    *
    * @return Future[ [[noisecluster.jvm.control.cluster.ClusterState]] ]
    */
  case class GetClusterState()

  /**
    * Creates a new source messenger actor.
    *
    * @param pingInterval the time between target status requests
    * @param localHandlers the local system handlers to use
    * @return the new actor instance
    */
  def props(
    pingInterval: FiniteDuration,
    localHandlers: LocalHandlers
  )(implicit ec: ExecutionContext): Props = Props(classOf[SourceMessenger], pingInterval, localHandlers, ec)
}
