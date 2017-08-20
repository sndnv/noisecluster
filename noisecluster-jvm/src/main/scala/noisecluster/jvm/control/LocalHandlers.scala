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
package noisecluster.jvm.control

import scala.concurrent.Future

/**
  * Base trait for implementing local system handlers for targets and sources.
  *
  * Handlers are executed based on explicit user action or if the target/source is configured to
  * automatically react to events (such as all sources going offline, a new target starting, etc).
  */
trait LocalHandlers {
  /**
    * Instructs the local system to start audio capture or rendering.
    *
    * @return always true, if the future is successful
    */
  def startAudio(): Future[Boolean]

  /**
    * Instructs the local system to stop audio capture or rendering.
    *
    * @return always true, if the future is successful
    */
  def stopAudio(): Future[Boolean]

  /**
    * Instructs the local system to start receiving or sending data.
    *
    * @return always true, if the future is successful
    */
  def startTransport(): Future[Boolean]

  /**
    * Instructs the local system to stop receiving or sending data.
    *
    * @return always true, if the future is successful
    */
  def stopTransport(): Future[Boolean]

  /**
    * Instructs the local system to stop or restart the application.
    *
    * @param restart set to true to restart the application, instead of stopping it
    * @return always true, if the future is successful
    */
  def stopApplication(restart: Boolean): Future[Boolean]

  /**
    * Instructs the local system to stop or restart the host.
    *
    * @param restart set to true to restart the host, instead of stopping it
    * @return always true, if the future is successful
    */
  def stopHost(restart: Boolean): Future[Boolean]

  /**
    * Instructs the local system to set the host's master volume to the specified level.
    *
    * @param level the volume level (in %)
    * @return always true, if the future is successful
    */
  def setHostVolume(level: Int): Future[Boolean]

  /**
    * Instructs the local system to mute the host.
    *
    * @return always true, if the future is successful
    */
  def muteHost(): Future[Boolean]

  /**
    * Instructs the local system to unmute the host.
    *
    * @return always true, if the future is successful
    */
  def unmuteHost(): Future[Boolean]

  /**
    * Retrieves the host's master volume level.
    *
    * @return the volume level (in %)
    */
  def getHostVolume: Future[Int]

  /**
    * Retrieves the host's muted state.
    *
    * @return true, if the host is muted
    */
  def isHostMuted: Future[Boolean]
}
