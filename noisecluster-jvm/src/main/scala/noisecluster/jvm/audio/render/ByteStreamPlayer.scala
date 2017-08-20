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

/**
  * Create an audio player with the specified format that renders byte data.
  *
  * @param format the audio format of the data to be rendered
  * @param lineBufferSize the audio line buffer size (optional; if not set, the system chooses a value)
  * @throws javax.sound.sampled.LineUnavailableException if the specified format is not supported by the system
  *
  * @see [[javax.sound.sampled.SourceDataLine#open]]
  * @see [[javax.sound.sampled.AudioSystem#getLine]]
  */
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

  /**
    * Writes the supplied data to the audio line.
    *
    * @note If it is attempted to write more data than can be currently written to the line
    *       the [[javax.sound.sampled.SourceDataLine#write]] call will block.
    *
    * @param data the data to write
    * @param length the number of bytes to write
    * @return the number of bytes actually written
    * @see [[javax.sound.sampled.SourceDataLine#write]]
    */
  def write(data: Array[Byte], length: Int): Int = {
    line.write(data, 0, length)
  }

  /**
    * Writes the supplied data to the audio line.
    *
    * @note If it is attempted to write more data than can be currently written to the line
    *       the extra data will be discarded.
    *
    * @param data the data to write
    * @param length the number of bytes to write
    * @return the number of bytes actually written
    * @see [[javax.sound.sampled.SourceDataLine#write]]
    * @see [[javax.sound.sampled.SourceDataLine#available]]
    */
  def writeNonBlocking(data: Array[Byte], length: Int): Int = {
    line.write(data, 0, length.min(line.available))
  }

  /**
    * Makes the audio line available for rendering.
    *
    * @note Flushes all data queued from the line before rendering starts.
    *
    * @see [[javax.sound.sampled.SourceDataLine#flush]]
    * @see [[javax.sound.sampled.SourceDataLine#start]]
    * @throws IllegalStateException if the rendering was already started
    */
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

  /**
    * Stops audio rendering.
    *
    * @note Rendering can be restarted at any time via a call to [[start]].
    *
    * @see [[noisecluster.jvm.audio.render.ByteStreamPlayer#close]]
    * @see [[noisecluster.jvm.audio.render.ByteStreamPlayer#start]]
    * @throws IllegalStateException if the rendering is not running
    */
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

  /**
    * Closes the stopped audio line and makes the player unavailable for further rendering.
    *
    * @see [[noisecluster.jvm.audio.render.ByteStreamPlayer#stop]]
    * @see [[javax.sound.sampled.SourceDataLine#close]]
    * @throws IllegalStateException if rendering is still active
    */
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
  /**
    * Creates a new player with the specified audio format and a system-defined buffer size.
    *
    * @param format the audio format of the data to be rendered
    * @throws javax.sound.sampled.LineUnavailableException if the specified format is not supported by the system
    * @see [[javax.sound.sampled.SourceDataLine#open]]
    * @see [[javax.sound.sampled.AudioSystem#getLine]]
    * @return the new player instance
    */
  def apply(format: AudioFormat)(implicit loggingActorSystem: ActorSystem): ByteStreamPlayer =
    new ByteStreamPlayer(format, None)

  /**
    * Creates a new player with the specified audio format and buffer size.
    *
    * @param format the audio format of the data to be rendered
    * @param lineBufferSize the audio line buffer size
    * @throws javax.sound.sampled.LineUnavailableException if the specified format is not supported by the system
    * @see [[javax.sound.sampled.SourceDataLine#open]]
    * @see [[javax.sound.sampled.AudioSystem#getLine]]
    * @return the new player instance
    */
  def apply(format: AudioFormat, lineBufferSize: Int)(implicit loggingActorSystem: ActorSystem): ByteStreamPlayer =
    new ByteStreamPlayer(format, Some(lineBufferSize))
}
