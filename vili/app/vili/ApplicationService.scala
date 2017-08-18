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
import scala.util.control.NonFatal

class ApplicationService(config: Config, interop: SourceService)(implicit ec: ExecutionContext, system: ActorSystem) {
  private val applicationStopTimeout: Int = config.getInt("app.stopTimeout") //in seconds
  private val sampleRate: Int = config.getInt("audio.format.sampleRate")
  private val sampleSizeInBits: Int = config.getInt("audio.format.sampleSizeInBits")

  val localHandlers = new LocalHandlers {
    override def startAudio(): Future[Boolean] = {
      try {
        if (interop.StartAudio(sampleRate, sampleSizeInBits)) {
          Future.successful(true)
        }
        else {
          Future.failed(new IllegalStateException("Failed to start audio capture"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def stopAudio(): Future[Boolean] = {
      try {
        if (interop.StopAudio()) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to stop audio capture"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def startTransport(): Future[Boolean] = {
      try {
        if (interop.StartTransport()) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to start transport"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def stopTransport(): Future[Boolean] = {
      try {
        if (interop.StopTransport()) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to stop transport"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
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
      try {
        if (interop.SetHostVolume(level)) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to set host volume"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def muteHost(): Future[Boolean] = {
      try {
        if (interop.MuteHost()) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to mute host"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def unmuteHost(): Future[Boolean] = {
      try {
        if (interop.UnmuteHost()) {
          Future.successful(true)
        } else {
          Future.failed(new IllegalStateException("Failed to unmute host"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def getHostVolume: Future[Int] = {
      try {
        Option(interop.GetHostVolume()) match {
          case Some(volume) => Future.successful(volume)
          case None => Future.failed(new RuntimeException("Failed to retrieve host volume"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }

    override def isHostMuted: Future[Boolean] = {
      try {
        Option(interop.IsHostMuted()) match {
          case Some(state) => Future.successful(state)
          case None => Future.failed(new RuntimeException("Failed to retrieve mute state"))
        }
      } catch {
        case NonFatal(e) =>
          Future.failed(e)
      }
    }
  }

  def shutdown(): Unit = {
    interop.StopAudio()
    interop.StopTransport()
  }
}
