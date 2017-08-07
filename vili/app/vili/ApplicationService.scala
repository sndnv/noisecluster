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
package vili

import akka.actor.ActorSystem
import com.typesafe.config.Config
import noisecluster.jvm.control.LocalHandlers
import noisecluster.win.interop.SourceService

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ApplicationService(config: Config, interop: SourceService)(implicit ec: ExecutionContext, system: ActorSystem) {
  private val applicationStopTimeout: Int = config.getInt("app.stopTimeout") //in seconds

  val localHandlers = new LocalHandlers {
    override def startAudio(): Future[Boolean] = {
      Future {
        if (interop.StartAudio()) {
          true
        } else {
          throw new IllegalStateException("Failed to start audio capture")
        }
      }
    }

    override def stopAudio(): Future[Boolean] = {
      Future {
        if (interop.StopAudio()) {
          true
        } else {
          throw new IllegalStateException("Failed to stop audio capture")
        }
      }
    }

    override def startTransport(): Future[Boolean] = {
      Future {
        if (interop.StartTransport()) {
          true
        } else {
          throw new IllegalStateException("Failed to start transport")
        }
      }
    }

    override def stopTransport(): Future[Boolean] = {
      Future {
        if (interop.StopTransport()) {
          true
        } else {
          throw new IllegalStateException("Failed to stop transport")
        }
      }
    }

    override def stopApplication(restart: Boolean): Future[Boolean] = {
      if (applicationStopTimeout > 0) {
        if (restart) {
          Future.failed(new NotImplementedError("Application restart in not available"))
          //TODO - implement as a service restart call?
        } else {
          system.scheduler.scheduleOnce(applicationStopTimeout.seconds) {
            System.exit(0)
          }

          Future.successful(true)
        }
      } else {
        Future.failed(new RuntimeException(s"Application stop/restart is disabled by config"))
      }
    }

    override def stopHost(restart: Boolean): Future[Boolean] = {
      Future.failed(new RuntimeException(s"Cannot stop source host"))
    }

    override def setHostVolume(level: Int): Future[Boolean] = {
      Future {
        if (interop.SetHostVolume(level)) {
          true
        } else {
          throw new IllegalStateException("Failed to set host volume")
        }
      }
    }

    override def muteHost(): Future[Boolean] = {
      Future {
        if (interop.MuteHost()) {
          true
        } else {
          throw new IllegalStateException("Failed to mute host")
        }
      }
    }

    override def unmuteHost(): Future[Boolean] = {
      Future {
        if (interop.UnmuteHost()) {
          true
        } else {
          throw new IllegalStateException("Failed to unmute host")
        }
      }
    }
  }

  def shutdown(): Unit = {
    interop.StopAudio()
    interop.StopTransport()
  }
}
