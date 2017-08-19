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

import akka.actor.{Address, Props}
import akka.cluster.ClusterEvent.{MemberRemoved, UnreachableMember}
import akka.pattern.pipe
import noisecluster.jvm.control._
import noisecluster.jvm.control.cluster.Messages._

import scala.concurrent.ExecutionContext

class TargetMessenger(
  private val localHandlers: LocalHandlers,
  private val lastSourceDownAction: Option[NodeAction]
)(implicit ec: ExecutionContext) extends Messenger(localHandlers) {
  private var sources = Map.empty[String, Address]

  private def handleLastSourceDown(service: ServiceLevel, action: ServiceAction): Unit = {
    val messages: Seq[Messages.ControlMessage] = getMessagesForServiceAction(service, action)

    if(messages.isEmpty) {
      log.error("Cannot perform action [{}] for service [{}]", action, service)
    } else {
      messages.foreach(message => self ! message)
    }
  }

  addReceiver {
    //Cluster Management
    case UnreachableMember(member) =>
      if (member.hasRole("source") && sources.exists(_._2 == member.address) && sources.size == 1) {
        lastSourceDownAction match {
          case Some(action) =>
            action.delay match {
              case Some(delay) =>
                val memberAddress = member.address
                context.system.scheduler.scheduleOnce(delay) {
                  if (clusterRef.state.unreachable.exists(_.address == memberAddress)) {
                    handleLastSourceDown(action.service, action.action)
                  }
                }

              case None =>
                handleLastSourceDown(action.service, action.action)
            }

          case None => //do nothing
        }
      }

    case MemberRemoved(member, _) =>
      if (member.hasRole("source")) {
        val source = sources.find(_._2 == member.address)

        source match {
          case Some((sourceName, _)) =>
            sources -= sourceName
            if (sources.isEmpty) {
              lastSourceDownAction match {
                case Some(action) => handleLastSourceDown(action.service, action.action)
                case None => //do nothing
              }
            }

          case None =>
            log.warning("Received [MemberRemoved] event for unregistered source node [{}]", member.address)
        }
      }

    case Messages.Ping() =>
      getLocalState.map(Pong) pipeTo sender

    case Messages.RegisterSource() =>
      val sourceName = sender.path.name
      val sourceAddress = sender.path.address

      if (!sources.contains(sourceName)) {
        sources += sourceName -> sourceAddress
        sender ! RegisterTarget()
        log.info("Registered source [{}] with address [{}]", sourceName, sourceAddress)
      } else {
        log.warning("Registration message received for already registered source node [{}] with address [{}]", sourceName, sourceAddress)
      }
  }
}

object TargetMessenger {
  def props(
    handlers: LocalHandlers,
    lastSourceDownAction: Option[NodeAction]
  )(implicit ec: ExecutionContext): Props = Props(classOf[TargetMessenger], handlers, lastSourceDownAction, ec)
}
