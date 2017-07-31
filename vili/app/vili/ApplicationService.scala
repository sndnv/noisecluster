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
import noisecluster.jvm.audio.AudioFormatContainer
import noisecluster.jvm.control.LocalHandlers

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ApplicationService(config: Config)(implicit ec: ExecutionContext, system: ActorSystem) {
  private val stream: Int = config.getInt("transport.stream")
  private val address: String = config.getString("transport.address")
  private val port: Int = config.getInt("transport.port")
  private val interfaceOpt: Option[String] = if (config.hasPath("transport.interface")) Some(config.getString("transport.interface")) else None
  private val applicationStopTimeout: Int = config.getInt("app.stopTimeout") //in seconds

  val localHandlers = new LocalHandlers {
    override def startAudio(formatContainer: Option[AudioFormatContainer]): Future[Boolean] = {
      Future {
        //TODO - add JNI call

        true
      }
    }

    override def stopAudio(restart: Boolean): Future[Boolean] = {
      Future {
        //TODO - add JNI call

        true
      }
    }

    override def startTransport(): Future[Boolean] = {
      Future {
        //TODO - add JNI call

        true
      }
    }

    override def stopTransport(restart: Boolean): Future[Boolean] = {
      Future {
        //TODO - add JNI call

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
      Future.failed(new RuntimeException(s"Cannot stop source host"))
    }
  }

  def audioFormat: Option[AudioFormatContainer] = {
    //TODO - implement
    None
  }

  def shutdown(): Unit = {
    //TODO - implement
  }
}
