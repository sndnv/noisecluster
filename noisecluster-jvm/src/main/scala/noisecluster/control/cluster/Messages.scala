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
package noisecluster.control.cluster

import noisecluster.audio.AudioFormatContainer

object Messages {

  sealed trait ControlMessage

  case class StartAudio(formatContainer: Option[AudioFormatContainer]) extends ControlMessage

  case class StopAudio(restart: Boolean) extends ControlMessage

  case class StartTransport() extends ControlMessage

  case class StopTransport(restart: Boolean) extends ControlMessage

  case class StopApplication(restart: Boolean) extends ControlMessage

  case class StopHost(restart: Boolean) extends ControlMessage

  sealed trait SystemMessage

  case class RegisterSource() extends SystemMessage

  case class RegisterTarget() extends SystemMessage

  case class Ping() extends SystemMessage

  case class Pong(state: NodeState) extends SystemMessage

}
