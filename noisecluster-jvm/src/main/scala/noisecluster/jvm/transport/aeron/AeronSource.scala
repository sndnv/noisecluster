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

import java.nio.ByteBuffer

import akka.actor.ActorSystem
import akka.event.Logging
import io.aeron.{Aeron, Publication}
import noisecluster.jvm.transport.Source
import org.agrona.concurrent.UnsafeBuffer
import org.agrona.{BitUtil, BufferUtil, DirectBuffer}

class AeronSource(
  private val stream: Int,
  private val channel: String,
  private val bufferSize: Int //in bytes
)(implicit loggingActorSystem: ActorSystem, aeron: Aeron) extends Source {
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val publication = aeron.addPublication(channel, stream)
  private val buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(bufferSize, BitUtil.CACHE_LINE_LENGTH))

  private def offer(messageSize: Int): Long = {
    val result = publication.offer(buffer, 0, messageSize)
    result match {
      case r if r >= 0 =>
        log.debug("Offered [{}] bytes on channel [{}] with stream [{}]", messageSize, channel, stream)

      case Publication.BACK_PRESSURED =>
        log.warning("Transport for channel [{}] and stream [{}] failed due to back pressure", channel, stream)

      case Publication.NOT_CONNECTED =>
        val message = s"Cannot use transport for channel [$channel] with stream [$stream]; transport is not connected"
        log.error(message)
        throw new IllegalStateException(message)

      case Publication.ADMIN_ACTION =>
        log.warning("Transport for channel [{}] and stream [{}] failed due to admin action", channel, stream)

      case Publication.CLOSED =>
        val message = s"Cannot use transport for channel [$channel] and stream [$stream]; transport is already closed"
        log.error(message)
        throw new IllegalStateException(message)

      case r if r < 0 =>
        val message = s"Transport failed for channel [$channel] and stream [$stream]; unknown offer result encountered: [$result]"
        log.error(message)
        throw new RuntimeException(message)
    }

    result
  }

  def send(source: DirectBuffer, startIndex: Int, length: Int): Long = {
    buffer.putBytes(0, source, startIndex, length)
    offer(length)
  }

  def send(source: ByteBuffer, startIndex: Int, length: Int): Long = {
    buffer.putBytes(0, source, startIndex, length)
    offer(length)
  }

  def send(source: ByteBuffer, length: Int): Long = {
    buffer.putBytes(0, source, length)
    offer(length)
  }

  override def send(source: Array[Byte]): Unit = {
    buffer.putBytes(0, source)
    offer(source.length)
  }

  override def send(source: Array[Byte], offset: Int, length: Int): Unit = {
    buffer.putBytes(0, source, offset, length)
    offer(length)
  }

  override def close(): Unit = {
    if (!publication.isClosed) {
      log.info("Closing transport for channel [{}] and stream [{}]", channel, stream)
      publication.close()
      log.info("Closed transport for channel [{}] and stream [{}]", channel, stream)
    } else {
      val message = s"Cannot close transport for channel [$channel] and stream [$stream]; transport is already closed"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object AeronSource {
  def apply(
    stream: Int,
    address: String,
    port: Int,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): AeronSource =
    new AeronSource(
      stream,
      s"aeron:udp?endpoint=$address:$port",
      bufferSize
    )

  def apply(
    stream: Int,
    address: String,
    port: Int,
    interface: String,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): AeronSource =
    new AeronSource(
      stream,
      s"aeron:udp?endpoint=$address:$port|interface=$interface",
      bufferSize
    )

  def apply(
    stream: Int,
    channel: String,
    bufferSize: Int
  )(implicit loggingActorSystem: ActorSystem, aeron: Aeron): AeronSource =
    new AeronSource(
      stream,
      channel,
      bufferSize
    )
}
