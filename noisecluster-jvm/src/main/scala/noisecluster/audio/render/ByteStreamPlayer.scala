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
package noisecluster.audio.render

import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, SourceDataLine}

import noisecluster.audio.{Buffers, Formats}

//TODO - warn about buffer size from 'open' method
//TODO - catch exceptions
//TODO - make async ? (impacts performance/latency ?)

class ByteStreamPlayer(
  val lineBufferSize: Int,   //in bytes
  val format: AudioFormat
) {
  private val lineInfo = new DataLine.Info(classOf[SourceDataLine], format)
  private val line = AudioSystem.getLine(lineInfo).asInstanceOf[SourceDataLine]
  line.open(format, lineBufferSize)

  //TODO - warn about blocking or make async
  def enqueueData(data: Array[Byte], length: Int): Unit = {
    line.write(data, 0, length)
  }

  //TODO - warn about blocking or make async
  def enqueueData(data: Array[Byte]): Unit = {
    enqueueData(data, data.length)
  }

  def start(): Unit = {
    line.start()
  }

  //TODO - warn about blocking
  def stop(): Unit = {
    line.drain()
    line.stop()
    line.close()
  }

  def pause(): Unit = {
    line.stop()
  }

  def resume(): Unit = {
    line.flush()
    line.start()
  }
}

object ByteStreamPlayer {
  def apply(lineBufferSize: Int, format: AudioFormat): ByteStreamPlayer = new ByteStreamPlayer(lineBufferSize, format)
  def apply(lineBufferSize: Int): ByteStreamPlayer = new ByteStreamPlayer(lineBufferSize, Formats.Default)
  def apply(format: AudioFormat): ByteStreamPlayer = new ByteStreamPlayer(Buffers.DefaultLineBufferSize, format)
  def apply(): ByteStreamPlayer = new ByteStreamPlayer(Buffers.DefaultLineBufferSize, Formats.Default)
}
