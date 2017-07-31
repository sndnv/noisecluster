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
package controllers

import javax.inject.Inject

import core3.http.controllers.noauth.ClientController
import noisecluster.jvm.control.ServiceState
import noisecluster.jvm.control.cluster.Messages._
import noisecluster.jvm.control.cluster._
import play.api.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.filters.csrf.CSRF
import vili.ApplicationService

import scala.concurrent.{ExecutionContext, Future}

class   System @Inject()(/*TODO - enable control: SourceService, appService: vili.ApplicationService*/)(implicit ec: ExecutionContext, environment: Environment)
  extends ClientController() {
  private implicit val serviceStateWrites = Writes[ServiceState] { state => JsString(state.toString) }
  private implicit val nodeStateWrites = Json.writes[NodeState]
  private implicit val nodeInfoWrites = Json.writes[NodeInfo]
  private implicit val clusterStateWrites = Json.writes[ClusterState]

  def root() = PublicAction(
    { (request, _) =>
      implicit val r = request
      Future.successful(Redirect("/home"))
    }
  )

  def home() = PublicAction(
    { (request, _) =>
      implicit val r = request
      implicit val token = CSRF.getToken
      Future.successful(Ok(views.html.home("Home")))
    }
  )

  def nodes() = PublicAction(
    { (request, _) =>
      implicit val r = request
      implicit val token = CSRF.getToken
      Future.successful(Ok(views.html.nodes("Nodes")))
    }
  )

  def cluster() = PublicAction(
    { (request, _) =>
      implicit val r = request
      implicit val token = CSRF.getToken
      Future.successful(Ok(views.html.cluster("Cluster")))
    }
  )

  def status() = PublicAction(
    { (request, _) =>
      implicit val r = request
      //TODO - enable
      /*control.getClusterState.map {
        state =>
          Ok(
            Json.obj(
              "sources" -> control.activeSources,
              "targets" -> control.activeTargets,
              "state" -> state
            )
          )
      }*/
      Future.successful(
        Ok(
          Json.obj(
            "sources" -> 1,
            "targets" -> 3,
            "state" -> Json.obj(
              "localSource" -> Json.obj(
                "audio" -> "Restarting",
                "transport" -> "Stopping",
                "application" -> "Active",
                "host" -> "Restarting"
              ),
              "targets" -> Json.obj(
                "node_01" -> Json.obj(
                  "audio" -> "Active",
                  "transport" -> "Active",
                  "application" -> "Active",
                  "host" -> "Active"
                ),
                "node_02" -> Json.obj(
                  "audio" -> "Stopped",
                  "transport" -> "Stopped",
                  "application" -> "Active",
                  "host" -> "Active"
                ),
                "node_03" -> Json.obj()
              ),
              "pings" -> 23,
              "pongs" -> 13
            )
          )
        )
      )
    }
  )

  def processMessage = PublicAction(
    { (request, _) =>
      implicit val r = request

      System.Forms.message.bindFromRequest.fold(
        form => {
          throw new IllegalArgumentException(s"Failed to validate input: [${form.errors}]")
        }
        ,
        params => {
          /* TODO - enable
          val message: ControlMessage = (params.service, params.action) match {
            case ("audio", "start") => StartAudio(appService.audioFormat)
            case ("audio", "stop") => StopAudio(restart = false)
            case ("audio", "restart") => StopAudio(restart = true)

            case ("transport", "start") => StartTransport()
            case ("transport", "stop") => StopTransport(restart = false)
            case ("transport", "restart") => StopTransport(restart = true)

            case ("application", "stop") => StopApplication(restart = false)
            case ("application", "restart") => StopApplication(restart = true)

            case ("host", "stop") => StopHost(restart = false)
            case ("host", "restart") => StopHost(restart = true)

            case _ =>
              throw new IllegalArgumentException(
                s"Unexpected service [${params.service}] and/or action [${params.action}] requested"
              )
          }

          params.target match {
            case Some("self") => control.processMessage(message)
            case Some(target) => control.forwardMessage(target, message)
            case None => control.forwardMessage(message)
          }*/

          Future.successful(Ok("")) //TODO
        }
      )
    }
  )
}

object System {
  case class MessageRequest(target: Option[String], service: String, action: String)

  object Forms {
    val message = Form(
      mapping(
        "target" -> optional(nonEmptyText),
        "service" -> nonEmptyText,
        "action" -> nonEmptyText
      )(MessageRequest.apply)(MessageRequest.unapply)
    )
  }
}