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

import java.net._
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.event.Logging
import noisecluster.jvm.transport

import scala.concurrent.blocking
import scala.util.control.NonFatal

/**
  * Target using basic UDP for transport.
  *
  * @note If a UDP multicast address is specified, the target will join that multicast group. If a UDP unicast address
  *       is specified, it is used as the local address to be used for transmission. If no address is specified,
  *       any local address will be used for transmission.
  * @param localPort  the local system port to use for transmission
  * @param bufferSize the data buffer size (in bytes)
  * @param address    the local address to bind to or the multicast group to join (optional)
  * @see [[java.net.DatagramSocket#DatagramSocket(int)]]
  * @see [[java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)]]
  */
class Target(
  private val localPort: Int,
  private val bufferSize: Int,
  private val address: Option[InetAddress]
)(implicit loggingActorSystem: ActorSystem) extends transport.Target {

  private val log = Logging.getLogger(loggingActorSystem, this)
  private val socket = if (address.exists(_.isMulticastAddress)) {
    val socket = new MulticastSocket(localPort)
    socket.joinGroup(address.get)
    socket
  } else {
    address match {
      case Some(localAddress) => new DatagramSocket(localPort, localAddress)
      case None => new DatagramSocket(localPort)
    }
  }

  private val isRunning = new AtomicBoolean(false)

  override def isActive: Boolean = isRunning.get

  override def start(dataHandler: (Array[Byte], Int) => Unit): Unit = {
    if (isRunning.compareAndSet(false, true)) {
      log.info("Starting transport for channel [{}:{}]", address, localPort)
      val buffer = Array.ofDim[Byte](bufferSize)

      blocking {
        try {
          while (isRunning.get) {
            val packet = new DatagramPacket(buffer, buffer.length)
            socket.receive(packet)
            dataHandler(packet.getData, packet.getLength)
          }
        } catch {
          case e: SocketException =>
            if (isRunning.get) {
              log.error("Unexpected socket exception encountered while receiving data from channel [{}:{}]: [{}]", address, localPort, e.getMessage)
              e.printStackTrace()
            }

          case NonFatal(e) =>
            log.error("Exception encountered while receiving data from channel [{}:{}]: [{}]", address, localPort, e.getMessage)
            e.printStackTrace()
        }
      }

      log.info("Stopped transport for channel [{}:{}]", address, localPort)
    } else {
      val message = s"Cannot start transport for channel [$address:$localPort]; transport is already active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def stop(): Unit = {
    if (isRunning.compareAndSet(true, false)) {
      log.info("Stopping transport for channel [{}:{}]", address, localPort)
    } else {
      val message = s"Cannot stop transport for channel [$address:$localPort]; transport is not active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def close(): Unit = {
    if (!isRunning.get) {
      socket match {
        case multicastSocket: MulticastSocket => multicastSocket.leaveGroup(address.get)
        case _ => //do nothing
      }

      socket.close()
    } else {
      val message = s"Cannot close transport for channel [$address:$localPort]; transport is already closed"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object Target {
  /**
    * Creates a new UDP target with the specified parameters.
    *
    * @note If a UDP multicast address is specified, the target will join that multicast group. If a UDP unicast address
    *       is specified, it is used as the local address to be used for transmission. If no address is specified,
    *       any local address will be used for transmission.
    *
    * @param address the local address to bind to or the multicast group to join
    * @param localPort the local system port to use for transmission
    * @param bufferSize the data buffer size (in bytes)
    * @return the new target instance
    * @see [[java.net.DatagramSocket#DatagramSocket(int)]]
    * @see [[java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)]]
    */
  def apply(
    address: String,
    localPort: Int,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(localPort, bufferSize, Some(InetAddress.getByName(address)))

  /**
    * Creates a new UDP unicast target with the specified parameters.
    *
    * @note No specific local address will be used for the transmission.
    *
    * @param localPort the local system port to use for transmission
    * @param bufferSize the data buffer size (in bytes)
    * @return the new target instance
    * @see [[java.net.DatagramSocket#DatagramSocket(int)]]
    */
  def apply(
    localPort: Int,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(localPort, bufferSize, None)

  /**
    * Creates a new UDP target with the specified parameters and the default buffer size.
    *
    * @note If a UDP multicast address is specified, the target will join that multicast group. If a UDP unicast address
    *       is specified, it is used as the local address to be used for transmission. If no address is specified,
    *       any local address will be used for transmission.
    *
    * @param address the local address to bind to or the multicast group to join
    * @param localPort the local system port to use for transmission
    * @return the new target instance
    * @see [[java.net.DatagramSocket#DatagramSocket(int)]]
    * @see [[java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)]]
    * @see [[noisecluster.jvm.transport.udp.Defaults#BufferSize]]
    */
  def apply(
    address: String,
    localPort: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(localPort, Defaults.BufferSize, Some(InetAddress.getByName(address)))

  /**
    * Creates a new UDP unicast target with the specified parameters and the default buffer size.
    *
    * @note No specific local address will be used for the transmission.
    *
    * @param localPort the local system port to use for transmission
    * @return the new target instance
    * @see [[java.net.DatagramSocket#DatagramSocket(int)]]
    * @see [[noisecluster.jvm.transport.udp.Defaults#BufferSize]]
    */
  def apply(
    localPort: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(localPort, Defaults.BufferSize, None)
}
