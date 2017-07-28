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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import noisecluster.jvm.audio.AudioFormatContainer
import noisecluster.jvm.audio.render.ByteStreamPlayer
import noisecluster.jvm.control.LocalHandlers
import noisecluster.jvm.transport.aeron.{Contexts, Defaults, Target}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import sys.process._

class Service(config: Config) {
  private val stream: Int = config.getInt("transport.stream")
  private val address: String = config.getString("transport.address")
  private val port: Int = config.getInt("transport.port")
  private val interfaceOpt: Option[String] = if(config.hasPath("transport.interface")) Some(config.getString("transport.interface")) else None
  private val applicationStopTimeout: Int = config.getInt("app.stopTimeout") //in seconds
  private val hostStopTimeout: Int = config.getInt("host.stopTimeout") //in seconds

  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private implicit val system = ActorSystem("ve")

  private val driver = MediaDriver.launch(Contexts.Driver.lowLatency)
  private implicit val aeron = Aeron.connect(Contexts.System.default)

  private var audioOpt: Option[ByteStreamPlayer] = None
  private var transportOpt: Option[Target] = None

  val localHandlers = new LocalHandlers {
    override def startAudio(formatContainer: Option[AudioFormatContainer]): Future[Boolean] = {
      Future {
        audioOpt match {
          case Some(audio) =>
            audio.start()

          case None =>
            formatContainer match {
              case Some(format) =>
                audioOpt = Some(ByteStreamPlayer(format.toAudioFormat))

              case None =>
                throw new IllegalArgumentException("Cannot start audio; no format specified")
            }
        }

        true
      }
    }

    override def stopAudio(restart: Boolean): Future[Boolean] = {
      Future {
        audioOpt match {
          case Some(audio) =>
            audio.stop()

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
                transport.start()

              case None =>
                val target = interfaceOpt match {
                  case Some(interface) =>
                    Target(stream, address, port, interface, audio.write, Defaults.IdleStrategy, Defaults.FragmentLimit)

                  case None =>
                    Target(stream, address, port, audio.write)
                }

                target.start()
                transportOpt = Some(target)
            }

          case None =>
            throw new IllegalStateException("Cannot start transport; no audio available")
        }

        true
      }
    }

    override def stopTransport(restart: Boolean): Future[Boolean] = {
      Future {
        transportOpt match {
          case Some(transport) =>
            transport.stop()

          case None =>
            throw new IllegalStateException("Cannot stop transport; no transport available")
        }

        true
      }
    }

    override def stopApplication(restart: Boolean): Future[Boolean] = {
      if(applicationStopTimeout > 0) {
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
      if(hostStopTimeout > 0) {
        s"/usr/bin/sudo /sbin/shutdown ${if (restart) "-r" else "-h"} `/bin/date --date 'now + $hostStopTimeout seconds' '+%H:%M'`".! match {
          case 0 => stopApplication(restart = false)
          case x => Future.failed(new RuntimeException(s"Unexpected exit code returned by shutdown command: [$x]"))
        }
      } else {
        Future.failed(new RuntimeException(s"Host stop/restart is disabled by config"))
      }
    }
  }
}
