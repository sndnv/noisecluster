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
package ve

import javax.sound.sampled.AudioFormat

import akka.actor.ActorSystem
import com.typesafe.config.Config
import noisecluster.jvm.audio.render.ByteStreamPlayer
import noisecluster.jvm.control.LocalHandlers
import noisecluster.jvm.transport.Target
import ve.providers.TransportProvider

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._
import scala.util.control.NonFatal

class ApplicationService(config: Config)(implicit ec: ExecutionContext, system: ActorSystem) {
  private val applicationStopTimeout: Int = config.getInt("app.stopTimeout") //in seconds
  private val hostStopTimeout: Int = config.getInt("host.stopTimeout") //in seconds
  private val audioFormat: AudioFormat = new AudioFormat(
    config.getInt("audio.format.sampleRate").toFloat,
    config.getInt("audio.format.sampleSizeInBits"),
    config.getInt("audio.format.channels"),
    config.getBoolean("audio.format.signed"),
    config.getBoolean("audio.format.bigEndian")
  )

  private val transportProvider: TransportProvider = config.getString("transport.provider") match {
    case "aeron" => new ve.providers.transport.Aeron(config.getConfig("transport.aeron"))
    case "udp" => new ve.providers.transport.Udp(config.getConfig("transport.udp"))
  }

  private var audioOpt: Option[ByteStreamPlayer] = None
  private var transportOpt: Option[Target] = None

  val localHandlers = new LocalHandlers {
    override def startAudio(): Future[Boolean] = {
      Future {
        audioOpt match {
          case Some(_) =>
            throw new IllegalStateException("Audio is already available")

          case None =>
            val audio = ByteStreamPlayer(audioFormat)
            audioOpt = Some(audio)
            audio.start()
        }

        true
      }
    }

    override def stopAudio(): Future[Boolean] = {
      Future {
        audioOpt match {
          case Some(audio) =>
            audio.stop()
            audio.close()
            audioOpt = None

          case None =>
            throw new IllegalStateException("Cannot stop audio; no audio available")
        }

        true
      }
    }

    override def startTransport(): Future[Boolean] = {
      Future {
        audioOpt match {
          case Some(audio) =>
            transportOpt match {
              case Some(transport) =>
                Future {
                  transport.start(audio.write)
                }

              case None =>
                val target: Target = config.getString("transport.provider").toLowerCase match {
                  case "aeron" => transportProvider.createTarget()
                  case "udp" => transportProvider.createTarget()
                }

                transportOpt = Some(target)
                Future {
                  target.start(audio.write)
                }
            }

          case None =>
            throw new IllegalStateException("Cannot start transport; no audio available")
        }

        true
      }
    }

    override def stopTransport(): Future[Boolean] = {
      Future {
        transportOpt match {
          case Some(transport) =>
            transport.stop()
            transport.close()
            transportOpt = None

          case None =>
            throw new IllegalStateException("Cannot stop transport; no transport available")
        }

        true
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
      if (hostStopTimeout > 0) {
        s"/usr/bin/sudo /sbin/shutdown ${if (restart) "-r" else "-h"} `/bin/date --date 'now + $hostStopTimeout seconds' '+%H:%M'`".! match {
          case 0 => stopApplication(restart = false)
          case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by shutdown command: [$x]"))
        }
      } else {
        Future.failed(new RuntimeException(s"Host stop/restart is disabled by config"))
      }
    }
  }

  def shutdown(): Unit = {
    try {
      audioOpt match {
        case Some(audio) =>
          if (audio.isActive) audio.stop()
          audio.close()
          audioOpt = None
        case None => //do nothing
      }
    } catch {
      case NonFatal(e) => e.printStackTrace()
    }

    try {
      transportOpt match {
        case Some(transport) =>
          if (transport.isActive) transport.stop()
          transport.close()
          transportOpt = None
        case None => //do nothing
      }
    } catch {
      case NonFatal(e) => e.printStackTrace()
    }

    transportProvider.shutdown()
  }
}
