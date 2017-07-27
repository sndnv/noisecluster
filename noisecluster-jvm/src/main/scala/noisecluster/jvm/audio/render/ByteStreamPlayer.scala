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
package noisecluster.jvm.audio.render

import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, SourceDataLine}

import akka.actor.ActorSystem
import akka.event.Logging

class ByteStreamPlayer(
  private val format: AudioFormat,
  private val lineBufferSize: Option[Int]
)(implicit loggingActorSystem: ActorSystem) {
  private val isRunning = new AtomicBoolean(false)
  private val log = Logging.getLogger(loggingActorSystem, this)
  private val lineInfo = new DataLine.Info(classOf[SourceDataLine], format)
  private val line = AudioSystem.getLine(lineInfo).asInstanceOf[SourceDataLine]

  lineBufferSize match {
    case Some(bufferSize) => line.open(format, bufferSize)
    case None => line.open(format)
  }

  def isActive: Boolean = isRunning.get

  //docs - warn about blocking
  def write(data: Array[Byte], length: Int): Int = {
    line.write(data, 0, length)
  }

  //docs - warn about discarding data
  def writeNonBlocking(data: Array[Byte], length: Int): Int = {
    line.write(data, 0, length.min(line.available))
  }

  def start(): Unit = {
    if (isRunning.compareAndSet(false, true)) {
      log.info("Starting audio rendering for line [{}]", lineInfo)
      line.flush()
      line.start()
    } else {
      val message = s"Cannot start audio rendering for line [$lineInfo]; audio is already active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  def stop(): Unit = {
    if (isRunning.compareAndSet(true, false)) {
      log.info("Stopping audio rendering for line [{}]", line)
      line.stop()
    } else {
      val message = s"Cannot stop audio rendering for line [$line]; rendering is not active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }

  def close(): Unit = {
    if (!isRunning.get) {
      log.info("Closing audio rendering for line [{}]", line)
      line.close()
      log.info("Closed audio rendering for line [{}]", line)
    } else {
      val message = s"Cannot close audio rendering for line [$line]; rendering is still active"
      log.warning(message)
      throw new IllegalStateException(message)
    }
  }
}

object ByteStreamPlayer {
  def apply(format: AudioFormat)(implicit loggingActorSystem: ActorSystem): ByteStreamPlayer =
    new ByteStreamPlayer(format, None)

  def apply(format: AudioFormat, lineBufferSize: Int)(implicit loggingActorSystem: ActorSystem): ByteStreamPlayer =
    new ByteStreamPlayer(format, Some(lineBufferSize))
}
