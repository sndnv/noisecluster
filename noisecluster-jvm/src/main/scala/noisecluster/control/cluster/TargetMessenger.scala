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

import akka.actor.{Address, Props}
import noisecluster.control.cluster.Messages.{Pong, RegisterTarget}
import noisecluster.control._

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class TargetMessenger(private val localHandlers: LocalHandlers)(implicit ec: ExecutionContext) extends Messenger {
  private var sources = Map.empty[String, Address]

  private var localState = NodeState(
    audio = ServiceState.Stopped,
    transport = ServiceState.Stopped,
    application = ServiceState.Active,
    host = ServiceState.Active
  )

  private def updateState(level: ServiceLevel, state: ServiceState): Unit = {
    val newState = level match {
      case ServiceLevel.Audio => localState.copy(audio = state)
      case ServiceLevel.Transport => localState.copy(transport = state)
      case ServiceLevel.Application => localState.copy(application = state)
      case ServiceLevel.Host => localState.copy(host = state)
    }

    localState = newState
  }

  override def receive: Receive = {
    case Messages.StartAudio(formatContainer) =>
      updateState(ServiceLevel.Audio, ServiceState.Starting)
      localHandlers
        .startAudio(formatContainer)
        .map(_ => self ! TargetMessenger.UpdateLocalState(ServiceLevel.Audio, ServiceState.Active))
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting audio with format [{}]: [{}]", formatContainer, e)
        }

    case Messages.StopAudio(restart) =>
      updateState(ServiceLevel.Audio, if (restart) ServiceState.Restarting else ServiceState.Stopping)
      localHandlers
        .stopAudio(restart)
        .map(_ => self ! TargetMessenger.UpdateLocalState(ServiceLevel.Audio, ServiceState.Stopped))
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] audio: [{}]", if (restart) "restarting" else "stopping", e)
        }

    case Messages.StartTransport() =>
      updateState(ServiceLevel.Transport, ServiceState.Starting)
      localHandlers
        .startTransport()
        .map(_ => self ! TargetMessenger.UpdateLocalState(ServiceLevel.Transport, ServiceState.Active))
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting transport: [{}]", e)
        }

    case Messages.StopTransport(restart) =>
      updateState(ServiceLevel.Transport, if (restart) ServiceState.Restarting else ServiceState.Stopping)
      localHandlers
        .stopTransport(restart)
        .map(_ => self ! TargetMessenger.UpdateLocalState(ServiceLevel.Transport, ServiceState.Stopped))
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] transport: [{}]", if (restart) "restarting" else "stopping", e)
        }

    case Messages.StopApplication(restart) =>
      updateState(ServiceLevel.Application, if (restart) ServiceState.Restarting else ServiceState.Stopping)
      localHandlers
        .stopApplication(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] application: [{}]", if (restart) "restarting" else "stopping", e)
        }

    case Messages.StopHost(restart) =>
      updateState(ServiceLevel.Host, if (restart) ServiceState.Restarting else ServiceState.Stopping)
      localHandlers
        .stopHost(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] host: [{}]", if (restart) "restarting" else "stopping", e)
        }

    case TargetMessenger.UpdateLocalState(level, state) =>
      updateState(level, state)

    //Cluster Management
    case Messages.Ping() =>
      sender ! Pong(localState)

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
  private case class UpdateLocalState(level: ServiceLevel, state: ServiceState)

  def props(handlers: LocalHandlers)(implicit ec: ExecutionContext): Props = Props(classOf[TargetMessenger], handlers, ec)
}
