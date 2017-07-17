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

object Formats {
  //TODO - or default @ 48k, 24 bit, 2 chan, signed, LE
  val Default: AudioFormat = new AudioFormat(48000, 16, 2, true, false) //TODO - default ?
  val DefaultIeee: AudioFormat = new AudioFormat(48000, 32, 2, true, false) //TODO - works?
}
