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

import akka.actor.ActorRef
import com.typesafe.config.Config
import noisecluster.jvm.control.LocalHandlers

import scala.concurrent.ExecutionContext

/**
  * Target service.
  *
  * @param systemName           the actor system name to be used for the service
  * @param messengerName        the name of the target messenger used by the service
  * @param localHandlers        the local system handlers to use
  * @param lastSourceDownAction the action to take when the last available source becomes unreachable (optional)
  * @param overrideConfig       override configuration for the service and actor system (optional)
  */
class TargetService(
  private val systemName: String,
  private val messengerName: String,
  private val localHandlers: LocalHandlers,
  private val lastSourceDownAction: Option[NodeAction] = None,
  private val overrideConfig: Option[Config] = None
)(implicit ec: ExecutionContext) extends Service(systemName, overrideConfig) {
  override protected val messenger: ActorRef =
    system.actorOf(
      TargetMessenger.props(localHandlers, lastSourceDownAction),
      s"$TargetActorNamePrefix$messengerName"
    )
}
