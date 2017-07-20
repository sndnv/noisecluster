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

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put

abstract class Messenger extends Actor with ActorLogging {
  //Cluster Setup
  protected val clusterRef = Cluster(context.system)
  override def preStart(): Unit = clusterRef.subscribe(self, classOf[MemberEvent])
  override def postStop(): Unit = clusterRef.unsubscribe(self)

  //Mediator Setup
  protected val mediatorRef: ActorRef = DistributedPubSub(context.system).mediator
  mediatorRef ! Put(self)
}
