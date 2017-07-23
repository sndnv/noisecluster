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
package noisecluster.audio

import javax.sound.sampled.AudioFormat

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
