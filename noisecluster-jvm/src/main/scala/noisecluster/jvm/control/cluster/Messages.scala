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
package noisecluster.jvm.control.cluster

object Messages {

  /**
    * Base trait for all cluster control messages.
    *
    * Messages are sent/received based on explicit user action or if the target/source is configured to
    * automatically react to events (such as all sources going offline, a new target starting, etc).
    */
  sealed trait ControlMessage

  case class StartAudio() extends ControlMessage

  case class StopAudio() extends ControlMessage

  case class StartTransport() extends ControlMessage

  case class StopTransport() extends ControlMessage

  case class StopApplication(restart: Boolean) extends ControlMessage

  case class StopHost(restart: Boolean) extends ControlMessage

  case class SetHostVolume(level: Int) extends ControlMessage

  case class MuteHost() extends ControlMessage

  case class UnmuteHost() extends ControlMessage

  /**
    * Base trait for all system messages.
    *
    * Messages are sent/received based on internal system events
    * (such as a new target connecting, a source requesting a status update, etc).
    */
  sealed trait SystemMessage

  case class RegisterSource() extends SystemMessage

  case class RegisterTarget() extends SystemMessage

  case class Ping() extends SystemMessage

  case class Pong(state: NodeState) extends SystemMessage

}
