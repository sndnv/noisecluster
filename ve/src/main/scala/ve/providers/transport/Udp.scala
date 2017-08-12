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
package ve.providers.transport

import akka.actor.ActorSystem
import com.typesafe.config.Config
import noisecluster.jvm.transport.{Target, udp}
import ve.providers.TransportProvider

class Udp(config: Config)(implicit system: ActorSystem) extends TransportProvider {

  override def createTarget(): Target = {
    val addressOpt: Option[String] = if(config.hasPath("address")) {
      Some(config.getString("address"))
    } else {
      None
    }
    val port: Int = config.getInt("port")

    addressOpt match {
      case Some(address) => udp.Target(address, port)
      case None => udp.Target(port)
    }
  }

  override def shutdown(): Unit = {}
}
