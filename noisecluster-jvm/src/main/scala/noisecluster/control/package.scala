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
package noisecluster

import java.time.LocalDateTime
import javax.sound.sampled.AudioFormat

package object control {
  sealed trait ServiceLevel
  object ServiceLevel {
    case object Audio extends ServiceLevel
    case object Transport extends ServiceLevel
    case object Application extends ServiceLevel
    case object Host extends ServiceLevel
  }

  sealed trait ServiceState
  object ServiceState {
    case object Starting extends ServiceState
    case object Active extends ServiceState
    case object Stopping extends ServiceState
    case object Stopped extends ServiceState
    case object Restarting extends ServiceState
  }

  case class NodeState(
    audio: ServiceState,
    transport: ServiceState,
    application: ServiceState,
    host: ServiceState
  )

  case class NodeInfo(
    state: NodeState,
    lastUpdate: LocalDateTime
  )

  case class AudioFormatContainer(
    encoding: String,
    sampleRate: Float,
    sampleSizeInBits: Int,
    channels: Int,
    frameSize: Int,
    frameRate: Float,
    bigEndian: Boolean
  ) {
    def toAudioFormat: AudioFormat =
      new AudioFormat(
        new AudioFormat.Encoding(encoding),
        sampleRate,
        sampleSizeInBits,
        channels,
        frameSize,
        frameRate,
        bigEndian
      )
  }
}
