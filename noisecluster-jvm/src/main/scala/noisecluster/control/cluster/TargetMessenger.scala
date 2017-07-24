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
import noisecluster.control._
import noisecluster.control.cluster.Messages.{Pong, RegisterTarget}

import scala.concurrent.ExecutionContext

class TargetMessenger(private val localHandlers: LocalHandlers)(implicit ec: ExecutionContext) extends Messenger(localHandlers) {
  private var sources = Map.empty[String, Address]

  addReceiver {
    //Cluster Management
    case Messages.Ping() =>
      sender ! Pong(getLocalState)

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
  def props(handlers: LocalHandlers)(implicit ec: ExecutionContext): Props = Props(classOf[TargetMessenger], handlers, ec)
}
