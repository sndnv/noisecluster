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

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import noisecluster.control.LocalHandlers
import noisecluster.control.cluster.SourceMessenger.ForwardMessage

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SourceService(
  private val systemName: String,
  private val messengerName: String,
  private val pingInterval: FiniteDuration,
  private val localHandlers: LocalHandlers,
  private val overrideConfig: Option[Config] = None
)(implicit ec: ExecutionContext, timeout: Timeout) extends Service(systemName, overrideConfig) {
  override protected val messenger: ActorRef = system.actorOf(SourceMessenger.props(pingInterval, localHandlers), s"$SourceActorNamePrefix$messengerName")

  def getClusterState: Future[ClusterState] = {
    (messenger ? SourceMessenger.GetClusterState()).mapTo[ClusterState]
  }

  def forwardMessage(target: String, message: Messages.ControlMessage): Unit = {
    messenger ! ForwardMessage(Some(s"$TargetActorNamePrefix$target"), message)
  }

  def forwardMessage(message: Messages.ControlMessage): Unit = {
    messenger ! ForwardMessage(None, message)
  }
}
