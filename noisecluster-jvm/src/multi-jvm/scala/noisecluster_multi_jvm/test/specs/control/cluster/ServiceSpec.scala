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
package noisecluster_multi_jvm.test.specs.control.cluster

import akka.remote.testconductor.RoleName
import akka.remote.testkit._
import akka.remote.transport.ThrottlerTransportAdapter
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import noisecluster_multi_jvm.test.specs.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration._

object ServiceTestConfig extends MultiNodeConfig {

  import collection.JavaConverters._

  val baseConfig: Config = ConfigFactory.load()
    .withValue("akka.actor.provider", ConfigValueFactory.fromAnyRef("akka.cluster.ClusterActorRefProvider"))
    .withValue("akka.remote.artery.advanced.test-mode", ConfigValueFactory.fromAnyRef("on"))
    .withValue("akka.remote.netty.tcp.applied-adapters", ConfigValueFactory.fromIterable(Seq("trttl", "gremlin").asJava))

  commonConfig(baseConfig)

  val node1: RoleName = role(s"source")
  val node2: RoleName = role(s"target")
  val node3: RoleName = role(s"target")

  nodeConfig(node1)(baseConfig)
  nodeConfig(node2)(baseConfig)
  nodeConfig(node3)(baseConfig)

  def getLocalNodeClusterPort(node: RoleName): Int = {
    node match {
      case x if x == node1 => 20001
      case x if x == node2 => 20002
      case x if x == node3 => 20003
    }
  }

  testTransport(on = true)
}

class ServiceSpec extends MultiNodeSpec(ServiceTestConfig) with UnitSpec {
  override def initialParticipants: Int = roles.size

  import ServiceTestConfig._

  //TODO
}
