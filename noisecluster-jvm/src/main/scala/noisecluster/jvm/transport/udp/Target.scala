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

class Target(
  private val address: String,
  private val port: Int,
  private val bufferSize: Int
)(implicit loggingActorSystem: ActorSystem) extends transport.Target {
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val socket = new MulticastSocket(port)
  private val group = InetAddress.getByName(address)
  socket.joinGroup(group)

  private val isRunning = new AtomicBoolean(false)

  override def isActive: Boolean = isRunning.get

  //docs - warn about blocking
  override def start(dataHandler: (Array[Byte], Int) => Unit): Unit = {
    if (isRunning.compareAndSet(false, true)) {
      log.info("Starting transport for channel [{}:{}]", address, port)
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
              log.error("Unexpected socket exception encountered while receiving data from channel [{}:{}]: [{}]", address, port, e.getMessage)
              e.printStackTrace()
            }

          case NonFatal(e) =>
            log.error("Exception encountered while receiving data from channel [{}:{}]: [{}]", address, port, e.getMessage)
            e.printStackTrace()
        }
      }

      log.info("Stopped transport for channel [{}:{}]", address, port)
    } else {
      val message = s"Cannot start transport for channel [$address:$port]; transport is already active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def stop(): Unit = {
    if (isRunning.compareAndSet(true, false)) {
      log.info("Stopping transport for channel [{}:{}]", address, port)
    } else {
      val message = s"Cannot stop transport for channel [$address:$port]; transport is not active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def close(): Unit = {
    if (!isRunning.get) {
      socket.leaveGroup(group)
      socket.close()
    } else {
      val message = s"Cannot close transport for channel [$address:$port]; transport is already closed"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object Target {
  def apply(
    address: String,
    port: Int,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(address, port, bufferSize)

  def apply(
    address: String,
    port: Int
  )(implicit loggingActorSystem: ActorSystem): Target = new Target(address, port, Defaults.BufferSize)
}
