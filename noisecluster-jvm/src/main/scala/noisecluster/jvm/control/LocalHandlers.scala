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

import noisecluster.jvm.audio.AudioFormatContainer

import scala.concurrent.Future

trait LocalHandlers {
  def startAudio(formatContainer: Option[AudioFormatContainer]): Future[Boolean]

  def stopAudio(restart: Boolean): Future[Boolean]

  def startTransport(): Future[Boolean]

  def stopTransport(restart: Boolean): Future[Boolean]

  def stopApplication(restart: Boolean): Future[Boolean]

  def stopHost(restart: Boolean): Future[Boolean]
}