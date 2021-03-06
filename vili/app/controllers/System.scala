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

import akka.actor.Address
import core3.http.controllers.noauth.ClientController
import noisecluster.jvm.control.ServiceState
import noisecluster.jvm.control.cluster.Messages._
import noisecluster.jvm.control.cluster._
import play.api.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.filters.csrf.CSRF

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class System @Inject()(control: SourceService, appService: vili.ApplicationService)(implicit ec: ExecutionContext, environment: Environment)
  extends ClientController() {
  private implicit val serviceStateWrites = Writes[ServiceState] { state => JsString(state.toString) }
  private implicit val nodeStateWrites = Json.writes[NodeState]
  private implicit val nodeInfoWrites = Json.writes[NodeInfo]
  private implicit val akkaAddressWrites = Writes[Address] { address => JsString(address.toString) }
  private implicit val memberInfoWrites = Json.writes[MemberInfo]
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
      control.getClusterState.map {
        state =>
          Ok(
            Json.obj(
              "sources" -> control.activeSources,
              "targets" -> control.activeTargets,
              "state" -> state
            )
          )
      }
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
          try {
            val messages: Seq[ControlMessage] = (params.service.toLowerCase, params.action.toLowerCase) match {
              case ("audio", "play") => if(params.target.getOrElse("") == "self") {
                Seq(StartAudio(), UnmuteHost())
              } else {
                Seq(StartTransport(), UnmuteHost())
              }

              case ("audio", "quiet") => if(params.target.getOrElse("") == "self") {
                Seq(MuteHost(), StopAudio())
              } else {
                Seq(MuteHost(), StopTransport())
              }

              case ("host", "volume") => params.level match {
                case Some(level) => Seq(SetHostVolume(level))
                case None => throw new IllegalArgumentException(
                  s"No [level] parameter supplied to service [host] and action [volume]"
                )
              }

              case ("host", "mute") => Seq(MuteHost())
              case ("host", "unmute") => Seq(UnmuteHost())

              case ("audio", "start") => Seq(StartAudio())
              case ("audio", "stop") => Seq(StopAudio())
              case ("transport", "start") => Seq(StartTransport())
              case ("transport", "stop") => Seq(StopTransport())
              case ("application", "stop") => Seq(StopApplication(restart = false))
              case ("application", "restart") => Seq(StopApplication(restart = true))
              case ("host", "stop") => Seq(StopHost(restart = false))
              case ("host", "restart") => Seq(StopHost(restart = true))

              case _ =>
                throw new IllegalArgumentException(
                  s"Unexpected service [${params.service}] and/or action [${params.action}] requested"
                )
            }

            params.target match {
              case Some("self") => messages.foreach { message => control.processMessage(message) }
              case Some(target) => messages.foreach { message => control.forwardMessage(target, message) }
              case None => messages.foreach { message => control.forwardMessage(message) }
            }

            Future.successful(NoContent)
          } catch {
            case NonFatal(e) =>
              e.printStackTrace()
              Future.successful(InternalServerError(s"Exception encountered: [$e]"))
          }
        }
      )
    }
  )

  def processClusterAction = PublicAction(
    { (request, _) =>
      implicit val r = request
      System.Forms.action.bindFromRequest.fold(
        form => {
          throw new IllegalArgumentException(s"Failed to validate input: [${form.errors}]")
        }
        ,
        params => {
          (params.action.toLowerCase match {
            case "down" => control.setTargetToDown(params.target)
            case "leave" => control.setTargetToLeaving(params.target)
            case _ =>
              throw new IllegalArgumentException(s"Unexpected action [${params.action}] requested")
          }).map {
            result =>
              if (result) {
                NoContent
              } else {
                InternalServerError("Unable to complete operation")
              }
          }.recover {
            case NonFatal(e) => InternalServerError(s"Exception encountered: [$e]")
          }
        }
      )
    }
  )
}

object System {

  case class MessageRequest(target: Option[String], service: String, action: String, level: Option[Int])

  case class ClusterActionRequest(target: String, action: String)

  object Forms {
    val message = Form(
      mapping(
        "target" -> optional(nonEmptyText),
        "service" -> nonEmptyText,
        "action" -> nonEmptyText,
        "level" -> optional(number)
      )(MessageRequest.apply)(MessageRequest.unapply)
    )

    val action = Form(
      mapping(
        "target" -> nonEmptyText,
        "action" -> nonEmptyText
      )(ClusterActionRequest.apply)(ClusterActionRequest.unapply)
    )
  }

}
