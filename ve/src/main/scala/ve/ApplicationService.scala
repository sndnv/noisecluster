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

/**
  * Target service for managing application resources.
  *
  * @param config application configuration
  */
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

  private val serviceNameOpt: Option[String] = if (config.hasPath("app.serviceName")) Some(config.getString("app.serviceName")) else None
  private val startVolumeOpt: Option[Int] = if (config.hasPath("audio.start.volume")) Some(config.getInt("audio.start.volume")) else None
  private val startMutedOpt: Option[Boolean] = if (config.hasPath("audio.start.muted")) Some(config.getBoolean("audio.start.muted")) else None

  private val transportProvider: TransportProvider = config.getString("transport.provider") match {
    case "aeron" => new ve.providers.transport.Aeron(config.getConfig("transport.aeron"))
    case "udp" => new ve.providers.transport.Udp(config.getConfig("transport.udp"))
  }

  private var audioOpt: Option[ByteStreamPlayer] = None
  private var transportOpt: Option[Target] = None

  private def audioWriteHandler(data: Array[Byte], length: Int): Unit = {
    audioOpt match {
      case Some(audio) => audio.write(data, length)
      case None => //do nothing
    }
  }

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
        transportOpt match {
          case Some(transport) =>
            Future {
              transport.start(audioWriteHandler)
            }

          case None =>
            val target: Target = config.getString("transport.provider").toLowerCase match {
              case "aeron" => transportProvider.createTarget()
              case "udp" => transportProvider.createTarget()
            }

            transportOpt = Some(target)
            Future {
              target.start(audioWriteHandler)
            }
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
          serviceNameOpt match {
            case Some(serviceName) =>
              val command = s"/usr/bin/sudo /usr/sbin/service $serviceName restart"
              command.! match {
                case 0 => Future.successful(true)
                case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by command [$command]: [$x]"))
              }
            case None => Future.failed(new RuntimeException("No service name specified for application restart."))
          }
        } else {
          system.scheduler.scheduleOnce(applicationStopTimeout.seconds) {
            System.exit(0)
          }

          Future.successful(true)
        }
      } else {
        Future.failed(new RuntimeException("Application stop/restart is disabled by config"))
      }
    }

    override def stopHost(restart: Boolean): Future[Boolean] = {
      if (hostStopTimeout > 0) {
        try {
          val command = s"/usr/bin/sudo /sbin/shutdown ${if (restart) "-r" else "-h"} `/bin/date --date 'now + $hostStopTimeout seconds' '+%H:%M'`"
          command.! match {
            case 0 => stopApplication(restart = false)
            case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by command [$command]: [$x]"))
          }
        } catch {
          case NonFatal(e) => Future.failed(e)
        }
      } else {
        Future.failed(new RuntimeException("Host stop/restart is disabled by config"))
      }
    }

    override def setHostVolume(level: Int): Future[Boolean] = {
      try {
        val command = s"/usr/bin/pactl set-sink-volume @DEFAULT_SINK@ $level%"
        command.! match {
          case 0 => Future.successful(true)
          case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by command [$command]: [$x]"))
        }
      } catch {
        case NonFatal(e) => Future.failed(e)
      }
    }

    override def muteHost(): Future[Boolean] = {
      try {
        val command = "/usr/bin/pactl set-sink-mute @DEFAULT_SINK@ 1"
        command.! match {
          case 0 => Future.successful(true)
          case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by command [$command]: [$x]"))
        }
      } catch {
        case NonFatal(e) => Future.failed(e)
      }
    }

    override def unmuteHost(): Future[Boolean] = {
      try {
        val command = "/usr/bin/pactl set-sink-mute @DEFAULT_SINK@ 0"
        command.! match {
          case 0 => Future.successful(true)
          case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by command [$command]: [$x]"))
        }
      } catch {
        case NonFatal(e) => Future.failed(e)
      }
    }

    /**
      * Retrieves data for the host's default audio sink.
      *
      * @return the processed data, if available
      */
    private def getDefaultSinkData: Option[Seq[String]] = {
      val pactlSinks = "/usr/bin/pactl list sinks".!!.split("Sink #")

      "/usr/bin/pactl info".lineStream
        .find(_.contains("Default Sink"))
        .map(_.split(":").last.trim)
        .flatMap { defaultSinkName => pactlSinks.find(_.contains(defaultSinkName)).map(_.split("\n")) }
    }

    override def getHostVolume: Future[Int] = {
      try {
        val volume = getDefaultSinkData
          .flatMap {
            sinkData =>
              sinkData.find(c => c.contains("Volume") && !c.contains("Base Volume"))
                .flatMap("""(\d*)%""".r.findFirstMatchIn)
                .map(_.group(1))
          }
          .map(_.toInt)

        volume match {
          case Some(vol) => Future.successful(vol)
          case None => Future.failed(new RuntimeException("Failed to retrieve host volume"))
        }
      } catch {
        case NonFatal(e) => Future.failed(e)
      }
    }

    override def isHostMuted: Future[Boolean] = {
      try {
        val muted = getDefaultSinkData
          .flatMap {
            sinkData =>
              sinkData
                .find(c => c.contains("Mute"))
                .flatMap("""(yes|no)""".r.findFirstMatchIn)
                .map(_.group(1))
          }
          .flatMap {
            state =>
              state.toLowerCase match {
                case "yes" => Some(true)
                case "no" => Some(false)
                case _ => None
              }
          }

        muted match {
          case Some(state) => Future.successful(state)
          case None => Future.failed(new RuntimeException("Failed to retrieve host mute state"))
        }
      } catch {
        case NonFatal(e) => Future.failed(e)
      }
    }
  }

  //sets the initial host master volume, if specified
  startVolumeOpt.map {
    startVolume =>
      localHandlers.setHostVolume(startVolume)
  }

  //sets the initial host muted state, if specified
  startMutedOpt.map {
    startMuted =>
      if (startMuted) localHandlers.muteHost()
      else localHandlers.unmuteHost()
  }

  /**
    * Stops the audio, transport and disposes of all resources.
    */
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
