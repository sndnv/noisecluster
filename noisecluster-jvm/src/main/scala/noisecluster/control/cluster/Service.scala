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
package noisecluster.control.cluster

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.{Cluster, MemberStatus}
import com.typesafe.config.Config

abstract class Service(private val systemName: String, private val overrideConfig: Option[Config]) {
  protected val system: ActorSystem = overrideConfig match {
    case Some(config) => ActorSystem(systemName, config)
    case None => ActorSystem(systemName)
  }

  protected val cluster: Cluster = Cluster(system)
  protected val messenger: ActorRef

  def terminate(): Unit = {
    cluster.leave(cluster.selfAddress)
    system.terminate()
  }

  def activeSources: Int = cluster.state.members.count {
    member =>
      member.status == MemberStatus.Up && member.roles.contains("source")
  }

  def activeTargets: Int = cluster.state.members.count {
    member =>
      member.status == MemberStatus.Up && member.roles.contains("target")
  }
}
