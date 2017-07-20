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
package noisecluster.control.cluster

import java.time.LocalDateTime

import akka.pattern.{ask, pipe}
import akka.actor.{Address, Props}
import akka.cluster.MemberStatus
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.DistributedPubSubMediator._
import akka.util.Timeout
import noisecluster.control._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class SourceMessenger(private val pingInterval: FiniteDuration)(implicit ec: ExecutionContext, timeout: Timeout) extends Messenger {
  private var targets = Map.empty[String, Option[NodeInfo]]
  private var targetsByAddress = Map.empty[Address, String]
  private var pingsSent: Int = 0
  private var pongsReceived: Int = 0

  context.system.scheduler.schedule(pingInterval, pingInterval) {
    targets.keys.foreach {
      targetName =>
        mediatorRef ! Send(s"/user/$targetName", Messages.Ping(), localAffinity = false)
        pingsSent += 1
    }
  }

  override def receive: Receive = {
    case SourceMessenger.ForwardMessage(target, message) =>
      target match {
        case Some(targetName) =>
          if (targets.contains(targetName)) {
            (mediatorRef ? Send(s"/user/$targetName", message, localAffinity = false)).mapTo[Boolean] pipeTo sender
          } else {
            val logMessage = s"Failed to send message [$message] to unregistered target node [$targetName]"
            log.error(logMessage)
            sender ! Future.failed(new RuntimeException(logMessage))
          }

        case None =>
          Future.sequence(
            targets.keys.map {
              targetName =>
                (mediatorRef ? Send(s"/user/$targetName", message, localAffinity = false)).mapTo[Boolean].recover {
                  case NonFatal(e) =>
                    log.error("Target [{}] responded with exception to message [{}]: [{}]", targetName, message, e)
                    false
                }
            }
          ).map(_.forall(identity)) pipeTo sender
      }

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

        if(!targets.contains(targetName)) {
          log.warning("Received [MemberRemoved] event for unregistered target node [{}]", targetName)
        }
        else {
          targets -= targetName
          targetsByAddress -= targetAddress
        }
      }

    case Messages.RegisterTarget() =>
      val targetName = sender.path.name
      val targetAddress = sender.path.address

      if(targets.contains(targetName)) {
        log.warning("Registration message received for already registered target node [{}] with address [{}]", targetName, targetAddress)
      }
      else {
        targets += targetName -> None
        targetsByAddress += targetAddress -> targetName
        log.info("Registered target [{}] with address [{}]", targetName, targetAddress)
      }

    case message: Messages.Pong =>
      val targetName = sender.path.name
      if(targets.contains(targetName)) {
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

  def props(pingInterval: FiniteDuration)(implicit ec: ExecutionContext, timeout: Timeout): Props = Props(classOf[SourceMessenger], pingInterval, ec, timeout)
}
