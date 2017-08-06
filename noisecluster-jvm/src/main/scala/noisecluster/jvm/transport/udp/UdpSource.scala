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
package noisecluster.jvm.transport.udp

import java.net.{DatagramPacket, InetAddress, MulticastSocket}

import akka.actor.ActorSystem
import akka.event.Logging
import noisecluster.jvm.transport.Source

import scala.util.control.NonFatal

class UdpSource(private val address: String, private val port: Int)(implicit loggingActorSystem: ActorSystem) extends Source {
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val socket = new MulticastSocket(port)
  private val group = InetAddress.getByName(address)
  socket.joinGroup(group)

  override def send(source: Array[Byte]): Unit = {
    send(source, 0, source.length)
  }

  override def send(source: Array[Byte], offset: Int, length: Int): Unit = {
    try {
      socket.send(new DatagramPacket(source, offset, length, group, port))
    } catch {
      case NonFatal(e) =>
        log.error("Exception encountered while sending data on channel [{}:{}]: [{}]", address, port, e.getMessage)
        e.printStackTrace()
        throw e
    }
  }

  override def close(): Unit = {
    if (!socket.isClosed) {
      socket.leaveGroup(group)
      socket.close()
    } else {
      val message = s"Cannot close transport for channel [$address:$port]; transport is already closed"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object UdpSource {
  def apply(address: String, port: Int)(implicit loggingActorSystem: ActorSystem): UdpSource = new UdpSource(address, port)
}
