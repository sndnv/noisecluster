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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * Base cluster messenger implementation.
  *
  * @param localHandlers the local system handlers to use
  */
abstract class Messenger(
  private val localHandlers: LocalHandlers
)(implicit ec: ExecutionContext) extends Actor with ActorLogging {
  //Cluster Setup
  protected val clusterRef = Cluster(context.system)

  override def preStart(): Unit = clusterRef.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = clusterRef.unsubscribe(self)

  //Mediator Setup
  protected val mediatorRef: ActorRef = DistributedPubSub(context.system).mediator
  mediatorRef ! Put(self)

  //Node Setup
  private var localState = NodeState(
    audio = ServiceState.Stopped,
    transport = ServiceState.Stopped,
    application = ServiceState.Active,
    host = ServiceState.Active,
    volume = 0,
    muted = false
  )

  /**
    * Updates the state of the specified service.
    *
    * @param level the service to update
    * @param state the new service state
    */
  private def updateLocalState(level: ServiceLevel, state: ServiceState): Unit = {
    val newState = level match {
      case ServiceLevel.Audio => localState.copy(audio = state)
      case ServiceLevel.Transport => localState.copy(transport = state)
      case ServiceLevel.Application => localState.copy(application = state)
      case ServiceLevel.Host => localState.copy(host = state)
    }

    localState = newState
  }

  /**
    * Updates the local master volume.
    *
    * @param volume the new volume
    */
  private def updateLocalState(volume: Int): Unit = {
    localState = localState.copy(volume = volume)
  }

  /**
    * Updates the local muted state.
    *
    * @param muted the new state
    */
  private def updateLocalState(muted: Boolean): Unit = {
    localState = localState.copy(muted = muted)
  }

  /**
    * Retrieves the local node state.
    *
    * @return the node state
    */
  protected def getLocalState: Future[NodeState] = {
    (for {
      //attempts to retrieve the host's master volume and muted state
      volume <- localHandlers.getHostVolume
      muted <- localHandlers.isHostMuted
    } yield {
      localState.copy(
        volume = volume,
        muted = muted
      )
    }).recover {
      case NonFatal(e) =>
        //failed to retrieve the host's master volume and muted state;
        //the last available state is returned instead
        log.error("Exception encountered while processing local state: [{}]", e)
        localState
    }
  }

  /**
    * Default messenger behaviour.
    */
  private var receivers: Actor.Receive = {
    case Messages.StartAudio() =>
      val previousState = localState.audio
      updateLocalState(ServiceLevel.Audio, ServiceState.Starting)

      localHandlers
        .startAudio()
        .map {
          result =>
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Audio, ServiceState.Active)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting audio: [{}]", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Audio, previousState)
            throw e
        }

    case Messages.StopAudio() =>
      val previousState = localState.audio
      updateLocalState(ServiceLevel.Audio, ServiceState.Stopping)

      localHandlers
        .stopAudio()
        .map {
          result =>
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Audio, ServiceState.Stopped)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while stopping audio: [{}]", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Audio, previousState)
            throw e
        }

    case Messages.StartTransport() =>
      val previousState = localState.transport
      updateLocalState(ServiceLevel.Transport, ServiceState.Starting)

      localHandlers
        .startTransport()
        .map {
          result =>
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Transport, ServiceState.Active)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while starting transport: [{}]", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Transport, previousState)
            throw e
        }

    case Messages.StopTransport() =>
      val previousState = localState.transport
      updateLocalState(ServiceLevel.Transport, ServiceState.Stopping)

      localHandlers
        .stopTransport()
        .map {
          result =>
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Transport, ServiceState.Stopped)
            result
        }
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while stopping transport: [{}]", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Transport, previousState)
            throw e
        }

    case Messages.StopApplication(restart) =>
      val previousState = localState.application
      updateLocalState(ServiceLevel.Application, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopApplication(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] application: [{}]", if (restart) "restarting" else "stopping", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Application, previousState)
            throw e
        }

    case Messages.StopHost(restart) =>
      val previousState = localState.host
      updateLocalState(ServiceLevel.Host, if (restart) ServiceState.Restarting else ServiceState.Stopping)

      localHandlers
        .stopHost(restart)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while [{}] host: [{}]", if (restart) "restarting" else "stopping", e)
            self ! Messenger.UpdateLocalStateWithServiceLevel(ServiceLevel.Host, previousState)
            throw e
        }

    case Messages.SetHostVolume(level) =>
      val previousState = localState.volume
      updateLocalState(level)

      localHandlers
        .setHostVolume(level)
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while setting host volume: [{}]", e)
            self ! Messenger.UpdateLocalStateWithVolume(previousState)
            throw e
        }

    case Messages.MuteHost() =>
      val previousState = localState.muted
      updateLocalState(muted = true)

      localHandlers
        .muteHost()
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while muting host: [{}]", e)
            self ! Messenger.UpdateLocalStateWithMuted(previousState)
            throw e
        }

    case Messages.UnmuteHost() =>
      val previousState = localState.muted
      updateLocalState(muted = false)

      localHandlers
        .unmuteHost()
        .recover {
          case NonFatal(e) =>
            log.error("Exception encountered while unmuting host: [{}]", e)
            self ! Messenger.UpdateLocalStateWithMuted(previousState)
            throw e
        }

    case Messenger.UpdateLocalStateWithServiceLevel(level, state) =>
      updateLocalState(level, state)

    case Messenger.UpdateLocalStateWithVolume(level) =>
      updateLocalState(level)

    case Messenger.UpdateLocalStateWithMuted(muted) =>
      updateLocalState(muted)
  }

  /**
    * Adds a new messenger behaviour.
    *
    * @param next the new behaviour
    */
  protected def addReceiver(next: Actor.Receive): Unit = {
    receivers = receivers orElse next
  }

  override def receive: Receive = receivers
}

object Messenger {

  private case class UpdateLocalStateWithServiceLevel(level: ServiceLevel, state: ServiceState)

  private case class UpdateLocalStateWithVolume(volume: Int)

  private case class UpdateLocalStateWithMuted(muted: Boolean)

}
