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
package noisecluster.transport.aeron

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.event.Logging
import io.aeron._
import io.aeron.logbuffer._
import org.agrona.DirectBuffer
import org.agrona.concurrent._

class Target(
  private val stream: Int,
  private val channel: String,
  private val dataHandler: (Array[Byte], Int) => Unit,
  private val idleStrategy: IdleStrategy,
  private val fragmentLimit: Int
)(implicit loggingActorSystem: ActorSystem, aeron: Aeron) {
  private val isRunning = new AtomicBoolean(false)
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val subscription = aeron.addSubscription(channel, stream)

  private val fragmentHandler = new FragmentHandler {
    override def onFragment(buffer: DirectBuffer, offset: Int, length: Int, header: Header): Unit = {
      val data = new Array[Byte](length)
      buffer.getBytes(offset, data)
      dataHandler(data, length)
    }
  }

  private val fragmentAssembler = new FragmentAssembler(fragmentHandler)

  def isActive: Boolean = isRunning.get

  //docs - warn about blocking
  def start(): Unit = {
    if (isRunning.compareAndSet(false, true)) {
      log.info("Starting transport for channel [{}] and stream [{}]", channel, stream)

      //will block until stopped
      while (isRunning.get) {
        val fragmentsRead = subscription.poll(fragmentAssembler, fragmentLimit)
        idleStrategy.idle(fragmentsRead)
      }

      log.info("Stopped transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot start transport for channel [$channel] and stream [$stream]; transport is already active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  def stop(): Unit = {
    if (isRunning.compareAndSet(true, false)) {
      log.info("Stopping transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot stop transport for channel [$channel] and stream [$stream]; transport is not active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  def close(): Unit = {
    if (!isRunning.get) {
      log.info("Closing transport for channel [{}] and stream [{}]", channel, stream)
      subscription.close()
      aeron.close()
      log.info("Closed transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot close transport for channel [$channel] and stream [$stream]; transport is still active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object Target {
  def apply(
    stream: Int,
    address: String,
    port: Int,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      s"aeron:udp?endpoint=$address:$port",
      dataHandler,
      idleStrategy,
      fragmentLimit
    )

  def apply(
    stream: Int,
    address: String,
    port: Int,
    interface: String,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      s"aeron:udp?endpoint=$address:$port|interface=$interface",
      dataHandler,
      idleStrategy,
      fragmentLimit
    )

  def apply(
    stream: Int,
    channel: String,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      channel,
      dataHandler,
      idleStrategy,
      fragmentLimit
    )

  def apply(
    stream: Int,
    address: String,
    port: Int,
    dataHandler: (Array[Byte], Int) => Unit
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    Target(
      stream,
      address,
      port,
      dataHandler,
      Defaults.IdleStrategy,
      Defaults.FragmentLimit
    )
}
