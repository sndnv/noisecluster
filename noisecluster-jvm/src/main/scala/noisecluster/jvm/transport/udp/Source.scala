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

import java.net.{DatagramPacket, DatagramSocket, InetAddress, MulticastSocket}

import akka.actor.ActorSystem
import akka.event.Logging
import noisecluster.jvm.transport

import scala.util.control.NonFatal

//docs - will only attempt to do multicasting if the first target is a multicast address
class Source(
  private val targets: Seq[(InetAddress, Int)],
  private val localPort: Int
)(implicit loggingActorSystem: ActorSystem) extends transport.Source {
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val isMulticast = targets.head._1.isMulticastAddress

  private val socket = if (isMulticast) {
    new MulticastSocket(localPort)
  } else {
    new DatagramSocket(localPort)
  }

  if (isMulticast && targets.size > 1) {
    log.warning("Transport is in multicast mode but encountered multiple targets; the extra targets will be ignored!")
  }

  if (!isMulticast && targets.exists(_._1.isMulticastAddress)) {
    log.warning("Transport is in unicast mode but encountered one or more multicast addresses; multicasting may not function correctly!")
  }

  override def send(source: Array[Byte]): Unit = {
    send(source, 0, source.length)
  }

  override def send(source: Array[Byte], offset: Int, length: Int): Unit = {
    if (isMulticast) {
      try {
        socket.send(new DatagramPacket(source, offset, length, targets.head._1, targets.head._2))
      } catch {
        case NonFatal(e) =>
          log.error("Exception encountered while sending data on channel [{}:{}]: [{}]", targets.head, localPort, e.getMessage)
          e.printStackTrace()
          throw e
      }
    } else {
      targets.foreach {
        target =>
          try {
            socket.send(new DatagramPacket(source, offset, length, target._1, target._2))
          } catch {
            case NonFatal(e) =>
              log.error("Exception encountered while sending data on channel [{}:{}]: [{}]", target, localPort, e.getMessage)
              e.printStackTrace()
              throw e
          }
      }
    }
  }

  override def close(): Unit = {
    if (!socket.isClosed) {
      socket match {
        case multicastSocket: MulticastSocket =>
          multicastSocket.leaveGroup(targets.head._1)
        case _ => //do nothing
      }

      socket.close()
    } else {
      val message = s"Cannot close transport for channel [$targets:$localPort]; transport is already closed"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object Source {
  def apply(targetAddress: String, targetPort: Int, localPort: Int)(implicit loggingActorSystem: ActorSystem): Source = {
    new Source(Seq((InetAddress.getByName(targetAddress), targetPort)), localPort)
  }

  def apply(targets: Seq[(String, Int)], localPort: Int)(implicit loggingActorSystem: ActorSystem): Source = {
    new Source(
      targets.map {
        case (address, port) =>
          (InetAddress.getByName(address), port)
      },
      localPort
    )
  }
}
