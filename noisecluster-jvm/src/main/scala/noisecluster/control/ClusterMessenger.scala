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
package noisecluster.control

import akka.actor.{Actor, Address, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class ClusterMessenger(
  private val handlers: LocalHandlers
) extends Actor {
  private val receivedMessages = mutable.HashMap.empty[Address, Long]

  //cluster setup
  private val cluster = Cluster(context.system)
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)

  //mediator setup
  private val mediator = DistributedPubSub(context.system).mediator
  mediator ! Put(self)
  //TODO - log subscription

  override def receive: Receive = {
    case Messages.StartTransport() =>
      //TODO

    case Messages.StopTransport() =>
      //TODO

    case Messages.Restart(level) =>
      //TODO

    //Cluster Monitoring
    case state: CurrentClusterState =>
      state.members.foreach {
        member =>
          if (member.status == MemberStatus.Up)
            receivedMessages += member.address -> 0L
      }

    case MemberUp(member) =>
      receivedMessages += member.address -> 0L

    case MemberRemoved(member, _) =>
      receivedMessages -= member.address

    //Messenger Life-Cycle
    //TODO
  }
}

object ClusterMessenger {
  def props(handlers: LocalHandlers): Props = Props(classOf[ClusterMessenger], handlers)
}