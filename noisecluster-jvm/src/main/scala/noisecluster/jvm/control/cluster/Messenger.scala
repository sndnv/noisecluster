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

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import noisecluster.jvm.control.{LocalHandlers, ServiceLevel, ServiceState}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

abstract class Messenger(private val localHandlers: LocalHandlers)(implicit ec: ExecutionContext) extends Actor with ActorLogging {
  //Cluster Setup
  protected val clusterRef = Cluster(context.system)

  override def preStart(): Unit = clusterRef.subscribe(self, classOf[MemberEvent])

  override def postStop(): Unit = clusterRef.unsubscribe(self)

  //Mediator Setup
  protected val mediatorRef: ActorRef = DistributedPubSub(context.system).mediator
  mediatorRef ! Put(self)

  //Node Setup
  private var localState = NodeState(
    audio = ServiceState.Stopped,
    transport = ServiceState.Stopped,
    application = ServiceState.Active,
    host = ServiceState.Active
  )

  private def updateLocalState(level: ServiceLevel, state: ServiceState): Unit = {
    val newState = level match {
      case ServiceLevel.Audio => localState.copy(audio = state)
      case ServiceLevel.Transport => localState.copy(transport = state)
      case ServiceLevel.Application => localState.copy(application = state)
      case ServiceLevel.Host => localState.copy(host = state)
    }

    localState = newState
  }

  protected def getLocalState: NodeState = localState

  private var receivers: Actor.Receive = {
    case Messages.StartAudio(formatContainer) =>
      updateLocalState(ServiceLevel.Audio, ServiceState.Starting)

      localHandlers
        .startAudio(formatContainer)
        .map {
          result =>
            self ! Messenger.UpdateLocalState(ServiceLevel.Audio, ServiceState.Active)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting audio with format [{}]: [{}]", formatContainer, e)
            throw e
        }

    case Messages.StopAudio(restart) =>
      updateLocalState(ServiceLevel.Audio, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopAudio(restart)
        .map {
          result =>
            self ! Messenger.UpdateLocalState(ServiceLevel.Audio, ServiceState.Stopped)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] audio: [{}]", if (restart) "restarting" else "stopping", e)
            throw e
        }

    case Messages.StartTransport() =>
      updateLocalState(ServiceLevel.Transport, ServiceState.Starting)

      localHandlers
        .startTransport()
        .map {
          result =>
            self ! Messenger.UpdateLocalState(ServiceLevel.Transport, ServiceState.Active)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting transport: [{}]", e)
            throw e
        }

    case Messages.StopTransport(restart) =>
      updateLocalState(ServiceLevel.Transport, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopTransport(restart)
        .map {
          result =>
            self ! Messenger.UpdateLocalState(ServiceLevel.Transport, ServiceState.Stopped)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] transport: [{}]", if (restart) "restarting" else "stopping", e)
            throw e
        }

    case Messages.StopApplication(restart) =>
      updateLocalState(ServiceLevel.Application, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopApplication(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] application: [{}]", if (restart) "restarting" else "stopping", e)
            throw e
        }

    case Messages.StopHost(restart) =>
      updateLocalState(ServiceLevel.Host, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopHost(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] host: [{}]", if (restart) "restarting" else "stopping", e)
            throw e
        }

    case Messenger.UpdateLocalState(level, state) =>
      updateLocalState(level, state)
  }

  protected def addReceiver(next: Actor.Receive): Unit = {
    receivers = receivers orElse next
  }

  override def receive: Receive = receivers
}

object Messenger {

  private case class UpdateLocalState(level: ServiceLevel, state: ServiceState)

}
