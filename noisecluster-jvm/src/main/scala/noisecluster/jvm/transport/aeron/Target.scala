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
package noisecluster.jvm.transport.aeron

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.event.Logging
import io.aeron._
import io.aeron.logbuffer._
import noisecluster.jvm.transport
import org.agrona.DirectBuffer
import org.agrona.concurrent._

import scala.concurrent.blocking

/**
  * Target using Aeron for transport
  *
  * <br><br>
  * See <a href='https://github.com/real-logic/aeron'>Aeron on Github</a> for more information on configuration and usage.
  *
  * @param stream the Aeron stream ID to use
  * @param channel the Aeron channel to use
  * @param idleStrategy the Aeron idle strategy to use
  * @param fragmentLimit the number of message fragments to process per poll operation
  * @see [[io.aeron.Subscription#poll(io.aeron.logbuffer.FragmentHandler, int)]]
  * @see [[org.agrona.concurrent.IdleStrategy]]
  */
class Target(
  private val stream: Int,
  private val channel: String,
  private val idleStrategy: IdleStrategy,
  private val fragmentLimit: Int
)(implicit loggingActorSystem: ActorSystem, aeron: Aeron) extends transport.Target {
  private val isRunning = new AtomicBoolean(false)
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val subscription = aeron.addSubscription(channel, stream)

  override def isActive: Boolean = isRunning.get

  override def start(dataHandler: (Array[Byte], Int) => Unit): Unit = {
    if (isRunning.compareAndSet(false, true)) {
      log.info("Starting transport for channel [{}] and stream [{}]", channel, stream)

      val fragmentAssembler = new FragmentAssembler(
        new FragmentHandler {
          override def onFragment(buffer: DirectBuffer, offset: Int, length: Int, header: Header): Unit = {
            val data = new Array[Byte](length)
            buffer.getBytes(offset, data)
            dataHandler(data, length)
          }
        }
      )

      blocking {
        while (isRunning.get) {
          val fragmentsRead = subscription.poll(fragmentAssembler, fragmentLimit)
          idleStrategy.idle(fragmentsRead)
        }
      }

      log.info("Stopped transport for channel [{}] and stream [{}]", channel, stream)

    } else {
      val message = s"Cannot start transport for channel [$channel] and stream [$stream]; transport is already active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def stop(): Unit = {
    if (isRunning.compareAndSet(true, false)) {
      log.info("Stopping transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot stop transport for channel [$channel] and stream [$stream]; transport is not active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  override def close(): Unit = {
    if (!isRunning.get) {
      log.info("Closing transport for channel [{}] and stream [{}]", channel, stream)
      subscription.close()
      log.info("Closed transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot close transport for channel [$channel] and stream [$stream]; transport is still active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object Target {
  /**
    * Creates a new Aeron target using UDP with the specified parameters.
    *
    * @param stream the Aeron stream ID to use
    * @param address the UDP address to use
    * @param port the UDP port to use
    * @param idleStrategy the Aeron idle strategy to use
    * @param fragmentLimit the number of message fragments to process per poll operation
    * @return the new target instance
    * @see [[io.aeron.Subscription#poll(io.aeron.logbuffer.FragmentHandler, int)]]
    * @see [[org.agrona.concurrent.IdleStrategy]]
    */
  def apply(
    stream: Int,
    address: String,
    port: Int,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      s"aeron:udp?endpoint=$address:$port",
      idleStrategy,
      fragmentLimit
    )

  /**
    * Creates a new Aeron target using UDP with the specified parameters.
    *
    * @param stream the Aeron stream ID to use
    * @param address the UDP address to use
    * @param port the UDP port to use
    * @param interface the local interface to bind to
    * @param idleStrategy the Aeron idle strategy to use
    * @param fragmentLimit the number of message fragments to process per poll operation
    * @return the new target instance
    * @see [[io.aeron.Subscription#poll(io.aeron.logbuffer.FragmentHandler, int)]]
    * @see [[org.agrona.concurrent.IdleStrategy]]
    */
  def apply(
    stream: Int,
    address: String,
    port: Int,
    interface: String,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      s"aeron:udp?endpoint=$address:$port|interface=$interface",
      idleStrategy,
      fragmentLimit
    )

  /**
    * Creates a new Aeron target with the specified parameters.
    *
    * @param stream the Aeron stream ID to use
    * @param channel the Aeron channel to use
    * @param idleStrategy the Aeron idle strategy to use
    * @param fragmentLimit the number of message fragments to process per poll operation
    * @return the new target instance
    * @see [[io.aeron.Subscription#poll(io.aeron.logbuffer.FragmentHandler, int)]]
    * @see [[org.agrona.concurrent.IdleStrategy]]
    */
  def apply(
    stream: Int,
    channel: String,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    new Target(
      stream,
      channel,
      idleStrategy,
      fragmentLimit
    )

  /**
    * Creates a new Aeron target using UDP with the specified parameters,
    * the default idle strategy and the default fragment limit.
    *
    * @param stream the Aeron stream ID to use
    * @param address the UDP address to use
    * @param port the UDP port to use
    * @return the new target instance
    * @see [[io.aeron.Subscription#poll(io.aeron.logbuffer.FragmentHandler, int)]]
    * @see [[org.agrona.concurrent.IdleStrategy]]
    * @see [[noisecluster.jvm.transport.aeron.Defaults#IdleStrategy]]
    * @see [[noisecluster.jvm.transport.aeron.Defaults#FragmentLimit]]
    */
  def apply(
    stream: Int,
    address: String,
    port: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): Target =
    Target(
      stream,
      address,
      port,
      Defaults.IdleStrategy,
      Defaults.FragmentLimit
    )
}
