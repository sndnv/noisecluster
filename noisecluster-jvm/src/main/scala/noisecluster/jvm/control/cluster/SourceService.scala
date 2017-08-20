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

import akka.actor.{ActorRef, Address}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import noisecluster.jvm.control.LocalHandlers
import noisecluster.jvm.control.cluster.SourceMessenger.ForwardMessage

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Source service responsible for handling message forwarding.
  *
  * @param systemName     the actor system name to be used for the service
  * @param messengerName  the name of the source messenger used by the service
  * @param pingInterval   the time between target status requests
  * @param localHandlers  the local system handlers to use
  * @param overrideConfig override configuration for the service and actor system (optional)
  * @see [[noisecluster.jvm.control.cluster.SourceMessenger]]
  */
class SourceService(
  private val systemName: String,
  private val messengerName: String,
  private val pingInterval: FiniteDuration,
  private val localHandlers: LocalHandlers,
  private val overrideConfig: Option[Config] = None
)(implicit ec: ExecutionContext, timeout: Timeout) extends Service(systemName, overrideConfig) {
  override protected val messenger: ActorRef = system.actorOf(SourceMessenger.props(pingInterval, localHandlers), s"$SourceActorNamePrefix$messengerName")

  /**
    * Retrieves the cluster state as seen by the local source.
    *
    * @return the requested cluster state
    */
  def getClusterState: Future[ClusterState] = {
    (messenger ? SourceMessenger.GetClusterState()).mapTo[ClusterState]
  }

  /**
    * Forwards the supplied control message to specified target.
    *
    * @param target  the name of the recipient target
    * @param message the message to forward
    */
  def forwardMessage(target: String, message: Messages.ControlMessage): Unit = {
    messenger ! ForwardMessage(Some(s"$TargetActorNamePrefix$target"), message)
  }

  /**
    * Forwards the supplied control message to all registered targets.
    *
    * @param message the message to forward
    */
  def forwardMessage(message: Messages.ControlMessage): Unit = {
    messenger ! ForwardMessage(None, message)
  }

  /**
    * Sends the supplied control message to the local source.
    *
    * @param message the message to send
    */
  def processMessage(message: Messages.ControlMessage): Unit = {
    messenger ! message
  }

  /**
    * Retrieves a target's address based on its name and the current cluster state.
    *
    * @param target the target's name
    * @return the target's address
    */
  private def getTargetAddressFromState(target: String): Future[Address] = {
    getClusterState.map {
      state =>
        state.targetAddresses.get(target) match {
          case Some(address) =>
            address

          case None =>
            throw new IllegalArgumentException(s"Target node [$target] not found")
        }
    }
  }

  /**
    * Sets the specified target's cluster state to down.
    *
    * @param target the target's name
    * @return true, if the operation was successful
    */
  def setTargetToDown(target: String): Future[Boolean] = {
    getTargetAddressFromState(target).map {
      address =>
        cluster.down(address)
        true
    }
  }

  /**
    * Sets the specified target's cluster state to leaving.
    *
    * @param target the target's name
    * @return true, if the operation was successful
    */
  def setTargetToLeaving(target: String): Future[Boolean] = {
    getTargetAddressFromState(target).map {
      address =>
        cluster.leave(address)
        true
    }
  }
}
