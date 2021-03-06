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
import io.aeron.driver.MediaDriver
import noisecluster.jvm.transport.{Target, aeron}
import ve.providers.TransportProvider

import scala.util.control.NonFatal

/**
  * Aeron target transport provider.
  *
  * @param config the config to use for the provider and targets setup
  */
class Aeron(config: Config)(implicit system: ActorSystem) extends TransportProvider {
  private val driver: MediaDriver = MediaDriver.launch(aeron.Contexts.Driver.lowLatency)
  private implicit val aeronSystem: io.aeron.Aeron = io.aeron.Aeron.connect(aeron.Contexts.System.default)

  override def createTarget(): Target = {
    val stream: Int = config.getInt("stream")
    val address: String = config.getString("address")
    val port: Int = config.getInt("port")
    val interfaceOpt: Option[String] =
      if (config.hasPath("interface"))
        Some(config.getString("interface"))
      else
        None

    interfaceOpt match {
      case Some(interface) =>
        aeron.Target(
          stream,
          address,
          port,
          interface,
          aeron.Defaults.IdleStrategy,
          aeron.Defaults.FragmentLimit
        )

      case None =>
        aeron.Target(
          stream,
          address,
          port
        )
    }
  }

  override def shutdown(): Unit = {
    try {
      aeronSystem.close()
    } catch {
      case NonFatal(e) => e.printStackTrace()
    }

    try {
      driver.close()
    } catch {
      case NonFatal(e) => e.printStackTrace()
    }
  }
}
